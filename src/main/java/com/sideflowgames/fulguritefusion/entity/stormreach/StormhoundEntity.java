package com.sideflowgames.fulguritefusion.entity.stormreach;

import com.sideflowgames.fulguritefusion.stormreach.Stormreach;
import com.sideflowgames.fulguritefusion.stormreach.StormreachStorm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

// Stormhound: a wolf of crackling static that only exists while a Stormreach storm rages. Hunts in packs of 3-5,
// leaps at you, and its bite (5 damage) sparks. When the storm passes, every Stormhound fades away.
// Drops Static Gel.
public class StormhoundEntity extends Monster implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.stormhound.idle");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.stormhound.walk");
    private static final RawAnimation BITE = RawAnimation.begin().thenLoop("animation.stormhound.attack");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public StormhoundEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // Hounds called by the Storm Titan don't need a storm: they hunt for 60 seconds, then fade.
    private int summonedTicks;

    public void setSummoned(int ticks) {
        summonedTicks = ticks;
        setPersistenceRequired();
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SummonedTicks", summonedTicks);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        summonedTicks = tag.getIntOr("SummonedTicks", 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            // Fades away once the storm is over (or anywhere outside a Stormreach storm); a summoned hound fades
            // when its time is up instead.
            boolean fade = summonedTicks > 0 ? --summonedTicks == 0 : tickCount % 20 == 0 && !StormreachStorm.isStorming(server);
            if (fade) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.5), getZ(), 25, 0.4, 0.4, 0.4, 0.2);
                server.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.6f, 1.6f);
                discard();
            }
        } else if (random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.05, 0);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel serverLevel, Entity target) {
        boolean hit = super.doHurtTarget(serverLevel, target);
        if (hit && level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
        }
        return hit;
    }

    // Spawns on open ground in the Stormreach, only during a storm.
    public static boolean checkSpawn(EntityType<StormhoundEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                     BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason) || reason == EntitySpawnReason.SPAWN_ITEM_USE) return true;
        ServerLevel server = level.getLevel();
        return Stormreach.is(server) && StormreachStorm.isStorming(server) && level.getDifficulty() != Difficulty.PEACEFUL
                && level.canSeeSky(pos) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 64 * 64;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_SOUNDS.get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.CLASSIC).adultSounds().growlSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_SOUNDS.get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.CLASSIC).adultSounds().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_SOUNDS.get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.CLASSIC).adultSounds().deathSound().value();
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 4, s -> s.setAndContinue(
                swinging ? BITE : s.isMoving() ? RUN : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
