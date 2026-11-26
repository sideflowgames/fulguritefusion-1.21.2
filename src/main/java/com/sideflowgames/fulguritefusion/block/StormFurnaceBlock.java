package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

// The Storm Furnace: a vanilla furnace changed by a lightning strike, in three tiers
// (Weak, Enhanced, Charged). Higher tiers come from the lightning upgrade ritual.
// Right-click to open its menu; sneak + right-click to see its status.
public class StormFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<StormFurnaceBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("tier").forGetter(StormFurnaceBlock::getTier),
            propertiesCodec()).apply(instance, StormFurnaceBlock::new));
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    // 1 = Weak, 2 = Enhanced, 3 = Charged
    private final int tier;

    public StormFurnaceBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    public int getTier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StormFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.STORM_FURNACE_BE.get(), StormFurnaceBlockEntity::serverTick);
    }

    // Right-click: open the menu. Sneak + right-click (empty hand): show status text.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StormFurnaceBlockEntity furnace) {
            if (player.isShiftKeyDown()) {
                player.sendOverlayMessage(Component.literal(furnace.statusText()));
            } else {
                player.openMenu(furnace, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.SUCCESS;
    }

    // Drop the contents when the furnace is broken.
    // 26.1: blocks no longer get onRemove. Called from the block entity just before it is removed
    // (or after removal for blocks without one); newState is the block now standing here.
    public void onRemoveLegacy(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Upgrading to the next tier keeps the contents, so only drop them when it is really removed.
        boolean upgrading = newState.getBlock() instanceof StormFurnaceBlock;
        if (!state.is(newState.getBlock()) && !upgrading && level.getBlockEntity(pos) instanceof StormFurnaceBlockEntity furnace) {
            for (int i = 0; i < furnace.getItems().getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), furnace.getItems().getStackInSlot(i));
            }
        }
    }

    // Charged particles: always some sparks, more while burning,
    // and a lot more (spraying farther out) when the furnace isn't grounded.
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean grounded = StormFurnaceBlockEntity.isGrounded(level, pos);
        int count = state.getValue(LIT) ? 3 : 1;
        double spread = 1.2;
        if (!grounded) {
            count *= 4;
            spread = 2.4;
        }
        for (int i = 0; i < count; i++) {
            if (random.nextFloat() < 0.6f) {
                double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * spread;
                double y = pos.getY() + random.nextDouble() * (grounded ? 1.1 : 1.6);
                double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * spread;
                double speed = grounded ? 0.02 : 0.08;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z,
                        (random.nextDouble() - 0.5) * speed, speed, (random.nextDouble() - 0.5) * speed);
            }
        }
    }
}
