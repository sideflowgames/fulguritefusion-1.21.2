package com.sideflowgames.fulguritefusion.brew;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Updraft Vent: step on it and a gust throws you about 12 blocks straight up, then lets you drift down gently.
// Sneak to stand on it without launching.
public class UpdraftVentBlock extends Block {
    private static final double LAUNCH = 1.6;

    public UpdraftVentBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity.isSteppingCarefully() || !(entity instanceof LivingEntity living)) {
            super.stepOn(level, pos, state, entity);
            return;
        }
        // Players move themselves on their own client; everything else is moved by the server.
        boolean mine = entity instanceof Player ? level.isClientSide() : !level.isClientSide();
        if (mine) {
            Vec3 v = entity.getDeltaMovement();
            entity.setDeltaMovement(v.x, LAUNCH, v.z);
            entity.hurtMarked = true;
        }
        entity.resetFallDistance();
        if (!level.isClientSide()) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
            level.playSound(null, pos, SoundEvents.BREEZE_JUMP, SoundSource.BLOCKS, 0.6f, 1.2f);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double distance) {
        entity.resetFallDistance();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.25 + random.nextDouble() * 0.15, 0);
        }
    }
}
