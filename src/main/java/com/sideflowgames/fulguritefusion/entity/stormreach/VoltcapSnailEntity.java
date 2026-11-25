package com.sideflowgames.fulguritefusion.entity.stormreach;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

// Voltcap Snail: a slow snail of the Voltcap Mushroom Fields with a glowing mushroom-cap shell. When hurt it
// hides in its shell and takes much less damage for a few seconds. Breed and lure with Voltcaps. Drops a Voltshell.
public class VoltcapSnailEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.voltcap_snail.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.voltcap_snail.walk");
    private static final RawAnimation HIDE = RawAnimation.begin().thenLoop("animation.voltcap_snail.hide");
    private static final int HIDE_TICKS = 80;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int hiding;

    public VoltcapSnailEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.08)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BreedGoal(this, 1.0));
        goalSelector.addGoal(2, new TemptGoal(this, 1.3, this::isFood, false));
        goalSelector.addGoal(3, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 4.0f));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModBlocks.VOLTCAP_ITEM.get());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (hiding > 0) amount *= 0.25f;
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            hiding = HIDE_TICKS;
            level().broadcastEntityEvent(this, (byte) 64);
        }
        return hurt;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 64) hiding = HIDE_TICKS;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (hiding > 0) {
            hiding--;
            navigation.stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        }
        if (level().isClientSide && random.nextInt(30) == 0) {
            level().addParticle(ParticleTypes.GLOW, getRandomX(0.4), getY(0.8), getRandomZ(0.4), 0, 0.02, 0);
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.VOLTCAP_SNAIL.get().create(level);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, s -> s.setAndContinue(
                hiding > 0 ? HIDE : s.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
