package com.sideflowgames.fulguritefusion.entity.stormreach;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

// Crystal Stalker: a spider grown over with tempest crystal. Climbs cave walls, leaps at prey, and its bite
// (5 damage) leaves you Slowed for 3 seconds. Drops String and sometimes a Tempest Crystal.
public class CrystalStalkerEntity extends Spider implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.crystal_stalker.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.crystal_stalker.walk");
    private static final RawAnimation BITE = RawAnimation.begin().thenLoop("animation.crystal_stalker.attack");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CrystalStalkerEntity(EntityType<? extends Spider> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 4.0);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel serverLevel, Entity target) {
        boolean hit = super.doHurtTarget(serverLevel, target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
        }
        return hit;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_CLUSTER_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 4, s -> s.setAndContinue(
                swinging ? BITE : s.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
