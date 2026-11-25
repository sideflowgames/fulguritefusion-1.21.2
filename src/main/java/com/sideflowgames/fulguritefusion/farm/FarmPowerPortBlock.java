package com.sideflowgames.fulguritefusion.farm;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Farm Power Port: takes the place of an edge casing in a Shock Farm (not a corner) and passes any Fm it receives
// straight into the controller. Feed it from a battery, a wire or a Machine Connector.
// FORMED: part of a formed farm; its own model hides and the farm model draws a power socket panel there.
public class FarmPowerPortBlock extends BaseEntityBlock {
    public static final MapCodec<FarmPowerPortBlock> CODEC = simpleCodec(FarmPowerPortBlock::new);

    public FarmPowerPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GalvanicSoilBlock.FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(GalvanicSoilBlock.FORMED);
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
        return new FarmPowerPortBlockEntity(pos, state);
    }
}
