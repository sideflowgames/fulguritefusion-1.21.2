package com.sideflowgames.fulguritefusion.entity;

import java.util.EnumSet;
import java.util.UUID;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

// Stormwing Hatchling: hatches from an incubated egg. It can't fly yet: it walks, leaps and glides.
// After one Minecraft day (config) it grows into an adult Stormwing.
//   - Loyal: follows the player who placed the egg, and grows into a loyal bird.
//   - Feral: does anything it can to escape (runs, leaps 3+ blocks high, glides away) and grows into a
//     strong hostile bird. Feral hatchlings look exactly like loyal ones; the only way to make one safe is to kill it.
public class StormwingHatchlingEntity extends PathfinderMob implements GeoEntity, StormwingVariant.Holder {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(StormwingHatchlingEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.stormwing.nest");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.stormwing.fly");
    private static final double GLIDE_FALL = -0.12;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean feral;
    @Nullable private UUID owner;
    private int age;

    public StormwingHatchlingEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public void setup(StormwingVariant variant, boolean feral, @Nullable UUID owner) {
        entityData.set(VARIANT, variant.ordinal());
        this.feral = feral;
        this.owner = owner;
        setPersistenceRequired();
    }

    @Override
    public StormwingVariant stormwingVariant() {
        return StormwingVariant.byId(entityData.get(VARIANT));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LeapAwayGoal(this));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 16f, 1.2, 1.6) {
            @Override
            public boolean canUse() {
                return feral && super.canUse();
            }
        });
        goalSelector.addGoal(3, new FollowOwnerGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Nullable
    private Player ownerPlayer() {
        return owner != null ? level().getPlayerByUUID(owner) : null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Glide: wings out, slow fall.
        Vec3 v = getDeltaMovement();
        if (!onGround() && v.y < GLIDE_FALL) {
            setDeltaMovement(v.x * 1.02, GLIDE_FALL, v.z * 1.02);
            fallDistance = 0;
        }
        if (!level().isClientSide && ++age >= Config.HATCHLING_GROW_MINUTES.get() * 1200) {
            growUp((ServerLevel) level());
        }
    }

    private void growUp(ServerLevel level) {
        StormwingEntity adult = ModEntities.STORMWING.get().create(level);
        if (adult == null) return;
        adult.moveTo(getX(), getY() + 0.5, getZ(), getYRot(), 0f);
        adult.raise(stormwingVariant(), feral, owner);
        if (hasCustomName()) adult.setCustomName(getCustomName());
        level.addFreshEntity(adult);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1, getZ(), 60, 1.0, 0.8, 1.0, 0.3);
        level.playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.NEUTRAL, 1.2f, 0.7f);
        discard();
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {}

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 1.5f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", entityData.get(VARIANT));
        tag.putBoolean("Feral", feral);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Age", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VARIANT, tag.getInt("Variant"));
        feral = tag.getBoolean("Feral");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        age = tag.getInt("Age");
    }

    // ---------- Animation ----------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,
                state -> state.setAndContinue(onGround() ? SIT : FLAP)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ---------- Goals ----------

    // Feral: when a player gets close, leap high (3+ blocks) away from them and glide off.
    private static class LeapAwayGoal extends Goal {
        private final StormwingHatchlingEntity chick;
        private long nextLeap;

        LeapAwayGoal(StormwingHatchlingEntity chick) {
            this.chick = chick;
            setFlags(EnumSet.of(Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!chick.feral || !chick.onGround() || chick.level().getGameTime() < nextLeap) return false;
            return chick.level().getNearestPlayer(chick, 8) != null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            Player player = chick.level().getNearestPlayer(chick, 8);
            if (player == null) return;
            Vec3 away = chick.position().subtract(player.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
            chick.setDeltaMovement(away.x * 0.7, 0.85, away.z * 0.7);
            chick.hasImpulse = true;
            chick.level().playSound(null, chick.blockPosition(), SoundEvents.PARROT_FLY, SoundSource.NEUTRAL, 1.0f, 1.4f);
            nextLeap = chick.level().getGameTime() + 30 + chick.random.nextInt(30);
        }
    }

    // Loyal: walks after its owner; catches up by teleporting if left far behind.
    private static class FollowOwnerGoal extends Goal {
        private final StormwingHatchlingEntity chick;
        private int repath;

        FollowOwnerGoal(StormwingHatchlingEntity chick) {
            this.chick = chick;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player owner = chick.ownerPlayer();
            return !chick.feral && owner != null && chick.distanceToSqr(owner) > 16;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = chick.ownerPlayer();
            return !chick.feral && owner != null && chick.distanceToSqr(owner) > 6;
        }

        @Override
        public void stop() {
            chick.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player owner = chick.ownerPlayer();
            if (owner == null) return;
            chick.getLookControl().setLookAt(owner, 10f, chick.getMaxHeadXRot());
            if (chick.distanceToSqr(owner) > 24 * 24 && owner.onGround()) {
                chick.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                chick.getNavigation().stop();
            } else if (--repath <= 0) {
                repath = 10;
                chick.getNavigation().moveTo(owner, 1.2);
            }
        }
    }
}
