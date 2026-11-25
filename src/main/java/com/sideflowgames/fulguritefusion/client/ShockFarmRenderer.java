package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.farm.FarmBlocks;
import com.sideflowgames.fulguritefusion.farm.ShockFarmControllerBlock;
import com.sideflowgames.fulguritefusion.farm.ShockFarmControllerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// The Shock Farm. Not formed: the ghost build outline while the controller gets a redstone signal.
// Formed: its parts hide and the whole farm is drawn here as one model (geo/block/shock_farm_5 or _7.geo.json,
// textures/block/machine/shock_farm_5 / _7 + _glowmask), turned to face the way the controller does. Farm Collectors
// and Farm Power Ports merge in too: the frame is drawn over them, with a panel of their own face on the farm's outer
// wall so you can still find them. While the farm runs, the prong coils spin.
public class ShockFarmRenderer extends GeoBlockRenderer<ShockFarmControllerBlockEntity> {
    public ShockFarmRenderer(BlockEntityRendererProvider.Context context) {
        super(new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(ShockFarmControllerBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        if (level == null) return;
        if (be.formedSize() == 0) {
            if (GhostBuild.powered(level, pos)) {
                GhostBuild.render(GhostBuild.get(level, pos, l -> GhostBuild.shockFarm(l, pos)), pos, level, partialTick, poseStack, buffers);
            }
            return;
        }
        super.render(be, partialTick, poseStack, buffers, packedLight, packedOverlay);
        panels(be, level, poseStack, buffers, packedOverlay);
        if (be.getBlockState().getValue(ShockFarmControllerBlock.ACTIVE)) arcs(be, level, partialTick, poseStack, buffers);
    }

    // While running: flickering bolts from each prong's coil down into the middle of the pad.
    private static void arcs(ShockFarmControllerBlockEntity be, Level level, float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        BlockState self = be.getBlockState();
        Direction f = self.getValue(ShockFarmControllerBlock.FACING), side = f.getClockWise();
        int size = be.formedSize(), r = size == 2 ? 3 : 2, h = size == 2 ? 3 : 2;
        long t = level.getGameTime();
        if ((t / 2) % 3 == 0) return; // flicker
        net.minecraft.world.phys.Vec3 fwd = net.minecraft.world.phys.Vec3.atLowerCornerOf(f.getNormal());
        net.minecraft.world.phys.Vec3 lat = net.minecraft.world.phys.Vec3.atLowerCornerOf(side.getNormal());
        net.minecraft.world.phys.Vec3 origin = new net.minecraft.world.phys.Vec3(0.5, 0, 0.5);
        net.minecraft.world.phys.Vec3 pad = origin.add(fwd.scale(r)).add(0, 1.2, 0);
        com.mojang.blaze3d.vertex.VertexConsumer vc = buffers.getBuffer(net.minecraft.client.renderer.RenderType.lightning());
        org.joml.Matrix4f m = poseStack.last().pose();
        java.util.Random rnd = new java.util.Random(t / 2 * 31 + be.getBlockPos().asLong());
        int i = 0;
        for (int sf : new int[] {0, 2 * r}) {
            for (int sl : new int[] {-r, r}) {
                if (rnd.nextInt(4) == 0) { i++; continue; }
                net.minecraft.world.phys.Vec3 tip = origin.add(fwd.scale(sf)).add(lat.scale(sl)).add(0, h + 1.05, 0);
                net.minecraft.world.phys.Vec3 prev = tip;
                int steps = 8;
                for (int k = 1; k <= steps; k++) {
                    double a = k / (double) steps;
                    net.minecraft.world.phys.Vec3 next = tip.lerp(pad, a);
                    if (k < steps) next = next.add((rnd.nextDouble() - 0.5) * 0.5, (rnd.nextDouble() - 0.5) * 0.3, (rnd.nextDouble() - 0.5) * 0.5);
                    bolt(vc, m, prev, next, 0.035f);
                    prev = next;
                }
                i++;
            }
        }
    }

    private static final ResourceLocation COLLECTOR_FACE = ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/block/farm_collector_side.png");
    private static final ResourceLocation PORT_FACE = ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/block/farm_power_port.png");

    // A panel showing the collector / power port face, mounted on the outer wall of the merged farm where each one stands.
    private static void panels(ShockFarmControllerBlockEntity be, Level level, PoseStack poseStack, MultiBufferSource buffers, int overlay) {
        if (level.getBlockEntity(be.getBlockPos()) != be) return; // a guide picture, not a farm in the world
        BlockState self = be.getBlockState();
        Direction f = self.getValue(ShockFarmControllerBlock.FACING), side = f.getClockWise();
        int r = be.formedSize() == 2 ? 3 : 2;
        BlockPos ctrl = be.getBlockPos();
        PoseStack.Pose pose = poseStack.last();
        for (int fwd = 0; fwd <= 2 * r; fwd++) {
            for (int lat = -r; lat <= r; lat++) {
                boolean edge = fwd == 0 || fwd == 2 * r || Math.abs(lat) == r;
                boolean corner = (fwd == 0 || fwd == 2 * r) && Math.abs(lat) == r;
                if (!edge || corner || (fwd == 0 && lat == 0)) continue;
                BlockPos q = ctrl.relative(f, fwd).relative(side, lat);
                BlockState s = level.getBlockState(q);
                ResourceLocation tex = s.is(FarmBlocks.COLLECTOR.get()) ? COLLECTOR_FACE : s.is(FarmBlocks.POWER_PORT.get()) ? PORT_FACE : null;
                if (tex == null) continue;
                Direction out = fwd == 0 ? f.getOpposite() : fwd == 2 * r ? f : lat > 0 ? side : side.getOpposite();
                int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, q.relative(out));
                float nx = out.getStepX(), nz = out.getStepZ();
                float rx = nz, rz = -nx;                // right, seen from outside
                float cx = q.getX() - ctrl.getX() + 0.5f + nx * (0.5f + 1 / 16f + 0.004f);
                float cz = q.getZ() - ctrl.getZ() + 0.5f + nz * (0.5f + 1 / 16f + 0.004f);
                float cy = q.getY() - ctrl.getY() + 0.5f, h = 7 / 16f;
                com.mojang.blaze3d.vertex.VertexConsumer vc = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(tex));
                corner(vc, pose, cx - rx * h, cy - h, cz - rz * h, 0, 1, light, overlay, nx, nz);
                corner(vc, pose, cx + rx * h, cy - h, cz + rz * h, 1, 1, light, overlay, nx, nz);
                corner(vc, pose, cx + rx * h, cy + h, cz + rz * h, 1, 0, light, overlay, nx, nz);
                corner(vc, pose, cx - rx * h, cy + h, cz - rz * h, 0, 0, light, overlay, nx, nz);
            }
        }
    }

