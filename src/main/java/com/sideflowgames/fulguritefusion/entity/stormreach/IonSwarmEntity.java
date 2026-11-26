package com.sideflowgames.fulguritefusion.entity.stormreach;

import com.sideflowgames.fulguritefusion.stormreach.StormreachStorm;
import com.sideflowgames.fulguritefusion.titan.TitanArmorEvents;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

// Ion Swarm: a buzzing cloud of loose sparks. It doesn't bite; it just drifts into you. Anything inside its 3.5
// block aura takes 2.5 storm damage every half second and loses Fm from its gear. Hard to hit: arrows and other
// projectiles pass through it for half damage. 24 health. Drops Ion Motes.
public class IonSwarmEntity extends StormBeast {
    @Nullable private BlockPos wander;

    public IonSwarmEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "ion_swarm");
        setNoGravity(true);
        fieldRadius = 3.5;
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.ATTACK_DAMAGE, 2.5)
                .add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.FLYING_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        targetGoals();
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            Vec3 jitter = new Vec3(random.nextGaussian() * 0.6, random.nextGaussian() * 0.3, random.nextGaussian() * 0.6);
            steer(target.getEyePosition().subtract(0, 0.6, 0).add(jitter), 0.2, 0.06);
        } else {
            if (wander == null || random.nextInt(80) == 0 || wander.closerToCenterThan(position(), 1.5)) {
                wander = blockPosition().offset(random.nextInt(11) - 5, random.nextInt(5) - 2, random.nextInt(11) - 5);
            }
            steer(Vec3.atCenterOf(wander), 0.08, 0.05);
        }
        if (tickCount % 10 == 0) {
            float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            for (LivingEntity e : victims(position().add(0, 0.45, 0), 3.5)) {
                if (StormDamage.hit(this, e, dmg)) StormDamage.drain(e, 2_000);
            }
        }
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) amount *= 0.5f;
        return super.hurtServer(serverLevel, source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(1.2), getRandomY(), getRandomZ(1.2), 0, 0, 0);
            }
        }
    }

    @Override
    protected boolean moving(com.geckolib.animation.state.AnimationTest<?> s) {
        return getTarget() != null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEEHIVE_WORK;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FIRE_EXTINGUISH;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }
}
