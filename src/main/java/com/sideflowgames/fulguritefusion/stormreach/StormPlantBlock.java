package com.sideflowgames.fulguritefusion.stormreach;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Short Stormreach plants (Arcgrass, Arc Fern): grow on Arcgrass and Charged Loam (and any dirt), break when
// what's under them goes, and can be replaced by placing a block over them.
public class StormPlantBlock extends BushBlock {
    public static final MapCodec<StormPlantBlock> CODEC = simpleCodec(StormPlantBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

    public StormPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
