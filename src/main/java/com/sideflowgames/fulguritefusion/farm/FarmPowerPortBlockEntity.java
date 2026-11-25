package com.sideflowgames.fulguritefusion.farm;

import com.sideflowgames.fulguritefusion.power.FmHandler;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Passes Fm into the Shock Farm Controller on the same edge ring (up to 6 blocks away).
public class FarmPowerPortBlockEntity extends BlockEntity implements FmHandler {
    @Nullable private BlockPos controller;

    public FarmPowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(FarmBlocks.POWER_PORT_BE.get(), pos, state);
    }

    @Nullable
    private ShockFarmControllerBlockEntity farm() {
        if (level == null) return null;
        if (controller != null && level.getBlockEntity(controller) instanceof ShockFarmControllerBlockEntity c
                && c.acceptsPort(worldPosition)) return c;
        controller = null;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                BlockPos p = worldPosition.offset(dx, 0, dz);
                if (level.getBlockEntity(p) instanceof ShockFarmControllerBlockEntity c && c.acceptsPort(worldPosition)) {
                    controller = p;
                    return c;
                }
            }
        }
        return null;
    }

    @Override
    public int receiveFm(int amount, boolean simulate) {
        ShockFarmControllerBlockEntity c = farm();
        return c == null ? 0 : c.receiveFm(amount, simulate);
    }

    @Override
    public int getFm() {
        ShockFarmControllerBlockEntity c = farm();
        return c == null ? 0 : c.getFm();
    }

    @Override
    public int getMaxFm() {
        ShockFarmControllerBlockEntity c = farm();
        return c == null ? 0 : c.getMaxFm();
    }
}
