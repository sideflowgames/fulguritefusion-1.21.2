package com.sideflowgames.fulguritefusion.entity.stormreach;

import net.minecraft.core.HolderLookup;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModEntities;
import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

// Rubberback Sloth: a slow, friendly sloth of the Rubberwood Jungle whose fur soaks up rubber sap.
// Use an empty bucket on an adult to collect a Latex Bucket; it refills after 5 minutes.
// Breed and lure with Rubber Saplings. Very slow and never panics.
public class RubberbackSlothEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rubberback_sloth.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rubberback_sloth.walk");
    private static final int REFILL_TICKS = 6000;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int sapCooldown;

    public RubberbackSlothEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BreedGoal(this, 1.0));
        goalSelector.addGoal(2, new TemptGoal(this, 1.2, this::isFood, false));
        goalSelector.addGoal(3, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8, 0.002f));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModBlocks.RUBBER_SAPLING_ITEM.get());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BUCKET) && !isBaby()) {
            if (sapCooldown > 0) return InteractionResult.PASS;
            if (!level().isClientSide()) {
                sapCooldown = REFILL_TICKS;
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.LATEX_BUCKET.get())));
                level().playSound(null, blockPosition(), SoundEvents.COW_MILK, SoundSource.NEUTRAL, 1.0f, 0.8f);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && sapCooldown > 0) sapCooldown--;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        CompoundTag tag = new CompoundTag();
        legacySaveRubberbackSlothEntity(tag, this.registryAccess());
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveRubberbackSlothEntity(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        
        tag.putInt("SapCooldown", sapCooldown);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        legacyLoadRubberbackSlothEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadRubberbackSlothEntity(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        
        sapCooldown = tag.getIntOr("SapCooldown", 0);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.RUBBERBACK_SLOTH.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PANDA_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PANDA_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PANDA_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // It walks too slowly for GeckoLib's normal "is moving" check, so compare positions instead.
        controllers.add(new AnimationController<>("main", 8, s -> s.setAndContinue(isCrawling() ? WALK : IDLE)));
    }

    private boolean isCrawling() {
        double dx = getX() - xo, dz = getZ() - zo;
        return dx * dx + dz * dz > 1.0E-6;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
