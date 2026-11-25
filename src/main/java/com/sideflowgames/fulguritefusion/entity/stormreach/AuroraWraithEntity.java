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

// Aurora Wraith (signature beast of the Aurora Tundra): a tall, floating spirit draped in shifting aurora light.
//   - touch: 12 storm damage
//   - Aurora Beam: a 2-second beam of cold light (3 storm damage every quarter second, Slowness and Mining Fatigue)
//   - Gather: calls up to 2 Ion Swarms to its side (never more than 4 around it)
//   - Blink: when hurt it may vanish and reappear somewhere around you
// 320 health. Drops an Aurora Shroud and a pile of Raw Aurorite.
public class AuroraWraithEntity extends StormElite {
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.4f, 1.0f, 0.7f), 1.4f);
    private static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(0.7f, 0.45f, 1.0f), 1.4f);
    private int beamCooldown = 60, gatherCooldown = 200, blinkCooldown = 0, meleeCooldown = 0;
    @Nullable private BlockPos wander;

    public AuroraWraithEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "aurora_wraith", BossEvent.BossBarColor.GREEN);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 320.0).add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 10.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override
    protected void registerGoals() {
        targetGoals();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel server = (ServerLevel) level();
        if (beamCooldown > 0) beamCooldown--;
        if (gatherCooldown > 0) gatherCooldown--;
        if (blinkCooldown > 0) blinkCooldown--;
        if (meleeCooldown > 0) meleeCooldown--;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            if (wander == null || random.nextInt(100) == 0 || wander.closerToCenterThan(position(), 1.5)) {
                wander = blockPosition().offset(random.nextInt(17) - 8, random.nextInt(7) - 3, random.nextInt(17) - 8);
            }
            steer(Vec3.atCenterOf(wander), 0.08, 0.04);
            return;
        }
        if (action() == 1) {
            steer(position(), 0.0, 0.2);
            if (actionTicks() % 5 == 0 && beam(target, 3.0f, actionTicks() % 10 == 0 ? GREEN : VIOLET)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), this);
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 1), this);
            }
            return;
        }
        // Drifts at arm's length, a little above the target.
        steer(target.getEyePosition().add(0, 0.5, 0), 0.22, 0.06);
        if (meleeCooldown == 0 && distanceToSqr(target) < 6.0) {
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            doHurtTarget(target);
            meleeCooldown = 25;
        }
        double d = distanceTo(target);
        if (beamCooldown == 0 && d < 20 && hasLineOfSight(target)) {
            setAction(1, 40);
            beamCooldown = 130;
            playSound(SoundEvents.BEACON_ACTIVATE, 1.5f, 1.6f);
        } else if (gatherCooldown == 0) {
            gatherCooldown = 400;
            int near = server.getEntitiesOfClass(IonSwarmEntity.class, getBoundingBox().inflate(16)).size();
            for (int i = 0; i < 2 && near < 4; i++, near++) {
                IonSwarmEntity swarm = com.sideflowgames.fulguritefusion.ModEntities.ION_SWARM.get().create(server);
                if (swarm == null) continue;
                swarm.moveTo(getX() + random.nextGaussian() * 2, getY(0.5), getZ() + random.nextGaussian() * 2, 0, 0);
                swarm.setTarget(target);
                server.addFreshEntity(swarm);
            }
            setAction(2, 20);
            playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5f, 0.6f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && blinkCooldown == 0 && !level().isClientSide && getTarget() != null && random.nextInt(3) == 0) blink(getTarget());
        return hurt;
    }

    private void blink(LivingEntity target) {
        ServerLevel server = (ServerLevel) level();
        for (int i = 0; i < 10; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            Vec3 to = target.position().add(Math.cos(a) * 7, 2 + random.nextInt(3), Math.sin(a) * 7);
            if (server.noCollision(this, getBoundingBox().move(to.subtract(position())))) {
                server.sendParticles(GREEN, getX(), getY(0.5), getZ(), 30, 0.5, 1.0, 0.5, 0);
                teleportTo(to.x, to.y, to.z);
                server.sendParticles(VIOLET, getX(), getY(0.5), getZ(), 30, 0.5, 1.0, 0.5, 0);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.6f);
                blinkCooldown = 80;
                return;
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(2) == 0) {
            level().addParticle(random.nextBoolean() ? GREEN : VIOLET, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0, -0.02, 0);
        }
    }

    @Override
    protected boolean moving(software.bernie.geckolib.animation.AnimationState<?> s) {
        return getDeltaMovement().lengthSqr() > 0.004;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.4f;
    }
}
