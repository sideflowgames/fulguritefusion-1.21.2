package com.sideflowgames.fulguritefusion.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;

// 26.1 / GeckoLib 5 port helper for the mod's animated multiblock models. GeckoLib 5 draws from a render state;
// this keeps the block entity in that state so the mod's models can still read it (formed, tier, fill level),
// moves the old per-bone tweaks (Animated) onto GeckoLib 5's bone snapshots, and runs the old extra drawing
// (items, glowing markers, bolts) after the model through LegacyRender.
public abstract class LegacyGeoBlockRenderer<T extends BlockEntity & GeoAnimatable>
        extends GeoBlockRenderer<T, LegacyGeoBlockRenderer.State<T>> {

    public static class State<T> extends BlockEntityRenderState {
        @Nullable public T blockEntity;
        public float partialTick;
    }

    // A model that sets some bones itself each frame (was GeoModel.setCustomAnimations in GeckoLib 4).
    public interface Animated<T> {
        void animate(T be, float partialTick, Function<String, BoneSnapshot> bones);
    }

    // The block entity behind a render state (for the models' texture/model lookups).
    @SuppressWarnings("unchecked")
    public static <T> T be(GeoRenderState state) {
        return ((State<T>) state).blockEntity;
    }

    public static float partialTick(GeoRenderState state) {
        return ((State<?>) state).partialTick;
    }

    protected LegacyGeoBlockRenderer(BlockEntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    // Whether to draw at all (the machines only draw once built).
    protected boolean visible(T be) {
        return true;
    }

    // The light to draw the model with.
    protected int light(T be, int packedLight) {
        return packedLight;
    }

    // Old-style drawing after the model, at the block's corner.
    protected void renderExtras(T be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {}

    @Override
    public State<T> createRenderState() {
        return new State<>();
    }

    @Override
    public void captureDefaultRenderState(T animatable, Void relatedObject, State<T> state, float partialTick) {
        super.captureDefaultRenderState(animatable, relatedObject, state, partialTick);
        state.blockEntity = animatable;
        state.partialTick = partialTick;
        int light = light(animatable, state.lightCoords);
        state.lightCoords = light;
        state.addGeckolibData(DataTickets.PACKED_LIGHT, light);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<State<T>> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        T be = info.renderState().blockEntity;
        if (be != null && getGeoModel() instanceof Animated<?> animated) {
            @SuppressWarnings("unchecked") Animated<T> a = (Animated<T>) animated;
            a.animate(be, info.renderState().partialTick, name -> snapshots.get(name).orElse(null));
        }
    }

    @Override
    public void submit(State<T> state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        T be = state.blockEntity;
        if (be == null || !visible(be)) return;
        super.submit(state, poseStack, collector, camera);
        LegacyRender.draw(poseStack, collector, buffers -> renderExtras(be, state.partialTick, poseStack, buffers, state.lightCoords));
    }
}
