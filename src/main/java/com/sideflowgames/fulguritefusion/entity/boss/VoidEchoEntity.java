package com.sideflowgames.fulguritefusion.entity.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

// Void Echo: the Void Arc's shield anchor. A copy of the Void Arc that looks just like it. Echoes hover around you
// and lunge in for a clawing strike (6 damage, Darkness). Meanwhile the real Void Arc swaps places with its echoes,
// so you have to keep track of which is which. 35 health.
public class VoidEchoEntity extends BossAnchorEntity {
    private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("animation.void_arc.float");
    private static final RawAnimation STRIKE = RawAnimation.begin().thenLoop("animation.void_arc.attack");
    private double angle;
    private int lungeTimer = 60;
    private int lunging;

    public VoidEchoEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 35.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            StormBossEntity boss = owner();
            if (boss != null) setDeltaMovement(boss.position().add(Math.cos(angle) * 6, 0, Math.sin(angle) * 6).subtract(position()).scale(0.1));
            return;
        }
        getLookControl().setLookAt(target, 60, 60);
        Vec3 goal;
        if (lunging > 0) {
            lunging--;
            goal = target.position();
            if (distanceToSqr(target) < 5.0 && doHurtTarget((net.minecraft.server.level.ServerLevel) level(), target)) {
                swing(InteractionHand.MAIN_HAND);
                lunging = 0;
            }
        } else {
            angle += 0.025;
            goal = target.position().add(Math.cos(angle) * 7, 1.5, Math.sin(angle) * 7);
            if (--lungeTimer <= 0) {
                lungeTimer = 70 + random.nextInt(40);
                lunging = 25;
            }
        }
        Vec3 to = goal.subtract(position());
        Vec3 desired = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(lunging > 0 ? 0.6 : 0.3) : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().add(desired.subtract(getDeltaMovement()).scale(0.15)));
        float yaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * (180.0 / Math.PI)) - 90.0f;
        setYRot(yaw);
        yBodyRot = yaw;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel serverLevel, Entity target) {
        boolean hit = super.doHurtTarget(serverLevel, target);
        if (hit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0), this);
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.REVERSE_PORTAL, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0, 0);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 4, s -> s.setAndContinue(swinging ? STRIKE : FLOAT)));
    }
}
