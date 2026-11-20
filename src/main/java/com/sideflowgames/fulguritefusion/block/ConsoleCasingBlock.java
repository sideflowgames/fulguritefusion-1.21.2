package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

// Console Casing: the body of the Reactor Control Console. Loose it's a plain panel; when the console forms,
// each casing becomes the part its spot calls for (frame, power gauge, blinking displays, fuel screens)
// and turns to face the same way as the console. Right-click: the console's status.
public class ConsoleCasingBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ConsoleCasingBlock> CODEC = simpleCodec(ConsoleCasingBlock::new);
    public static final EnumProperty<ConsolePart> PART = EnumProperty.create("part", ConsolePart.class);

    public ConsoleCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, ConsolePart.NONE));
    }

    // Screens glow a little.
    public static int light(BlockState state) {
        ConsolePart part = state.getValue(PART);
        return part == ConsolePart.NONE || part == ConsolePart.FRAME ? 0 : 5;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return ReactorConsoleBlockEntity.showStatusFrom(level, pos, player);
    }
}
