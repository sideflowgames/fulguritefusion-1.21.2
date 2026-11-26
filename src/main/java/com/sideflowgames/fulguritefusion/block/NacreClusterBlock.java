package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Abyssal Nacre Cluster: grows on prismarine touching water near an active Conduit in a deep ocean
// (see event/MaterialSourceEvents). Mine it for Abyssal Nacre. Pops off if the block it grows on goes.
public class NacreClusterBlock extends DirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<NacreClusterBlock> CODEC = simpleCodec(NacreClusterBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public NacreClusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(WATERLOGGED, true));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace()).setValue(WATERLOGGED, water);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    public static boolean growsOn(BlockState state) {
        return state.is(Blocks.PRISMARINE) || state.is(Blocks.PRISMARINE_BRICKS) || state.is(Blocks.DARK_PRISMARINE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> Block.box(3, 0, 3, 13, 7, 13);
            case DOWN -> Block.box(3, 9, 3, 13, 16, 13);
            case NORTH -> Block.box(3, 3, 9, 13, 13, 16);
            case SOUTH -> Block.box(3, 3, 0, 13, 13, 7);
            case WEST -> Block.box(9, 3, 3, 16, 13, 13);
            case EAST -> Block.box(0, 3, 3, 7, 13, 13);
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction f = state.getValue(FACING);
        return growsOn(level.getBlockState(pos.relative(f.getOpposite())));
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess scheduledTicks, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource shapeRandom) {
        if (state.getValue(WATERLOGGED)) scheduledTicks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return dir == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)
                ? Blocks.WATER.defaultBlockState() : state;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
