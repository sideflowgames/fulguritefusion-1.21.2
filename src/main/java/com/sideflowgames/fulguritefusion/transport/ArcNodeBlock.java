package com.sideflowgames.fulguritefusion.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Arc Sender and Arc Receiver. Each fixes onto the side of an inventory (FACING points at that inventory). Link a
// Sender to a Receiver no more than 5 blocks away with an Arc Tuner; once a second the Sender throws everything in its
// inventory (up to a whole chest, 27 stacks) across to the Receiver's inventory in one bolt of lightning, for
// 400 Fm a bolt.
public class ArcNodeBlock extends BaseEntityBlock {
    public static final MapCodec<ArcNodeBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("sender").forGetter(b -> b.sender), propertiesCodec()).apply(i, ArcNodeBlock::new));
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);

    private final boolean sender;

    public ArcNodeBlock(boolean sender, Properties properties) {
        super(properties);
        this.sender = sender;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public boolean isSender() {
        return sender;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // Faces the block it was placed against.
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return sender ? new ArcSenderBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || !sender ? null : createTickerHelper(type, TransportBlocks.ARC_SENDER_BE.get(), ArcSenderBlockEntity::serverTick);
    }

    // Holding an Arc Tuner: let the Tuner handle the click (link / unlink) instead of the status message.
    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ArcTunerItem) return InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (sender && level.getBlockEntity(pos) instanceof ArcSenderBlockEntity be) {
            BlockPos t = be.target();
            String link = t == null ? "not linked (use an Arc Tuner)" : "linked to " + t.getX() + ", " + t.getY() + ", " + t.getZ();
            player.sendOverlayMessage(Component.literal("Arc Sender: " + link + " | " + be.getFm() + " / " + be.getMaxFm() + " Fm")
                    .withStyle(t == null ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        } else {
            player.sendOverlayMessage(Component.literal("Arc Receiver: link a Sender to it with an Arc Tuner")
                    .withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.CONSUME;
    }
}
