package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// The Fulgurite Charger.
// Right-click with Empty Spark Cells to load them (up to 16).
// Right-click empty-handed to collect charged cells, or to see its status.
// Sneak + right-click empty-handed to take back uncharged cells.
public class FulguriteChargerBlock extends BaseEntityBlock {
    public static final MapCodec<FulguriteChargerBlock> CODEC = simpleCodec(FulguriteChargerBlock::new);

    public FulguriteChargerBlock(Properties properties) {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FulguriteChargerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.FULGURITE_CHARGER_BE.get(), FulguriteChargerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.EMPTY_SPARK_CELL.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FulguriteChargerBlockEntity charger) {
            charger.insertCells(stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FulguriteChargerBlockEntity charger) {
            ItemStack taken = player.isShiftKeyDown() ? charger.takeInput() : charger.takeOutput();
            if (!taken.isEmpty()) {
                player.getInventory().placeItemBackInInventory(taken);
            } else {
                player.displayClientMessage(Component.literal(charger.statusText()), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FulguriteChargerBlockEntity charger) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), charger.takeInput());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), charger.takeOutput());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
