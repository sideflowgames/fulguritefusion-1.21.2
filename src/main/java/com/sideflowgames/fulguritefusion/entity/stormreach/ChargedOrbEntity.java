package com.sideflowgames.fulguritefusion.entity.stormreach;

import com.sideflowgames.fulguritefusion.ModEntities;
import com.sideflowgames.fulguritefusion.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

// A lobbed ball of charge (Voltspitter spit, Peak Colossus boulders, Prism Sentinel shards). Bursts on whatever it
// hits: storm damage to everything within its splash radius, more at the centre. Never hurts storm beasts.
public class ChargedOrbEntity extends ThrowableItemProjectile {
    private float damage = 6.0f;
    private float splash = 1.5f;

    public ChargedOrbEntity(EntityType<? extends ChargedOrbEntity> type, Level level) {
        super(type, level);
    }

    public ChargedOrbEntity(Level level, LivingEntity owner, float damage, float splash) {
        super(ModEntities.CHARGED_ORB.get(), owner, level);
        this.damage = damage;
        this.splash = splash;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CHARGED_ORB.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > 200) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel server)) return;
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(splash),
                e -> e.isAlive() && !(e instanceof StormBeast))) {
            double d = Math.sqrt(e.distanceToSqr(position()));
            float amount = (float) (damage * Math.max(0.4, 1 - d / (splash + 1)));
            StormDamage.hit(this, getOwner(), e, amount);
        }
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 30, splash * 0.4, 0.3, splash * 0.4, 0.3);
        server.sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        server.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.0f, 0.6f);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("Splash", splash);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        splash = tag.getFloat("Splash");
    }
}
