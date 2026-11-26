package com.sideflowgames.fulguritefusion.block.rubber;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// A plain bucket set down on the ground (right-click the top of a block with an empty bucket).
// Under a Tree Tap it fills with latex, 0..4. Break it, or right-click it with an empty hand, to pick it up:
// full gives a Latex Bucket, otherwise you get your bucket back (any partial latex is lost).
public class TapBucketBlock extends Block {
    public static final MapCodec<TapBucketBlock> CODEC = simpleCodec(TapBucketBlock::new);
    public static final int FULL = 4;
    public static final IntegerProperty LATEX = IntegerProperty.create("latex", 0, FULL);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 9, 12);

    public TapBucketBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LATEX, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LATEX);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess scheduledTicks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource shapeRandom) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, scheduledTicks, pos, direction, neighborPos, neighbor, shapeRandom);
    }

    public static ItemStack contents(BlockState state) {
        return new ItemStack(state.getValue(LATEX) >= FULL ? ModItems.LATEX_BUCKET.get() : Items.BUCKET);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            int latex = state.getValue(LATEX);
            if (latex > 0 && latex < FULL && !player.isShiftKeyDown()) {
                player.sendOverlayMessage(Component.literal("Latex: " + latex + "/" + FULL + " (sneak + right-click to take it now; the latex is lost)"));
                return InteractionResult.SUCCESS;
            }
            level.removeBlock(pos, false);
            ItemStack item = contents(state);
            if (!player.getInventory().add(item)) player.drop(item, false);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6f, latex >= FULL ? 0.7f : 1.2f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Items.BUCKET);
    }
}
