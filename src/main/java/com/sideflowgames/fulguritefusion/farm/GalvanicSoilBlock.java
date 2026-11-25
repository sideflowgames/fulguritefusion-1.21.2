package com.sideflowgames.fulguritefusion.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Galvanic Soil: the spawning pad of a Shock Farm. On its own it does nothing; in a formed, powered farm, mobs of
// the biome are drawn up out of it.
public class GalvanicSoilBlock extends Block {
    // Part of a formed Shock Farm: the block hides and the controller draws the farm as one model.
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public GalvanicSoilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(FORMED) || random.nextInt(8) != 0 || !level.getBlockState(pos.above()).isAir()) return;
        level.addParticle(random.nextBoolean() ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.SOUL,
                pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
    }
}
