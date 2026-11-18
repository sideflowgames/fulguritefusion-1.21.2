package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;

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

// Collider Injector: fires a Spark Cell into the collider. With a Storm Collider in the middle, each injector
// also holds one material; the cell picks it up on the way through, and the two materials make the pairing.
// (A plain Mini Collider ignores injector materials.) Load by hand or with a hopper/pipe.
public class ColliderInjectorBlockEntity extends BlockEntity {
    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // Hoppers and pipes can only put materials in.
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
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    };

    public ColliderInjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.COLLIDER_INJECTOR_BE.get(), pos, state);
    }

    public ItemStack peek() {
        return items.getStackInSlot(0);
    }

    public ItemStack takeOne() {
        return items.extractItem(0, 1, false);
    }

    public ItemStack insert(ItemStack stack) {
        return items.insertItem(0, stack, false);
    }

    public ItemStack takeAll() {
        return items.extractItem(0, 64, false);
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return insertOnly;
    }

    public String statusText() {
        ItemStack stack = peek();
        return "Collider Injector: " + (stack.isEmpty() ? "empty" : stack.getCount() + "x " + stack.getHoverName().getString());
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
