package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

// Collider Input: holds the charged Spark Cells the Mini Collider fires, two per shot.
// Load it by hand (right-click with Spark Cells) or with a hopper/pipe from any side.
public class ColliderInputBlockEntity extends BlockEntity {
    public static final int MAX_CELLS = 16;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.SPARK_CELL.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return MAX_CELLS;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // What hoppers and pipes see: insert only, so a hopper underneath can't pull the cells back out.
    private final IItemHandler insertOnly = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return MAX_CELLS;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
    };

    public ColliderInputBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.COLLIDER_INPUT_BE.get(), pos, state);
    }

    public int cellCount() {
        return items.getStackInSlot(0).getCount();
    }

    // Take two cells for a shot. Returns false (and takes nothing) if there aren't two.
    public boolean takePair() {
        if (cellCount() < 2) return false;
        items.extractItem(0, 2, false);
        return true;
    }

    // Insert from the player's hand. Returns what didn't fit.
    public ItemStack insert(ItemStack stack) {
        return items.insertItem(0, stack, false);
    }

    public ItemStack takeAll() {
        return items.extractItem(0, MAX_CELLS, false);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return insertOnly;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
    }
}
