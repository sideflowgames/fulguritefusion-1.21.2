package com.sideflowgames.fulguritefusion.item;

import com.sideflowgames.fulguritefusion.ModDataComponents;

import net.minecraft.world.item.ItemStack;

// Forged gear that runs on stored Fm instead of durability. When empty it doesn't break; its abilities stop
// until it is recharged (Fulgurite Charger, or a lightning strike for worn Stormforged armor).
public interface FmChargeable {
    int maxFm();

    static boolean is(ItemStack stack) {
        return stack.getItem() instanceof FmChargeable;
    }

    static int max(ItemStack stack) {
        return stack.getItem() instanceof FmChargeable c ? c.maxFm() : 0;
    }

    static int get(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FM_CHARGE.get(), 0);
    }

    static void set(ItemStack stack, int fm) {
        stack.set(ModDataComponents.FM_CHARGE.get(), Math.max(0, Math.min(max(stack), fm)));
    }

    static boolean hasCharge(ItemStack stack) {
        return is(stack) && get(stack) > 0;
    }

    // Takes up to amount; returns how much was taken.
    static int use(ItemStack stack, int amount) {
        int have = get(stack);
        int taken = Math.min(have, amount);
        if (taken > 0) set(stack, have - taken);
        return taken;
    }

    // Adds up to amount; returns how much went in.
    static int add(ItemStack stack, int amount) {
        int have = get(stack);
        int added = Math.min(max(stack) - have, amount);
        if (added > 0) set(stack, have + added);
        return Math.max(0, added);
    }

    static float fraction(ItemStack stack) {
        int max = max(stack);
        return max <= 0 ? 0 : get(stack) / (float) max;
    }
}
