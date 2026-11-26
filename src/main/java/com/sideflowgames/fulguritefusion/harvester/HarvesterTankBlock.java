package com.sideflowgames.fulguritefusion.harvester;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

// Harvester Tank: two of these stack in one front corner of a Latex Harvester. Together they hold the latex
// (128 buckets); once built, the harvester's model shows how full it is. Pipes can drain it from any side, and an empty bucket takes a bucket of latex.
public class HarvesterTankBlock extends BaseEntityBlock {
    public static final MapCodec<HarvesterTankBlock> CODEC = simpleCodec(HarvesterTankBlock::new);

    public HarvesterTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HarvesterPart.FORMED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HarvesterPart.FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HarvesterPartBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HarvesterPartBlockEntity part) {
            if (part.controllerPos() != null) HarvesterPart.interact(level, part.controllerPos(), player);
            else player.sendOverlayMessage(Component.literal("Not part of a Latex Harvester yet"));
        }
        return InteractionResult.SUCCESS;
    }

    // 26.1: blocks no longer get onRemove. Called from the block entity just before it is removed
    // (or after removal for blocks without one); newState is the block now standing here.
    public void onRemoveLegacy(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) HarvesterPart.notify(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        onRemoveLegacy(state, level, pos, level.getBlockState(pos), moved);
    }
}
