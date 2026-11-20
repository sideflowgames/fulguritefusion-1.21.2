package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

// The console's working parts: Console Power Port (Fm out), Console Data Port (a Data Cable plugs into its back)
// and Console Fuel Input (hoppers/pipes feed fuel in). FORMED = part of a formed console; forming also turns
// them to face the same way as the console. Right-click: the console's status.
public class ConsolePortBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ConsolePortBlock> CODEC = simpleCodec(ConsolePortBlock::new);

    public ConsolePortBlock(Properties properties) {
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return ReactorConsoleBlockEntity.showStatusFrom(level, pos, player);
    }
}
