package com.sideflowgames.fulguritefusion.entity;

import com.sideflowgames.fulguritefusion.ModEntities;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.item.SpearItem;
import com.sideflowgames.fulguritefusion.item.StormSpearItem;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

// A thrown Fulgurite Spear or Storm Spear. Works like a thrown trident: hits once, then can be picked up.
// A charged Storm Spear calls lightning where it lands (a visual bolt, no fire) that hurts everything within
// 3 blocks except whoever threw it.
public class SpearEntity extends AbstractArrow {
    private static final EntityDataAccessor<Boolean> STORM = SynchedEntityData.defineId(SpearEntity.class, EntityDataSerializers.BOOLEAN);
    // Prime Spear: stronger, and flies back to its thrower once it has hit something (like Loyalty).
    private static final EntityDataAccessor<Boolean> PRIME = SynchedEntityData.defineId(SpearEntity.class, EntityDataSerializers.BOOLEAN);
    private int returnTicks;
    private static final float STRIKE_DAMAGE = 8.0f;
    private static final double STRIKE_RADIUS = 3.0;

    private boolean dealtDamage;
    private boolean charged;
    private boolean struck;

    public SpearEntity(EntityType<? extends SpearEntity> type, Level level) {
        super(type, level);
    }

    public SpearEntity(Level level, LivingEntity owner, ItemStack stack, boolean storm, boolean charged) {
        super(ModEntities.SPEAR.get(), owner, level, stack, null);
        this.entityData.set(STORM, storm);
        this.entityData.set(PRIME, stack.getItem() instanceof com.sideflowgames.fulguritefusion.item.PrimeSpearItem);
        this.charged = charged;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STORM, false);
        builder.define(PRIME, false);
    }

    public boolean isStorm() {
        return entityData.get(STORM);
    }

    public boolean isPrime() {
        return entityData.get(PRIME);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.FULGURITE_SPEAR.get());
    }

    @Override
    public void tick() {
        if (inGroundTime > 4) dealtDamage = true;
        Entity owner = getOwner();
        if (isPrime() && (dealtDamage || isNoPhysics() || getY() < level().getMinBuildHeight()) && owner != null) {
            if (!owner.isAlive() || owner.isSpectator() || owner.level() != level()) {
                if (!level().isClientSide && pickup == Pickup.ALLOWED) spawnAtLocation(getPickupItem(), 0.1f);
                discard();
                return;
            }
            // Fly back to the thrower, through blocks, like a Loyalty trident.
            setNoPhysics(true);
            Vec3 to = owner.getEyePosition().subtract(position());
            setPosRaw(getX(), getY() + to.y * 0.045, getZ());
            if (level().isClientSide) yOld = getY();
            setDeltaMovement(getDeltaMovement().scale(0.95).add(to.normalize().scale(0.15)));
            if (returnTicks++ == 0) playSound(SoundEvents.TRIDENT_RETURN, 10.0f, 1.2f);
        }
        super.tick();
        if (level().isClientSide && isPrime() && tickCount % 2 == 0) {
            level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.75f, 0.45f, 1.0f), 1.0f),
                    getX(), getY(), getZ(), 0, 0, 0);
        }
        if (level().isClientSide && isStorm() && !inGround && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        return dealtDamage ? null : super.findHitEntity(start, end);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        float damage = isPrime() ? com.sideflowgames.fulguritefusion.item.PrimeSpearItem.THROWN
                + (charged ? com.sideflowgames.fulguritefusion.item.PrimeSpearItem.LIGHTNING : 0)
                : isStorm() ? StormSpearItem.THROWN + (charged ? StormSpearItem.LIGHTNING : 0)
                : SpearItem.FULGURITE_THROWN + SpearItem.FULGURITE_LIGHTNING;
        Entity owner = getOwner();
        DamageSource source = damageSources().trident(this, owner == null ? this : owner);
        dealtDamage = true;
        target.hurt(source, damage);
        setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        playSound(SoundEvents.TRIDENT_HIT, 1.0f, 1.0f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
        }
        if (isStorm() && charged) strike(target.position());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (isStorm() && charged) strike(result.getLocation());
    }

    private void strike(Vec3 at) {
        if (struck || !(level() instanceof ServerLevel server)) return;
        struck = true;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            server.addFreshEntity(bolt);
        }
        Entity owner = getOwner();
        DamageSource lightning = new DamageSource(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT), this, owner);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(STRIKE_RADIUS),
                e -> e != owner && e.isAlive())) {
            e.hurt(lightning, STRIKE_DAMAGE);
        }
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.3, at.z, 50, 1.2, 0.3, 1.2, 0.4);
    }

    // A returning Prime Spear can be caught by its thrower.
    @Override
    protected boolean tryPickup(net.minecraft.world.entity.player.Player player) {
        return super.tryPickup(player) || (isPrime() && isNoPhysics() && ownedBy(player) && player.getInventory().add(getPickupItem()));
    }

    @Override
    public void playerTouch(net.minecraft.world.entity.player.Player player) {
        if (!isPrime() || ownedBy(player) || getOwner() == null) super.playerTouch(player);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected void tickDespawn() {
        if (pickup != Pickup.ALLOWED) super.tickDespawn();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DealtDamage", dealtDamage);
        tag.putBoolean("Charged", charged);
        tag.putBoolean("Struck", struck);
        tag.putBoolean("Storm", isStorm());
        tag.putBoolean("Prime", isPrime());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dealtDamage = tag.getBoolean("DealtDamage");
        charged = tag.getBoolean("Charged");
        struck = tag.getBoolean("Struck");
        entityData.set(STORM, tag.getBoolean("Storm"));
        entityData.set(PRIME, tag.getBoolean("Prime"));
    }
}
