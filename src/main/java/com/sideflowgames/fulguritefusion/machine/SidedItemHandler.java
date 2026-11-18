package com.sideflowgames.fulguritefusion.machine;

import java.util.function.BiPredicate;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

// What a hopper or pipe sees on one face of a machine:
// a Pull face only accepts items, a Push face only gives items out.
public class SidedItemHandler implements IItemHandler {
    private final IItemHandler base;
    private final boolean allowInsert;
    private final boolean allowExtract;
    private final BiPredicate<Integer, ItemStack> canExtract;

    public SidedItemHandler(IItemHandler base, boolean allowInsert, boolean allowExtract,
                            BiPredicate<Integer, ItemStack> canExtract) {
        this.base = base;
        this.allowInsert = allowInsert;
        this.allowExtract = allowExtract;
        this.canExtract = canExtract;
    }

    @Override
    public int getSlots() {
        return base.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return base.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return allowInsert ? base.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!allowExtract || !canExtract.test(slot, base.getStackInSlot(slot))) return ItemStack.EMPTY;
        return base.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return base.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return allowInsert && base.isItemValid(slot, stack);
    }
}
