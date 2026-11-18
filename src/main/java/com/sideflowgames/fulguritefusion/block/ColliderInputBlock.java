package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Collider Input: part of the Mini Collider ring (center of one side). Spark Cells go in here.
// Right-click with Spark Cells to load them, right-click empty-handed to see how many are loaded,
// sneak + right-click empty-handed to take them back out.
public class ColliderInputBlock extends BaseEntityBlock {
    public static final MapCodec<ColliderInputBlock> CODEC = simpleCodec(ColliderInputBlock::new);

    public ColliderInputBlock(Properties properties) {
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
        return new ColliderInputBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.SPARK_CELL.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ColliderInputBlockEntity input) {
            ItemStack left = input.insert(stack.copy());
            int added = stack.getCount() - left.getCount();
            if (!player.getAbilities().instabuild) stack.shrink(added);
            player.displayClientMessage(Component.literal("Collider Input: " + input.cellCount() + " / "
                    + ColliderInputBlockEntity.MAX_CELLS + " Spark Cells loaded"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ColliderInputBlockEntity input) {
            if (player.isShiftKeyDown()) {
                ItemStack out = input.takeAll();
                if (!out.isEmpty() && !player.getInventory().add(out)) player.drop(out, false);
            }
            player.displayClientMessage(Component.literal("Collider Input: " + input.cellCount() + " / "
                    + ColliderInputBlockEntity.MAX_CELLS + " Spark Cells loaded (2 per shot)"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ColliderInputBlockEntity input) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), input.getItems().getStackInSlot(0));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
