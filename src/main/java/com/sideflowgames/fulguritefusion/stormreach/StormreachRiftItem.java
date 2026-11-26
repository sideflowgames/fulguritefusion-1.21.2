package com.sideflowgames.fulguritefusion.stormreach;

import net.minecraft.world.InteractionResult;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

// Stormreach Rift: developer-only (the player named Dev, development build). Use it to go to the Stormreach, and
// again to come back to your spawn point. Get one with /stormreach rift. It isn't in any creative tab, and if anyone
// else ends up holding one it vanishes.
public class StormreachRiftItem extends Item {
    public StormreachRiftItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!Stormreach.developer(player)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.literal("The Stormreach is still in development. It's coming in Fulgurite Fusion 2.0.")
                        .withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer sp) StormreachTravel.travel(sp);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        if (!level.isClientSide() && entity instanceof Player && !Stormreach.developer(entity)) stack.setCount(0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag flag) {
        java.util.List<Component> tooltip = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut);
        tooltip.add(Component.literal("Developer only: takes you to the Stormreach and back.").withStyle(ChatFormatting.GRAY));
    }
}
