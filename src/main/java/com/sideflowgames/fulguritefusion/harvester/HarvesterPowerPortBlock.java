package com.sideflowgames.fulguritefusion.harvester;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

// Harvester Power Port: the other front corner of a Latex Harvester. Put a Machine Connector on it, or set
// another machine against it, to power the harvester.
public class HarvesterPowerPortBlock extends BaseEntityBlock {
    public static final MapCodec<HarvesterPowerPortBlock> CODEC = simpleCodec(HarvesterPowerPortBlock::new);

    public HarvesterPowerPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HarvesterPart.FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HarvesterPart.FORMED);
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
        return new HarvesterPartBlockEntity(pos, state);
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
