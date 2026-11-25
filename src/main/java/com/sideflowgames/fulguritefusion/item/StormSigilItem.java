package com.sideflowgames.fulguritefusion.item;

import java.util.List;

import com.sideflowgames.fulguritefusion.stormreach.Stormreach;
import com.sideflowgames.fulguritefusion.titan.TitanTrials;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Storm Sigil: forged from the three guardians' pieces in the Storm Forge, and dropped again by the Storm Titan
// each time it falls.
//   Use it anywhere: you, and everyone within 8 blocks of you, are carried into your arena in the Titan Trials.
//   Use it in the Trials: back to where you came from.
//   Use it on the Titan Altar: it's used up, and the Titan wakes.
// While the Stormreach is still in development, only the developer can use it.
public class StormSigilItem extends Item {
    public StormSigilItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (!Stormreach.allowed(player)) {
            player.displayClientMessage(Component.literal("The Titan Trials are still in development. They're coming in Fulgurite Fusion 2.0.")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        if (TitanTrials.is(level)) TitanTrials.home(sp);
        else TitanTrials.enter(sp);
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use to enter the Titan Trials, bringing").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("anyone within 8 blocks. Use there to leave.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Use on the Titan Altar to wake the Titan.").withStyle(ChatFormatting.GRAY));
    }
}
