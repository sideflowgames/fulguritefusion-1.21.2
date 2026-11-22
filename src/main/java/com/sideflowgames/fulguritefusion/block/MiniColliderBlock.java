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

// Mini Collider: the center block of a 7x7 ring. Two Spark Cells race around the ring in opposite
// directions, get fired in by the injectors and collide here. Power comes out of the top.
// Right-click to see its status (or what's wrong with the ring).
public class MiniColliderBlock extends BaseEntityBlock {
    public static final MapCodec<MiniColliderBlock> CODEC = simpleCodec(MiniColliderBlock::new);

    public MiniColliderBlock(Properties properties) {
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
        return new MiniColliderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.MINI_COLLIDER_BE.get(), MiniColliderBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MiniColliderBlockEntity collider) {
            collider.interact(player); // menu (sneak: status line)
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ColliderParts.FORMED, ColliderParts.INPUT);
    }

    // Breaking the center block un-forms its ring.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) ColliderCoreBlockEntity.unformRing(level, pos, state);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // Formed: the ring model draws this spot too, so the block hides and mustn't hide the faces around it.
    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(ColliderParts.FORMED) ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }
}
