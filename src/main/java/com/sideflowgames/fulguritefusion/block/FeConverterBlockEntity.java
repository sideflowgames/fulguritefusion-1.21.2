package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.power.DaisyChain;
import com.sideflowgames.fulguritefusion.power.FmHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

// Turns Fm into Forge Energy (FE) at 1 Fm = 10 FE and pushes it out of the front face.
// FE cables that pull power can also take it from the front.
public class FeConverterBlockEntity extends BlockEntity implements FmHandler {
    public static final int MAX_FM = 2000;
    public static final int MAX_FM_PER_TICK = 100; // = 1000 FE per tick

    private int fm;
    private final DaisyChain chain = new DaisyChain();
    private int remainderFe; // FE left over from a partly-spent Fm, so nothing is lost to rounding

    // What FE cables see on the front face: extract-only.
    private final IEnergyStorage feOutput = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return takeFe(toExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, (long) fm * Config.FE_PER_FM.get() + remainderFe);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, (long) MAX_FM * Config.FE_PER_FM.get());
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    public FeConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FE_CONVERTER_BE.get(), pos, state);
    }

    // Spend stored Fm (10 FE each) to hand out up to 'maxFe' FE.
    private int takeFe(int maxFe, boolean simulate) {
        int FE_PER_FM = Config.FE_PER_FM.get();
        int limit = Math.min(maxFe, MAX_FM_PER_TICK * FE_PER_FM);
        int available = fm * FE_PER_FM + remainderFe;
        int given = Math.min(limit, available);
        if (!simulate && given > 0) {
            int total = available - given;
            fm = total / FE_PER_FM;
            remainderFe = total % FE_PER_FM;
            setChanged();
        }
        return given;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FeConverterBlockEntity be) {
        Direction front = state.getValue(FeConverterBlock.FACING);
        shareChain(level, pos, front, be);
        if (be.fm <= 0 && be.remainderFe <= 0) return;
        IEnergyStorage target = com.sideflowgames.fulguritefusion.util.LegacyCaps.energyAt(level, pos.relative(front), front.getOpposite());
        if (target != null && target.canReceive()) {
            int offer = be.takeFe(Integer.MAX_VALUE, true);
            int accepted = target.receiveEnergy(offer, false);
            if (accepted > 0) be.takeFe(accepted, false);
        }
    }

    // Fm comes in on every face except the front.
    @Override
    public int receiveFm(int amount, boolean simulate) {
        chain.touch(level, 0); // fed directly by a battery or wire: start of a daisy chain
        return accept(amount, simulate);
    }

    @Override
    public int receiveFmFromChain(int amount, int depth) {
        chain.touch(level, depth);
        return accept(amount, false);
    }

    @Override
    public void touchChain(int depth) {
        chain.touch(level, depth);
    }

    private int accept(int amount, boolean simulate) {
        int accepted = Math.max(0, Math.min(amount, MAX_FM - fm));
        if (!simulate && accepted > 0) {
            fm += accepted;
            setChanged();
        }
        return accepted;
    }

    // Daisy chain: pass power to touching machines (never out of the front, stops after 8 machines).
    private static void shareChain(Level level, BlockPos pos, Direction front, FeConverterBlockEntity be) {
        int shared = be.chain.share(level, pos, be, be.fm, front);
        if (shared > 0) {
            be.fm -= shared;
            be.setChanged();
        }
    }

    @Override
    public int getFm() {
        return fm;
    }

    @Override
    public int getMaxFm() {
        return MAX_FM;
    }

    // 26.1: FE cables use the transaction-based EnergyHandler. Extract-only, same rules as feOutput;
    // an undone transaction puts the Fm back exactly.
    private final net.neoforged.neoforge.transfer.transaction.SnapshotJournal<int[]> feJournal =
            new net.neoforged.neoforge.transfer.transaction.SnapshotJournal<>() {
                @Override
                protected int[] createSnapshot() {
                    return new int[] {fm, remainderFe};
                }

                @Override
                protected void revertToSnapshot(int[] snapshot) {
                    fm = snapshot[0];
                    remainderFe = snapshot[1];
                }

                @Override
                protected void onRootCommit(int[] original) {
                    setChanged();
                }
            };

    private final net.neoforged.neoforge.transfer.energy.EnergyHandler feHandler = new net.neoforged.neoforge.transfer.energy.EnergyHandler() {
        @Override
        public long getAmountAsLong() {
            return feOutput.getEnergyStored();
        }

        @Override
        public long getCapacityAsLong() {
            return feOutput.getMaxEnergyStored();
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            if (amount <= 0 || takeFe(amount, true) <= 0) return 0;
            feJournal.updateSnapshots(transaction);
            return takeFe(amount, false);
        }
    };

    public net.neoforged.neoforge.transfer.energy.EnergyHandler getEnergyHandler(Direction side) {
        return side == getBlockState().getValue(FeConverterBlock.FACING) ? feHandler : null;
    }

    public String statusText() {
        return "FE Converter: " + fm + " / " + MAX_FM + " Fm (= " + feOutput.getEnergyStored() + " FE ready)";
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveFeConverterBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveFeConverterBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        tag.putInt("Fm", fm);
        tag.putInt("RemainderFe", remainderFe);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadFeConverterBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadFeConverterBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        fm = tag.getIntOr("Fm", 0);
        remainderFe = tag.getIntOr("RemainderFe", 0);
    }
}
