package com.sideflowgames.fulguritefusion.pipe;

import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// A Fluid Pipe that touches a tank which only gives fluid out (like a Latex Harvester's tank) pulls from it twice a
// second, half a bucket each time, and hands it straight to the places along its connected pipes that take fluid
// in (like a Latex Vulcanizer). Pipes hold no fluid of their own, so nothing is lost when one is broken.
public class FluidPipeBlockEntity extends BlockEntity {
    public static final int INTERVAL = 10;       // ticks
    public static final int PER_PULL = 500;      // mB, so 1 bucket a second from each source
    public static final int MAX_PIPES = 1024;

    private long lastFlow = -1000;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FLUID_PIPE_BE.get(), pos, state);
    }

    public long lastFlow() {
        return lastFlow;
    }

    private record Target(BlockPos pos, IFluidHandler handler) {}

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % INTERVAL != 0) return;
        for (Direction d : Direction.values()) {
            if (!state.getValue(FluidPipeBlock.SIDES.get(d))) continue;
            BlockPos n = pos.relative(d);
            if (level.getBlockState(n).getBlock() instanceof FluidPipeBlock) continue;
            IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, n, d.getOpposite());
            if (source == null || !givesOnly(source)) continue;
            be.pull(level, n, source);
        }
    }

    // A tank that will give fluid out but won't take any in.
    private static boolean givesOnly(IFluidHandler h) {
        FluidStack sample = h.drain(1, FluidAction.SIMULATE);
        return !sample.isEmpty() && h.fill(sample.copyWithAmount(1000), FluidAction.SIMULATE) == 0;
    }

    private void pull(Level level, BlockPos sourcePos, IFluidHandler source) {
        FluidStack available = source.drain(PER_PULL, FluidAction.SIMULATE);
        if (available.isEmpty()) return;
        Set<BlockPos> pipes = new HashSet<>();
        List<Target> targets = walk(level, sourcePos, pipes);
        int moved = 0;
        for (Target t : targets) {
            int left = available.getAmount() - moved;
            if (left <= 0) break;
            int fits = t.handler().fill(available.copyWithAmount(left), FluidAction.SIMULATE);
            if (fits <= 0) continue;
            FluidStack taken = source.drain(available.copyWithAmount(fits), FluidAction.EXECUTE);
            if (taken.isEmpty()) break;
            moved += t.handler().fill(taken, FluidAction.EXECUTE);
        }
        if (moved > 0) showFlow(level, pipes);
    }

    // Every pipe joined to this one, and every place along them that takes fluid in (not the source itself).
    private List<Target> walk(Level level, BlockPos sourcePos, Set<BlockPos> pipes) {
        List<Target> targets = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        pipes.add(worldPosition);
        while (!queue.isEmpty() && pipes.size() < MAX_PIPES) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof FluidPipeBlock)) continue;
            for (Direction d : Direction.values()) {
                if (!s.getValue(FluidPipeBlock.SIDES.get(d))) continue;
                BlockPos n = p.relative(d);
                if (level.getBlockState(n).getBlock() instanceof FluidPipeBlock) {
                    if (pipes.add(n)) queue.add(n);
                } else if (!n.equals(sourcePos)) {
                    IFluidHandler h = level.getCapability(Capabilities.FluidHandler.BLOCK, n, d.getOpposite());
                    if (h != null && !givesOnly(h)) targets.add(new Target(n, h));
                }
            }
        }
        return targets;
    }

    // Light up the windows along the line.
    private static void showFlow(Level level, Set<BlockPos> pipes) {
        long now = level.getGameTime();
        for (BlockPos p : pipes) {
            if (level.getBlockEntity(p) instanceof FluidPipeBlockEntity pipe) pipe.lastFlow = now;
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof FluidPipeBlock block && !s.getValue(FluidPipeBlock.FILLED)) {
                level.setBlock(p, s.setValue(FluidPipeBlock.FILLED, true), Block.UPDATE_CLIENTS);
                level.scheduleTick(p, block, 40);
            }
        }
    }
}
