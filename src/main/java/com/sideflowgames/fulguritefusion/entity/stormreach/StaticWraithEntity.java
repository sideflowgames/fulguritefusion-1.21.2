package com.sideflowgames.fulguritefusion.entity.stormreach;

import com.sideflowgames.fulguritefusion.item.FmChargeable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

// Static Wraith: a floating shadow of dead static that haunts the deep caves. Drifts through the air toward you;
// each touch deals 4 damage and drains 20,000 Fm from your charged gear (armor and held items).
// Drops Wraith Essence.
public class StaticWraithEntity extends Monster implements GeoEntity {
    private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("animation.static_wraith.float");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenLoop("animation.static_wraith.attack");
    private static final int DRAIN = 20_000;
    private static final double SPEED = 0.16;
    private static final double STEERING = 0.07;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable private BlockPos wander;
    private int attackCooldown;

    public StaticWraithEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 20.0).add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // Steers smoothly toward its target (or a wander spot), floating through the air.
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (attackCooldown > 0) attackCooldown--;
        LivingEntity target = getTarget();
        Vec3 goal;
        if (target != null && target.isAlive()) {
            goal = target.getEyePosition().subtract(0, 0.4, 0);
            getLookControl().setLookAt(target, 30, 30);
            if (attackCooldown == 0 && distanceToSqr(target) < 2.5 && doHurtTarget(target)) {
                attackCooldown = 25;
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                drain(target);
            }
        } else {
            if (wander == null || random.nextInt(100) == 0 || wander.closerToCenterThan(position(), 1.5)) {
                wander = blockPosition().offset(random.nextInt(13) - 6, random.nextInt(7) - 3, random.nextInt(13) - 6);
            }
            goal = Vec3.atCenterOf(wander);
        }
        Vec3 to = goal.subtract(position());
        double speed = target != null ? SPEED : SPEED * 0.4;
        Vec3 desired = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(speed) : Vec3.ZERO;
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.add(desired.subtract(v).scale(STEERING)));
        Vec3 m = getDeltaMovement();
        if (m.horizontalDistanceSqr() > 1.0E-5) {
            float yaw = (float) (Mth.atan2(m.z, m.x) * (180.0 / Math.PI)) - 90.0f;
            setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.2f);
            yBodyRot = getYRot();
        }
    }

    private void drain(LivingEntity target) {
        int left = DRAIN;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            if (left > 0 && FmChargeable.hasCharge(stack)) left -= FmChargeable.use(stack, left);
        }
        if (left < DRAIN && level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
            server.sendParticles(ParticleTypes.SOUL, getX(), getY(0.5), getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, -0.02, 0);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, s -> s.setAndContinue(swinging ? ATTACK : FLOAT)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
