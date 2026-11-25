package com.sideflowgames.fulguritefusion.item;

import com.sideflowgames.fulguritefusion.ModArmorMaterials;
import com.sideflowgames.fulguritefusion.ModDataComponents;
import com.sideflowgames.fulguritefusion.event.StormArmorEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// Titan Prime armor: a Prime piece and the Titan piece of the same type fused in the Titan Forge with Stormreach
// ores. It counts as Titan armor (so every Titan, Prime and Stormforged ability still works) with more protection
// and toughness, a 32,000,000 Fm store, and 4 hearts per piece (Titan's 2.5 plus 1.5).
public class TitanPrimeArmorItem extends TitanArmorItem {
    public static final int MAX_FM = 32_000_000;

    public TitanPrimeArmorItem(Type type, Properties properties) {
        super(ModArmorMaterials.TITAN_PRIME, type, properties);
    }

    @Override
    public int maxFm() {
        return MAX_FM;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Titan Prime").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        int fm = FmChargeable.get(stack);
        tooltip.add(Component.literal(String.format("Charge: %,d / %,d Fm", fm, MAX_FM))
                .withStyle(fm > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
        tooltip.add(Component.literal("+4 hearts").withStyle(ChatFormatting.GRAY));
        switch (getType()) {
            case HELMET -> {
                int h = stack.getOrDefault(ModDataComponents.HYDROGEN.get(), 0);
                tooltip.add(Component.literal("Breathe underwater (hydrogen tank " + (h * 100 / StormArmorEvents.HYDROGEN_MAX) + "% full)")
                        .withStyle(ChatFormatting.GRAY));
            }
            case BOOTS -> tooltip.add(Component.literal("No fall damage").withStyle(ChatFormatting.GRAY));
            default -> {}
        }
        tooltip.add(Component.literal("Counts as Titan armor: every Titan, Prime").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("and Stormforged set bonus still applies").withStyle(ChatFormatting.GOLD));
    }
}
