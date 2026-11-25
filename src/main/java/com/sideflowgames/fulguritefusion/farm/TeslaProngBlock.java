package com.sideflowgames.fulguritefusion.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Tesla Prong: sits on top of a Shock Farm's corner pillars and arcs into the mobs on the pad.
public class TeslaProngBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 3, 11), Block.box(7, 3, 7, 9, 13, 9),
            Block.box(5.5, 12, 5.5, 10.5, 16, 10.5));

    public TeslaProngBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GalvanicSoilBlock.FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GalvanicSoilBlock.FORMED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
