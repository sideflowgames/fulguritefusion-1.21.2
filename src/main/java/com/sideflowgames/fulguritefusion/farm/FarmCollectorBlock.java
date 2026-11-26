package com.sideflowgames.fulguritefusion.farm;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// Right-click: open it like a chest. Sneak + right-click: take the stored XP.
// FORMED: part of a formed farm; its own model hides and the farm model draws a collector hatch panel there.
public class FarmCollectorBlock extends BaseEntityBlock {
    public static final MapCodec<FarmCollectorBlock> CODEC = simpleCodec(FarmCollectorBlock::new);

    public FarmCollectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GalvanicSoilBlock.FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(GalvanicSoilBlock.FORMED);
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
        return new FarmCollectorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FarmCollectorBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            int xp = be.takeXp();
            if (xp > 0) {
                player.giveExperiencePoints(xp);
                level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);
            } else {
                player.sendOverlayMessage(Component.literal("No XP stored").withStyle(ChatFormatting.GRAY));
            }
        } else {
            player.openMenu(be);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    // 26.1: blocks no longer get onRemove. Called from the block entity just before it is removed
    // (or after removal for blocks without one); newState is the block now standing here.
    public void onRemoveLegacy(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FarmCollectorBlockEntity be) {
            Containers.dropContents(level, pos, be);
            if (level instanceof ServerLevel server && be.xp() > 0) ExperienceOrb.award(server, Vec3.atCenterOf(pos), be.takeXp());
        }
    }
}
