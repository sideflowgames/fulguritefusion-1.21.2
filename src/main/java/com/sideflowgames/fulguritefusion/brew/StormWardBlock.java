package com.sideflowgames.fulguritefusion.brew;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Storm Ward: while it has Fm (20 Fm a tick, fed through a Machine Connector), storm beasts and elites can't spawn
// within 32 blocks. Unpowered, it does nothing.
public class StormWardBlock extends BaseEntityBlock {
    public static final MapCodec<StormWardBlock> CODEC = simpleCodec(StormWardBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public StormWardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StormWardBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, UtilityBlocks.STORM_WARD_BE.get(), StormWardBlockEntity::serverTick);
    }

    // 26.1: blocks no longer get onRemove. Called from the block entity just before it is removed
    // (or after removal for blocks without one); newState is the block now standing here.
    public void onRemoveLegacy(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) StormWardBlockEntity.forget(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        onRemoveLegacy(state, level, pos, level.getBlockState(pos), moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
                pos.getY() + 1.05, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0, 0.08, 0);
    }
}
