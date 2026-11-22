package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

// Shared by every center block of the 7x7 collider ring (Mini Collider, Storm Collider).
//
// Structure (one layer, 7x7, the center block in the middle):
//   - 21 Fulgurite Ring blocks around the edge
//   - 1 Collider Input in the center of one side (Spark Cells go in here)
//   - 2 Collider Injectors in the centers of the two sides next to it
//   - the 4th side's center is a ring block
//
// A shot: two Spark Cells leave the input in opposite directions, each does one full lap of the ring,
// then carries on to the first injector it reaches, which fires it into the center block where they collide.
// What the collision does is up to the center block.
public abstract class ColliderCoreBlockEntity extends BlockEntity implements software.bernie.geckolib.animatable.GeoBlockEntity {
    public static final int RADIUS = 3;
    public static final int TICKS_PER_BLOCK = 2;

    // The formed ring is one GeckoLib model, drawn by the center block (see client/ColliderRenderer).
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    // Game time the shot in flight was fired (synced to clients so they can draw it), or -1.
    protected long shotStart = -1;

    // Cell trail colours: one electric blue, one violet.
    private static final DustParticleOptions TRAIL_A = new DustParticleOptions(new Vector3f(0.35f, 0.8f, 1.0f), 1.2f);
    private static final DustParticleOptions TRAIL_B = new DustParticleOptions(new Vector3f(0.8f, 0.5f, 1.0f), 1.2f);
    private static final DustParticleOptions TRAIL_A_FADE = new DustParticleOptions(new Vector3f(0.35f, 0.8f, 1.0f), 0.6f);
    private static final DustParticleOptions TRAIL_B_FADE = new DustParticleOptions(new Vector3f(0.8f, 0.5f, 1.0f), 0.6f);

    // Local ring coordinates (u = sideways, v = toward the input). Built once.
    private static final int[][] PATH = buildPath();
    public static final int INJECTOR_INDEX = PATH.length - RADIUS - 1;

    // For drawing a shot: path point i of the "+" cell as {u, v} (the other cell mirrors u).
    public static int[] pathPoint(int i) {
        return PATH[Math.max(0, Math.min(PATH.length - 1, i))];
    }

    public static int pathLength() {
        return PATH.length;
    }

    public long shotStart() {
        return shotStart;
    }

    protected int shotTimer = -1; // -1 = no shot in flight
    private int checkTimer;
    protected Direction inputDir;  // null = ring incomplete
    protected String problem = "Not checked yet";

    protected ColliderCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Path of the "+" cell: start at the input (0, 3), one full lap, then on to the injector at (3, 0),
    // then straight into the center at (0, 0). The other cell uses the mirror image (u flipped).
    private static int[][] buildPath() {
        List<int[]> p = new ArrayList<>();
        int u = 0, v = RADIUS;
        p.add(new int[]{u, v});
        while (u < RADIUS) p.add(new int[]{++u, v});
        while (v > -RADIUS) p.add(new int[]{u, --v});
        while (u > -RADIUS) p.add(new int[]{--u, v});
        while (v < RADIUS) p.add(new int[]{u, ++v});
        while (u < 0) p.add(new int[]{++u, v});
        while (u < RADIUS) p.add(new int[]{++u, v});
        while (v > 0) p.add(new int[]{u, --v});
        while (u > 0) p.add(new int[]{--u, v});
        return p.toArray(new int[0][]);
    }

    protected static int travelTicks() {
        return (PATH.length - 1) * TICKS_PER_BLOCK;
    }

    // ---------- What each center block does ----------

    // Everything (besides 2 loaded cells) is ready for the next shot.
    protected abstract boolean readyToFire();

    // Two cells have just left the input.
    protected void onFire() {}

    // The cells met in the middle.
    protected abstract void onCollide(ServerLevel level);

    // The ring broke while cells were in flight.
    protected void onShotLost() {}

    // Runs every tick (power in or out).
    protected void tickPower(Level level) {}

    // ---------- Structure ----------

    protected BlockPos local(Direction toInput, int u, int v) {
        return worldPosition.relative(toInput, v).relative(toInput.getClockWise(), u);
    }

    protected BlockPos inputPos() {
        return local(inputDir, 0, RADIUS);
    }

    // mirror = 1 or -1: the injector each cell is fired from.
    protected BlockPos injectorPos(int mirror) {
        return local(inputDir, RADIUS * mirror, 0);
    }

