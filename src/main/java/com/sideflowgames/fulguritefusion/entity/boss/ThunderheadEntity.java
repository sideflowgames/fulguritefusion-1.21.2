package com.sideflowgames.fulguritefusion.entity.boss;

import java.util.ArrayList;
import java.util.List;

import com.sideflowgames.fulguritefusion.ModEntities;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

// Thunderhead (Overworld mini-boss): a huge storm-cloud beast, about 7 blocks across. Summoned by using a Charged
// Spark Cell on a Lightning Rod under open sky during a thunderstorm.
//   - Circles high above you and calls lightning down on you every 3 seconds (10 damage around the strike).
//   - Every 8 seconds it dive-bombs: rams straight at you (16 damage), then climbs back up.
//   - Below half health it strikes faster and calls three bolts at a time.
//   - Shields at 2/3 and 1/3 health: it sinks lower and 3 (then 4) Storm Cores circle beneath it. Break them all.
// 300 health. Always drops the Storm Eye.
public class ThunderheadEntity extends StormBossEntity {
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.thunderhead.fly");
    private static final RawAnimation DIVE = RawAnimation.begin().thenLoop("animation.thunderhead.dive");

    private static final EntityDataAccessor<Boolean> DIVING = SynchedEntityData.defineId(ThunderheadEntity.class, EntityDataSerializers.BOOLEAN);

    private int boltTimer = 60;
    private int diveTimer = 160;
    private int diving; // ticks left in the current dive
    private boolean hitThisDive;

    public ThunderheadEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 300.0).add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 10.0).add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DIVING, false);
    }

    public boolean isDiving() {
        return entityData.get(DIVING);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = getTarget();
        Vec3 goal;
        double speed;
        if (target == null || !target.isAlive()) {
            goal = position().add(Math.sin(tickCount / 40.0) * 6, 0, Math.cos(tickCount / 40.0) * 6);
            speed = 0.15;
        } else if (isShielded()) {
            // Hangs low over its cores while shielded, still calling lightning.
            double a = tickCount / 40.0;
            goal = target.position().add(Math.cos(a) * 4, 8, Math.sin(a) * 4);
            speed = 0.3;
            diving = 0;
            if (--boltTimer <= 0) {
                boltTimer = 50;
                stormBolt(target.position(), 10.0f, 2.5);
            }
        } else if (diving > 0) {
            diving--;
            goal = target.getEyePosition();
            speed = 0.9;
            if (!hitThisDive && getBoundingBox().inflate(0.8).intersects(target.getBoundingBox())) {
                doHurtTarget(target);
                hitThisDive = true;
                diving = 0;
            }
        } else {
            // Circle above the target.
            double a = tickCount / 30.0;
            goal = target.position().add(Math.cos(a) * 12, 11 + Math.sin(tickCount / 50.0) * 2, Math.sin(a) * 12);
            speed = 0.35;
            if (--boltTimer <= 0) {
                boltTimer = enraged() ? 40 : 60;
                stormBolt(target.position(), 10.0f, 2.5);
                if (enraged()) {
                    for (int i = 0; i < 2; i++) {
                        stormBolt(target.position().add(random.nextInt(9) - 4, 0, random.nextInt(9) - 4), 10.0f, 2.5);
                    }
                }
            }
            if (--diveTimer <= 0) {
                diveTimer = enraged() ? 110 : 160;
                diving = 40;
                hitThisDive = false;
                playSound(SoundEvents.PHANTOM_SWOOP, 3.0f, 0.5f);
            }
        }
        entityData.set(DIVING, diving > 0);
        steer(goal, speed);
        if (target != null) getLookControl().setLookAt(target, 30, 30);
    }

    private void steer(Vec3 goal, double speed) {
        Vec3 to = goal.subtract(position());
        Vec3 desired = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(speed) : Vec3.ZERO;
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.add(desired.subtract(v).scale(0.12)));
        Vec3 m = getDeltaMovement();
        if (m.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(m.z, m.x) * (180.0 / Math.PI)) - 90.0f;
            setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.15f);
            yBodyRot = getYRot();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(ParticleTypes.CLOUD, getRandomX(1.0), getRandomY(), getRandomZ(1.0), 0, -0.02, 0);
            }
            if (random.nextInt(6) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(1.0), getRandomY(), getRandomZ(1.0), 0, 0, 0);
            }
        } else if (tickCount % 100 == 0 && level() instanceof ServerLevel server) {
            server.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.5f, 1.4f);
        }
    }

    @Override
    protected List<Entity> spawnAnchors(ServerLevel level, int shieldNumber) {
        List<Entity> out = new ArrayList<>();
        int n = shieldNumber == 1 ? 3 : 4;
        for (int i = 0; i < n; i++) {
            StormCoreEntity core = ModEntities.STORM_CORE.get().create(level);
            if (core == null) continue;
            double a = Math.PI * 2 * i / n;
            core.moveTo(getX() + Math.cos(a) * 7, getY(), getZ() + Math.sin(a) * 7, 0, 0);
            core.setAngle(a);
            core.setOwner(this);
            level.addFreshEntity(core);
            out.add(core);
        }
        return out;
    }

    @Override
    protected String shieldHint() {
        return "The Thunderhead is shielded! Destroy the Storm Cores.";
    }

    @Override
    protected ParticleOptions shieldParticle() {
        return ParticleTypes.ELECTRIC_SPARK;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BREEZE_IDLE_AIR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BREEZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BREEZE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 6, s -> s.setAndContinue(isDiving() ? DIVE : FLY)));
    }
}
