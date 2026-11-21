package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.power.FmHandler;
import com.sideflowgames.fulguritefusion.power.FmSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Gives up to RATE Fm every tick, from nothing: pushed into machines on all six faces (as the first machine
// of a daisy chain), and drawn by a wire network through a Machine Connector on top.
public class CreativePowerSourceBlockEntity extends BlockEntity implements FmSource {
    public static final int RATE = 4086;

    private long budgetTick = -1;
    private int used;

    public CreativePowerSourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CREATIVE_POWER_SOURCE_BE.get(), pos, state);
    }

    private int left() {
        long now = level.getGameTime();
        if (now != budgetTick) {
            budgetTick = now;
            used = 0;
        }
        return RATE - used;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreativePowerSourceBlockEntity be) {
        for (Direction dir : Direction.values()) {
            int left = be.left();
            if (left <= 0) return;
            if (level.getBlockEntity(pos.relative(dir)) instanceof FmHandler machine && machine.acceptsFrom(dir.getOpposite())) {
                be.used += machine.receiveFmFromChain(left, 1);
            }
        }
    }

    @Override
    public int extractFm(int max, boolean simulate) {
        if (level == null) return 0;
        int out = Math.max(0, Math.min(max, left()));
        if (!simulate) used += out;
        return out;
    }
}
