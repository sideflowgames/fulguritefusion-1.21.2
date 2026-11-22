package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.harvester.LatexHarvesterBlock;
import com.sideflowgames.fulguritefusion.harvester.LatexHarvesterBlockEntity;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// Draws a built Latex Harvester (geo/block/latex_harvester.geo.json): the whole 3x3x2 ring around the trunk, with the
// trim in the casing tier's colour. Every block of the ring hides once it's built, so this model draws them all,
// including the controller's screen, the power port's coil and the tank column, whose window shows the latex level.
// The tank and port can be in either front corner: the model has both and shows the matching pair.
public class LatexHarvesterRenderer extends GeoBlockRenderer<LatexHarvesterBlockEntity> {
    public LatexHarvesterRenderer(BlockEntityRendererProvider.Context context) {
        super(new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(LatexHarvesterBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (!be.getBlockState().getValue(LatexHarvesterBlock.FORMED)) return;
        // Light from the open space in front of the controller (the ring's own blocks and the trunk are solid).
        Direction facing = be.getBlockState().getValue(LatexHarvesterBlock.FACING);
        int light = be.getLevel() == null ? packedLight : LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(facing));
        super.render(be, partialTick, poseStack, buffers, light, packedOverlay);
    }

    static float degrees(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> -90f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    // Turn to the controller's facing, then move to the trunk's base (one block behind the controller and one down).
    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees(facing)));
        poseStack.translate(0, -1, 1);
    }

    @Override
    public boolean shouldRenderOffScreen(LatexHarvesterBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(LatexHarvesterBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(3);
    }

    private static class Model extends GeoModel<LatexHarvesterBlockEntity> {
        @Override
        public ResourceLocation getModelResource(LatexHarvesterBlockEntity be) {
            return rl("geo/block/latex_harvester.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(LatexHarvesterBlockEntity be) {
            return rl("textures/block/machine/latex_harvester_t" + be.shownTier() + ".png");
        }

        @Override
        public ResourceLocation getAnimationResource(LatexHarvesterBlockEntity be) {
            return rl("animations/block/latex_harvester.animation.json");
        }

        @Override
        public void setCustomAnimations(LatexHarvesterBlockEntity be, long instanceId, AnimationState<LatexHarvesterBlockEntity> state) {
            // Tank on the controller's clockwise side = the model's -x side (GeckoLib mirrors x).
            boolean cw = be.shownTankClockwise();
            hide("tank_n", !cw);
            hide("port_p", !cw);
            hide("tank_p", cw);
            hide("port_n", cw);
            float fill = be.shownFill();
            for (String name : new String[]{"latex_n", "latex_p"}) {
                GeoBone b = getAnimationProcessor().getBone(name);
                if (b == null) continue;
                b.setHidden(fill <= 0);
                b.setScaleY(Math.max(0.01f, fill));
            }
            // The status lamp blinks while it's tapping the tree.
            boolean working = be.getBlockState().getValue(LatexHarvesterBlock.WORKING);
            float time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + state.getPartialTick();
            hide("lamp", working && ((long) time / 10) % 2 == 1);
            // Probes: each stabs into the bark, holds while it draws latex, then pulls back out (offset from each
            // other so they don't move together). Idle, they rest pulled back.
            String[] sides = {"n", "p", "b"};
            for (int i = 0; i < sides.length; i++) {
                for (int k = 0; k < 2; k++) {
                    GeoBone b = getAnimationProcessor().getBone("probe_" + sides[i] + (k == 0 ? "lo" : "hi"));
                    if (b == null) continue;
                    float out = working ? retract(((time + i * 13 + k * 20) % 40f) / 40f) : 3f;
                    if (i == 0) b.setPosX(-out);
                    else if (i == 1) b.setPosX(out);
                    else b.setPosZ(out);
                }
                // Latex drips from the lower probe into its cup.
                GeoBone drip = getAnimationProcessor().getBone("drip_" + sides[i]);
                if (drip != null) {
                    drip.setHidden(!working);
                    float p = ((time + i * 9) % 16f) / 16f;
                    drip.setPosY(-2.5f * p * p);
                }
            }
        }

        // How far a probe is pulled back (pixels) at point p (0-1) of its cycle: a quick stab, a long draw, a slow pull.
        private static float retract(float p) {
            if (p < 0.1f) return 3f * (1f - p / 0.1f);
            if (p < 0.7f) return 0f;
            return 3f * (p - 0.7f) / 0.3f;
        }

        private void hide(String bone, boolean hidden) {
            GeoBone b = getAnimationProcessor().getBone(bone);
            if (b != null) b.setHidden(hidden);
        }

        private static ResourceLocation rl(String path) {
            return ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, path);
        }
    }
}
