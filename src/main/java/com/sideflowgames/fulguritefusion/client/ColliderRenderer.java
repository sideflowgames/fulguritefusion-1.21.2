package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.block.ColliderCoreBlockEntity;
import com.sideflowgames.fulguritefusion.block.ColliderParts;
import com.sideflowgames.fulguritefusion.block.StormColliderBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.Random;

// The Mini / Storm Collider. While its ring is incomplete: the ghost build outline (when powered by redstone).
// Once formed: the whole 7x7 ring as one industrial model (geo/block/collider_ring.geo.json; textures
// mini_collider / storm_collider in textures/block/machine/), turned to face its Collider Input.
// During a shot, two glowing particles race along the groove on top of the ring, the injectors flash as they
// fire, and the core lights up when they collide. The Storm Collider's corner crystals throw small arcs.
public class ColliderRenderer<T extends ColliderCoreBlockEntity> extends GeoBlockRenderer<T> {
    private static final float GROOVE_Y = 9.5f / 16f;

    public ColliderRenderer(BlockEntityRendererProvider.Context context) {
        super(new Model<>());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    private static boolean formed(BlockState s) {
        return s.hasProperty(ColliderParts.FORMED) && s.getValue(ColliderParts.FORMED);
    }

    @Override
    public void render(T be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();
        if (!formed(state)) {
            if (level != null && GhostBuild.powered(level, pos)) {
                GhostBuild.render(GhostBuild.get(level, pos, l -> GhostBuild.colliderRing(l, pos)), pos, level, partialTick, poseStack, buffers);
            }
            return;
        }
        super.render(be, partialTick, poseStack, buffers, packedLight, packedOverlay);
        if (level == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees(state.getValue(ColliderParts.INPUT))));
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = poseStack.last().pose();
        float time = level.getGameTime() + partialTick;
        boolean storm = be instanceof StormColliderBlockEntity;

        long start = be.shotStart();
        if (start >= 0) {
            float t = time - start;
            float head = t / ColliderCoreBlockEntity.TICKS_PER_BLOCK;
            int last = ColliderCoreBlockEntity.pathLength() - 1;
            if (head <= last) {
                // The two cells: a bright head with a fading streak behind it. Blue one way round, violet the other.
                for (int mirror : new int[]{1, -1}) {
                    int r = mirror > 0 ? 90 : 205, g = mirror > 0 ? 205 : 130;
                    float[] prev = at(head, mirror);
                    for (int k = 1; k <= 8; k++) {
                        float f = head - k * 0.2f;
                        if (f < 0) break;
                        float[] p = at(f, mirror);
                        int alpha = 230 - k * 26;
                        ribbon(glow, m, prev, p, 0.07f - k * 0.006f, r, g, 255, alpha / 3);
                        ribbon(glow, m, prev, p, 0.025f, 230, 245, 255, alpha);
                        prev = p;
                    }
                    float[] h = at(head, mirror);
                    box(glow, m, h[0], h[1], h[2], 0.09f, r, g, 255, 150);
                    box(glow, m, h[0], h[1], h[2], 0.045f, 240, 250, 255, 255);
                }
            }
            // Injectors flash as the cells pass through them.
            float inj = head - ColliderCoreBlockEntity.INJECTOR_INDEX;
            if (inj > -0.5f && inj < 2.5f) {
                int a = (int) (220 * (1 - Math.abs(inj - 1) / 1.5f));
                if (a > 0) {
                    for (int mirror : new int[]{1, -1}) {
                        int[] p = ColliderCoreBlockEntity.pathPoint(ColliderCoreBlockEntity.INJECTOR_INDEX);
                        box(glow, m, p[0] * mirror - Math.signum(p[0] * mirror) * 0.95f, GROOVE_Y, -p[1], 0.16f, 150, 230, 255, a);
                    }
                }
            }
            // The collision: a flash at the core that grows and fades.
            float hit = t - ColliderCoreBlockEntity.shotLength();
            if (hit > -2 && hit < 10) {
                float k = Math.max(0, hit) / 10f;
                box(glow, m, 0, 0.5f, 0, 0.3f + k * 0.6f, 230, 245, 255, (int) (230 * (1 - k)));
            }
        }

        // Storm Collider: lightning crackles off the corner crystals down to the rail beside them, and now and
        // then a big arc jumps along a side from one crystal to the next.
        if (storm) {
            long tick = level.getGameTime();
            long window = tick / 2; // each arc lasts 2 ticks
            for (int c = 0; c < 4; c++) {
                Random r = new Random(window * 131 + c * 17);
                if (r.nextFloat() > 0.12f) continue;
                float sx = (c & 1) == 0 ? 1 : -1, sz = (c & 2) == 0 ? 1 : -1;
                float[] tip = {sx * (3 + 5 / 16f), 30 / 16f, sz * (3 + 5 / 16f)};
                float[] to = r.nextBoolean() ? new float[]{sx * 2, GROOVE_Y, sz * 3} : new float[]{sx * 3, GROOVE_Y, sz * 2};
                bolt(glow, m, tip, to, r, 0.12f, 0.55f);
            }
            Random big = new Random(window * 977);
            if (big.nextFloat() < 0.025f) {
                int side = big.nextInt(4);
                float[] a0 = corner(side), a1 = corner((side + 1) % 4);
                bolt(glow, m, a0, a1, big, 0.35f, 1f);
            }
        }
        poseStack.popPose();
    }

