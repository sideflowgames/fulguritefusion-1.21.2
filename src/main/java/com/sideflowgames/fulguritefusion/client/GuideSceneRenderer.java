package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// 26.1: the guide book's turning 3D build pictures. GUI drawing is now collected first and drawn later, so the
// picture is drawn into its own texture by this "picture in picture" renderer (registered in FulguriteFusionClient),
// with the same perspective camera, blocks and formed machine models as before.
public class GuideSceneRenderer extends PictureInPictureRenderer<GuideSceneRenderer.State> {
    // One placed block (lift: how high above its spot it is while dropping in), and whether its block entity
    // (the formed machine model) should be drawn too.
    public record Block(BlockPos pos, BlockState state, float lift, @Nullable BlockEntity blockEntity) {}

    public record State(List<Block> blocks, float fov, float distance, float pitch, float yaw, float cx, float cy, float cz,
                        int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea,
                        @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
        public State(List<Block> blocks, float fov, float distance, float pitch, float yaw, float cx, float cy, float cz,
                     int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
            this(blocks, fov, distance, pitch, yaw, cx, cy, cz, x0, y0, x1, y1, 1f, scissorArea,
                    PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
        }
    }

    private final Projection projection = new Projection();
    private final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("Fulgurite Fusion guide scene");
    private final RandomSource random = RandomSource.create();

    public GuideSceneRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected String getTextureLabel() {
        return "fulguritefusion_guide_scene";
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void renderToTexture(State state, PoseStack ignored) {
        Minecraft mc = Minecraft.getInstance();
        int guiScale = (int) mc.getWindow().getGuiScale();
        float w = (state.x1() - state.x0()) * guiScale, h = (state.y1() - state.y0()) * guiScale;
        projection.setupPerspective(0.05f, 4000f, state.fov(), w, h);
        RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection), ProjectionType.PERSPECTIVE);
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        PoseStack ps = new PoseStack();
        ps.translate(0, 0, -state.distance());
        ps.mulPose(Axis.XP.rotationDegrees(state.pitch()));
        ps.mulPose(Axis.YP.rotationDegrees(state.yaw()));
        ps.translate(-state.cx(), -state.cy(), -state.cz());

        FeatureRenderDispatcher features = mc.gameRenderer.getFeatureRenderDispatcher();
        var storage = features.getSubmitNodeStorage();
        var models = mc.getModelManager().getBlockStateModelSet();
        CameraRenderState camera = new CameraRenderState();
        List<BlockStateModelPart> parts = new ArrayList<>();
        for (Block b : state.blocks()) {
            ps.pushPose();
            ps.translate(b.pos().getX(), b.pos().getY() + b.lift(), b.pos().getZ());
            BlockStateModel model = models.get(b.state());
            random.setSeed(42L);
            parts.clear();
            model.collectParts(random, parts);
            if (!parts.isEmpty()) {
                storage.submitMultiLayerBlockModel(ps, new ArrayList<>(parts), model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT),
                        BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            }
            BlockEntity be = b.blockEntity();
            if (be != null) {
                try {
                    BlockEntityRenderer renderer = mc.getBlockEntityRenderDispatcher().getRenderer(be);
                    if (renderer != null) {
                        BlockEntityRenderState rs = renderer.createRenderState();
                        renderer.extractRenderState(be, rs, 0f, Vec3.ZERO, null);
                        rs.lightCoords = LightCoordsUtil.FULL_BRIGHT;
                        renderer.submit(rs, ps, storage, camera);
                    }
                } catch (Exception e) {
                    // A picture isn't worth a crash.
                }
            }
            ps.popPose();
        }
        features.renderAllFeatures();
    }

    @Override
    public void close() {
        super.close();
        projectionBuffer.close();
    }
}
