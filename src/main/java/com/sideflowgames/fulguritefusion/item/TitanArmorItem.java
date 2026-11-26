package com.sideflowgames.fulguritefusion.item;

import net.minecraft.world.item.equipment.ArmorType;

import com.sideflowgames.fulguritefusion.ModArmorMaterials;
import com.sideflowgames.fulguritefusion.ModDataComponents;
import com.sideflowgames.fulguritefusion.event.StormArmorEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// Titan armor: dropped by the Storm Titan, one piece per kill. It counts as Prime armor (so a full set has every
// Prime and Stormforged ability) and holds 16,000,000 Fm. Each piece adds 2.5 hearts. A full Titan set adds
// (see event/TitanArmorEvents):
//   - Charged regeneration: heal 1 health every 2 seconds while it's storming (always, in the Stormreach).
//   - Conduit aura: every 2 seconds, monsters within 6 blocks take 4 damage from arcs of lightning.
//   - Can't be knocked back, immune to fire and lava.
//   - Static dash: double-tap a movement key to dash a few blocks that way, leaving a trail that hurts monsters
//     (3 second cooldown).
public class TitanArmorItem extends PrimeArmorItem {
    public static final int MAX_FM = 16_000_000;

    public TitanArmorItem(ArmorType type, Properties properties) {
        this(ModArmorMaterials.TITAN, type, properties);
    }

    protected TitanArmorItem(net.minecraft.world.item.equipment.ArmorMaterial material, ArmorType type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public int maxFm() {
        return MAX_FM;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag flag) {
        java.util.List<Component> tooltip = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut);
        tooltip.add(Component.literal("Titan").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        int fm = FmChargeable.get(stack);
        tooltip.add(Component.literal(String.format("Charge: %,d / %,d Fm", fm, MAX_FM))
                .withStyle(fm > 0 ? ChatFormatting.GOLD : ChatFormatting.RED));
        tooltip.add(Component.literal("+2.5 hearts").withStyle(ChatFormatting.GRAY));
        switch (getType()) {
            case HELMET -> {
                int h = stack.getOrDefault(ModDataComponents.HYDROGEN.get(), 0);
                tooltip.add(Component.literal("Breathe underwater (hydrogen tank " + (h * 100 / StormArmorEvents.HYDROGEN_MAX) + "% full)")
                        .withStyle(ChatFormatting.GRAY));
            }
            case BOOTS -> tooltip.add(Component.literal("No fall damage").withStyle(ChatFormatting.GRAY));
            default -> {}
        }
        tooltip.add(Component.literal("Full set: every Prime bonus, plus").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" regenerate while it's storming").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" a lightning aura that strikes nearby monsters").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" no knockback, immune to fire and lava").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" static dash: double-tap a movement key").withStyle(ChatFormatting.GOLD));
    }
}