    private static void corner(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int light, int overlay, float nx, float nz) {
        vc.addVertex(pose.pose(), x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, 0, nz);
    }

    private static void bolt(com.mojang.blaze3d.vertex.VertexConsumer vc, org.joml.Matrix4f m, net.minecraft.world.phys.Vec3 a,
                             net.minecraft.world.phys.Vec3 b, float w) {
        float r = 0.55f, g = 0.85f, bl = 1.0f, al = 0.85f;
        // Two crossed ribbons so the bolt shows from any side.
        for (int k = 0; k < 2; k++) {
            float dx = k == 0 ? w : 0, dz = k == 0 ? 0 : w;
            vc.addVertex(m, (float) a.x - dx, (float) a.y, (float) a.z - dz).setColor(r, g, bl, al);
            vc.addVertex(m, (float) a.x + dx, (float) a.y, (float) a.z + dz).setColor(r, g, bl, al);
            vc.addVertex(m, (float) b.x + dx, (float) b.y, (float) b.z + dz).setColor(r, g, bl, al);
            vc.addVertex(m, (float) b.x - dx, (float) b.y, (float) b.z - dz).setColor(r, g, bl, al);
            vc.addVertex(m, (float) b.x - dx, (float) b.y, (float) b.z - dz).setColor(r, g, bl, al);
            vc.addVertex(m, (float) b.x + dx, (float) b.y, (float) b.z + dz).setColor(r, g, bl, al);
            vc.addVertex(m, (float) a.x + dx, (float) a.y, (float) a.z + dz).setColor(r, g, bl, al);
            vc.addVertex(m, (float) a.x - dx, (float) a.y, (float) a.z - dz).setColor(r, g, bl, al);
        }
    }

    static float degrees(Direction toPad) {
        return switch (toPad) {
            case SOUTH -> 180f;
            case EAST -> -90f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees(facing)));
    }

    @Override
    public boolean shouldRenderOffScreen(ShockFarmControllerBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(ShockFarmControllerBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(8);
    }

    // Bone name of the frame segment at (lateral, forward) from the controller; lateral counts clockwise of facing.
    public static String edgeBone(int lateral, int forward) {
        return "e_" + (lateral < 0 ? "m" + (-lateral) : String.valueOf(lateral)) + "_" + forward;
    }

    private static class Model extends GeoModel<ShockFarmControllerBlockEntity> {
        private static ResourceLocation rl(String path) {
            return ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, path);
        }

        private static final ResourceLocation GEO5 = rl("geo/block/shock_farm_5.geo.json"), GEO7 = rl("geo/block/shock_farm_7.geo.json");
        private static final ResourceLocation TEX5 = rl("textures/block/machine/shock_farm_5.png"), TEX7 = rl("textures/block/machine/shock_farm_7.png");
        private static final ResourceLocation ANIM = rl("animations/block/shock_farm.animation.json");

        @Override
        public ResourceLocation getModelResource(ShockFarmControllerBlockEntity be) {
            return be.formedSize() == 2 ? GEO7 : GEO5;
        }

        @Override
        public ResourceLocation getTextureResource(ShockFarmControllerBlockEntity be) {
            return be.formedSize() == 2 ? TEX7 : TEX5;
        }

        @Override
        public ResourceLocation getAnimationResource(ShockFarmControllerBlockEntity be) {
            return ANIM;
        }

        @Override
        public void setCustomAnimations(ShockFarmControllerBlockEntity be, long instanceId, AnimationState<ShockFarmControllerBlockEntity> state) {
            Level level = be.getLevel();
            if (level == null) return;
            BlockState self = be.getBlockState();
            Direction f = self.getValue(ShockFarmControllerBlock.FACING), side = f.getClockWise();
            int r = be.formedSize() == 2 ? 3 : 2;
            BlockPos ctrl = be.getBlockPos();
            // The whole frame shows, collectors and power ports included (they merge in; see panels()).
            for (int fwd = 0; fwd <= 2 * r; fwd++) {
                for (int lat = -r; lat <= r; lat++) {
                    GeoBone b = getAnimationProcessor().getBone(edgeBone(lat, fwd));
                    if (b != null) b.setHidden(false);
                }
            }
            boolean running = self.getValue(ShockFarmControllerBlock.ACTIVE);
            float time = level.getGameTime() + state.getPartialTick();
            for (int i = 0; i < 4; i++) {
                GeoBone coil = getAnimationProcessor().getBone("coil_" + i);
                if (coil != null) coil.setRotY(running ? time * 0.25f : 0f);
            }
            GeoBone glow = getAnimationProcessor().getBone("pad_glow");
            if (glow != null) glow.setHidden(!running);
        }
    }
}
