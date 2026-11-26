package com.sideflowgames.fulguritefusion.stormreach;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

// Ion Bulb: a glowing bulb that grows underwater on the floor of the Ion Lakes. Only lives in water.
public class IonBulbBlock extends VegetationBlock implements LiquidBlockContainer {
    public static final MapCodec<IonBulbBlock> CODEC = simpleCodec(IonBulbBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 10, 13);

    public IonBulbBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && level.getFluidState(pos).is(Fluids.WATER);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return fluid.is(Fluids.WATER) && fluid.getAmount() == 8 ? super.getStateForPlacement(context) : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess scheduledTicks, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource shapeRandom) {
        BlockState result = super.updateShape(state, level, scheduledTicks, pos, dir, neighborPos, neighbor, shapeRandom);
        if (!result.isAir()) scheduledTicks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return result;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Fluids.WATER.getSource(false);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable net.minecraft.world.entity.LivingEntity player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return false;
    }
}
