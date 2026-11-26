package com.sideflowgames.fulguritefusion.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Data Cable: a round metal pipe carrying fuel signals from the Reactor Control Console's data ports to the
// Ring Injection Ports. Free-standing (needs nothing under it). It joins other cables, Console Data Ports and
// Ring Injection Ports on any side; the pipe itself is drawn by DataCableRenderer (straight runs, smooth curved
// elbows at 90 degree turns, a hub where three or more meet).
public class DataCableBlock extends BaseEntityBlock {
    public static final MapCodec<DataCableBlock> CODEC = simpleCodec(DataCableBlock::new);
    private static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape[] SHAPES = buildShapes();

    public DataCableBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : SIDES.values()) state = state.setValue(side, false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (BooleanProperty side : SIDES.values()) builder.add(side);
    }

    public static boolean connectsTo(BlockState state) {
        return state.getBlock() instanceof DataCableBlock
                || state.is(ModBlocks.CONSOLE_DATA_PORT.get())
                || state.is(ModBlocks.RING_INJECTION_PORT.get());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values()) {
            BlockState n = context.getLevel().getBlockState(context.getClickedPos().relative(d));
            state = state.setValue(SIDES.get(d), connectsTo(n));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess scheduledTicks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource shapeRandom) {
        return state.setValue(SIDES.get(direction), connectsTo(neighbor));
    }

    // ---------- Shape: a 6-pixel-wide core plus an arm per connection ----------

    private static VoxelShape[] buildShapes() {
        double a = 5, b = 11;
        VoxelShape core = Block.box(a, a, a, b, b, b);
        VoxelShape[] arms = new VoxelShape[6];
        for (Direction d : Direction.values()) {
            double x0 = a, y0 = a, z0 = a, x1 = b, y1 = b, z1 = b;
            switch (d) {
                case DOWN -> y0 = 0;
                case UP -> y1 = 16;
                case NORTH -> z0 = 0;
                case SOUTH -> z1 = 16;
                case WEST -> x0 = 0;
                case EAST -> x1 = 16;
            }
            arms[d.ordinal()] = Block.box(x0, y0, z0, x1, y1, z1);
        }
        VoxelShape[] shapes = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = core;
            for (Direction d : Direction.values()) if ((mask & (1 << d.ordinal())) != 0) s = Shapes.or(s, arms[d.ordinal()]);
            shapes[mask] = s;
        }
        return shapes;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction d : Direction.values()) if (state.getValue(SIDES.get(d))) mask |= 1 << d.ordinal();
        return SHAPES[mask];
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    // The block model is empty; the renderer draws the pipe.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DataCableBlockEntity(pos, state);
    }
}
