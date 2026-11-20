package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

// A Fusion Reactor part with a front: the Transfer Portal, Receiver Portal and Fusion Injector.
// The front faces you when placed; it's the side the charge leaves (portal/injector) or arrives at (receiver).
// FORMED = part of a formed ring: the block hides and the Fusion Collider draws a fitting on the smooth ring instead.
public class ReactorFacingBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ReactorFacingBlock> CODEC = simpleCodec(ReactorFacingBlock::new);

    public ReactorFacingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FormableBlock.FORMED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FormableBlock.FORMED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
