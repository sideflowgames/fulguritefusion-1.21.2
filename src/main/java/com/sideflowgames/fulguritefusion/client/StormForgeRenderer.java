package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sideflowgames.fulguritefusion.block.StormForgeBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

// Draws the items lying on the Forge Anvil (and the finished item, floating), plus the ghost build outline
// while the anvil gets a redstone signal.
public class StormForgeRenderer implements BlockEntityRenderer<StormForgeBlockEntity> {
    public StormForgeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(StormForgeBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = be.getLevel();
        if (level == null) return;
        var items = Minecraft.getInstance().getItemRenderer();
        float time = level.getGameTime() + partialTick;

        ItemStack result = be.result();
        if (!result.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 1.35 + Math.sin(time * 0.08) * 0.05, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(time * 2));
            pose.scale(0.6f, 0.6f, 0.6f);
            items.renderStatic(result, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers, level, 0);
            pose.popPose();
        } else {
            List<ItemStack> contents = be.contents();
            int n = contents.size();
            for (int i = 0; i < n; i++) {
                pose.pushPose();
                double a = n == 1 ? 0 : i * Math.PI * 2 / n;
                double r = n == 1 ? 0 : 0.22;
                pose.translate(0.5 + Math.cos(a) * r, 1.02 + i * 0.003, 0.5 + Math.sin(a) * r);
                if (be.isWorking()) pose.translate(0, 0.02 * Math.sin(time * 0.6 + i), 0);
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(a) + 90));
                pose.scale(0.38f, 0.38f, 0.38f);
                items.renderStatic(contents.get(i), ItemDisplayContext.FIXED, be.isWorking() ? 0xF000F0 : light,
                        OverlayTexture.NO_OVERLAY, pose, buffers, level, i);
                pose.popPose();
            }
        }

        if (GhostBuild.powered(level, be.getBlockPos())) {
            GhostBuild.render(GhostBuild.get(level, be.getBlockPos(), l -> be instanceof com.sideflowgames.fulguritefusion.block.TitanForgeBlockEntity
                            ? GhostBuild.titanForge(l, be.getBlockPos()) : GhostBuild.stormForge(l, be.getBlockPos())),
                    be.getBlockPos(), level, partialTick, pose, buffers);
        }
    }

    @Override
    public AABB getRenderBoundingBox(StormForgeBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(4);
    }
}
