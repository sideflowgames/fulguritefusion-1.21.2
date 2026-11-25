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

// Prism Sentinel (signature beast of the Crystal Caverns Rim): a slow crystal construct that stands guard and
// splits the storm's light into weapons.
//   - fists: 14 storm damage
//   - Prism Lances: three quick beams at you (7 storm damage each, needs a clear line)
//   - Shard Volley: five crystal shards in a spread (8 storm damage, bursting over 2 blocks)
//   - Refraction: glows bright for 3 seconds, taking 80% less damage and turning arrows away
// 420 health, very heavy armor, can't be knocked back. Drops a Sentinel Lens and a pile of Prismite Shards.
public class PrismSentinelEntity extends StormElite {
    private static final DustParticleOptions PRISM = new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.95f), 1.2f);
    private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0.5f, 0.95f, 1.0f), 1.2f);
    private int lanceCooldown = 50, volleyCooldown = 100, refractCooldown = 160;

    public PrismSentinelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "prism_sentinel", BossEvent.BossBarColor.PINK);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 420.0).add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE, 14.0).add(Attributes.ARMOR, 24.0).add(Attributes.ARMOR_TOUGHNESS, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 36.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        groundGoals(1.0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel server = (ServerLevel) level();
        if (lanceCooldown > 0) lanceCooldown--;
        if (volleyCooldown > 0) volleyCooldown--;
        if (refractCooldown > 0) refractCooldown--;
        LivingEntity target = getTarget();
        if (action() == 3) {
            server.sendParticles(PRISM, getX(), getY(0.5), getZ(), 4, 0.8, 1.2, 0.8, 0);
            return;
        }
        if (target == null) return;
        if (action() == 1) {
            getNavigation().stop();
            if (actionTicks() % 8 == 0) {
                beam(target, 7.0f, actionTicks() % 16 == 0 ? PRISM : CYAN);
                playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 1.8f);
            }
            return;
        }
        if (action() == 2) {
            getNavigation().stop();
            if (actionTicks() == 6) {
                double dx = target.getX() - getX(), dz = target.getZ() - getZ(), dy = target.getY(0.4) - getY(0.8);
                for (int i = -2; i <= 2; i++) {
                    Vec3 dir = new Vec3(dx, 0, dz).yRot(i * 0.18f);
                    ChargedOrbEntity shard = new ChargedOrbEntity(server, this, 8.0f, 2.0f);
                    shard.setPos(getX(), getY(0.8), getZ());
                    shard.shoot(dir.x, dy + Math.sqrt(dx * dx + dz * dz) * 0.15, dir.z, 1.1f, 1.0f);
                    server.addFreshEntity(shard);
                }
                playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 2.0f, 0.6f);
            }
            return;
        }
        double d = distanceTo(target);
        if (refractCooldown == 0 && getHealth() < getMaxHealth() * 0.75f) {
            setAction(3, 60);
            refractCooldown = 260;
            playSound(SoundEvents.BEACON_POWER_SELECT, 2.0f, 1.4f);
        } else if (lanceCooldown == 0 && d > 3 && d < 24 && hasLineOfSight(target)) {
            setAction(1, 25);
            lanceCooldown = 70;
        } else if (volleyCooldown == 0 && d > 5 && d < 28) {
            setAction(2, 16);
            volleyCooldown = 120;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (action() == 3) {
            if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) return false;
            amount *= 0.2f;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.AMETHYST_CLUSTER_STEP, 1.5f, 0.5f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
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
    public float getVoicePitch() {
        return 0.5f;
    }
}
