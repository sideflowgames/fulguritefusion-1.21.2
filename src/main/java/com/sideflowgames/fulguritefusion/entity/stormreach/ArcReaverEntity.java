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

// Arc Reaver: a lean, blade-armed stalker of living arc. Fast, and every few seconds it dashes at you from up to
// 14 blocks away in a streak of sparks, slashing as it arrives. 50 health, 8 storm damage.
// Drops Reaver Blade Shards.
public class ArcReaverEntity extends StormBeast {
    private int dashCooldown = 40;
    private boolean dashHit;
    private Vec3 dashDir = Vec3.ZERO;

    public ArcReaverEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "arc_reaver");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.ARMOR, 6.0).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        groundGoals(1.25);
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        if (dashCooldown > 0) dashCooldown--;
        LivingEntity target = getTarget();
        ServerLevel server = (ServerLevel) level();
        if (action() == 1) {
            if (actionTicks() > 3) setDeltaMovement(dashDir.x, getDeltaMovement().y, dashDir.z);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.5), getZ(), 6, 0.2, 0.4, 0.2, 0.05);
            if (!dashHit && target != null && getBoundingBox().inflate(0.6).intersects(target.getBoundingBox())) {
                dashHit = true;
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                doHurtTarget((net.minecraft.server.level.ServerLevel) level(), target);
            }
            return;
        }
        if (target == null || dashCooldown > 0 || !onGround()) return;
        double d = distanceTo(target);
        if (d < 4 || d > 14 || !hasLineOfSight(target)) return;
        Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
        dashDir = dir.scale(0.95);
        setDeltaMovement(dashDir.x, 0.2, dashDir.z);
        dashHit = false;
        setAction(1, 12);
        dashCooldown = StormreachStorm.isStorming(server) ? 50 : 70;
        playSound(SoundEvents.BREEZE_JUMP, 1.0f, 1.4f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BREEZE_IDLE_GROUND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BREEZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BREEZE_DEATH;
    }
}
