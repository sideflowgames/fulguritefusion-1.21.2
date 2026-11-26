package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

// 26.1 port helper: a block entity renderer with the old render(...) method. The render state just remembers
// the block entity; drawing happens at submit time through LegacyRender.
public abstract class LegacyBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, LegacyBlockEntityRenderer.State<T>> {
    public static class State<T> extends BlockEntityRenderState {
        @Nullable public T blockEntity;
        public float partialTick;
    }

    protected abstract void render(T be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay);

    @Override
    public State<T> createRenderState() {
        return new State<>();
    }

    @Override
    public void extractRenderState(T be, State<T> state, float partialTick, Vec3 cameraPosition,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        state.blockEntity = be;
        state.partialTick = partialTick;
    }

    @Override
    public void submit(State<T> state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        T be = state.blockEntity;
        if (be == null) return;
        LegacyRender.draw(poseStack, collector,
                buffers -> render(be, state.partialTick, poseStack, buffers, state.lightCoords, OverlayTexture.NO_OVERLAY));
    }
}
