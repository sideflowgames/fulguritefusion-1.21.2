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

// Tempest Raptor: a storm bird that circles high above you, then folds its wings and dives, slashing as it passes
// (8 storm damage and a hard knock) before climbing away for another pass. Dives more often in a storm.
// 36 health. Drops Tempest Feathers.
public class TempestRaptorEntity extends StormBeast {
    private int diveCooldown = 60;
    private float circle;
    @Nullable private BlockPos wander;

    public TempestRaptorEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "tempest_raptor");
        setNoGravity(true);
        fieldRadius = 4.0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0).add(Attributes.ARMOR, 4.0).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    protected void registerGoals() {
        targetGoals();
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        if (diveCooldown > 0) diveCooldown--;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            if (wander == null || random.nextInt(120) == 0 || wander.closerToCenterThan(position(), 2)) {
                wander = blockPosition().offset(random.nextInt(31) - 15, random.nextInt(9) - 3, random.nextInt(31) - 15);
            }
            steer(Vec3.atCenterOf(wander), 0.25, 0.04);
            return;
        }
        if (action() == 1) {
            steer(target.position().add(0, target.getBbHeight() * 0.5, 0), 0.95, 0.25);
            if (getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())) {
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                doHurtTarget((net.minecraft.server.level.ServerLevel) level(), target);
                setAction(0, 0);
                setDeltaMovement(getDeltaMovement().add(0, 0.6, 0));
                diveCooldown = StormreachStorm.isStorming((ServerLevel) level()) ? 50 : 80;
            }
            return;
        }
        circle += 0.05f;
        Vec3 goal = target.position().add(Math.cos(circle) * 9, 10, Math.sin(circle) * 9);
        steer(goal, 0.45, 0.06);
        if (diveCooldown == 0 && getY() > target.getY() + 5 && hasLineOfSight(target)) {
            setAction(1, 40);
            diveCooldown = 90;
            playSound(SoundEvents.PHANTOM_SWOOP, 2.0f, 0.8f);
        }
    }

    @Override
    protected boolean moving(com.geckolib.animation.state.AnimationTest<?> s) {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }
}
