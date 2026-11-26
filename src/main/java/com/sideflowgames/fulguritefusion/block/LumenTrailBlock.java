package com.sideflowgames.fulguritefusion.block;

import net.minecraft.world.InteractionResult;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Lumen Trail: the thin glowing slime a Lumen Slug leaves behind. Use a glass bottle on it to collect
// Lumen Gel. It fades on its own after 5 minutes. No collision; anything placed there replaces it.
public class LumenTrailBlock extends Block {
    public static final MapCodec<LumenTrailBlock> CODEC = simpleCodec(LumenTrailBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    public static final int FADE_TICKS = 6000;

    public LumenTrailBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess scheduledTicks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource shapeRandom) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, FADE_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.GLASS_BOTTLE)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            level.removeBlock(pos, false);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.LUMEN_GEL.get())));
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.2f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + 0.1,
                    pos.getZ() + random.nextDouble(), 0, 0.01, 0);
        }
    }
}
