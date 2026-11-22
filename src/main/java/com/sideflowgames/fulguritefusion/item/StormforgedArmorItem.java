package com.sideflowgames.fulguritefusion.item;

import com.sideflowgames.fulguritefusion.ModArmorMaterials;
import com.sideflowgames.fulguritefusion.ModDataComponents;
import com.sideflowgames.fulguritefusion.event.StormArmorEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// Stormforged armor (mid game, made in the Storm Forge). Netherite-level protection that never breaks.
// Its abilities run on stored Fm (see StormArmorEvents) and stop when the piece is empty:
//   - Full set: immune to lightning (strikes recharge it), knockback resistance, faster movement.
//   - Helmet: breathe underwater by splitting water; the hydrogen is stored (10 minutes) and vented on surfacing.
//   - Boots: fall damage cut to a quarter.
public class StormforgedArmorItem extends ArmorItem implements FmChargeable {
    public static final int MAX_FM = 1_000_000;

    public StormforgedArmorItem(Type type, Properties properties) {
        super(ModArmorMaterials.STORMFORGED, type, properties);
    }

    protected StormforgedArmorItem(net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public int maxFm() {
        return MAX_FM;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int fm = FmChargeable.get(stack);
        tooltip.add(Component.literal(String.format("Charge: %,d / %,d Fm", fm, MAX_FM))
                .withStyle(fm > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        switch (getType()) {
            case HELMET -> {
                int h = stack.getOrDefault(ModDataComponents.HYDROGEN.get(), 0);
                tooltip.add(Component.literal("Breathe underwater (hydrogen tank " + (h * 100 / StormArmorEvents.HYDROGEN_MAX) + "% full)")
                        .withStyle(ChatFormatting.GRAY));
            }
            case BOOTS -> tooltip.add(Component.literal("Fall damage cut to a quarter").withStyle(ChatFormatting.GRAY));
            default -> {}
        }
        tooltip.add(Component.literal("Full set: lightning immunity, knockback resistance, speed").withStyle(ChatFormatting.DARK_AQUA));
    }
}
