package com.sideflowgames.fulguritefusion.brew;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// Ion Lamp: a full-bright lamp of trapped Ion Motes that crackles with sparks.
public class IonLampBlock extends Block {
    public IonLampBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.15 + random.nextDouble() * 0.7,
                    pos.getY() + 0.15 + random.nextDouble() * 0.7, pos.getZ() + 0.15 + random.nextDouble() * 0.7,
                    (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1);
        }
        if (random.nextInt(60) == 0) {
            level.playLocalSound(pos, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 0.15f, 1.9f, false);
        }
    }
}
