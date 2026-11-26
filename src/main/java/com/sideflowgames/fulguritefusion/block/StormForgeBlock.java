package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// The Forge Anvil: core of the Storm Forge (see StormForgeBlockEntity).
public class StormForgeBlock extends BaseEntityBlock {
    public static final MapCodec<StormForgeBlock> CODEC = simpleCodec(StormForgeBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 4, 14),
            Block.box(4, 4, 4, 12, 10, 12),
            Block.box(1, 10, 2, 15, 16, 14));

    public StormForgeBlock(Properties properties) {
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StormForgeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.STORM_FORGE_BE.get(), StormForgeBlockEntity::serverTick);
    }

    // Holding an item: set one on the anvil.
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof StormForgeBlockEntity forge)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            if (!forge.result().isEmpty()) {
                give(player, forge.takeBack());
            } else {
                String refusal = forge.insert(stack);
                if (refusal != null) player.sendOverlayMessage(Component.literal(refusal));
            }
        }
        return InteractionResult.SUCCESS;
    }

    // Empty hand: take the result or the last item back. Sneaking: status.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StormForgeBlockEntity forge) {
            if (player.isSecondaryUseActive()) forge.showStatus(player);
            else give(player, forge.takeBack());
        }
        return InteractionResult.SUCCESS;
    }

    private static void give(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    // 26.1: blocks no longer get onRemove. Called from the block entity just before it is removed
    // (or after removal for blocks without one); newState is the block now standing here.
    public void onRemoveLegacy(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof StormForgeBlockEntity forge) forge.dropAll();
    }
}
