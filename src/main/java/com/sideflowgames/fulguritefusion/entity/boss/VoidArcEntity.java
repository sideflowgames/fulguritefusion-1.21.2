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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

// Void Arc (End mini-boss): a hooded wraith of void energy, about 6 blocks tall. Summoned by using an End Crystal
// on a Lightning Rod in the End.
//   - Hangs back out of reach, then blinks to a spot right behind you and strikes (14 damage, 2 seconds of
//     Darkness), then blinks away again. Watch for the purple flicker where it's about to appear.
//   - Every 5 seconds it fires a void bolt: lightning at your feet (9 damage).
//   - While it hangs back, about half of your arrows pass straight through it. Below half health it blinks in
//     twice as often.
//   - Shields at 2/3 and 1/3 health: it splits off 2 (then 3) Void Echoes that look just like it, and keeps
//     swapping places with them. Kill every echo.
// 320 health. Always drops the Void Shard.
public class VoidArcEntity extends StormBossEntity {
    private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("animation.void_arc.float");
    private static final RawAnimation STRIKE = RawAnimation.begin().thenLoop("animation.void_arc.attack");
    private static final EntityDataAccessor<Boolean> FADED = SynchedEntityData.defineId(VoidArcEntity.class, EntityDataSerializers.BOOLEAN);

    private int blinkTimer = 80;
    private int strikeWindow; // ticks left to land the strike after blinking in
    private int boltTimer = 100;

    public VoidArcEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 320.0).add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 8.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FADED, true);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean isFaded() {
        return entityData.get(FADED);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            steer(position().add(Math.sin(tickCount / 30.0) * 4, Math.sin(tickCount / 20.0), Math.cos(tickCount / 30.0) * 4), 0.1);
            return;
        }
        getLookControl().setLookAt(target, 60, 60);
        if (isShielded()) {
            // Circles with its echoes and every 3 seconds swaps places with one of them.
            strikeWindow = 0;
            double a = tickCount / 25.0;
            steer(target.position().add(Math.cos(a) * 7, 1.5, Math.sin(a) * 7), 0.3);
            if (tickCount % 60 == 0) swapWithEcho();
            entityData.set(FADED, true);
            return;
        }
        if (strikeWindow > 0) {
            strikeWindow--;
            steer(target.getEyePosition(), 0.6);
            if (distanceToSqr(target) < 6.0 && doHurtTarget(target)) {
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                strikeWindow = 0;
            }
            if (strikeWindow == 0) blinkAway(target);
        } else {
            // Hover at range, circling.
            double a = tickCount / 25.0;
            steer(target.position().add(Math.cos(a) * 9, 4 + Math.sin(tickCount / 15.0), Math.sin(a) * 9), 0.3);
            if (--blinkTimer <= 0) {
                blinkTimer = enraged() ? 50 : 100;
                blinkBehind(target);
            }
            if (--boltTimer <= 0) {
                boltTimer = 100;
                stormBolt(target.position(), 9.0f, 2.0);
            }
        }
        entityData.set(FADED, strikeWindow == 0);
    }

    private void blinkBehind(LivingEntity target) {
        Vec3 back = target.getLookAngle().multiply(1, 0, 1).normalize().scale(-3.0);
        Vec3 to = target.position().add(back).add(0, 0.5, 0);
        teleportFx();
        if (level().noCollision(this, getBoundingBox().move(to.subtract(position())))) teleportTo(to.x, to.y, to.z);
        teleportFx();
        strikeWindow = 30;
    }

    private void blinkAway(LivingEntity target) {
        teleportFx();
        for (int i = 0; i < 10; i++) {
            Vec3 to = target.position().add(random.nextInt(17) - 8, 4 + random.nextInt(5), random.nextInt(17) - 8);
            if (level().noCollision(this, getBoundingBox().move(to.subtract(position())))) {
                teleportTo(to.x, to.y, to.z);
                break;
            }
        }
        teleportFx();
    }

    private void swapWithEcho() {
        if (!(level() instanceof ServerLevel server)) return;
        List<VoidEchoEntity> echoes = server.getEntitiesOfClass(VoidEchoEntity.class, getBoundingBox().inflate(32), e -> e.owner() == this);
        if (echoes.isEmpty()) return;
        VoidEchoEntity echo = echoes.get(random.nextInt(echoes.size()));
        Vec3 mine = position(), theirs = echo.position();
        teleportFx();
        teleportTo(theirs.x, theirs.y, theirs.z);
        echo.teleportTo(mine.x, mine.y, mine.z);
        teleportFx();
    }

    @Override
    protected List<Entity> spawnAnchors(ServerLevel level, int shieldNumber) {
        List<Entity> out = new ArrayList<>();
        int n = shieldNumber == 1 ? 2 : 3;
        for (int i = 0; i < n; i++) {
            VoidEchoEntity echo = ModEntities.VOID_ECHO.get().create(level);
            if (echo == null) continue;
            double a = Math.PI * 2 * (i + 1) / (n + 1);
            echo.moveTo(getX() + Math.cos(a) * 4, getY(), getZ() + Math.sin(a) * 4, getYRot(), 0);
            echo.setAngle(a);
            echo.setOwner(this);
            if (getTarget() != null) echo.setTarget(getTarget());
            level.addFreshEntity(echo);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, echo.getX(), echo.getY(0.5), echo.getZ(), 60, 0.6, 1.5, 0.6, 0.1);
            out.add(echo);
        }
        teleportFx();
        return out;
    }

    @Override
    protected String shieldHint() {
        return "The Void Arc splits apart! Destroy its Void Echoes.";
    }

    @Override
    protected ParticleOptions shieldParticle() {
        return ParticleTypes.REVERSE_PORTAL;
    }

    private void teleportFx() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 60, 0.6, 1.5, 0.6, 0.05);
            server.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 0.7f);
        }
    }

    private void steer(Vec3 goal, double speed) {
        Vec3 to = goal.subtract(position());
        Vec3 desired = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(speed) : Vec3.ZERO;
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.add(desired.subtract(v).scale(0.15)));
        Vec3 m = getDeltaMovement();
        if (m.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(m.z, m.x) * (180.0 / Math.PI)) - 90.0f;
            setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.25f);
            yBodyRot = getYRot();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0), this);
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(isFaded() ? 6 : 2) == 0) {
            level().addParticle(ParticleTypes.REVERSE_PORTAL, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0, 0);
        }
    }

    // Arrows and other projectiles have a chance to pass through it while it's faded.
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isFaded() && source.getDirectEntity() != source.getEntity() && random.nextInt(2) == 0) return false;
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, s -> s.setAndContinue(swinging ? STRIKE : FLOAT)));
    }
}
