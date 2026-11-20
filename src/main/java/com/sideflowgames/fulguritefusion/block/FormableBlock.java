package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Reinforced Fulgurite: a solid Fusion Reactor ring block. FORMED = part of a finished ring, so the block
// itself stops drawing and the Fusion Collider draws the smooth ring over it.
public class FormableBlock extends Block {
    public static final MapCodec<FormableBlock> CODEC = simpleCodec(FormableBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public FormableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
