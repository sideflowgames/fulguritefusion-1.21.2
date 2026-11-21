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

// Fusion Collider: the heart of the Fusion Reactor, in the middle of the blast chamber.
// Right-click: the reactor's status (what's built, or the first block that's missing or wrong).
// Once the blast chamber forms, the block itself hides and FusionColliderRenderer draws the energy core:
// a plasma orb in a cage of spinning rings, with lightning arcing to two prongs facing the beam ports.
public class FusionColliderBlock extends BaseEntityBlock {
    public static final MapCodec<FusionColliderBlock> CODEC = simpleCodec(FusionColliderBlock::new);

    public FusionColliderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FormableBlock.FORMED, false));
    }

    // FORMED = the blast chamber is formed: the block hides and the renderer draws the energy core instead.
    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FormableBlock.FORMED);
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
        return new FusionColliderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.FUSION_COLLIDER_BE.get(), FusionColliderBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FusionColliderBlockEntity reactor) {
            reactor.check();
            player.displayClientMessage(Component.literal(reactor.statusText()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Taking the collider out un-forms the ring.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FusionColliderBlockEntity reactor) {
            reactor.unform();
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
