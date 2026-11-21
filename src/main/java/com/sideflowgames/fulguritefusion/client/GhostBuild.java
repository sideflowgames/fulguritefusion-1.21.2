package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.block.ColliderCoreBlockEntity;
import com.sideflowgames.fulguritefusion.block.ConsolePortBlock;
import com.sideflowgames.fulguritefusion.block.FusionColliderBlockEntity;
import com.sideflowgames.fulguritefusion.block.ReactorConsoleBlock;
import com.sideflowgames.fulguritefusion.block.ReactorFacingBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Ghost build outlines. While a structure's core block gets a redstone signal (a lever on it, a redstone block
// next to it...), every block still missing from the build is drawn as a faint, glowing copy of the block that
// belongs there. Spots that must be left empty but have something in them get a red box.
//
// Covered: the Fusion Reactor (from the Fusion Collider: blast chamber, inner and outer rings, injectors,
// portals and ports), the Reactor Control Console (from the console block) and the 7x7 collider ring
// (from a Storm Collider or Mini Collider).
//
// Where the build allows a choice (which way the chamber opens, which corners get the portals and ports),
// the ghost follows what's already placed, and uses a default until then.
public final class GhostBuild {
    private GhostBuild() {}

    // want == null: the spot must be empty (drawn as a red box when it isn't).
    public record Ghost(BlockPos pos, BlockState want) {}

    private static final int REFRESH_TICKS = 10;   // re-check the build twice a second
    private static final double MAX_DISTANCE = 96; // don't draw ghosts further than this from the camera

    private record Cached(long time, List<Ghost> ghosts) {}
    private static final Map<BlockPos, Cached> CACHE = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    // The ghosts for a core, re-checked every half second.
    public static List<Ghost> get(Level level, BlockPos core, java.util.function.Function<Level, List<Ghost>> build) {
        long now = level.getGameTime();
        Cached c = CACHE.get(core);
        if (c == null || now - c.time >= REFRESH_TICKS || now < c.time) {
            if (CACHE.size() > 64) CACHE.clear();
            c = new Cached(now, build.apply(level));
            CACHE.put(core.immutable(), c);
        }
        return c.ghosts;
    }

    public static boolean powered(Level level, BlockPos core) {
        return level.hasNeighborSignal(core);
    }

    // ---------- Fusion Reactor ----------

    public static List<Ghost> fusionReactor(Level level, BlockPos core) {
        List<Ghost> out = new ArrayList<>();
        final int CH = FusionColliderBlockEntity.CHAMBER, RING = FusionColliderBlockEntity.RING,
                OUTER = FusionColliderBlockEntity.OUTER, INJ = FusionColliderBlockEntity.INJECTOR;
        Block frame = ModBlocks.BLAST_CHAMBER_FRAME.get(), glass = ModBlocks.FULGURITE_BLAST_GLASS.get();

        // Which way the chamber opens: as built, or toward injectors already placed, or east/west.
        boolean openX = air(level, core.offset(CH, 0, 0)) && air(level, core.offset(-CH, 0, 0));
        boolean openZ = air(level, core.offset(0, 0, CH)) && air(level, core.offset(0, 0, -CH));
        boolean axisX;
        if (openX != openZ) axisX = openX;
        else axisX = !(is(level, core.offset(0, 0, INJ), ModBlocks.FUSION_INJECTOR.get())
                || is(level, core.offset(0, 0, -INJ), ModBlocks.FUSION_INJECTOR.get()));

        // Blast chamber.
        for (int dx = -CH; dx <= CH; dx++) {
            for (int dy = -CH; dy <= CH; dy++) {
                for (int dz = -CH; dz <= CH; dz++) {
                    int edges = (Math.abs(dx) == CH ? 1 : 0) + (Math.abs(dy) == CH ? 1 : 0) + (Math.abs(dz) == CH ? 1 : 0);
                    if (edges == 0) continue;
                    BlockPos p = core.offset(dx, dy, dz);
                    if (edges >= 2) need(level, out, p, frame.defaultBlockState());
                    else {
                        boolean opening = dy == 0 && (axisX ? (dz == 0 && Math.abs(dx) == CH) : (dx == 0 && Math.abs(dz) == CH));
                        if (opening) clear(level, out, p);
                        else need(level, out, p, glass.defaultBlockState());
                    }
                }
            }
        }

        // Fusion Injectors on the inner ring's inside face, facing the chamber.
        int[][] injectors = axisX ? new int[][]{{INJ, 0}, {-INJ, 0}} : new int[][]{{0, INJ}, {0, -INJ}};
        for (int[] s : injectors) {
            Direction face = Direction.fromDelta(-Integer.signum(s[0]), 0, -Integer.signum(s[1]));
            BlockState want = ModBlocks.FUSION_INJECTOR.get().defaultBlockState().setValue(ReactorFacingBlock.FACING, face);
            BlockPos p = core.offset(s[0], 0, s[1]);
            BlockState have = level.getBlockState(p);
            if (!have.is(want.getBlock()) || !have.hasProperty(ReactorFacingBlock.FACING)
                    || have.getValue(ReactorFacingBlock.FACING) != face) {
                out.add(new Ghost(p.immutable(), want));
            }
        }

        // Receiver Portals: the pair already started, or the first pair.
        Block receiver = ModBlocks.RECEIVER_PORTAL.get();
        int[][] receivers = FusionColliderBlockEntity.RECEIVER_PAIRS[0];
        search:
        for (int[][] pair : FusionColliderBlockEntity.RECEIVER_PAIRS) {
            for (int[] s : pair) {
                if (is(level, core.offset(s[0], 0, s[1]), receiver)) {
                    receivers = pair;
                    break search;
                }
            }
        }
        for (int[] s : receivers) need(level, out, core.offset(s[0], 0, s[1]), receiver.defaultBlockState());
        ring(level, out, core, RING, new int[][]{receivers[0], receivers[1], injectors[0], injectors[1]});

        // Outer ring: Transfer Portals in line with the receivers, Ring Injection Ports at one corner.
        int[][] portals = {FusionColliderBlockEntity.transferFor(receivers[0]), FusionColliderBlockEntity.transferFor(receivers[1])};
        for (int[] s : portals) need(level, out, core.offset(s[0], 0, s[1]), ModBlocks.TRANSFER_PORTAL.get().defaultBlockState());
        Block port = ModBlocks.RING_INJECTION_PORT.get();
        int corner = 0;
        search:
        for (int c = 0; c < FusionColliderBlockEntity.CORNERS.length; c++) {
            for (int[] s : FusionColliderBlockEntity.injectionSpots(c)) {
                if (is(level, core.offset(s[0], 0, s[1]), port)) {
                    corner = c;
                    break search;
                }
            }
        }
        int[][] ports = FusionColliderBlockEntity.injectionSpots(corner);
        for (int[] s : ports) need(level, out, core.offset(s[0], 0, s[1]), port.defaultBlockState());
        ring(level, out, core, OUTER, new int[][]{portals[0], portals[1], ports[0], ports[1]});
        return out;
    }

