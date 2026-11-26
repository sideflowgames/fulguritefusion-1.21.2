package com.sideflowgames.fulguritefusion.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

// Moves items between a machine and whatever inventory (chest, hopper, pipe...) touches one of its faces.
public final class ItemTransfer {
    private ItemTransfer() {}

    private static IItemHandler neighbour(Level level, BlockPos pos, Direction dir) {
        return com.sideflowgames.fulguritefusion.util.LegacyCaps.itemsAt(level, pos.relative(dir), dir.getOpposite());
    }

    // Push the stack in one of our slots out into the neighbour.
    public static void push(Level level, BlockPos pos, Direction dir, IItemHandler ours, int slot) {
        ItemStack stack = ours.getStackInSlot(slot);
        if (stack.isEmpty()) return;
        IItemHandler target = neighbour(level, pos, dir);
        if (target == null) return;
        ItemStack left = ItemHandlerHelper.insertItem(target, stack.copy(), false);
        int moved = stack.getCount() - left.getCount();
        if (moved > 0) ours.extractItem(slot, moved, false);
    }

    // Pull items the slot will accept out of the neighbour.
    public static void pull(Level level, BlockPos pos, Direction dir, IItemHandler ours, int slot) {
        IItemHandler source = neighbour(level, pos, dir);
        if (source == null) return;
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack offered = source.extractItem(i, 64, true);
            if (offered.isEmpty()) continue;
            ItemStack left = ours.insertItem(slot, offered, true);
            int canTake = offered.getCount() - left.getCount();
            if (canTake > 0) {
                ours.insertItem(slot, source.extractItem(i, canTake, false), false);
                return;
            }
        }
    }
}
