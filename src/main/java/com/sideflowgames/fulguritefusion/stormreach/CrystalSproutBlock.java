package com.sideflowgames.fulguritefusion.stormreach;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Crystal Sprouts: small glowing growths of tempest crystal on the Shattered Highlands' Stormstone. Decorative.
public class CrystalSproutBlock extends VegetationBlock {
    public static final MapCodec<CrystalSproutBlock> CODEC = simpleCodec(CrystalSproutBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 9, 13);

    public CrystalSproutBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.STORMSTONE.get()) || state.is(ModBlocks.COBBLED_STORMSTONE.get())
                || state.is(ModBlocks.POLISHED_STORMSTONE.get()) || state.is(ModBlocks.STORMSTONE_BRICKS.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
