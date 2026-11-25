package com.sideflowgames.fulguritefusion.entity.stormreach;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

// Glimmerwing: a small glowing moth that flutters among the Stormreach forests in loose swarms, trailing sparks.
// Harmless. Drops a little Glowstone Dust.
public class GlimmerwingEntity extends Bat implements GeoEntity {
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.glimmerwing.fly");
    private static final double SPEED = 0.12;
    private static final double STEERING = 0.08;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable private BlockPos target;

    public GlimmerwingEntity(EntityType<? extends Bat> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bat.createAttributes().add(Attributes.MAX_HEALTH, 4.0);
    }

    // Flits between spots 2-8 blocks above the ground or treetops; never roosts.
    @Override
    protected void customServerAiStep() {
        if (isResting()) setResting(false);
        if (target == null || random.nextInt(60) == 0 || target.closerToCenterThan(position(), 1.5)) {
            int x = blockPosition().getX() + random.nextInt(11) - 5;
            int z = blockPosition().getZ() + random.nextInt(11) - 5;
            int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (ground <= level().getMinBuildHeight() + 1) ground = blockPosition().getY() - 4;
            target = new BlockPos(x, ground + 2 + random.nextInt(7), z);
        }
        Vec3 to = Vec3.atCenterOf(target).subtract(position());
        Vec3 desired = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(SPEED) : Vec3.ZERO;
        Vec3 v = getDeltaMovement();
        Vec3 steered = v.add(desired.subtract(v).scale(STEERING));
        setDeltaMovement(steered);
        float yaw = (float) (Mth.atan2(steered.z, steered.x) * (180.0 / Math.PI)) - 90.0f;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.3f);
        zza = 0.5f;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.WAX_OFF, getX(), getY(0.3), getZ(), 0, -0.02, 0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 64 * 64;
    }

    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, s -> s.setAndContinue(FLY)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
