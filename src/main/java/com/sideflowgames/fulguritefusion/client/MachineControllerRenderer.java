package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.block.MachineControllerBlockEntity;
import com.sideflowgames.fulguritefusion.machine.MachineShape;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// Draws a formed multiblock machine as one big Blockbench model, centered on the whole structure.
// Files per machine (e.g. press): geo/block/press.geo.json, animations/block/press.animation.json,
// textures/block/machine/press.png (+ press_glowmask.png). Models face -Z (north) like Blockbench's default,
// with the structure's center at the origin. Nothing is drawn until the machine is built.
public class MachineControllerRenderer extends GeoBlockRenderer<MachineControllerBlockEntity> {
    public MachineControllerRenderer(BlockEntityRendererProvider.Context context) {
        super(new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(MachineControllerBlockEntity machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!machine.isFormed()) return;
        super.render(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }

    // Turn the model to the machine's facing, then slide it to the center of the structure.
    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        float degrees = switch (facing) {
            case SOUTH -> 180f;
            case EAST -> -90f;
            case WEST -> 90f;
            default -> 0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        MachineShape shape = animatable.type().shape;
        // In this turned frame the front is -Z, the back is +Z, and "right" (as you face the front) is -X.
        poseStack.translate(-shape.centerRight(), 0, shape.centerBack());
    }

    @Override
    public boolean shouldRenderOffScreen(MachineControllerBlockEntity blockEntity) {
        return true;
    }

    // The model is bigger than one block: keep drawing it while any part of the machine is on screen.
    @Override
    public AABB getRenderBoundingBox(MachineControllerBlockEntity machine) {
        return new AABB(machine.getBlockPos()).inflate(machine.type().shape.size);
    }

    private static class Model extends GeoModel<MachineControllerBlockEntity> {
        @Override
        public ResourceLocation getModelResource(MachineControllerBlockEntity machine) {
            return rl("geo/block/" + machine.type().id + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(MachineControllerBlockEntity machine) {
            return rl("textures/block/machine/" + machine.type().id + ".png");
        }

        @Override
        public ResourceLocation getAnimationResource(MachineControllerBlockEntity machine) {
            return rl("animations/block/" + machine.type().id + ".animation.json");
        }

        private static ResourceLocation rl(String path) {
            return ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, path);
        }
    }
}
