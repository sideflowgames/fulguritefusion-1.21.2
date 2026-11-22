package com.sideflowgames.fulguritefusion.block.rubber;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

// Tree Tap: goes on the side of a Rubber Log, spout facing out. A bucket placed on the ground below it
// (up to 6 blocks down, through air) slowly fills with latex: one drip-level every treeTapSeconds (default 60),
// full at 4. Break or right-click the full bucket to get a Latex Bucket.
public class TreeTapBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TreeTapBlock> CODEC = simpleCodec(TreeTapBlock::new);
    private static final int REACH = 6;

    // Spout and back plate, for each facing (facing = the way the spout points).
    private static final VoxelShape NORTH = Block.box(6, 6, 6, 10, 12, 16);
    private static final VoxelShape SOUTH = Block.box(6, 6, 0, 10, 12, 10);
    private static final VoxelShape WEST = Block.box(6, 6, 6, 16, 12, 10);
    private static final VoxelShape EAST = Block.box(0, 6, 6, 10, 12, 10);

    public TreeTapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isHorizontal()) {
            BlockState state = defaultBlockState().setValue(FACING, face);
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) return state;
        }
        for (Direction d : context.getNearestLookingDirections()) {
            if (!d.getAxis().isHorizontal()) continue;
            BlockState state = defaultBlockState().setValue(FACING, d.getOpposite());
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) return state;
        }
        return null;
    }

    // The log it's tapped into is behind the spout.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).is(RubberTree.LOGS);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
        if (!old.is(this)) level.scheduleTick(pos, this, interval());
    }

    private static int interval() {
        return Config.TREE_TAP_SECONDS.get() * 20;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos bucket = findBucket(level, pos);
        if (bucket != null) {
            BlockState b = level.getBlockState(bucket);
            int latex = b.getValue(TapBucketBlock.LATEX);
            if (latex < TapBucketBlock.FULL) level.setBlock(bucket, b.setValue(TapBucketBlock.LATEX, latex + 1), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, interval());
    }

    // The first bucket straight below, through air or other see-through, non-solid blocks.
    @Nullable
    static BlockPos findBucket(BlockGetter level, BlockPos tap) {
        BlockPos.MutableBlockPos p = tap.mutable();
        for (int i = 0; i < REACH; i++) {
            p.move(Direction.DOWN);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof TapBucketBlock) return p.immutable();
            if (!s.getCollisionShape(level, p).isEmpty()) return null;
        }
        return null;
    }

    // Latex drips from the spout while there's a bucket below to catch it.
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() > 0.25f) return;
        BlockPos bucket = findBucket(level, pos);
        if (bucket == null || level.getBlockState(bucket).getValue(TapBucketBlock.LATEX) >= TapBucketBlock.FULL) return;
        // The spout's tip points down at the middle of the tap's block.
        level.addParticle(com.sideflowgames.fulguritefusion.ModParticles.LATEX_DRIP.get(),
                pos.getX() + 0.5, pos.getY() + 0.47, pos.getZ() + 0.5, 0, 0, 0);
    }
}
