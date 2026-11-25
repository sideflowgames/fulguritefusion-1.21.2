package com.sideflowgames.fulguritefusion.entity.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

// Titan Conduit: raised by the Storm Titan at the start of each regeneration cycle.
//   Ungrounded: it pours the storm into the Titan (heals it 1.5 health a second) and every 2.5 seconds arcs into
//   the nearest player within 20 blocks (10 damage). Nothing can hurt it.
//   Grounded: place a Lightning Rod within 3 blocks of it. Its current now runs into the rod instead: it stops
//   healing the Titan and stops hitting players, and it can be broken (40 health). Once grounded it stays grounded.
//   Broken: it bursts on the spot (no falling over). The burst hurts players within 4 blocks (8 damage, thrown back)
//   and blows away every Lightning Rod within 5 blocks (they drop as items), so the next cycle's conduits can't
//   rise next to a rod that's already standing.
public class TitanConduitEntity extends BossAnchorEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.titan_conduit.idle");
    private static final RawAnimation GROUNDED_ANIM = RawAnimation.begin().thenLoop("animation.titan_conduit.grounded");
    private static final EntityDataAccessor<Boolean> GROUNDED = SynchedEntityData.defineId(TitanConduitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Vector3f VIOLET = new Vector3f(0.72f, 0.42f, 1.0f);
    private static final Vector3f GOLD = new Vector3f(1.0f, 0.85f, 0.35f);

    private BlockPos rod;
    private int zapTimer = 50;

    public TitanConduitEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROUNDED, false);
    }

    public boolean isGrounded() {
        return entityData.get(GROUNDED);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) return;
        StormBossEntity boss = owner();
        if (!isGrounded() && tickCount % 10 == 0) {
            BlockPos found = findRod();
            if (found != null) {
                rod = found;
                entityData.set(GROUNDED, true);
                server.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.5f, 1.6f);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.5), getZ(), 40, 0.5, 1.0, 0.5, 0.3);
                for (Player p : server.players()) {
                    if (p.distanceToSqr(this) < 48 * 48) {
                        p.displayClientMessage(net.minecraft.network.chat.Component.literal("A conduit is grounded! Break it."), true);
                    }
                }
            }
        }
        Vec3 from = position().add(0, getBbHeight() * 0.85, 0);
        if (isGrounded()) {
            if (rod != null && tickCount % 3 == 0) beam(server, from, Vec3.atCenterOf(rod), GOLD);
            return;
        }
        if (boss != null) {
            if (tickCount % 20 == 0 && boss.getHealth() < boss.getMaxHealth()) boss.heal(1.5f);
            if (tickCount % 3 == 0) beam(server, from, boss.position().add(0, boss.getBbHeight() * 0.55, 0), VIOLET);
        }
        if (--zapTimer <= 0) {
            zapTimer = 50;
            Player p = level().getNearestPlayer(this, 20);
            if (p != null && !p.isCreative() && !p.isSpectator()) {
                beam(server, from, p.getEyePosition().subtract(0, 0.4, 0), VIOLET);
                p.hurt(damageSources().mobAttack(this), 10.0f);
                server.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 0.8f, 1.5f);
            }
        }
    }

    private BlockPos findRod() {
        BlockPos c = blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-3, -2, -3), c.offset(3, 4, 3))) {
            if (level().getBlockState(p).is(Blocks.LIGHTNING_ROD)) return p.immutable();
        }
        return null;
    }

    private static void beam(ServerLevel server, Vec3 from, Vec3 to, Vector3f color) {
        int steps = (int) Math.max(6, from.distanceTo(to) * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            server.sendParticles(new DustParticleOptions(color, 1.3f), p.x, p.y, p.z, 1, 0.08, 0.08, 0.08, 0);
        }
    }

    // Only a grounded conduit can be hurt (commands like /kill still work).
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isGrounded() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (level() instanceof ServerLevel server && invulnerableTime <= 0) {
                server.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0f, 1.4f);
                invulnerableTime = 10;
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
        server.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 3, 0.6, 0.8, 0.6, 0);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 60, 0.8, 1.2, 0.8, 0.6);
        server.sendParticles(new DustParticleOptions(VIOLET, 2.0f), c.x, c.y, c.z, 50, 1.2, 1.4, 1.2, 0);
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 1.4f);
        server.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0f, 0.6f);
        for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(4), p -> !p.isCreative() && !p.isSpectator())) {
            if (p.hurt(damageSources().mobAttack(this), 8.0f)) {
                Vec3 push = p.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.0);
                p.push(push.x, 0.4, push.z);
                p.hurtMarked = true;
            }
        }
        BlockPos center = blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-5, -3, -5), center.offset(5, 5, 5))) {
            if (server.getBlockState(p).is(Blocks.LIGHTNING_ROD)) server.destroyBlock(p, true);
        }
    }

    // Burst and vanish at once instead of toppling over.
    @Override
    protected void tickDeath() {
        if (!level().isClientSide && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(new Vec3(0, Math.min(0, getDeltaMovement().y), 0));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(isGrounded() ? 4 : 2) == 0) {
            level().addParticle(isGrounded() ? ParticleTypes.SMOKE : ParticleTypes.ELECTRIC_SPARK,
                    getRandomX(0.6), getY() + getBbHeight() * (0.6 + random.nextDouble() * 0.4), getRandomZ(0.6), 0, 0.03, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Grounded", isGrounded());
        if (rod != null) tag.putLong("Rod", rod.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(GROUNDED, tag.getBoolean("Grounded"));
        if (tag.contains("Rod")) rod = BlockPos.of(tag.getLong("Rod"));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, s -> s.setAndContinue(isGrounded() ? GROUNDED_ANIM : IDLE)));
    }
}
