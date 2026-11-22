package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Storm Collider: the ring's pairing mode (swap it in for the Mini Collider in the middle of the ring).
// Right-click to see its status; sneak + right-click to take out what it has made.
public class StormColliderBlock extends BaseEntityBlock {
    public static final MapCodec<StormColliderBlock> CODEC = simpleCodec(StormColliderBlock::new);

    public StormColliderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ColliderParts.FORMED, false).setValue(ColliderParts.INPUT, net.minecraft.core.Direction.NORTH));
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StormColliderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.STORM_COLLIDER_BE.get(), StormColliderBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StormColliderBlockEntity collider) {
            if (player.isShiftKeyDown()) collider.takeOutputs(player);
            collider.interact(player); // menu (sneak: take products + status line)
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof StormColliderBlockEntity collider) {
            collider.dropContents();
        }
        if (!state.is(newState.getBlock())) ColliderCoreBlockEntity.unformRing(level, pos, state);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ColliderParts.FORMED, ColliderParts.INPUT);
    }

    // Formed: the ring model draws this spot too, so the block hides and mustn't hide the faces around it.
    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(ColliderParts.FORMED) ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }
}
