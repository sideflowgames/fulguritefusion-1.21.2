package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Holds nothing; it's here so DataCableRenderer can draw the round pipe with its curved elbows.
public class DataCableBlockEntity extends BlockEntity {
    public DataCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.DATA_CABLE_BE.get(), pos, state);
    }
}
