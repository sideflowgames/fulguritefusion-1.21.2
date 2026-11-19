package com.sideflowgames.fulguritefusion.power;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.block.ArcCasingBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

// The daisy-chain rule shared by every machine (everything except Storm Batteries).
// Machines touching each other pass power along, in any mix of types, but a chain stops after 8 machines.
//
// Position in the chain:
//   0 = fed directly by a battery or a wire network
//   0 = the Storm Furnace itself (it counts as the first machine, so the machine touching it is 1)
//   1, 2, 3 ... = each machine further along
// A machine at position 7 (the 8th) still receives power but passes none on.
public final class DaisyChain {
    public static final int SHARE_PER_TICK = 64;
    private static final int NONE = Integer.MAX_VALUE;
    private static final int FRESH_TICKS = 20;

    private int position = NONE;
    private long time;

    // Remember the closest chain position seen in the last second.
    public void touch(@Nullable Level level, int pos) {
        if (level == null) return;
        long now = level.getGameTime();
        if (now - time > FRESH_TICKS || pos <= position) {
            position = pos;
            time = now;
        }
    }

    public boolean canPassOn(@Nullable Level level) {
        return level != null && position != NONE && level.getGameTime() - time <= FRESH_TICKS
                && position + 1 < Config.CHAIN_MAX_MACHINES.get();
    }

    // Even out power with touching machines that are less full. Returns how much Fm was sent,
    // which the caller removes from its own store. 'skip' is a face to leave alone (e.g. a converter's output).
    public int share(Level level, BlockPos pos, FmHandler self, int available, @Nullable Direction skip) {
        if (!canPassOn(level)) return 0;
        int next = position + 1;
        int sent = 0;
        for (Direction dir : Direction.values()) {
            if (dir == skip) continue;
            BlockEntity be = level.getBlockEntity(pos.relative(dir));
            if (!(be instanceof FmHandler other) || other == self || other.isStorage()) continue;
            // A multiblock machine doesn't feed its own casings (that power would just come straight back).
            if (be instanceof ArcCasingBlockEntity casing && casing.belongsTo(pos)) continue;
            other.touchChain(next);
            int left = available - sent;
            if (left <= 0) continue;
            double mine = (double) left / self.getMaxFm();
            double theirs = (double) other.getFm() / other.getMaxFm();
            if (mine <= theirs) continue;
            int gap = (int) ((mine - theirs) * Math.min(self.getMaxFm(), other.getMaxFm()) / 2);
            int offer = Math.min(Math.min(gap, SHARE_PER_TICK), left);
            if (offer > 0) sent += other.receiveFmFromChain(offer, next);
        }
        return sent;
    }
}