    // A point along a cell's path, in the model's frame (input side = -Z). f = blocks travelled.
    private static float[] at(float f, int mirror) {
        int i = (int) Math.floor(f);
        float t = f - i;
        int[] a = ColliderCoreBlockEntity.pathPoint(i), b = ColliderCoreBlockEntity.pathPoint(i + 1);
        float u = (a[0] + (b[0] - a[0]) * t) * mirror, v = a[1] + (b[1] - a[1]) * t;
        // The last stretch (injector to core) runs through the nozzle, at the same height.
        return new float[]{u, GROOVE_Y, -v};
    }

    private static float degrees(Direction toInput) {
        return switch (toInput) {
            case SOUTH -> 180f;
            case EAST -> -90f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    // The model faces its input to -Z; turn it toward this collider's Collider Input.
    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        BlockState s = animatable.getBlockState();
        Direction d = s.hasProperty(ColliderParts.INPUT) ? s.getValue(ColliderParts.INPUT) : Direction.NORTH;
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees(d)));
    }

    // Corner crystal tips, going round the ring.
    private static float[] corner(int i) {
        float sx = (i == 0 || i == 3) ? 1 : -1, sz = i < 2 ? 1 : -1;
        return new float[]{sx * (3 + 5 / 16f), 30 / 16f, sz * (3 + 5 / 16f)};
    }

    // A jagged lightning bolt from a to b: a soft glow around a thin bright core, with a short side branch.
    static void bolt(VertexConsumer c, Matrix4f m, float[] a, float[] b, Random r, float jag, float strength) {
        int n = 8;
        float[][] pts = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float off = (i == 0 || i == n) ? 0 : jag;
            pts[i] = new float[]{a[0] + (b[0] - a[0]) * t + (r.nextFloat() - 0.5f) * off,
                    a[1] + (b[1] - a[1]) * t + (r.nextFloat() - 0.5f) * off,
                    a[2] + (b[2] - a[2]) * t + (r.nextFloat() - 0.5f) * off};
        }
        int glowA = (int) (70 * strength), coreA = (int) (255 * strength);
        for (int i = 0; i < n; i++) {
            ribbon(c, m, pts[i], pts[i + 1], 0.06f, 120, 190, 255, glowA);
            ribbon(c, m, pts[i], pts[i + 1], 0.014f, 225, 242, 255, coreA);
        }
        // A short branch off somewhere in the middle.
        float[] from = pts[2 + r.nextInt(n - 4)];
        float[] prev = from;
        for (int i = 0; i < 3; i++) {
            float[] next = {prev[0] + (r.nextFloat() - 0.5f) * jag * 1.6f, prev[1] - r.nextFloat() * jag, prev[2] + (r.nextFloat() - 0.5f) * jag * 1.6f};
            ribbon(c, m, prev, next, 0.03f, 120, 190, 255, glowA);
            ribbon(c, m, prev, next, 0.008f, 225, 242, 255, (int) (coreA * 0.7f));
            prev = next;
        }
    }

    // A flat strip from p to q, drawn twice crossed so it looks like a line from any angle.
    static void ribbon(VertexConsumer c, Matrix4f m, float[] p, float[] q, float w, int r, int g, int b, int a) {
        float dx = q[0] - p[0], dy = q[1] - p[1], dz = q[2] - p[2];
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4f) return;
        dx /= len;
        dy /= len;
        dz /= len;
        // First side direction: across the strip, level if possible.
        float ax = dz, ay = 0, az = -dx;
        float al = (float) Math.sqrt(ax * ax + az * az);
        if (al < 1e-3f) {
            ax = 1;
            az = 0;
            al = 1;
        }
        ax /= al;
        az /= al;
        // Second: at right angles to both.
        float bx = dy * az - dz * ay, by = dz * ax - dx * az, bz = dx * ay - dy * ax;
        for (float[] s : new float[][]{{ax * w, ay * w, az * w}, {bx * w, by * w, bz * w}}) {
            quad(c, m, p[0] - s[0], p[1] - s[1], p[2] - s[2], p[0] + s[0], p[1] + s[1], p[2] + s[2],
                    q[0] + s[0], q[1] + s[1], q[2] + s[2], q[0] - s[0], q[1] - s[1], q[2] - s[2], r, g, b, a);
            quad(c, m, q[0] - s[0], q[1] - s[1], q[2] - s[2], q[0] + s[0], q[1] + s[1], q[2] + s[2],
                    p[0] + s[0], p[1] + s[1], p[2] + s[2], p[0] - s[0], p[1] - s[1], p[2] - s[2], r, g, b, a);
        }
    }

    static void box(VertexConsumer c, Matrix4f m, float x, float y, float z, float h, int r, int g, int b, int a) {
        float x0 = x - h, x1 = x + h, y0 = y - h, y1 = y + h, z0 = z - h, z1 = z + h;
        quad(c, m, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a);
        quad(c, m, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, r, g, b, a);
        quad(c, m, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, r, g, b, a);
        quad(c, m, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        quad(c, m, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(c, m, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, r, g, b, a);
    }

    private static void quad(VertexConsumer c, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int r, int g, int b, int a) {
        c.addVertex(m, ax, ay, az).setColor(r, g, b, a);
        c.addVertex(m, bx, by, bz).setColor(r, g, b, a);
        c.addVertex(m, cx, cy, cz).setColor(r, g, b, a);
        c.addVertex(m, dx, dy, dz).setColor(r, g, b, a);
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(T be) {
        return new AABB(be.getBlockPos()).inflate(5);
    }

    private static class Model<T extends ColliderCoreBlockEntity> extends GeoModel<T> {
        private static final ResourceLocation GEO = rl("geo/block/collider_ring.geo.json");
        private static final ResourceLocation ANIM = rl("animations/block/collider_ring.animation.json");
        private static final ResourceLocation MINI = rl("textures/block/machine/mini_collider.png");
        private static final ResourceLocation STORM = rl("textures/block/machine/storm_collider.png");

        @Override
        public ResourceLocation getModelResource(T be) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(T be) {
            return be instanceof StormColliderBlockEntity ? STORM : MINI;
        }

        @Override
        public ResourceLocation getAnimationResource(T be) {
            return ANIM;
        }

        private static ResourceLocation rl(String path) {
            return ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, path);
        }
    }
}
