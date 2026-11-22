package com.sideflowgames.fulguritefusion.harvester;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.power.FmHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

// The Harvester Tank and Harvester Power Port: they remember which harvester they belong to and pass things
// through to it. The tank lets pipes (and buckets) drain latex; the power port takes Fm (a Machine Connector can
// sit on it, and touching machines can feed it).
public class HarvesterPartBlockEntity extends BlockEntity implements FmHandler {
    @Nullable private BlockPos controllerPos;

    public HarvesterPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.HARVESTER_PART_BE.get(), pos, state);
    }

    public void link(@Nullable BlockPos controller) {
        controllerPos = controller;
        setChanged();
    }

    @Nullable
    public BlockPos controllerPos() {
        return controllerPos;
    }

    public boolean free(BlockPos controller) {
        return controllerPos == null || controllerPos.equals(controller);
    }

    @Nullable
    public LatexHarvesterBlockEntity controller() {
        return level != null && controllerPos != null && level.getBlockEntity(controllerPos) instanceof LatexHarvesterBlockEntity h
                && h.isFormed() ? h : null;
    }

    private boolean isPort() {
        return getBlockState().getBlock() instanceof HarvesterPowerPortBlock;
    }

    // ---------- Power (power port only) ----------

    @Override
    public boolean acceptsConnector() {
        return isPort();
    }

    @Override
    public int receiveFm(int amount, boolean simulate) {
        LatexHarvesterBlockEntity h = controller();
        return isPort() && h != null ? h.receiveFm(amount, simulate) : 0;
    }

    @Override
    public int receiveFmFromChain(int amount, int depth) {
        LatexHarvesterBlockEntity h = controller();
        return isPort() && h != null ? h.receiveFmFromChain(amount, depth) : 0;
    }

    @Override
    public void touchChain(int depth) {
        LatexHarvesterBlockEntity h = controller();
        if (isPort() && h != null) h.touchChain(depth);
    }

    @Override
    public int getFm() {
        LatexHarvesterBlockEntity h = controller();
        return isPort() && h != null ? h.getFm() : 0;
    }

    @Override
    public int getMaxFm() {
        LatexHarvesterBlockEntity h = controller();
        return isPort() && h != null ? h.getMaxFm() : 1;
    }

    // ---------- Latex out (tank only): drain only ----------

    @Nullable
    public IFluidHandler fluidHandler() {
        return isPort() ? null : drain;
    }

    private final IFluidHandler drain = new IFluidHandler() {
        @Override public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            LatexHarvesterBlockEntity h = controller();
            return h == null ? FluidStack.EMPTY : h.tank().getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return LatexHarvesterBlockEntity.CAPACITY;
        }

        @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            LatexHarvesterBlockEntity h = controller();
            return h == null ? FluidStack.EMPTY : h.tank().drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            LatexHarvesterBlockEntity h = controller();
            return h == null ? FluidStack.EMPTY : h.tank().drain(maxDrain, action);
        }
    };

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) tag.put("Controller", NbtUtils.writeBlockPos(controllerPos));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controllerPos = NbtUtils.readBlockPos(tag, "Controller").orElse(null);
    }
}