    // One diamond ring tube, the same shape FusionColliderBlockEntity.checkRing checks.
    private static void ring(Level level, List<Ghost> out, BlockPos core, int radius, int[][] specials) {
        final int TUBE = FusionColliderBlockEntity.TUBE;
        BlockState glass = ModBlocks.FULGURITE_BLAST_GLASS.get().defaultBlockState();
        BlockState solid = ModBlocks.REINFORCED_FULGURITE.get().defaultBlockState();
        int reach = radius + TUBE;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                int l = Math.max(Math.abs(dx), Math.abs(dz)) - radius;
                if (Math.abs(l) > TUBE) continue;
                for (int dy = -TUBE; dy <= TUBE; dy++) {
                    if (dy == 0 && special(dx, dz, specials)) continue;
                    int al = Math.abs(l), ay = Math.abs(dy);
                    BlockPos p = core.offset(dx, dy, dz);
                    if ((al == TUBE && dy == 0) || (l == 0 && ay == TUBE)) need(level, out, p, glass);
                    else if (al == 1 && ay == 1) need(level, out, p, solid);
                    else if (al + ay <= 1) clear(level, out, p);
                }
            }
        }
    }

    private static boolean special(int dx, int dz, int[][] specials) {
        for (int[] s : specials) if (s[0] == dx && s[1] == dz) return true;
        return false;
    }

    // ---------- Collider ring (Storm Collider, Mini Collider) ----------

    public static List<Ghost> colliderRing(Level level, BlockPos core) {
        List<Ghost> out = new ArrayList<>();
        final int R = ColliderCoreBlockEntity.RADIUS;
        Direction toInput = Direction.NORTH;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (is(level, core.relative(d, R), ModBlocks.COLLIDER_INPUT.get())) {
                toInput = d;
                break;
            }
        }
        for (int u = -R; u <= R; u++) {
            for (int v = -R; v <= R; v++) {
                if (Math.max(Math.abs(u), Math.abs(v)) != R) continue;
                Block want;
                if (u == 0 && v == R) want = ModBlocks.COLLIDER_INPUT.get();
                else if (v == 0) want = ModBlocks.COLLIDER_INJECTOR.get();
                else want = ModBlocks.FULGURITE_RING.get();
                need(level, out, core.relative(toInput, v).relative(toInput.getClockWise(), u), want.defaultBlockState());
            }
        }
        return out;
    }

    // ---------- Reactor Control Console ----------

    // Same layout as ReactorConsoleBlockEntity: rows bottom to top, columns left to right facing the front.
    private static final String[] CONSOLE = {"F1dFd2F", "PgDCDsP", "FGDDDSF"};

    public static List<Ghost> console(Level level, BlockPos console) {
        List<Ghost> out = new ArrayList<>();
        BlockState self = level.getBlockState(console);
        Direction front = self.hasProperty(ReactorConsoleBlock.FACING) ? self.getValue(ReactorConsoleBlock.FACING) : Direction.NORTH;
        Direction right = front.getOpposite().getClockWise();
        for (int row = 0; row < 3; row++) {
            for (int col = -3; col <= 3; col++) {
                char c = CONSOLE[row].charAt(col + 3);
                if (c == 'C') continue;
                Block want = switch (c) {
                    case 'P' -> ModBlocks.CONSOLE_POWER_PORT.get();
                    case '1', '2' -> ModBlocks.CONSOLE_FUEL_INPUT.get();
                    case 'd' -> ModBlocks.CONSOLE_DATA_PORT.get();
                    default -> ModBlocks.CONSOLE_CASING.get();
                };
                BlockState state = want.defaultBlockState();
                if (state.hasProperty(ConsolePortBlock.FACING)) state = state.setValue(ConsolePortBlock.FACING, front);
                need(level, out, console.relative(right, col).above(row - 1), state);
            }
        }
        return out;
    }

    // ---------- Helpers ----------

    private static boolean air(Level level, BlockPos p) {
        return level.getBlockState(p).isAir();
    }

    private static boolean is(Level level, BlockPos p, Block block) {
        return level.getBlockState(p).is(block);
    }

    private static void need(Level level, List<Ghost> out, BlockPos p, BlockState want) {
        if (!level.getBlockState(p).is(want.getBlock())) out.add(new Ghost(p.immutable(), want));
    }

    private static void clear(Level level, List<Ghost> out, BlockPos p) {
        if (!level.getBlockState(p).isAir()) out.add(new Ghost(p.immutable(), null));
    }

    // ---------- Drawing ----------

    // Draws the ghosts. poseStack is at the core block's corner (as in a block entity renderer).
    public static void render(List<Ghost> ghosts, BlockPos core, Level level, float partialTick, PoseStack poseStack,
                              MultiBufferSource buffers) {
        if (ghosts.isEmpty()) return;
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float time = level.getGameTime() + partialTick;
        float alpha = 0.32f + 0.12f * Mth.sin(time * 0.15f);
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        VertexConsumer blocks = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        int light = LightTexture.FULL_BRIGHT;
        for (Ghost g : ghosts) {
            if (g.want == null) continue;
            if (cam.distanceToSqr(Vec3.atCenterOf(g.pos)) > MAX_DISTANCE * MAX_DISTANCE) continue;
            poseStack.pushPose();
            poseStack.translate(g.pos.getX() - core.getX(), g.pos.getY() - core.getY(), g.pos.getZ() - core.getZ());
            // Shrink a touch so ghosts don't flicker against neighbouring real blocks.
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.98f, 0.98f, 0.98f);
            poseStack.translate(-0.5, -0.5, -0.5);
            BakedModel model = dispatcher.getBlockModel(g.want);
            PoseStack.Pose pose = poseStack.last();
            RANDOM.setSeed(42L);
            for (Direction d : Direction.values()) {
                for (BakedQuad q : model.getQuads(g.want, d, RANDOM, ModelData.EMPTY, null)) {
                    blocks.putBulkData(pose, q, 0.75f, 0.9f, 1.0f, alpha, light, OverlayTexture.NO_OVERLAY);
                }
            }
            RANDOM.setSeed(42L);
            for (BakedQuad q : model.getQuads(g.want, null, RANDOM, ModelData.EMPTY, null)) {
                blocks.putBulkData(pose, q, 0.75f, 0.9f, 1.0f, alpha, light, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }
        // Spots that must be left empty: a red box round whatever is in the way.
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Ghost g : ghosts) {
            if (g.want != null) continue;
            if (cam.distanceToSqr(Vec3.atCenterOf(g.pos)) > MAX_DISTANCE * MAX_DISTANCE) continue;
            double x = g.pos.getX() - core.getX(), y = g.pos.getY() - core.getY(), z = g.pos.getZ() - core.getZ();
            LevelRenderer.renderLineBox(poseStack, lines, new AABB(x + 0.02, y + 0.02, z + 0.02, x + 0.98, y + 0.98, z + 0.98),
                    1.0f, 0.2f, 0.2f, 1.0f);
        }
    }
}
