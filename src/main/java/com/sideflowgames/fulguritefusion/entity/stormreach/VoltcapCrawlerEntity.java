package com.sideflowgames.fulguritefusion.entity.stormreach;

import com.sideflowgames.fulguritefusion.stormreach.Stormreach;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

// Voltcap Crawler: a beetle with a Voltcap growing on its back. At night it sits perfectly still among the
// mushrooms, looking like one. When you come within 6 blocks it scuttles at you; once beside you it swells for
// 1.5 seconds and bursts into a cloud of stinging spores (5 damage, plus Poison and Nausea in the cloud).
// Breaks no blocks. Drops a Voltcap.
public class VoltcapCrawlerEntity extends Monster implements GeoEntity {
    private static final RawAnimation HIDE = RawAnimation.begin().thenLoop("animation.voltcap_crawler.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.voltcap_crawler.walk");
    private static final RawAnimation SWELL = RawAnimation.begin().thenLoop("animation.voltcap_crawler.swell");
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(VoltcapCrawlerEntity.class, EntityDataSerializers.INT);
    private static final int FUSE_TICKS = 30;
    private static final double AMBUSH_RANGE = 6.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public VoltcapCrawlerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, (float) AMBUSH_RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUSE, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int fuse() {
        return entityData.get(FUSE);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        LivingEntity target = getTarget();
        int fuse = fuse();
        if (target != null && target.isAlive() && distanceToSqr(target) < 6.25) {
            if (fuse == 0) playSound(SoundEvents.CREEPER_PRIMED, 1.0f, 1.4f);
            fuse++;
        } else if (fuse > 0) {
            fuse--;
        }
        entityData.set(FUSE, fuse);
        if (fuse >= FUSE_TICKS) burst();
    }

    private void burst() {
        if (!(level() instanceof ServerLevel server)) return;
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.0), e -> e != this)) {
            e.hurtOrSimulate(damageSources().mobAttack(this), 5.0f);
        }
        AreaEffectCloud cloud = new AreaEffectCloud(server, getX(), getY(), getZ());
        cloud.setRadius(3.0f);
        cloud.setRadiusOnUse(-0.3f);
        cloud.setWaitTime(5);
        cloud.setDuration(120);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.setPotionContents(new PotionContents(java.util.Optional.empty(), java.util.Optional.of(0x3CC4E4),
                java.util.List.of(new MobEffectInstance(MobEffects.POISON, 100, 0), new MobEffectInstance(MobEffects.NAUSEA, 120, 0)), java.util.Optional.empty()));
        server.addFreshEntity(cloud);
        server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY(0.5), getZ(), 60, 1.5, 0.8, 1.5, 0.05);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.5), getZ(), 30, 1.0, 0.5, 1.0, 0.2);
        server.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.5f, 0.6f);
        discard();
    }

    // Spawns at night on the ground in the Voltcap Mushroom Fields (biome set in the biome file).
    public static boolean checkSpawn(EntityType<VoltcapCrawlerEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                     BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason) || reason == EntitySpawnReason.SPAWN_ITEM_USE) return true;
        ServerLevel server = level.getLevel();
        return Stormreach.is(server) && server.isDarkOutside() && level.getDifficulty() != Difficulty.PEACEFUL
                && level.canSeeSky(pos) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 3, s -> s.setAndContinue(
                fuse() > 0 ? SWELL : s.isMoving() ? WALK : HIDE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
