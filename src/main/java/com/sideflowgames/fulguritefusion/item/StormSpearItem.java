package com.sideflowgames.fulguritefusion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// Storm Spear (mid game, forged from a Fulgurite Spear): never breaks, runs on stored Fm.
// Melee 11 (+4 lightning while charged). Thrown 13; a charged throw (2,000 Fm) calls lightning down where it
// lands, hitting everything nearby except you.
public class StormSpearItem extends SpearItem implements FmChargeable {
    public static final float MELEE = 11, THROWN = 13, LIGHTNING = 4;
    public static final int THROW_COST = 2000, HIT_COST = 300;

    public StormSpearItem(Properties properties) {
        super(properties);
    }

    @Override
    public int maxFm() {
        return StormforgedTools.MAX_FM;
    }

    @Override
    public boolean isStorm() {
        return true;
    }

    @Override
    public float lightningBonus(ItemStack stack) {
        return FmChargeable.hasCharge(stack) ? LIGHTNING : 0;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity attacker) {
        FmChargeable.use(stack, HIT_COST);
        return true;
    }

    @Override
    protected boolean onThrow(ItemStack stack, Player player) {
        return FmChargeable.use(stack, THROW_COST) >= THROW_COST || player.getAbilities().instabuild;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override public boolean isBarVisible(ItemStack s) { return true; }
    @Override public int getBarWidth(ItemStack s) { return StormforgedTools.barWidth(s); }
    @Override public int getBarColor(ItemStack s) { return StormforgedTools.barColor(s); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("+" + (int) LIGHTNING + " lightning damage; thrown, it calls lightning where it lands")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Hold right-click, then let go to throw").withStyle(ChatFormatting.GRAY));
        StormforgedTools.tooltip(stack, tooltip, false);
    }
}
