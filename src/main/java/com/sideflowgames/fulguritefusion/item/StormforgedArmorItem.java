package com.sideflowgames.fulguritefusion.item;

import com.sideflowgames.fulguritefusion.ModArmorMaterials;
import com.sideflowgames.fulguritefusion.ModDataComponents;
import com.sideflowgames.fulguritefusion.event.StormArmorEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// Stormforged armor (mid game, made in the Storm Forge). Netherite-level protection that never breaks.
// Its abilities run on stored Fm (see StormArmorEvents) and stop when the piece is empty:
//   - Full set: immune to lightning (strikes recharge it), knockback resistance, faster movement.
//   - Helmet: breathe underwater by splitting water; the hydrogen is stored (10 minutes) and vented on surfacing.
//   - Boots: fall damage cut to a quarter.
public class StormforgedArmorItem extends Item implements FmChargeable {
    private final ArmorType armorType;
    private final net.minecraft.world.item.equipment.ArmorMaterial material;
    public static final int MAX_FM = 1_000_000;

    public StormforgedArmorItem(ArmorType type, Properties properties) {
        this(ModArmorMaterials.STORMFORGED, type, properties);
    }

    protected StormforgedArmorItem(net.minecraft.world.item.equipment.ArmorMaterial material, ArmorType type, Properties properties) {
        super(ModArmorMaterials.armor(properties, material, type));
        this.armorType = type;
        this.material = material;
    }

    // What ArmorItem used to provide.
    public ArmorType getType() {
        return armorType;
    }

    public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot() {
        return armorType.getSlot();
    }

    public net.minecraft.world.item.equipment.ArmorMaterial getMaterial() {
        return material;
    }

    @Override
    public int maxFm() {
        return MAX_FM;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag flag) {
        java.util.List<Component> tooltip = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut);
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
