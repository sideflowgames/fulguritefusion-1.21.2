package com.sideflowgames.fulguritefusion.entity.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

// Storm Core: the Thunderhead's shield anchor. A crackling orb that circles below the Thunderhead at about head
// height, zapping the nearest player within 10 blocks every 2.5 seconds (4 damage). 30 health.
public class StormCoreEntity extends BossAnchorEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.storm_core.idle");
    private double angle;
    private int zapTimer = 50;

    public StormCoreEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        StormBossEntity boss = owner();
        if (boss == null) return;
        angle += 0.03;
        Player near = level().getNearestPlayer(this, 24);
        double y = near != null ? near.getY() + 1.2 : boss.getY() - 2;
        Vec3 goal = new Vec3(boss.getX() + Math.cos(angle) * 7, y, boss.getZ() + Math.sin(angle) * 7);
        Vec3 to = goal.subtract(position());
        setDeltaMovement(to.scale(0.15));
        if (--zapTimer <= 0 && level() instanceof ServerLevel server) {
            zapTimer = 50;
            Player p = level().getNearestPlayer(this, 10);
            if (p != null && !p.isCreative() && !p.isSpectator()) {
                Vec3 from = position().add(0, 0.6, 0), at = p.getEyePosition().subtract(0, 0.4, 0);
                for (int i = 0; i <= 10; i++) {
                    Vec3 q = from.lerp(at, i / 10.0);
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0.0);
                }
                p.hurtOrSimulate(damageSources().lightningBolt(), 4.0f);
                server.playSound(null, blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 0.6f, 1.8f);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0, 0);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, s -> s.setAndContinue(IDLE)));
    }
}
