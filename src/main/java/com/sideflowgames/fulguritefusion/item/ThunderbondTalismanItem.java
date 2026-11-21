package com.sideflowgames.fulguritefusion.item;

import com.sideflowgames.fulguritefusion.entity.StormwingEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// Thunderbond Talisman: crafted only. Used on a Stormwing to bond with it so it can be ridden.
//   - A wild Stormwing: only during a thunderstorm, and only while it isn't angry. It becomes yours.
//   - Your own loyal Stormwing (hatched from an egg): any weather.
// The talisman is used up when the bond takes. After that, right-click the bird to ride it.
public class ThunderbondTalismanItem extends Item {
    public ThunderbondTalismanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof StormwingEntity bird)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        String refusal = bird.tryBond(player);
        if (refusal != null) {
            player.displayClientMessage(Component.literal(refusal).withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.literal("The Stormwing is bonded to you. Right-click it to ride.")
                .withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use on a calm Stormwing during a thunderstorm").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Bonds it to you so you can ride it").withStyle(ChatFormatting.GRAY));
    }
}
