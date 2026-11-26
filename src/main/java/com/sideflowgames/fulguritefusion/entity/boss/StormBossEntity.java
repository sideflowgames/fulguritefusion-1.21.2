package com.sideflowgames.fulguritefusion.entity.boss;

import net.minecraft.core.HolderLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;

// Shared parts of the three mini-bosses (Thunderhead, Magma Conduit, Void Arc):
//   - a boss health bar for players nearby
//   - never despawns, immune to lightning and fall damage, hard to knock back
//   - a "storm bolt" attack: a lightning strike (just for show, no block fire) that hurts everything near it
//     except the boss itself
//   - their key-piece drop is in each boss's loot table (always drops).
//   - Phase shields: the first time it drops to 2/3 health and again at 1/3, the boss can't be hurt and
//     raises shield anchors (Storm Cores, Magma Pylons or Void Echoes). Its health stops at the threshold, so
//     one big hit can't skip a phase. The shield drops once every anchor is destroyed.
public abstract class StormBossEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Boolean> SHIELDED = SynchedEntityData.defineId(StormBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final float[] SHIELD_AT = {2f / 3f, 1f / 3f};

    protected final ServerBossEvent bossBar;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<UUID> anchors = new ArrayList<>();
    private int shieldsUsed;
    private boolean inHurt;
    private boolean shieldPending;

    protected StormBossEntity(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        bossBar = new ServerBossEvent(getUUID(), getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        xpReward = 100;
        setPersistenceRequired();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossBar.setName(getDisplayName());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHIELDED, false);
    }

    public boolean isShielded() {
        return entityData.get(SHIELDED);
    }

    // Spawn this boss's shield anchors around it. Each one must be added to the level already.
    protected abstract List<Entity> spawnAnchors(ServerLevel level, int shieldNumber);

    // What the action bar tells players to do while the shield is up.
    protected abstract String shieldHint();

    // The particle the shield shimmers with and the anchor tethers are drawn in.
    protected abstract ParticleOptions shieldParticle();

    // Health fractions where the shield goes up (in order, highest first).
    protected float[] shieldThresholds() {
        return SHIELD_AT;
    }

    // Whether the boss can't be hurt while its anchors stand. The Storm Titan's regeneration cycle uses the same
    // anchor system but can still be hurt.
    protected boolean shieldBlocksDamage() {
        return true;
    }

    // Shown after the boss's name on the boss bar while the anchors stand.
    protected String shieldLabel() {
        return " - Shielded";
    }

    // Called when the last anchor is gone and the shield drops.
    protected void onShieldDropped() {}

    // Remove any anchors still standing (the shield then drops on the next tick).
    protected void clearAnchors() {
        if (level() instanceof ServerLevel server) {
            for (UUID id : anchors) {
                Entity e = server.getEntity(id);
                if (e != null) e.discard();
            }
        }
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (shieldPending && !isShielded()) {
            shieldPending = false;
            raiseShield();
        }
        if (isShielded() && level() instanceof ServerLevel server) {
            List<Entity> alive = new ArrayList<>();
            for (UUID id : anchors) {
                Entity e = server.getEntity(id);
                if (e != null && e.isAlive()) alive.add(e);
            }
            if (alive.isEmpty()) {
                dropShield();
            } else if (tickCount % 4 == 0) {
                // A tether from each anchor to the boss, so players can see what's keeping it safe.
                Vec3 to = position().add(0, getBbHeight() * 0.5, 0);
                for (Entity a : alive) {
                    Vec3 from = a.position().add(0, a.getBbHeight() * 0.5, 0);
                    for (int i = 0; i <= 12; i++) {
                        Vec3 p = from.lerp(to, i / 12.0);
                        server.sendParticles(shieldParticle(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                    }
                }
            }
        }
    }

    private void raiseShield() {
        if (!(level() instanceof ServerLevel server)) return;
        shieldsUsed++;
        anchors.clear();
        for (Entity e : spawnAnchors(server, shieldsUsed)) anchors.add(e.getUUID());
        if (anchors.isEmpty()) return;
        entityData.set(SHIELDED, true);
        bossBar.setName(getDisplayName().copy().append(Component.literal(shieldLabel())));
        server.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0f, 0.6f);
        tellNearby(server, shieldHint());
    }

    private void dropShield() {
        entityData.set(SHIELDED, false);
        anchors.clear();
        bossBar.setName(getDisplayName());
        if (level() instanceof ServerLevel server) {
            server.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0f, 0.6f);
            server.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0f, 0.5f);
            tellNearby(server, shieldBlocksDamage() ? "The shield breaks!" : "The cycle is broken!");
        }
        onShieldDropped();
    }

    protected void tellNearby(ServerLevel server, String text) {
        for (ServerPlayer p : server.players()) {
            if (p.distanceToSqr(this) < 64 * 64) p.sendOverlayMessage(Component.literal(text));
        }
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source, float amount) {
        if (isShielded() && shieldBlocksDamage() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (level() instanceof ServerLevel server && invulnerableTime <= 10) {
                server.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
                server.sendParticles(shieldParticle(), getX(), getY(0.5), getZ(), 20, getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.1);
                invulnerableTime = 20;
            }
            return false;
        }
        inHurt = true;
        try {
            return super.hurtServer(serverLevel, source, amount);
        } finally {
            inHurt = false;
        }
    }

    // Health can't fall past the next shield threshold in one hit; reaching it raises the shield.
    @Override
    public void setHealth(float health) {
        float[] at = shieldThresholds();
        if (inHurt && !level().isClientSide() && shieldsUsed < at.length) {
            float floor = getMaxHealth() * at[shieldsUsed];
            if (getHealth() >= floor && health <= floor) {
                health = floor;
                shieldPending = true;
            }
        }
        super.setHealth(health);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel server) {
            for (UUID id : anchors) {
                Entity e = server.getEntity(id);
                if (e != null) e.discard();
            }
        }
        super.die(source);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() && isShielded() && shieldBlocksDamage()) {
            for (int i = 0; i < 3; i++) {
                double a = random.nextDouble() * Math.PI * 2, b = random.nextDouble() * Math.PI - Math.PI / 2;
                double r = Math.max(getBbWidth(), getBbHeight()) * 0.6;
                level().addParticle(shieldParticle(), getX() + Math.cos(a) * Math.cos(b) * r, getY(0.5) + Math.sin(b) * r,
                        getZ() + Math.sin(a) * Math.cos(b) * r, 0, 0, 0);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        CompoundTag tag = new CompoundTag();
        legacySaveStormBossEntity(tag, this.registryAccess());
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveStormBossEntity(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        
        tag.putInt("ShieldsUsed", shieldsUsed);
        tag.putBoolean("Shielded", isShielded());
        tag.putInt("Anchors", anchors.size());
        for (int i = 0; i < anchors.size(); i++) com.sideflowgames.fulguritefusion.util.NbtBridge.putUUID(tag, "Anchor" + i, anchors.get(i));
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        legacyLoadStormBossEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadStormBossEntity(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        
        shieldsUsed = tag.getIntOr("ShieldsUsed", 0);
        anchors.clear();
        for (int i = 0; i < tag.getIntOr("Anchors", 0); i++) {
            if (com.sideflowgames.fulguritefusion.util.NbtBridge.hasUUID(tag, "Anchor" + i)) anchors.add(com.sideflowgames.fulguritefusion.util.NbtBridge.getUUID(tag, "Anchor" + i));
        }
        entityData.set(SHIELDED, tag.getBooleanOr("Shielded", false) && !anchors.isEmpty());
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }

    // The models are bigger than their hitboxes (hanging tendrils, orbiting shards), so don't cull them early.
    // 26.1: culling is decided by the renderer, which calls this (see StormreachMobRenderer / boss renderers).
    public AABB cullingBox() {
        return getBoundingBox().inflate(2.5);
    }

    // Anchors keep their own feet on solid ground near a spot: search a little up and down for a floor.
    protected static Vec3 groundNear(Level level, Vec3 at) {
        net.minecraft.core.BlockPos.MutableBlockPos p = new net.minecraft.core.BlockPos.MutableBlockPos();
        int x = net.minecraft.util.Mth.floor(at.x), z = net.minecraft.util.Mth.floor(at.z), y0 = net.minecraft.util.Mth.floor(at.y);
        for (int dy = 4; dy >= -10; dy--) {
            p.set(x, y0 + dy - 1, z);
            if (level.getBlockState(p).isSolid() && level.getBlockState(p.above()).isAir() && level.getBlockState(p.above(2)).isAir()) {
                return new Vec3(x + 0.5, y0 + dy, z + 0.5);
            }
        }
        return at;
    }

    // True once the boss is below half health; most bosses fight harder from then on.
    protected boolean enraged() {
        return getHealth() < getMaxHealth() * 0.5f;
    }

    // A lightning strike at a spot: the bolt is for show, the damage is ours (bosses don't hurt themselves).
    protected void stormBolt(Vec3 at, float damage, double radius) {
        if (!(level() instanceof ServerLevel server)) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (bolt != null) {
            bolt.snapTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            server.addFreshEntity(bolt);
        }
        DamageSource lightning = new DamageSource(server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.LIGHTNING_BOLT), this, this);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius, 3.0, radius),
                e -> e != this && e.isAlive() && !(e instanceof StormBossEntity))) {
            e.hurtOrSimulate(lightning, damage);
        }
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.3, at.z, 40, 1.0, 0.3, 1.0, 0.4);
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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
