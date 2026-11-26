package com.sideflowgames.fulguritefusion.entity.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

// Magma Pylon: the Magma Conduit's shield anchor. A basalt spire that bursts out of the ground. It doesn't move,
// sets anyone within 2.5 blocks on fire, and while any pylon stands the Conduit heals 1 health a second. 45 health.
public class MagmaPylonEntity extends BossAnchorEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.magma_pylon.idle");

    public MagmaPylonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 45.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 6.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        StormBossEntity boss = owner();
        if (tickCount % 20 == 0) {
            if (boss != null && boss.getHealth() < boss.getMaxHealth()) boss.heal(1.0f);
            for (LivingEntity e : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(2.5),
                    p -> !p.isCreative() && !p.isSpectator())) {
                e.igniteForSeconds(4);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(new Vec3(0, Math.min(0, getDeltaMovement().y), 0));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.FLAME, getRandomX(0.5), getY() + 2.8, getRandomZ(0.5), 0, 0.04, 0);
            if (random.nextInt(4) == 0) level().addParticle(ParticleTypes.LAVA, getRandomX(0.8), getY() + 0.3, getRandomZ(0.8), 0, 0, 0);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BASALT_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BASALT_BREAK;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, s -> s.setAndContinue(IDLE)));
    }
}
