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

// Peak Colossus (signature beast of the Tempest Peaks): a walking mountain of stormstone and crackling quartz.
//   - fists: 18 storm damage
//   - Quake (close): a slam that sends an 8-block shockwave (14 storm damage, thrown upward)
//   - Boulder (far): hurls a charged boulder that bursts over 3 blocks (14 storm damage)
//   - in a storm, calls three bolts around you every 10 seconds
// 450 health, heavy armor, can't be knocked back. Drops a Colossus Heart and a pile of Stormquartz.
public class PeakColossusEntity extends StormElite {
    private int quakeCooldown = 80, boulderCooldown = 60, stormCooldown = 100;

    public PeakColossusEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "peak_colossus", BossEvent.BossBarColor.BLUE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 450.0).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 18.0).add(Attributes.ATTACK_KNOCKBACK, 2.0).add(Attributes.ARMOR, 20.0)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 2.0);
    }

    @Override
    protected void registerGoals() {
        groundGoals(1.0);
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        ServerLevel server = (ServerLevel) level();
        if (quakeCooldown > 0) quakeCooldown--;
        if (boulderCooldown > 0) boulderCooldown--;
        if (stormCooldown > 0) stormCooldown--;
        LivingEntity target = getTarget();
        if (action() == 1) {
            getNavigation().stop();
            if (actionTicks() == 10) shockwave(position(), 8.0, 14.0f, 0.9);
            return;
        }
        if (action() == 2) {
            getNavigation().stop();
            if (actionTicks() == 8 && target != null) {
                ChargedOrbEntity rock = new ChargedOrbEntity(server, this, 14.0f, 3.0f);
                rock.setPos(getX(), getY(0.9), getZ());
                double dx = target.getX() - getX(), dz = target.getZ() - getZ(), dy = target.getY(0.4) - rock.getY();
                rock.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 1.2f, 1.0f);
                server.addFreshEntity(rock);
                playSound(SoundEvents.WITHER_SHOOT, 1.2f, 0.5f);
            }
            return;
        }
        if (target == null) return;
        double d = distanceTo(target);
        if (quakeCooldown == 0 && onGround() && d < 6) {
            setAction(1, 24);
            quakeCooldown = 120;
            playSound(SoundEvents.RAVAGER_ROAR, 2.0f, 0.5f);
        } else if (boulderCooldown == 0 && d > 8 && d < 30 && hasLineOfSight(target)) {
            setAction(2, 20);
            boulderCooldown = 70;
        } else if (stormCooldown == 0 && StormreachStorm.isStorming(server)) {
            stormCooldown = 200;
            for (int i = 0; i < 3; i++) {
                callBolt(target.position().add(random.nextGaussian() * 3, 0, random.nextGaussian() * 3), 2.5, 8.0f);
            }
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 1.5f, 0.5f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }
}
