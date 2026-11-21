package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.power.FmHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

// Passes Fm into the Forge Anvil it belongs to (the anvil sits 1 up and up to 2 across from an edge port).
public class ForgePowerPortBlockEntity extends BlockEntity implements FmHandler {
    @Nullable private BlockPos anvil;

    public ForgePowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FORGE_POWER_PORT_BE.get(), pos, state);
    }

    @Nullable
    private StormForgeBlockEntity forge() {
        if (level == null) return null;
        if (anvil != null && level.getBlockEntity(anvil) instanceof StormForgeBlockEntity f) return f;
        anvil = null;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!StormForgeBlockEntity.isEdge(dx, dz)) continue;
                BlockPos p = worldPosition.offset(-dx, 1, -dz);
                if (level.getBlockEntity(p) instanceof StormForgeBlockEntity f) {
                    anvil = p;
                    return f;
                }
            }
        }
        return null;
    }

    @Override
    public int receiveFm(int amount, boolean simulate) {
        StormForgeBlockEntity f = forge();
        return f == null ? 0 : f.receiveFm(amount, simulate);
    }

    @Override
    public int getFm() {
        StormForgeBlockEntity f = forge();
        return f == null ? 0 : f.getFm();
    }

    @Override
    public int getMaxFm() {
        StormForgeBlockEntity f = forge();
        return f == null ? 0 : f.getMaxFm();
    }
}
