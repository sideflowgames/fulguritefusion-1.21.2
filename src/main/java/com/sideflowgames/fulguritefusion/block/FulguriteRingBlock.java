package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Fulgurite Ring: the see-through ring block of a collider. Clicking any ring block of a finished ring
// (empty hand) opens that collider's menu; sneak shows its status line.
public class FulguriteRingBlock extends TransparentBlock {
    public static final MapCodec<FulguriteRingBlock> CODEC = simpleCodec(FulguriteRingBlock::new);

    public FulguriteRingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ColliderParts.FORMED, false));
    }

    @Override
    protected MapCodec<? extends TransparentBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ColliderCoreBlockEntity core = ColliderCoreBlockEntity.findFor(level, pos);
        if (core == null) return InteractionResult.PASS;
        if (!level.isClientSide()) core.interact(player);
        return InteractionResult.SUCCESS;
    }

    // Holding an item: use it as normal (so building next to the ring still works).
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND
                : InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ColliderParts.FORMED);
    }

    // Taking a block out of a formed ring un-forms it straight away.
    // 26.1: blocks no longer get onRemove. Called from the block entity just before it is removed
    // (or after removal for blocks without one); newState is the block now standing here.
    public void onRemoveLegacy(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) ColliderCoreBlockEntity.partRemoved(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        onRemoveLegacy(state, level, pos, level.getBlockState(pos), moved);
    }
}
