package com.sideflowgames.fulguritefusion.entity.boss;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

// Shared parts of a mini-boss's shield anchors (Storm Core, Magma Pylon, Void Echo). Each belongs to one boss;
// while any of its anchors lives, that boss can't be hurt. Anchors never despawn on their own, but vanish if their
// boss is gone for 5 seconds. They drop nothing.
public abstract class BossAnchorEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerId;
    private int ownerMissing;

    protected BossAnchorEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 5;
        setPersistenceRequired();
    }

    public void setOwner(StormBossEntity owner) {
        ownerId = owner.getUUID();
    }

    // The boss this anchor protects, if it's loaded and alive (server side only).
    protected StormBossEntity owner() {
        if (ownerId != null && level() instanceof ServerLevel server && server.getEntity(ownerId) instanceof StormBossEntity boss && boss.isAlive()) {
            return boss;
        }
        return null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (owner() == null) {
            if (++ownerMissing > 100) discard();
        } else {
            ownerMissing = 0;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
    }

    // Anchors don't hurt each other or their boss.
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof StormBossEntity || source.getEntity() instanceof BossAnchorEntity) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {}

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
