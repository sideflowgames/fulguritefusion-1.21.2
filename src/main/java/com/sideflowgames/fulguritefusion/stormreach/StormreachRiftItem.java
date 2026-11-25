package com.sideflowgames.fulguritefusion.stormreach;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!Stormreach.developer(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("The Stormreach is still in development. It's coming in Fulgurite Fusion 2.0.")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer sp) StormreachTravel.travel(sp);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player && !Stormreach.developer(entity)) stack.setCount(0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Developer only: takes you to the Stormreach and back.").withStyle(ChatFormatting.GRAY));
    }
}