    // Finds the input and checks every block of the ring. Sets inputDir (or null) and problem.
    public void checkStructure() {
        if (level == null) return;
        Direction found = null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(worldPosition.relative(d, RADIUS)).is(ModBlocks.COLLIDER_INPUT.get())) {
                if (found != null) {
                    fail("Only one Collider Input is allowed");
                    return;
                }
                found = d;
            }
        }
        if (found == null) {
            fail("No Collider Input: put one in the middle of a side, 3 blocks out");
            return;
        }
        for (int u = -RADIUS; u <= RADIUS; u++) {
            for (int v = -RADIUS; v <= RADIUS; v++) {
                if (Math.max(Math.abs(u), Math.abs(v)) != RADIUS) continue; // only the edge
                Block expected;
                if (u == 0 && v == RADIUS) expected = ModBlocks.COLLIDER_INPUT.get();
                else if (v == 0) expected = ModBlocks.COLLIDER_INJECTOR.get();
                else expected = ModBlocks.FULGURITE_RING.get();
                BlockPos p = local(found, u, v);
                if (!level.getBlockState(p).is(expected)) {
                    fail("Missing " + expected.getName().getString() + " at " + p.getX() + " " + p.getY() + " " + p.getZ());
                    return;
                }
            }
        }
        inputDir = found;
        problem = null;
        updateFormed();
    }

    public boolean isComplete() {
        return inputDir != null;
    }

    // For the menu's ring diagram: one bit per edge spot (ColliderRing order), set when the right block is there.
    // Lined up on the Collider Input if there is one (otherwise facing north).
    public int ringMask() {
        if (level == null) return 0;
        Direction dir = inputDir;
        if (dir == null) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(worldPosition.relative(d, RADIUS)).is(ModBlocks.COLLIDER_INPUT.get())) {
                    dir = d;
                    break;
                }
            }
        }
        if (dir == null) dir = Direction.NORTH;
        int mask = 0;
        for (int i = 0; i < ColliderRing.SPOTS; i++) {
            int u = ColliderRing.u(i), v = ColliderRing.v(i);
            Block expected = switch (ColliderRing.kind(u, v)) {
                case ColliderRing.INPUT -> ModBlocks.COLLIDER_INPUT.get();
                case ColliderRing.INJECTOR -> ModBlocks.COLLIDER_INJECTOR.get();
                default -> ModBlocks.FULGURITE_RING.get();
            };
            if (level.getBlockState(local(dir, u, v)).is(expected)) mask |= 1 << i;
        }
        return mask;
    }

    // The collider whose complete ring 'ringPos' is part of (ring, input or injector), or null.
    public static ColliderCoreBlockEntity findFor(BlockGetter level, BlockPos ringPos) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != RADIUS) continue;
                if (level.getBlockEntity(ringPos.offset(dx, 0, dz)) instanceof ColliderCoreBlockEntity core && core.isComplete()) {
                    return core;
                }
            }
        }
        return null;
    }

    public abstract String statusText();

    // Right-click on the collider or any part of its ring: open the menu (sneak: one-line status).
    public void interact(Player player) {
        checkStructure();
        if (player.isShiftKeyDown() || !(this instanceof MenuProvider menu)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(statusText()), true);
        } else {
            player.openMenu(menu, buf -> buf.writeBlockPos(worldPosition));
        }
    }

    // Seen by the menu.
    public int shotTimer() {
        return shotTimer;
    }

    public static int shotLength() {
        return travelTicks();
    }

    public int cellsLoaded() {
        return loadedCells();
    }

    // The input / injector block entities, or null while the ring is incomplete.
    public ColliderInputBlockEntity inputBlock() {
        return level != null && inputDir != null && level.getBlockEntity(inputPos()) instanceof ColliderInputBlockEntity i ? i : null;
    }

    public ColliderInjectorBlockEntity injectorBlock(int mirror) {
        return level != null && inputDir != null && level.getBlockEntity(injectorPos(mirror)) instanceof ColliderInjectorBlockEntity i ? i : null;
    }

    private void fail(String why) {
        inputDir = null;
        problem = why;
        updateFormed();
    }

    // ---------- Forming ----------

    // Keeps the blocks' formed look in step with the ring: formed while complete, back to normal when not.
    private void updateFormed() {
        if (level == null || level.isClientSide) return;
        BlockState s = getBlockState();
        if (!s.hasProperty(ColliderParts.FORMED)) return;
        boolean want = inputDir != null, have = s.getValue(ColliderParts.FORMED);
        Direction haveDir = s.getValue(ColliderParts.INPUT);
        if (want == have && (!want || haveDir == inputDir)) return;
        if (have) setRing(level, worldPosition, haveDir, false);
        if (want) setRing(level, worldPosition, inputDir, true);
        level.setBlock(worldPosition, s.setValue(ColliderParts.FORMED, want).setValue(ColliderParts.INPUT, want ? inputDir : haveDir), 3);
    }

    private static void setRing(Level level, BlockPos core, Direction toInput, boolean formed) {
        for (int u = -RADIUS; u <= RADIUS; u++) {
            for (int v = -RADIUS; v <= RADIUS; v++) {
                if (Math.max(Math.abs(u), Math.abs(v)) != RADIUS) continue;
                BlockPos p = core.relative(toInput, v).relative(toInput.getClockWise(), u);
                BlockState rs = level.getBlockState(p);
                if (rs.hasProperty(ColliderParts.FORMED) && rs.getValue(ColliderParts.FORMED) != formed) {
                    level.setBlock(p, rs.setValue(ColliderParts.FORMED, formed), 3);
                }
            }
        }
    }

    // The center block is being removed: show its ring blocks again.
    public static void unformRing(Level level, BlockPos core, BlockState coreState) {
        if (level.isClientSide || !coreState.hasProperty(ColliderParts.FORMED) || !coreState.getValue(ColliderParts.FORMED)) return;
        setRing(level, core, coreState.getValue(ColliderParts.INPUT), false);
    }

    // A ring block was removed: the collider it belonged to re-checks (and un-forms) straight away.
    public static void partRemoved(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != RADIUS) continue;
                if (level.getBlockEntity(pos.offset(dx, 0, dz)) instanceof ColliderCoreBlockEntity core) core.checkStructure();
            }
        }
    }

    // ---------- Syncing the shot to clients ----------

    private void syncShot(long start) {
        shotStart = start;
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("ShotStart", shotStart);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // Clients only take the shot timing from updates (the rest stays on the server).
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        shotStart = tag.contains("ShotStart") ? tag.getLong("ShotStart") : -1;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    protected int loadedCells() {
        if (level == null || inputDir == null) return 0;
        return level.getBlockEntity(inputPos()) instanceof ColliderInputBlockEntity input ? input.cellCount() : 0;
    }

    // ---------- Ticking ----------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ColliderCoreBlockEntity be) {
        if (++be.checkTimer >= 20 || be.inputDir == null) {
            be.checkTimer = 0;
            be.checkStructure();
        }

        if (be.shotTimer >= 0) {
            if (be.inputDir == null) {
                be.shotTimer = -1;
                be.syncShot(-1);
                be.onShotLost();
                be.fizzleEffects((ServerLevel) level);
            } else {
                be.shotTimer++;
                be.drawTrails((ServerLevel) level);
                if (be.shotTimer >= travelTicks()) {
                    be.shotTimer = -1;
                    be.syncShot(-1);
                    be.collisionEffects((ServerLevel) level);
                    be.onCollide((ServerLevel) level);
                }
            }
            be.setChanged();
        } else if (be.inputDir != null && be.loadedCells() >= 2 && be.readyToFire()
                && level.getBlockEntity(be.inputPos()) instanceof ColliderInputBlockEntity input && input.takePair()) {
            be.shotTimer = 0;
            be.syncShot(level.getGameTime());
            be.onFire();
            level.playSound(null, be.inputPos(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8f, 1.6f);
            be.setChanged();
        }

        be.tickPower(level);
    }

    // ---------- Effects ----------

    private Vec3 pathPoint(int index, int mirror) {
        int[] p = PATH[index];
        BlockPos b = local(inputDir, p[0] * mirror, p[1]);
        // The cells travel through the middle of the (see-through) ring blocks, then straight into the center.
        double y = 0.5;
        return new Vec3(b.getX() + 0.5, b.getY() + y, b.getZ() + 0.5);
    }

    private Vec3 pathAt(double f, int mirror) {
        int i = Math.min((int) Math.floor(f), PATH.length - 2);
        double t = Math.min(1.0, f - i);
        return pathPoint(i, mirror).lerp(pathPoint(i + 1, mirror), t);
    }

    private void drawTrails(ServerLevel level) {
        double head = (double) shotTimer / TICKS_PER_BLOCK;
        for (int k = 0; k < 4; k++) {
            double f = head - k * 0.3;
            if (f < 0) break;
            Vec3 a = pathAt(f, 1);
            Vec3 b = pathAt(f, -1);
            level.sendParticles(k == 0 ? TRAIL_A : TRAIL_A_FADE, a.x, a.y, a.z, 1, 0, 0, 0, 0);
            level.sendParticles(k == 0 ? TRAIL_B : TRAIL_B_FADE, b.x, b.y, b.z, 1, 0, 0, 0, 0);
        }
        if (shotTimer % 3 == 0) {
            Vec3 a = pathAt(head, 1);
            Vec3 b = pathAt(head, -1);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, a.x, a.y, a.z, 2, 0.05, 0.05, 0.05, 0.02);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, b.x, b.y, b.z, 2, 0.05, 0.05, 0.05, 0.02);
        }
        // The injectors flash as they fire.
        if (shotTimer == INJECTOR_INDEX * TICKS_PER_BLOCK) {
            for (int mirror : new int[]{1, -1}) {
                Vec3 inj = pathPoint(INJECTOR_INDEX, mirror);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, inj.x, inj.y + 0.6, inj.z, 12, 0.3, 0.3, 0.3, 0.1);
            }
            level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 2.0f);
        }
    }

    protected void collisionEffects(ServerLevel level) {
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 40, 0.4, 0.4, 0.4, 0.25);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 15, 0.2, 0.2, 0.2, 0.08);
        level.playSound(null, worldPosition, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.9f, 1.5f);
    }

    protected void fizzleEffects(ServerLevel level) {
        level.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5, 20, 0.3, 0.2, 0.3, 0.02);
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.2f);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ShotTimer", shotTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shotTimer = tag.contains("ShotTimer") ? tag.getInt("ShotTimer") : -1;
    }
}
