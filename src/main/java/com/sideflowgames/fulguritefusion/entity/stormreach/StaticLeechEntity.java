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

// Static Leech: a fat, glowing grub that leaps onto you and latches on, drinking your health (2 storm damage every
// half second, healing itself) and the charge in your gear for up to 4 seconds, or until you hit it off (look down: it clings to your chest). A full
// charged Titan set shocks it off before it can bite. 22 health. Drops Leech Fangs.
public class StaticLeechEntity extends StormBeast {
    @Nullable private LivingEntity host;
    private int latched;
    private int latchCooldown;

    public StaticLeechEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "static_leech");
        fieldRadius = 3.0;
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.ARMOR, 2.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45f));
        groundGoals(1.2);
    }

    @Override
    protected void onStormHit(Entity target) {
        if (host == null && latchCooldown == 0 && target instanceof LivingEntity living && !TitanArmorEvents.fullTitanSet(living)) {
            host = living;
            latched = 80;
            setNoGravity(true);
            setAction(1, 80);
        }
    }

    private void unlatch() {
        host = null;
        latched = 0;
        latchCooldown = 60;
        setNoGravity(false);
        setAction(0, 0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (latchCooldown > 0) latchCooldown--;
        if (host == null) return;
        if (!host.isAlive() || --latched <= 0 || TitanArmorEvents.fullTitanSet(host)) {
            unlatch();
            return;
        }
        // Clings to the host's chest (look down to hit it off).
        float yaw = host.getYRot() * Mth.DEG_TO_RAD;
        setPos(host.getX() - Mth.sin(yaw) * 0.55, host.getY() + host.getBbHeight() * 0.5, host.getZ() + Mth.cos(yaw) * 0.55);
        setDeltaMovement(Vec3.ZERO);
        setYRot(host.getYRot() + 180);
        yBodyRot = getYRot();
        if (latched % 10 == 0 && StormDamage.hit(this, host, 2.0f)) {
            heal(2.0f);
            StormDamage.drain(host, 3_000);
            ((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.5), getZ(), 6, 0.2, 0.2, 0.2, 0.1);
            playSound(SoundEvents.HONEY_DRINK, 0.8f, 0.6f);
        }
    }

    @Override
    public boolean isPushable() {
        return host == null && super.isPushable();
    }

    @Override
    protected void pushEntities() {
        if (host == null) super.pushEntities();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && host != null && source.getEntity() != null) unlatch();
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }
}
