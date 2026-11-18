package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;

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

// Collider Injector block. Right-click with an item to load it, right-click empty-handed to see what's
// loaded, sneak + right-click empty-handed to take it back out.
public class ColliderInjectorBlock extends BaseEntityBlock {
    public static final MapCodec<ColliderInjectorBlock> CODEC = simpleCodec(ColliderInjectorBlock::new);

    public ColliderInjectorBlock(Properties properties) {
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
        return new ColliderInjectorBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ColliderInjectorBlockEntity injector) {
            ItemStack left = injector.insert(stack.copy());
            int added = stack.getCount() - left.getCount();
            if (!player.getAbilities().instabuild) stack.shrink(added);
            player.displayClientMessage(Component.literal(injector.statusText()), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ColliderInjectorBlockEntity injector) {
            if (player.isShiftKeyDown()) {
                ItemStack out = injector.takeAll();
                if (!out.isEmpty() && !player.getInventory().add(out)) player.drop(out, false);
            }
            player.displayClientMessage(Component.literal(injector.statusText()), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ColliderInjectorBlockEntity injector) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), injector.peek());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
