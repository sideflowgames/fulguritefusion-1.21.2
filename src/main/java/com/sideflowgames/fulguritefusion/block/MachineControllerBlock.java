package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.machine.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

// The controller of a multiblock machine (Press, Furnace, Wire Extruder, Kiln, Crusher).
//   - Not built yet: right-click to outline where the Arc Casings go.
//   - Built: right-click to open the machine; sneak + right-click for a status line.
public class MachineControllerBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = ArcCasingBlock.FORMED;
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    public static final MapCodec<MachineControllerBlock> CODEC = simpleCodec(p -> new MachineControllerBlock(MachineType.PRESS, p));

    private final MachineType type;

    public MachineControllerBlock(MachineType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false).setValue(WORKING, false));
    }

    public MachineType type() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, WORKING);
    }

    // The front faces the player who placed it.
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
        return new MachineControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        return level.isClientSide() ? null
                : createTickerHelper(beType, ModBlocks.MACHINE_CONTROLLER_BE.get(), MachineControllerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) interact(level, pos, state, player);
        return InteractionResult.SUCCESS;
    }

    // Shared with the Arc Casings: clicking any part of a formed machine opens its menu.
    public static void interact(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof MachineControllerBlockEntity machine) {
            machine.requestCheck();
            if (!state.getValue(FORMED)) {
                int missing = machine.showPreview();
                player.sendOverlayMessage(Component.literal(missing == 0
                        ? "All Arc Casings are in place; the machine is forming..."
                        : missing + " Arc Casing" + (missing == 1 ? "" : "s") + " missing (outlined)"));
            } else if (player.isShiftKeyDown()) {
                player.sendOverlayMessage(Component.literal(machine.statusText()));
            } else {
                player.openMenu(machine, buf -> buf.writeBlockPos(pos));
            }
        }
    }

    // 26.1: blocks no longer get onRemove. Called from the block entity just before it is removed
    // (or after removal for blocks without one); newState is the block now standing here.
    public void onRemoveLegacy(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineControllerBlockEntity machine) {
            machine.breakApart();
        }
    }
}
