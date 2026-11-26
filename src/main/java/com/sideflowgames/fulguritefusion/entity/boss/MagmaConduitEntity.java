package com.sideflowgames.fulguritefusion.entity.boss;

import java.util.ArrayList;
import java.util.List;

import com.sideflowgames.fulguritefusion.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

// Magma Conduit (Nether mini-boss): a towering basalt golem, about 7 blocks tall, that draws power from lava.
// Summoned by throwing a Charged Spark Cell into lava in the Nether.
//   - Slams anything in reach (18 damage, big knockback, sets you on fire) and lobs fireballs from range.
//   - Every 6 seconds it calls lightning on you (8 damage).
//   - Standing in or beside lava it recharges: heals 3 health a second, glowing brighter. Keep it away from lava.
//   - Below half health it throws three fireballs at once.
//   - Shields at 2/3 and 1/3 health: 3 (then 4) Magma Pylons burst out of the ground around it. While any stands
//     it heals 1 health a second per pylon. Break them all.
// 360 health, immune to fire and lava. Always drops the Molten Heart.
public class MagmaConduitEntity extends StormBossEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.magma_conduit.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.magma_conduit.walk");
    private static final RawAnimation SLAM = RawAnimation.begin().thenLoop("animation.magma_conduit.attack");
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(MagmaConduitEntity.class, EntityDataSerializers.BOOLEAN);

    private int fireballTimer = 50;
    private int boltTimer = 120;

    public MagmaConduitEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        setPathfindingMalus(PathType.LAVA, 0.0f);
        setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0f);
        setPathfindingMalus(PathType.FIRE, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 360.0).add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.5).add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 16.0).add(Attributes.ARMOR_TOUGHNESS, 8.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean isCharging() {
        return entityData.get(CHARGING);
    }

    private boolean nearLava() {
        if (isInLava()) return true;
        for (BlockPos p : BlockPos.betweenClosed(blockPosition().offset(-2, -1, -2), blockPosition().offset(2, 0, 2))) {
            if (level().getFluidState(p).is(FluidTags.LAVA)) return true;
        }
        return false;
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        boolean charging = nearLava();
        entityData.set(CHARGING, charging);
        if (charging && tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(3.0f);
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        double dist = distanceToSqr(target);
        if (--fireballTimer <= 0 && dist > 16 && hasLineOfSight(target)) {
            fireballTimer = 60;
            int shots = enraged() ? 3 : 1;
            for (int i = 0; i < shots; i++) {
                Vec3 to = target.getEyePosition().subtract(getEyePosition()).add(random.nextGaussian() * 1.5, 0, random.nextGaussian() * 1.5);
                SmallFireball ball = new SmallFireball(level(), this, to.normalize());
                ball.setPos(getX(), getY(0.75), getZ());
                level().addFreshEntity(ball);
            }
            playSound(SoundEvents.BLAZE_SHOOT, 1.5f, 0.6f);
        }
        if (--boltTimer <= 0) {
            boltTimer = 120;
            stormBolt(target.position(), 8.0f, 2.0);
        }
    }

    @Override
    protected List<Entity> spawnAnchors(ServerLevel level, int shieldNumber) {
        List<Entity> out = new ArrayList<>();
        int n = shieldNumber == 1 ? 3 : 4;
        double start = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < n; i++) {
            MagmaPylonEntity pylon = ModEntities.MAGMA_PYLON.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (pylon == null) continue;
            double a = start + Math.PI * 2 * i / n;
            Vec3 at = groundNear(level, position().add(Math.cos(a) * 8, 0, Math.sin(a) * 8));
            pylon.snapTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
            pylon.setOwner(this);
            level.addFreshEntity(pylon);
            level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.5, at.z, 25, 0.6, 0.4, 0.6, 0.2);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1, at.z, 15, 0.5, 1.0, 0.5, 0.05);
            out.add(pylon);
        }
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.6f);
        return out;
    }

    @Override
    protected String shieldHint() {
        return "The Magma Conduit is shielded! Break the Magma Pylons.";
    }

    @Override
    protected ParticleOptions shieldParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel serverLevel, Entity target) {
        boolean hit = super.doHurtTarget(serverLevel, target);
        if (hit) {
            target.igniteForSeconds(5);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY(0.5), target.getZ(), 10, 0.4, 0.3, 0.4, 0.1);
                server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6f, 1.4f);
            }
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (random.nextInt(isCharging() ? 1 : 4) == 0) {
                level().addParticle(isCharging() ? ParticleTypes.FLAME : ParticleTypes.SMOKE, getRandomX(0.9), getRandomY(), getRandomZ(0.9), 0, 0.05, 0);
            }
            if (isCharging() && random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(1.0), getRandomY(), getRandomZ(1.0), 0, 0.1, 0);
            }
        }
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, s -> s.setAndContinue(
                swinging ? SLAM : s.isMoving() ? WALK : IDLE)));
    }
}
