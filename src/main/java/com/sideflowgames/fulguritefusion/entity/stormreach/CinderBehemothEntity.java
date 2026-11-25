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

// Cinder Behemoth (signature beast of the Ashen Scar): a hulking, four-legged furnace with a back of glowing
// cinder plates. Immune to fire and lava.
//   - jaws: 16 storm damage, sets you alight
//   - Firebreath (close): a 3-second cone of burning ash, 8 blocks long (4 storm damage every quarter second)
//   - Charge (far): lowers its head and rams you (20 storm damage, thrown back hard)
//   - Eruption: the ground under you glows, then bursts (12 storm damage, thrown up)
// 400 health. Drops a Behemoth Ember and a pile of Cinderite.
public class CinderBehemothEntity extends StormElite {
    private int breathCooldown = 60, chargeCooldown = 80, eruptCooldown = 140;
    private boolean chargeHit;
    @Nullable private Vec3 erupt;
    private Vec3 chargeDir = Vec3.ZERO;

    public CinderBehemothEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "cinder_behemoth", BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 400.0).add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 16.0).add(Attributes.ARMOR, 16.0).add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void registerGoals() {
        groundGoals(1.1);
    }

    @Override
    protected void onStormHit(Entity target) {
        target.igniteForSeconds(4);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel server = (ServerLevel) level();
        if (breathCooldown > 0) breathCooldown--;
        if (chargeCooldown > 0) chargeCooldown--;
        if (eruptCooldown > 0) eruptCooldown--;
        LivingEntity target = getTarget();
        if (erupt != null) {
            server.sendParticles(ParticleTypes.LAVA, erupt.x, erupt.y + 0.1, erupt.z, 2, 1.0, 0, 1.0, 0);
            server.sendParticles(ParticleTypes.FLAME, erupt.x, erupt.y + 0.1, erupt.z, 4, 1.2, 0, 1.2, 0.01);
            if (eruptCooldown == 120) {
                for (LivingEntity e : victims(erupt, 2.5)) {
                    if (StormDamage.hit(this, e, 12.0f)) {
                        e.push(0, 1.0, 0);
                        e.hurtMarked = true;
                        e.igniteForSeconds(4);
                    }
                }
                server.sendParticles(ParticleTypes.LAVA, erupt.x, erupt.y + 0.5, erupt.z, 30, 1.0, 1.0, 1.0, 0);
                server.sendParticles(ParticleTypes.EXPLOSION, erupt.x, erupt.y + 0.5, erupt.z, 2, 0.5, 0.5, 0.5, 0);
                server.playSound(null, BlockPos.containing(erupt), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f, 0.6f);
                erupt = null;
            }
        }
        if (action() == 1) {
            getNavigation().stop();
            if (target != null) getLookControl().setLookAt(target, 30, 30);
            Vec3 look = Vec3.directionFromRotation(0, yHeadRot);
            Vec3 mouth = position().add(0, getBbHeight() * 0.55, 0).add(look.scale(2.0));
            for (int i = 0; i < 6; i++) {
                Vec3 v = look.scale(0.6).add(random.nextGaussian() * 0.12, random.nextGaussian() * 0.08, random.nextGaussian() * 0.12);
                server.sendParticles(ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, 0, v.x, v.y, v.z, 1.0);
            }
            server.sendParticles(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y, mouth.z, 2, 0.3, 0.3, 0.3, 0.05);
            if (actionTicks() % 5 == 0) {
                for (LivingEntity e : victims(mouth, 8)) {
                    Vec3 to = e.position().subtract(mouth).normalize();
                    if (to.dot(look) > 0.75 && StormDamage.hit(this, e, 4.0f)) e.igniteForSeconds(5);
                }
            }
            return;
        }
        if (action() == 2) {
            setDeltaMovement(chargeDir.x, getDeltaMovement().y, chargeDir.z);
            if (!chargeHit && target != null && getBoundingBox().inflate(0.8).intersects(target.getBoundingBox())) {
                chargeHit = true;
                if (StormDamage.hit(this, target, 20.0f)) {
                    Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                    target.push(dir.x * 2.0, 0.6, dir.z * 2.0);
                    target.hurtMarked = true;
                }
            }
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.2), getZ(), 3, 0.6, 0.2, 0.6, 0.02);
            return;
        }
        if (target == null) return;
        double d = distanceTo(target);
        if (breathCooldown == 0 && d < 7 && hasLineOfSight(target)) {
            setAction(1, 60);
            breathCooldown = 160;
            playSound(SoundEvents.BLAZE_SHOOT, 2.0f, 0.5f);
        } else if (chargeCooldown == 0 && onGround() && d > 7 && d < 24 && hasLineOfSight(target)) {
            Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
            chargeDir = dir.scale(0.85);
            setDeltaMovement(dir.x * 0.85, 0.15, dir.z * 0.85);
            chargeHit = false;
            setAction(2, 16);
            chargeCooldown = 120;
            playSound(SoundEvents.RAVAGER_ROAR, 2.0f, 0.8f);
        } else if (eruptCooldown == 0) {
            erupt = target.position();
            eruptCooldown = 160;
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 1.2f, 0.6f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HOGLIN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.4f;
    }
}
