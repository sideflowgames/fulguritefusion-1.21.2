package com.sideflowgames.fulguritefusion.entity.boss;

import net.minecraft.core.HolderLookup;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;

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
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        if (owner() == null) {
            if (++ownerMissing > 100) discard();
        } else {
            ownerMissing = 0;
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        CompoundTag tag = new CompoundTag();
        legacySaveBossAnchorEntity(tag, this.registryAccess());
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveBossAnchorEntity(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        
        if (ownerId != null) com.sideflowgames.fulguritefusion.util.NbtBridge.putUUID(tag, "Owner", ownerId);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        legacyLoadBossAnchorEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadBossAnchorEntity(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        
        if (com.sideflowgames.fulguritefusion.util.NbtBridge.hasUUID(tag, "Owner")) ownerId = com.sideflowgames.fulguritefusion.util.NbtBridge.getUUID(tag, "Owner");
    }

    // Anchors don't hurt each other or their boss.
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source, float amount) {
        if (source.getEntity() instanceof StormBossEntity || source.getEntity() instanceof BossAnchorEntity) return false;
        return super.hurtServer(serverLevel, source, amount);
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {}

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canTeleport(Level from, Level to) {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot(net.minecraft.server.level.ServerLevel level) {
        return false;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
