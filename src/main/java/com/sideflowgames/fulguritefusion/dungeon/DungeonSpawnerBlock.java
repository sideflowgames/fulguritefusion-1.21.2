package com.sideflowgames.fulguritefusion.dungeon;

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

// Storm Cage: a dungeon's mob cage. When a player comes within 9 blocks it bursts open once, releasing its storm
// beasts, and goes dark for good (SPENT). Dungeons are a one-time clear.
public class DungeonSpawnerBlock extends BaseEntityBlock {
    public static final MapCodec<DungeonSpawnerBlock> CODEC = simpleCodec(DungeonSpawnerBlock::new);
    public static final BooleanProperty SPENT = BooleanProperty.create("spent");

    public DungeonSpawnerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SPENT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPENT);
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
        return new DungeonSpawnerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || state.getValue(SPENT) ? null
                : createTickerHelper(type, DungeonBlocks.SPAWNER_BE.get(), DungeonSpawnerBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SPENT) && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + random.nextDouble(),
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
        }
    }
}
