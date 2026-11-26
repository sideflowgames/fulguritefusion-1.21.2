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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

// Draws a thrown spear as its item, pointing the way it flies (two crossed copies so it reads from any angle).
public class SpearRenderer extends EntityRenderer<SpearEntity, SpearRenderer.State> {
    private static final float SCALE = 1.7f;
    private final net.minecraft.client.renderer.item.ItemModelResolver items;

    public static class State extends net.minecraft.client.renderer.entity.state.EntityRenderState {
        public final net.minecraft.client.renderer.item.ItemStackRenderState item = new net.minecraft.client.renderer.item.ItemStackRenderState();
        public float yRot, xRot;
    }

    public SpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpearEntity spear, State state, float partialTick) {
        super.extractRenderState(spear, state, partialTick);
        ItemStack stack = new ItemStack(spear.isPrime() ? ModItems.PRIME_SPEAR.get() : spear.isStorm() ? ModItems.STORM_SPEAR.get() : ModItems.FULGURITE_SPEAR.get());
        items.updateForTopItem(state.item, stack, ItemDisplayContext.NONE, spear.level(), null, spear.getId());
        state.yRot = Mth.lerp(partialTick, spear.yRotO, spear.getYRot());
        state.xRot = Mth.lerp(partialTick, spear.xRotO, spear.getXRot());
    }

    @Override
    public void submit(State state, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0f));
        pose.mulPose(Axis.ZP.rotationDegrees(state.xRot));
        // The texture's tip points to the top-right corner: turn it to point along the flight path.
        pose.mulPose(Axis.ZP.rotationDegrees(-45.0f));
        pose.scale(SCALE, SCALE, SCALE);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.mulPose(Axis.of(new org.joml.Vector3f(1, 1, 0).normalize()).rotationDegrees(90));
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
