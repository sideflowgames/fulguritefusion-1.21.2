package com.sideflowgames.fulguritefusion.backpack;

import com.sideflowgames.fulguritefusion.item.FmChargeable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// The Stormforged and Prime Backpacks: hold their own Fm (charge it in a Fulgurite Charger) to power the boosts of
// Stormforged upgrades. With no charge, those upgrades still work like the basic ones.
public class StormBackpackItem extends BackpackItem implements FmChargeable {
    public static final int MAX_FM = 500_000;

    public StormBackpackItem(BackpackBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public int maxFm() {
        return tier.maxFm;
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * FmChargeable.fraction(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return tier == BackpackTier.PRIME ? Mth.hsvToRgb(0.76f, 0.6f, 1f) : Mth.hsvToRgb(0.55f, 0.75f, 1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int fm = FmChargeable.get(stack);
        tooltip.add(Component.literal(String.format("Charge: %,d / %,d Fm", fm, maxFm()))
                .withStyle(fm > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
