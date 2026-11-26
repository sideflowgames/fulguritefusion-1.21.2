package com.sideflowgames.fulguritefusion.pipe;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.Map;

// Fluid Pipe: a copper pipe with a glass window. Joins other Fluid Pipes and anything that holds fluid on that side.
// It pulls from tanks that only give fluid out (a Latex Harvester's tank) and delivers to anything that takes
// fluid in (a Latex Vulcanizer), up to 1 bucket a second for each tank it pulls from. No setup needed.
// The window shows latex while fluid is moving through.
public class FluidPipeBlock extends BaseEntityBlock {
    public static final MapCodec<FluidPipeBlock> CODEC = simpleCodec(FluidPipeBlock::new);
    public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    private static final VoxelShape[] SHAPES = buildShapes();

    public FluidPipeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FILLED, false);
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
        builder.add(FILLED);
    }

    // Joins other pipes, and any block that has a fluid handler on the facing side.
    static boolean connects(LevelAccessor level, BlockPos pos, Direction d) {
        BlockPos n = pos.relative(d);
        BlockState s = level.getBlockState(n);
        if (s.getBlock() instanceof FluidPipeBlock) return true;
        return level instanceof Level l && com.sideflowgames.fulguritefusion.util.LegacyCaps.fluidsAt(l, n, d.getOpposite()) != null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values()) state = state.setValue(SIDES.get(d), connects(context.getLevel(), context.getClickedPos(), d));
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess scheduledTicks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource shapeRandom) {
        return state.setValue(SIDES.get(direction), connects((net.minecraft.world.level.LevelAccessor) level, pos, direction));
    }

    // A machine next to it forming or breaking apart (its fluid handler comes and goes) without a shape change.
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, net.minecraft.world.level.redstone.Orientation orientation, boolean moving) {
        super.neighborChanged(state, level, pos, block, orientation, moving);
        BlockState now = state;
        for (Direction d : Direction.values()) now = now.setValue(SIDES.get(d), connects(level, pos, d));
        if (now != state) level.setBlock(pos, now, Block.UPDATE_CLIENTS);
    }

    // Clears the window once nothing has flowed for a moment.
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FILLED)) return;
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe && level.getGameTime() - pipe.lastFlow() < 30) {
            level.scheduleTick(pos, this, 20);
        } else {
            level.setBlock(pos, state.setValue(FILLED, false), Block.UPDATE_CLIENTS);
        }
    }

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

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.FLUID_PIPE_BE.get(), FluidPipeBlockEntity::serverTick);
    }
}
