package com.sideflowgames.fulguritefusion.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

// Storm Battery: touching batteries form a bank of up to 6 blocks, built in any shape and any order.
// Overload rule, checked whenever a battery is placed: if the new block would connect more than 6 batteries
// (growing one bank, or joining separate banks together), the new block and the battery it touched
// blow up in a small overload. Nothing else is damaged.
// Breaking a battery keeps its stored Fm in the dropped item.
public class StormBatteryBlock extends BaseEntityBlock {
    public static final MapCodec<StormBatteryBlock> CODEC = simpleCodec(StormBatteryBlock::new);

    public StormBatteryBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : PipeBlock.PROPERTY_BY_DIRECTION.values()) state = state.setValue(side, false);
        registerDefaultState(state);
    }

    // One flag per side: true when another battery touches that side. The model hides the dark frame
    // on connected sides, so a bank looks like one big block.
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (BooleanProperty side : PipeBlock.PROPERTY_BY_DIRECTION.values()) builder.add(side);
    }

    private BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), level.getBlockState(pos.relative(dir)).is(this));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connected(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), neighbourState.is(this));
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
        return new StormBatteryBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.STORM_BATTERY_BE.get(), StormBatteryBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StormBatteryBlockEntity battery) {
            if (player.isShiftKeyDown()) {
                player.displayClientMessage(Component.literal(battery.statusText()), true);
            } else {
                player.openMenu(battery, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.SUCCESS;
    }

    // Check the bank one tick after placement (it isn't safe to blow things up in the middle of placing a block).
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int maxBank = Config.BATTERY_MAX_BANK.get();

        // Find the separate banks this block touches (ignoring this block itself).
        List<Set<BlockPos>> banks = new ArrayList<>();
        List<BlockPos> contacts = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighbour = pos.relative(dir);
            if (!level.getBlockState(neighbour).is(this)) continue;
            boolean known = false;
            for (Set<BlockPos> bank : banks) {
                if (bank.contains(neighbour)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                banks.add(StormBatteryBlockEntity.findBank(level, neighbour, pos, 64));
                contacts.add(neighbour);
            }
        }

        // Everything this block would connect: itself plus every bank it touches.
        int total = 1;
        for (Set<BlockPos> bank : banks) total += bank.size();
        if (total > maxBank) {
            // Too many batteries connected: the new block and the last battery it touched overload.
            overload(level, pos, contacts.get(contacts.size() - 1));
        }
    }

    // Breaking a battery keeps its charge: the dropped item carries the stored Fm and restores it when placed.
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof StormBatteryBlockEntity battery && battery.getFm() > 0) {
            for (ItemStack stack : drops) {
                if (stack.is(asItem())) {
                    CompoundTag tag = new CompoundTag();
                    tag.putInt("Fm", battery.getFm());
                    BlockItem.setBlockEntityData(stack, ModBlocks.STORM_BATTERY_BE.get(), tag);
                }
            }
        }
        return drops;
    }

    // Show the stored charge on the item's tooltip.
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        int fm = data != null ? data.copyTag().getInt("Fm") : 0;
        tooltip.add(Component.literal("Stored: " + fm + " Fm").withStyle(ChatFormatting.AQUA));
    }

    // A small overload: both blocks are destroyed (no drops, their charge is lost). No other blocks are harmed.
    private static void overload(ServerLevel level, BlockPos a, BlockPos b) {
        level.removeBlock(a, false);
        level.removeBlock(b, false);
        double x = (a.getX() + b.getX()) / 2.0 + 0.5;
        double y = (a.getY() + b.getY()) / 2.0 + 0.5;
        double z = (a.getZ() + b.getZ()) / 2.0 + 0.5;
        level.explode(null, x, y, z, 1.0f, Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 80, 0.6, 0.6, 0.6, 0.4);
    }
}
