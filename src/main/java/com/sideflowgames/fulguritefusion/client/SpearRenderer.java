package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.entity.SpearEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

// Draws a thrown spear as its item, pointing the way it flies (two crossed copies so it reads from any angle).
public class SpearRenderer extends EntityRenderer<SpearEntity> {
    private static final float SCALE = 1.7f;

    public SpearRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpearEntity spear, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        ItemStack stack = new ItemStack(spear.isStorm() ? ModItems.STORM_SPEAR.get() : ModItems.FULGURITE_SPEAR.get());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, spear.yRotO, spear.getYRot()) - 90.0f));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, spear.xRotO, spear.getXRot())));
        // The texture's tip points to the top-right corner: turn it to point along the flight path.
        pose.mulPose(Axis.ZP.rotationDegrees(-45.0f));
        pose.scale(SCALE, SCALE, SCALE);
        var items = Minecraft.getInstance().getItemRenderer();
        items.renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, spear.level(), spear.getId());
        pose.mulPose(Axis.of(new org.joml.Vector3f(1, 1, 0).normalize()).rotationDegrees(90));
        items.renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, spear.level(), spear.getId());
        pose.popPose();
        super.render(spear, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SpearEntity spear) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
