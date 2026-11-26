package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sideflowgames.fulguritefusion.backpack.BackpackBlock;
import com.sideflowgames.fulguritefusion.backpack.BackpackItem;
import com.sideflowgames.fulguritefusion.backpack.CuriosCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

// Draws a worn backpack (chestplate slot, or Curios' back slot) on the player's back, using the placed-block model.
public class BackpackLayer extends RenderLayer<net.minecraft.client.renderer.entity.state.AvatarRenderState, PlayerModel> {
    private final java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts = new java.util.ArrayList<>();
    private final net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();

    public BackpackLayer(RenderLayerParent<net.minecraft.client.renderer.entity.state.AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, int light,
                       net.minecraft.client.renderer.entity.state.AvatarRenderState renderState, float yRot, float xRot) {
        // 26.1: the render state doesn't carry the worn items we need, so look the player up by id.
        if (Minecraft.getInstance().level == null
                || !(Minecraft.getInstance().level.getEntity(renderState.id) instanceof net.minecraft.world.entity.player.Player player)) return;
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(stack.getItem() instanceof BackpackItem)) stack = com.sideflowgames.fulguritefusion.trinket.Trinkets.back(player);
        if (!(stack.getItem() instanceof BackpackItem)) stack = CuriosCompat.find(player, s -> s.getItem() instanceof BackpackItem);
        if (!(stack.getItem() instanceof BackpackItem item) || !(item.getBlock() instanceof BackpackBlock block)) return;
        BlockState state = block.defaultBlockState().setValue(BackpackBlock.FACING, Direction.NORTH);
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // Model space is upside down: flip it so the bag stands up, then sit its strap side against the back.
        pose.mulPose(Axis.XP.rotationDegrees(180));
        if (player.isCrouching()) pose.translate(0, 0.05, 0);
        pose.translate(-0.5, -0.78, -13 / 16.0);
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        random.setSeed(42L);
        parts.clear();
        model.collectParts(random, parts);
        collector.submitBlockModel(pose, net.minecraft.client.renderer.Sheets.cutoutBlockSheet(), new java.util.ArrayList<>(parts),
                net.minecraft.client.renderer.block.BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
