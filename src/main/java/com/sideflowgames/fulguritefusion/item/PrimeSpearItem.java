package com.sideflowgames.fulguritefusion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// Prime Spear (end game, forged from a Storm Spear): hits harder, its lightning is stronger, and it flies back
// to you after a throw (like Loyalty, without the enchantment).
public class PrimeSpearItem extends StormSpearItem {
    public static final float MELEE = 14, THROWN = 17, LIGHTNING = 6;

    public PrimeSpearItem(Properties properties) {
        super(properties);
    }

    @Override
    public int maxFm() {
        return PrimeTools.MAX_FM;
    }

    @Override
    public float lightningBonus(ItemStack stack) {
        return FmChargeable.hasCharge(stack) ? LIGHTNING : 0;
    }


    @Override public int getBarColor(ItemStack s) { return PrimeTools.barColor(s); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag flag) {
        java.util.List<Component> tooltip = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut);
        PrimeTools.primeLine(tooltip);
        tooltip.add(Component.literal("+" + (int) LIGHTNING + " lightning damage; thrown, it calls lightning and flies back to you")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Hold right-click, then let go to throw").withStyle(ChatFormatting.GRAY));
        StormforgedTools.tooltip(stack, tooltip, false);
    }
}
