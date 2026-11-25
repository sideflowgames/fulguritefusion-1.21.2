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

// Gloomarc Spider: a big, dark spider veined with violet arcs. It climbs walls like any spider, and every few
// seconds spits a strand of static web at you from up to 12 blocks: Slowness III and 3 storm damage. Its bite
// slows too. 40 health, 7 storm damage. Drops Gloomarc Silk.
public class GloomarcSpiderEntity extends StormBeast {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> CLIMBING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(GloomarcSpiderEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final DustParticleOptions WEB = new DustParticleOptions(new Vector3f(0.75f, 0.55f, 1.0f), 1.0f);
    private int webCooldown = 60;

    public GloomarcSpiderEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "gloomarc_spider");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 5.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLIMBING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4f));
        groundGoals(1.15);
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new net.minecraft.world.entity.ai.navigation.WallClimberNavigation(this, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) entityData.set(CLIMBING, horizontalCollision);
    }

    @Override
    public boolean onClimbable() {
        return entityData.get(CLIMBING);
    }

    @Override
    public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, Vec3 motion) {
        if (!state.is(net.minecraft.world.level.block.Blocks.COBWEB)) super.makeStuckInBlock(state, motion);
    }

    @Override
    protected void onStormHit(Entity target) {
        if (target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (webCooldown > 0) webCooldown--;
        LivingEntity target = getTarget();
        if (target == null || webCooldown > 0) return;
        double d = distanceTo(target);
        if (d < 3 || d > 12 || !hasLineOfSight(target)) return;
        setAction(1, 12);
        webCooldown = 100;
        if (beam(target, 3.0f, WEB)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2), this);
        }
        playSound(SoundEvents.SLIME_SQUISH, 1.0f, 1.6f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.SPIDER_STEP, 0.15f, 0.8f);
    }
}
