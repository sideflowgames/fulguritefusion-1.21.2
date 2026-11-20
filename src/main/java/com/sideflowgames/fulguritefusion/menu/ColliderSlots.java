package com.sideflowgames.fulguritefusion.menu;

import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.block.ColliderInputBlockEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

// Slot helpers for the collider menus.
final class ColliderSlots {
    private ColliderSlots() {}

    // Client-side stand-ins that accept the same items as the real ones (keeps clicks in sync).
    static ItemStackHandler clientCells() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(ModItems.SPARK_CELL.get());
            }

            @Override
            public int getSlotLimit(int slot) {
                return ColliderInputBlockEntity.MAX_CELLS;
            }
        };
    }

    static ItemStackHandler clientAny(int slots) {
        return new ItemStackHandler(slots);
    }

    // A slot that's there but can't be used (ring incomplete, so there's no injector/input to hold the item).
    static SlotItemHandler disabled(int x, int y) {
        return new SlotItemHandler(new ItemStackHandler(1), 0, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public boolean isActive() {
                return false;
            }
        };
    }

    // Take-only output slot.
    static SlotItemHandler output(IItemHandler handler, int index, int x, int y) {
        return new SlotItemHandler(handler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

}
