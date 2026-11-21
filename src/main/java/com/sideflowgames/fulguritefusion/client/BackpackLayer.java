package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sideflowgames.fulguritefusion.backpack.BackpackBlock;
import com.sideflowgames.fulguritefusion.backpack.BackpackItem;
import com.sideflowgames.fulguritefusion.backpack.CuriosCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

// Draws a worn backpack (chestplate slot, or Curios' back slot) on the player's back, using the placed-block model.
public class BackpackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public BackpackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(stack.getItem() instanceof BackpackItem)) stack = CuriosCompat.find(player, s -> s.getItem() instanceof BackpackItem);
        if (!(stack.getItem() instanceof BackpackItem item) || !(item.getBlock() instanceof BackpackBlock block)) return;
        BlockState state = block.defaultBlockState().setValue(BackpackBlock.FACING, Direction.NORTH);
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // Model space is upside down: flip it so the bag stands up, then sit its strap side against the back.
        pose.mulPose(Axis.XP.rotationDegrees(180));
        if (player.isCrouching()) pose.translate(0, 0.05, 0);
        pose.translate(-0.5, -0.78, -13 / 16.0);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, null);
        pose.popPose();
    }
}
