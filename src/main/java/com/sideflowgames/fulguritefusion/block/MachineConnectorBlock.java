package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.item.WireRollItem;
import com.sideflowgames.fulguritefusion.power.WireMath;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import com.sideflowgames.fulguritefusion.power.FmHandler;
import com.sideflowgames.fulguritefusion.power.FmSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Machine Connector: sits on top of a machine (Storm Furnace, Storm Battery, Charger, converters...).
// Wire Rolls string hanging wires between connectors to build a network.
// It pops off if the machine underneath is removed, dropping a roll for each wire attached to it.
// Right-click (empty hand) to see the network's status.
public class MachineConnectorBlock extends BaseEntityBlock {
    public static final MapCodec<MachineConnectorBlock> CODEC = simpleCodec(MachineConnectorBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2, 14),
            Block.box(6, 2, 6, 10, 9, 10),
            Block.box(5, 9, 5, 11, 11, 11));

    public MachineConnectorBlock(Properties properties) {
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // A machine = anything that stores, makes or uses Fm.
    public static boolean isMachine(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof FmHandler || be instanceof FmSource;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isMachine(level, pos.below());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineConnectorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.MACHINE_CONNECTOR_BE.get(), MachineConnectorBlockEntity::serverTick);
    }

    // Let Wire Rolls do their thing instead of the block's own right-click.
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof WireRollItem) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    // Removing a connector takes its wires down: one roll drops per wire, and the other end forgets it.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MachineConnectorBlockEntity connector) {
            for (MachineConnectorBlockEntity.Link link : connector.getLinks()) {
                if (level.getBlockEntity(link.other()) instanceof MachineConnectorBlockEntity other) other.removeLink(pos);
                ItemStack roll = new ItemStack(link.insulated() ? ModItems.INSULATED_WIRE.get() : ModItems.RAW_WIRE.get());
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), roll);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // Raw wires spark along their length while power is flowing.
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof MachineConnectorBlockEntity connector) || !connector.isPowered()) return;
        Vec3 a = WireMath.attachPoint(pos);
        for (MachineConnectorBlockEntity.Link link : connector.getLinks()) {
            if (link.insulated() || pos.asLong() > link.other().asLong()) continue; // draw each wire once
            if (random.nextFloat() < 0.35f) {
                Vec3 p = WireMath.point(a, WireMath.attachPoint(link.other()), random.nextDouble());
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 0, -0.02, 0);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineConnectorBlockEntity connector) {
            player.displayClientMessage(Component.literal(connector.statusText()), true);
        }
        return InteractionResult.SUCCESS;
    }
}
