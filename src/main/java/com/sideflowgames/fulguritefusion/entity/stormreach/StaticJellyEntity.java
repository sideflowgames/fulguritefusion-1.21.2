package com.sideflowgames.fulguritefusion.entity.stormreach;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
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

// Static Jelly: a glowing jellyfish that drifts through the air a few blocks above meadows, plains and lakes.
// Harmless unless touched: bumping into one zaps you for 2 damage. Drops Static Gel.
public class StaticJellyEntity extends AmbientCreature implements GeoEntity {
    private static final RawAnimation DRIFT = RawAnimation.begin().thenLoop("animation.static_jelly.drift");
    private static final double SPEED = 0.05;
    private static final double STEERING = 0.03;
    private static final int ZAP_COOLDOWN = 20;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable private BlockPos target;
    private int zapCooldown;

    public StaticJellyEntity(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0);
    }

    // Drifts slowly between spots 3-9 blocks above the ground.
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (target == null || random.nextInt(200) == 0 || target.closerToCenterThan(position(), 1.5)) {
            int x = blockPosition().getX() + random.nextInt(15) - 7;
            int z = blockPosition().getZ() + random.nextInt(15) - 7;
            int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (ground <= level().getMinBuildHeight() + 1) ground = blockPosition().getY() - 5; // over the void: stay level
            target = new BlockPos(x, ground + 3 + random.nextInt(7), z);
        }
        Vec3 to = Vec3.atCenterOf(target).subtract(position());
        Vec3 desired = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(SPEED) : Vec3.ZERO;
        Vec3 v = getDeltaMovement();
        Vec3 steered = v.add(desired.subtract(v).scale(STEERING));
        setDeltaMovement(steered);
        if (steered.horizontalDistanceSqr() > 1.0E-5) {
            float yaw = (float) (Mth.atan2(steered.z, steered.x) * (180.0 / Math.PI)) - 90.0f;
            setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.05f);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            if (zapCooldown > 0) zapCooldown--;
            if (zapCooldown == 0) {
                for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.1))) {
                    if (p.isCreative() || p.isSpectator()) continue;
                    if (p.hurt(damageSources().mobAttack(this), 2.0f)) {
                        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY(0.6), p.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
                        server.playSound(null, blockPosition(), SoundEvents.BEEHIVE_SHEAR, SoundSource.NEUTRAL, 0.6f, 1.8f);
                        zapCooldown = ZAP_COOLDOWN;
                    }
                }
            }
        } else if (random.nextInt(12) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.5), getY(0.2), getRandomZ(0.5), 0, -0.05, 0);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 96 * 96;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, s -> s.setAndContinue(DRIFT)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
