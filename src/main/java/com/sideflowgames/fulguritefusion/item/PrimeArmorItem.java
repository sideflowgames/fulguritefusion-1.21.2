package com.sideflowgames.fulguritefusion.item;

import com.sideflowgames.fulguritefusion.ModArmorMaterials;
import com.sideflowgames.fulguritefusion.ModDataComponents;
import com.sideflowgames.fulguritefusion.event.StormArmorEvents;
import com.sideflowgames.fulguritefusion.prime.PrimeEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// Prime armor (end game, forged from Stormforged armor). Everything Stormforged armor does (it counts as a
// Stormforged set), with more protection and an 8,000,000 Fm store, plus (see prime/PrimeEvents):
//   Boots: no fall damage.
//   Full Prime set: glide like an elytra (jump in mid-air) and hold jump while gliding for an Fm-powered boost;
//   every hit taken blasts the attacker with static discharge.
public class PrimeArmorItem extends StormforgedArmorItem {
    public static final int MAX_FM = 8_000_000;

    public PrimeArmorItem(Type type, Properties properties) {
        super(ModArmorMaterials.PRIME, type, properties);
    }

    @Override
    public int maxFm() {
        return MAX_FM;
    }

    // Gliding: the chestplate acts as an elytra while the whole Prime set is charged.
    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return getType() == Type.CHESTPLATE && PrimeEvents.fullPrimeSet(entity);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!entity.level().isClientSide && flightTicks % 20 == 0) FmChargeable.use(stack, PrimeEvents.GLIDE_FM_PER_SECOND);
        return true;
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13 * FmChargeable.fraction(stack)); }
    @Override public int getBarColor(ItemStack stack) { return PrimeTools.barColor(stack); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PrimeTools.primeLine(tooltip);
        int fm = FmChargeable.get(stack);
        tooltip.add(Component.literal(String.format("Charge: %,d / %,d Fm", fm, MAX_FM))
                .withStyle(fm > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
        switch (getType()) {
            case HELMET -> {
                int h = stack.getOrDefault(ModDataComponents.HYDROGEN.get(), 0);
                tooltip.add(Component.literal("Breathe underwater (hydrogen tank " + (h * 100 / StormArmorEvents.HYDROGEN_MAX) + "% full)")
                        .withStyle(ChatFormatting.GRAY));
            }
            case BOOTS -> tooltip.add(Component.literal("No fall damage").withStyle(ChatFormatting.GRAY));
            default -> {}
        }
        tooltip.add(Component.literal("Full set: Stormforged set bonuses, plus").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal(" glide (jump in mid-air), hold jump to boost").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal(" static discharge on anything that hits you").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
