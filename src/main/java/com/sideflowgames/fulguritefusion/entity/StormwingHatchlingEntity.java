package com.sideflowgames.fulguritefusion.entity;

import net.minecraft.core.HolderLookup;

import java.util.EnumSet;
import java.util.UUID;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModEntities;
import com.sideflowgames.fulguritefusion.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

// Stormwing Hatchling: hatches from an incubated egg. It can't fly yet: it walks, leaps and glides.
// After one Minecraft day (config) it grows into an adult Stormwing.
//   - Loyal: follows the player who placed the egg, and grows into a loyal bird.
//     The owner right-clicks it with an empty hand to make it stay or follow (it keeps that when it grows up).
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
    private boolean sitting;

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
        goalSelector.addGoal(1, new StayGoal(this));
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

    // The owner right-clicks with an empty hand: stay / follow.
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Main hand only: right-click fires once per hand, and two toggles would cancel out.
        if (!feral && owner != null && player.getUUID().equals(owner) && hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()) {
            if (!level().isClientSide()) {
                sitting = !sitting;
                getNavigation().stop();
                player.sendOverlayMessage(Component.literal(sitting ? "Hatchling: staying" : "Hatchling: following"));
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
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
        if (!level().isClientSide() && ++age >= Config.HATCHLING_GROW_MINUTES.get() * 1200) {
            growUp((ServerLevel) level());
        }
    }

    private void growUp(ServerLevel level) {
        StormwingEntity adult = ModEntities.STORMWING.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (adult == null) return;
        adult.snapTo(getX(), getY() + 0.5, getZ(), getYRot(), 0f);
        adult.raise(stormwingVariant(), feral, owner);
        adult.setSitting(sitting);
        if (hasCustomName()) adult.setCustomName(getCustomName());
        level.addFreshEntity(adult);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1, getZ(), 60, 1.0, 0.8, 1.0, 0.3);
        level.playSound(null, blockPosition(), ModSounds.STORMWING_CALL.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
        discard();
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {}

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
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
        return ModSounds.STORMWING_CALL.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    // Same calls as the adult, higher and softer.
    @Override
    public float getVoicePitch() {
        return 1.4f + (random.nextFloat() - 0.5f) * 0.1f;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STORMWING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STORMWING_DEATH.get();
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        CompoundTag tag = new CompoundTag();
        legacySaveStormwingHatchlingEntity(tag, this.registryAccess());
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveStormwingHatchlingEntity(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        
        tag.putInt("Variant", entityData.get(VARIANT));
        tag.putBoolean("Feral", feral);
        if (owner != null) com.sideflowgames.fulguritefusion.util.NbtBridge.putUUID(tag, "Owner", owner);
        tag.putInt("Age", age);
        tag.putBoolean("Sitting", sitting);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        legacyLoadStormwingHatchlingEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadStormwingHatchlingEntity(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        
        entityData.set(VARIANT, tag.getIntOr("Variant", 0));
        feral = tag.getBooleanOr("Feral", false);
        owner = com.sideflowgames.fulguritefusion.util.NbtBridge.hasUUID(tag, "Owner") ? com.sideflowgames.fulguritefusion.util.NbtBridge.getUUID(tag, "Owner") : null;
        age = tag.getIntOr("Age", 0);
        sitting = tag.getBooleanOr("Sitting", false);
    }

    // ---------- Animation ----------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 5,
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
            chick.needsSync = true;
            chick.level().playSound(null, chick.blockPosition(), SoundEvents.PARROT_FLY, SoundSource.NEUTRAL, 1.0f, 1.4f);
            nextLeap = chick.level().getGameTime() + 30 + chick.random.nextInt(30);
        }
    }

    // Told to stay: holds still (it still looks around).
    private static class StayGoal extends Goal {
        private final StormwingHatchlingEntity chick;

        StayGoal(StormwingHatchlingEntity chick) {
            this.chick = chick;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return chick.sitting && !chick.feral;
        }

        @Override
        public void start() {
            chick.getNavigation().stop();
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
            return !chick.feral && !chick.sitting && owner != null && chick.distanceToSqr(owner) > 16;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = chick.ownerPlayer();
            return !chick.feral && !chick.sitting && owner != null && chick.distanceToSqr(owner) > 6;
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
