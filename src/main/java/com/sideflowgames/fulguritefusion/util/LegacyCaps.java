package com.sideflowgames.fulguritefusion.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

// 26.1 port helper. NeoForge replaced the IItemHandler / IFluidHandler / IEnergyStorage capabilities with the
// transaction-based ResourceHandler / EnergyHandler. The mod's machines still use the old handler classes inside;
// these adapters connect them to the new capabilities both ways.
//
// Offering a handler: the new system can undo a change (when a transaction is aborted), which the old handlers
// can't do through their restricted views (an insert-only port can't give an item back). So each adapter is
// given the real storage behind the view, takes a copy of it before the first change in a transaction, and puts
// that copy back if the transaction is undone. That keeps hoppers and pipes from duplicating or losing items.
public final class LegacyCaps {
    private LegacyCaps() {}

    // ---------- using other blocks' capabilities with the old API ----------

    @Nullable
    public static IItemHandler itemsAt(Level level, BlockPos pos, @Nullable Direction side) {
        ResourceHandler<ItemResource> h = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        return h == null ? null : IItemHandler.of(h);
    }

    @Nullable
    public static IFluidHandler fluidsAt(Level level, BlockPos pos, @Nullable Direction side) {
        ResourceHandler<FluidResource> h = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        return h == null ? null : IFluidHandler.of(h);
    }

    @Nullable
    public static IEnergyStorage energyAt(Level level, BlockPos pos, @Nullable Direction side) {
        var h = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
        return h == null ? null : IEnergyStorage.of(h);
    }

    // ---------- offering the mod's handlers through the new capabilities ----------

    @Nullable
    public static ResourceHandler<ItemResource> items(@Nullable IItemHandler view, IItemHandlerModifiable... storage) {
        if (view == null) return null;
        IItemHandlerModifiable[] backing = storage.length > 0 ? storage
                : view instanceof IItemHandlerModifiable m ? new IItemHandlerModifiable[] {m} : new IItemHandlerModifiable[0];
        return new ItemAdapter(view, backing);
    }

    @Nullable
    public static ResourceHandler<FluidResource> fluids(@Nullable IFluidHandler view, FluidTank... storage) {
        if (view == null) return null;
        return new FluidAdapter(view, storage);
    }

    private static final class ItemAdapter implements ResourceHandler<ItemResource> {
        private final IItemHandler view;
        private final IItemHandlerModifiable[] backing;
        private final SnapshotJournal<ItemStack[][]> journal = new SnapshotJournal<>() {
            @Override
            protected ItemStack[][] createSnapshot() {
                ItemStack[][] copy = new ItemStack[backing.length][];
                for (int b = 0; b < backing.length; b++) {
                    copy[b] = new ItemStack[backing[b].getSlots()];
                    for (int i = 0; i < copy[b].length; i++) copy[b][i] = backing[b].getStackInSlot(i).copy();
                }
                return copy;
            }

            @Override
            protected void revertToSnapshot(ItemStack[][] snapshot) {
                for (int b = 0; b < backing.length; b++) {
                    for (int i = 0; i < snapshot[b].length; i++) backing[b].setStackInSlot(i, snapshot[b][i].copy());
                }
            }
        };

        ItemAdapter(IItemHandler view, IItemHandlerModifiable[] backing) {
            this.view = view;
            this.backing = backing;
        }

        @Override public int size() { return view.getSlots(); }
        @Override public ItemResource getResource(int index) { return ItemResource.of(view.getStackInSlot(index)); }
        @Override public long getAmountAsLong(int index) { return view.getStackInSlot(index).getCount(); }
        @Override public long getCapacityAsLong(int index, ItemResource resource) {
            int limit = view.getSlotLimit(index);
            return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
        }
        @Override public boolean isValid(int index, ItemResource resource) {
            return !resource.isEmpty() && view.isItemValid(index, resource.toStack());
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) return 0;
            int fits = amount - view.insertItem(index, resource.toStack(amount), true).getCount();
            if (fits <= 0) return 0;
            journal.updateSnapshots(transaction);
            return amount - view.insertItem(index, resource.toStack(amount), false).getCount();
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0 || !resource.matches(view.getStackInSlot(index))) return 0;
            int can = view.extractItem(index, amount, true).getCount();
            if (can <= 0) return 0;
            journal.updateSnapshots(transaction);
            return view.extractItem(index, can, false).getCount();
        }
    }

    private static final class FluidAdapter implements ResourceHandler<FluidResource> {
        private final IFluidHandler view;
        private final FluidTank[] backing;
        private final SnapshotJournal<FluidStack[]> journal = new SnapshotJournal<>() {
            @Override
            protected FluidStack[] createSnapshot() {
                FluidStack[] copy = new FluidStack[backing.length];
                for (int b = 0; b < backing.length; b++) copy[b] = backing[b].getFluid().copy();
                return copy;
            }

            @Override
            protected void revertToSnapshot(FluidStack[] snapshot) {
                for (int b = 0; b < backing.length; b++) backing[b].setFluid(snapshot[b].copy());
            }
        };

        FluidAdapter(IFluidHandler view, FluidTank[] backing) {
            this.view = view;
            this.backing = backing;
        }

        @Override public int size() { return view.getTanks(); }
        @Override public FluidResource getResource(int index) { return FluidResource.of(view.getFluidInTank(index)); }
        @Override public long getAmountAsLong(int index) { return view.getFluidInTank(index).getAmount(); }
        @Override public long getCapacityAsLong(int index, FluidResource resource) { return view.getTankCapacity(index); }
        @Override public boolean isValid(int index, FluidResource resource) {
            return !resource.isEmpty() && view.isFluidValid(index, resource.toStack(1));
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) return 0;
            if (view.fill(resource.toStack(amount), IFluidHandler.FluidAction.SIMULATE) <= 0) return 0;
            journal.updateSnapshots(transaction);
            return view.fill(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) return 0;
            if (view.drain(resource.toStack(amount), IFluidHandler.FluidAction.SIMULATE).isEmpty()) return 0;
            journal.updateSnapshots(transaction);
            return view.drain(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
    }
}
