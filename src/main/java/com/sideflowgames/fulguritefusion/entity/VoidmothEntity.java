package com.sideflowgames.fulguritefusion.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

// Voidmoth: an End moth that flies like a bat (erratic, hangs from ceilings to rest) and is drawn to End Rods.
// Neutral and never attacks, but every hit drains 1 XP level from the attacker and gives them Weakness.
// Drops Null Scale (1-2, more with Looting).
public class VoidmothEntity extends Bat implements GeoEntity {
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.voidmoth.fly");
    private static final RawAnimation REST = RawAnimation.begin().thenLoop("animation.voidmoth.rest");
    private static final int WEAKNESS_TICKS = 200; // 10 seconds

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    // Flight tuning: lower STEERING = smoother, wider turns.
    private static final double FLY_SPEED = 0.22;
    private static final double STEERING = 0.06;

    @Nullable private BlockPos endRod;
    @Nullable private BlockPos target;

    public VoidmothEntity(EntityType<? extends Bat> type, Level level) {
        super(type, level);
        setNoGravity(true); // it holds itself up while flying, so turns don't sag
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bat.createAttributes().add(Attributes.MAX_HEALTH, 12.0);
    }

    // Every hit drains the attacker.
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(serverLevel, source, amount);
        if (hurt && !level().isClientSide() && source.getEntity() instanceof Player player && !player.getAbilities().instabuild) {
            if (player.experienceLevel > 0) player.giveExperienceLevels(-1);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0));
            ServerLevel server = (ServerLevel) level();
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 25, 0.3, 0.5, 0.3, 0.05);
            server.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6f, 1.6f);
        }
        return hurt;
    }

    // Bat-style wandering, but smoother: the moth picks a spot nearby (often near an End Rod), curves toward
    // it gently instead of jerking around, and now and then lands under a ceiling to rest like a bat.
    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        if (isResting()) {
            super.customServerAiStep(serverLevel); // the bat's own resting and waking logic
            return;
        }
        if (tickCount % 40 == 0) {
            endRod = BlockPos.findClosestMatch(blockPosition(), 12, 6, p -> level().getBlockState(p).is(Blocks.END_ROD)).orElse(null);
        }
        if (target != null && (!level().isEmptyBlock(target) || target.getY() <= level().getMinY())) target = null;
        if (target == null || random.nextInt(80) == 0 || target.closerToCenterThan(position(), 2.0)) {
            BlockPos base = endRod != null && random.nextInt(3) != 0 ? endRod : blockPosition();
            target = new BlockPos(base.getX() + random.nextInt(9) - 4, base.getY() + random.nextInt(5) - 2,
                    base.getZ() + random.nextInt(9) - 4);
        }
        Vec3 toTarget = Vec3.atCenterOf(target).subtract(position());
        Vec3 velocity = getDeltaMovement();
        Vec3 desired = toTarget.lengthSqr() > 1.0E-4 ? toTarget.normalize().scale(FLY_SPEED) : Vec3.ZERO;
        Vec3 steered = velocity.add(desired.subtract(velocity).scale(STEERING));
        setDeltaMovement(steered);
        float yaw = (float) (Mth.atan2(steered.z, steered.x) * (180.0 / Math.PI)) - 90.0f;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.25f);
        zza = 0.5f;
        BlockPos above = blockPosition().above();
        if (random.nextInt(100) == 0 && level().getBlockState(above).isRedstoneConductor(level(), above)) {
            setResting(true);
        }
    }

    // Spawns over End stone on the outer islands (biomes set in the biome modifier).
    public static boolean checkSpawn(EntityType<VoidmothEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                     BlockPos pos, RandomSource random) {
        if (!level.getLevel().dimension().equals(Level.END)) return false;
        for (int i = 1; i <= 8; i++) {
            if (level.getBlockState(pos.below(i)).is(Blocks.END_STONE)) return true;
        }
        return false;
    }

    // Moths are silent apart from soft End sounds when hurt.
    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMITE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMITE_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("flight", 4,
                state -> state.setAndContinue(isResting() ? REST : FLY)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
