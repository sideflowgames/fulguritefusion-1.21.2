package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.block.MachineConnectorBlockEntity;
import com.sideflowgames.fulguritefusion.power.WireMath;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Draws the hanging wires: a thin, sagging cable from this connector's cap to each linked connector.
// Each wire is drawn once, by the connector with the lower position.
public class ConnectorRenderer extends LegacyBlockEntityRenderer<MachineConnectorBlockEntity> {
    private static final Identifier TEX_RAW =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/block/raw_wire.png");
    private static final Identifier TEX_INSULATED =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/block/insulated_wire.png");
    private static final Identifier TEX_HEAVY =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/block/heavy_wire.png");
    private static final Identifier TEX_RAW_REINFORCED =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/block/raw_reinforced_wire.png");
    private static final Identifier TEX_RAW_HEAVY =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/block/raw_heavy_wire.png");
    private static final Identifier TEX_REINFORCED =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/block/reinforced_wire.png");
    private static final int SEGMENTS = 24;

    public ConnectorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void render(MachineConnectorBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockPos pos = be.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        Vec3 a = WireMath.attachPoint(be.getLevel(), pos);
        PoseStack.Pose pose = poseStack.last();
        for (MachineConnectorBlockEntity.Link link : be.getLinks()) {
            if (pos.asLong() > link.other().asLong()) continue;
            Vec3 b = WireMath.attachPoint(be.getLevel(), link.other());
            var tier = link.tier();
            Identifier texture = switch (tier) {
                case RAW -> TEX_RAW;
                case INSULATED -> TEX_INSULATED;
                case RAW_REINFORCED -> TEX_RAW_REINFORCED;
                case REINFORCED -> TEX_REINFORCED;
                case RAW_HEAVY -> TEX_RAW_HEAVY;
                case HEAVY -> TEX_HEAVY;
            };
            float radius = tier.radius;
            VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutout(texture));
            Vec3 prev = WireMath.point(a, b, 0, tier);
            for (int i = 1; i <= SEGMENTS; i++) {
                Vec3 cur = WireMath.point(a, b, (double) i / SEGMENTS, tier);
                int light = be.getLevel() != null
                        ? LevelRenderer.getLightCoords(be.getLevel(), BlockPos.containing(prev.add(cur).scale(0.5)))
                        : packedLight;
                segment(vc, pose, prev.subtract(origin), cur.subtract(origin), light, i, radius);
                prev = cur;
            }
        }
    }

    // One piece of wire: two crossed thin strips, so it looks round from every angle.
    private static void segment(VertexConsumer vc, PoseStack.Pose pose, Vec3 from, Vec3 to, int light, int index, float radius) {
        Vec3 dir = to.subtract(from).normalize();
        Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side1 = dir.cross(up).normalize().scale(radius);
        Vec3 side2 = dir.cross(side1).normalize().scale(radius);
        float v0 = (index % 4) / 4f, v1 = v0 + 0.25f;
        quad(vc, pose, from, to, side1, side2.normalize(), light, v0, v1);
        quad(vc, pose, from, to, side2, side1.normalize(), light, v0, v1);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 from, Vec3 to, Vec3 side, Vec3 normal,
                             int light, float v0, float v1) {
        vertex(vc, pose, from.add(side), 0f, v0, light, normal);
        vertex(vc, pose, from.subtract(side), 0.25f, v0, light, normal);
        vertex(vc, pose, to.subtract(side), 0.25f, v1, light, normal);
        vertex(vc, pose, to.add(side), 0f, v1, light, normal);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, int light, Vec3 n) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }

    // Wires can stretch far past the connector block, so keep drawing them when it's off-screen.
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(MachineConnectorBlockEntity be) {
        return be.wireBounds();
    }
}
