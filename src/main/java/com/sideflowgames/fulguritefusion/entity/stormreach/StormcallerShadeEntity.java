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

// Stormcaller Shade: a hooded wraith that floats out of reach and calls the storm down on you. It marks the ground
// under your feet with a ring of sparks, and a moment later lightning falls there (10 storm damage). Keep moving.
// Calls faster during a storm. 40 health. Drops Shade Veils.
public class StormcallerShadeEntity extends StormBeast {
    private int callCooldown = 60;
    @Nullable private Vec3 mark;
    @Nullable private BlockPos wander;

    public StormcallerShadeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "stormcaller_shade");
        setNoGravity(true);
        fieldRadius = 6.0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 4.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override
    protected void registerGoals() {
        targetGoals();
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        ServerLevel server = (ServerLevel) level();
        if (callCooldown > 0) callCooldown--;
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            // Hovers 8 blocks away and 4 above, circling slowly.
            Vec3 off = position().subtract(target.position()).multiply(1, 0, 1);
            off = off.lengthSqr() < 1e-3 ? new Vec3(1, 0, 0) : off.normalize();
            off = off.yRot(0.02f).scale(8);
            steer(target.position().add(off).add(0, 4, 0), 0.25, 0.08);
            if (mark == null && callCooldown == 0 && hasLineOfSight(target)) {
                mark = target.position();
                setAction(1, 30);
                playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.0f, 0.6f);
            }
        } else {
            if (wander == null || random.nextInt(100) == 0 || wander.closerToCenterThan(position(), 1.5)) {
                wander = blockPosition().offset(random.nextInt(13) - 6, random.nextInt(7) - 3, random.nextInt(13) - 6);
            }
            steer(Vec3.atCenterOf(wander), 0.08, 0.05);
        }
        if (mark != null) {
            for (int i = 0; i < 8; i++) {
                double a = (tickCount * 0.3) + i * Math.PI / 4;
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, mark.x + Math.cos(a) * 2, mark.y + 0.1, mark.z + Math.sin(a) * 2, 1, 0, 0.05, 0, 0);
            }
            if (actionTicks() <= 1) {
                callBolt(mark, 2.5, 10.0f);
                mark = null;
                callCooldown = StormreachStorm.isStorming(server) ? 60 : 100;
            }
        }
    }

    @Override
    protected boolean moving(com.geckolib.animation.state.AnimationTest<?> s) {
        return getDeltaMovement().lengthSqr() > 0.002;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.5), getY(0.2), getRandomZ(0.5), 0, -0.03, 0);
        }
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
        return SoundEvents.EVOKER_DEATH;
    }
}
