package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Forge Power Port: takes the place of an edge casing in the Storm Forge's floor (not a corner) and passes
// any Fm it receives straight into the Forge Anvil. Put it against a battery, a wire, or a reactor power port.
public class ForgePowerPortBlock extends BaseEntityBlock {
    public static final MapCodec<ForgePowerPortBlock> CODEC = simpleCodec(ForgePowerPortBlock::new);

    public ForgePowerPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForgePowerPortBlockEntity(pos, state);
    }
}
