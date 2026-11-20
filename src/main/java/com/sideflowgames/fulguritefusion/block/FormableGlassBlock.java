package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

// Fulgurite Blast Glass. FORMED = part of a finished ring (hidden; the smooth ring is drawn instead).
// Glass in the blast chamber's faces never forms, so it always shows.
public class FormableGlassBlock extends TransparentBlock {
    public static final MapCodec<FormableGlassBlock> CODEC = simpleCodec(FormableGlassBlock::new);

    public FormableGlassBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FormableBlock.FORMED, false));
    }

    @Override
    protected MapCodec<? extends TransparentBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FormableBlock.FORMED);
    }
}
