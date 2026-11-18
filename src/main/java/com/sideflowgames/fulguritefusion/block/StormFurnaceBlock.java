package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

// The Storm Furnace: a vanilla furnace changed by a lightning strike.
// Right-click with fuel to load it. Right-click empty-handed to see its status.
public class StormFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<StormFurnaceBlock> CODEC = simpleCodec(StormFurnaceBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public StormFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StormFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.STORM_FURNACE_BE.get(), StormFurnaceBlockEntity::serverTick);
    }

    // Right-click holding fuel: load it into the furnace.
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.fuelValues().isFuel(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StormFurnaceBlockEntity furnace) {
            furnace.insertFuel(stack, player);
        }
        return InteractionResult.SUCCESS;
    }

    // Right-click empty-handed: show stored power, fuel and grounding.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StormFurnaceBlockEntity furnace) {
            player.displayClientMessage(Component.literal(furnace.statusText()), true);
        }
        return InteractionResult.SUCCESS;
    }

    // Drop any loaded fuel when the furnace is broken.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof StormFurnaceBlockEntity furnace) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), furnace.takeFuel());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // Charged particles: always a few sparks, more while burning.
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int count = state.getValue(LIT) ? 3 : 1;
        for (int i = 0; i < count; i++) {
            if (random.nextFloat() < 0.6f) {
                double x = pos.getX() - 0.1 + random.nextDouble() * 1.2;
                double y = pos.getY() + random.nextDouble() * 1.1;
                double z = pos.getZ() - 0.1 + random.nextDouble() * 1.2;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.02, 0);
            }
        }
    }
}
