package com.sideflowgames.fulguritefusion.entity;

import net.minecraft.core.HolderLookup;

import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// Arc Serpent: a coiled, half-see-through serpent of living lightning. It appears at a Stormwing's nest when the
// bird flies off to hunt, guards the nest until the bird comes back, then fades away.
//   - 300 health when it appears, draining slowly while the bird is away (never below 150 from the drain).
//   - Bite: 6 damage. Chain lightning: jumps through up to 3 nearby players and mobs.
//   - Conductive armor: every piece of iron or netherite armor adds extra shock damage.
//   - Last stand: below 25% health it bursts a ring of lightning once.
//   - Counterplay: right-click it with an Empty Spark Cell to siphon its charge (no cooldown; one cell per click,
//     never below 50 health). Each siphon weakens it by 25 and turns the cell into a Spark Cell.
//   - Drops a guaranteed Arc Core and Molted Plumes (only when killed, not when it fades away).
public class ArcSerpentEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.arc_serpent.idle");
    private static final RawAnimation SLITHER = RawAnimation.begin().thenLoop("animation.arc_serpent.slither");
    private static final RawAnimation STRIKE = RawAnimation.begin().thenPlay("animation.arc_serpent.strike");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("animation.arc_serpent.cast");

    public static final float MAX_HEALTH = 300f;
    private static final float DRAIN_FLOOR = 150f;
    private static final int FULL_DRAIN_TICKS = 12000; // 300 -> 150 over 10 minutes
    private static final float SIPHON_AMOUNT = 25f;
    private static final float SIPHON_FLOOR = 50f;
    private static final float CHAIN_DAMAGE = 4f;
    private static final float ARMOR_BONUS = 1.5f; // per iron or netherite piece
    private static final int GUARD_RANGE = 12;
    private static final int LEASH_RANGE = 24;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable private BlockPos nestPos;
    private int ticksGuarding;
    private int castCooldown = 60;
    private boolean lastStandUsed;

    public ArcSerpentEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        xpReward = 40;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    // Called by the Stormwing when it leaves: guard this nest.
    public void guard(BlockPos nest) {
        nestPos = nest;
        setHomeTo(nest, GUARD_RANGE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new StrikeGoal(this));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Anyone who comes close to the nest.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (p, lvl) -> nestPos != null && p.distanceToSqr(Vec3.atCenterOf(nestPos)) < GUARD_RANGE * GUARD_RANGE));
    }

    // ---------- Ticking ----------

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0, 0.02, 0);
            }
            return;
        }
        // Drains slowly while the bird is away, but never below 150 from the drain alone.
        ticksGuarding++;
        float cap = Math.max(DRAIN_FLOOR, MAX_HEALTH - (MAX_HEALTH - DRAIN_FLOOR) * ticksGuarding / FULL_DRAIN_TICKS);
        if (getHealth() > cap) setHealth(cap);

        LivingEntity target = getTarget();
        // Doesn't chase far from the nest.
        if (target != null && nestPos != null && target.distanceToSqr(Vec3.atCenterOf(nestPos)) > LEASH_RANGE * LEASH_RANGE) {
            setTarget(null);
            target = null;
        }
        // Chain lightning every 4 seconds at range.
        if (castCooldown > 0) castCooldown--;
        if (target != null && castCooldown == 0 && distanceToSqr(target) < 100 && hasLineOfSight(target)) {
            triggerAnim("action", "cast");
            chainLightning(target);
            castCooldown = 80;
        }
        // Last stand.
        if (!lastStandUsed && getHealth() < MAX_HEALTH * 0.25f) {
            lastStandUsed = true;
            lastStand();
        }
    }

    // ---------- Attacks ----------

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel serverLevel, Entity target) {
        triggerAnim("action", "strike");
        boolean hit = super.doHurtTarget(serverLevel, target);
        if (hit && target instanceof LivingEntity living) {
            float bonus = conductiveBonus(living);
            if (bonus > 0) {
                living.invulnerableTime = 0;
                living.hurtOrSimulate(damageSources().mobAttack(this), bonus);
            }
        }
        return hit;
    }

    // Extra shock for each piece of iron or netherite armor.
    private static float conductiveBonus(LivingEntity target) {
        int pieces = 0;
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlotGroup.ARMOR) {
            net.minecraft.world.item.equipment.Equippable gear = target.getItemBySlot(slot).get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
            if (gear != null && gear.assetId().isPresent()
                    && (gear.assetId().get().equals(net.minecraft.world.item.equipment.EquipmentAssets.IRON)
                        || gear.assetId().get().equals(net.minecraft.world.item.equipment.EquipmentAssets.NETHERITE))) {
                pieces++;
            }
        }
        return pieces * ARMOR_BONUS;
    }

    private void shock(LivingEntity victim, float damage) {
        victim.invulnerableTime = 0;
        victim.hurtOrSimulate(damageSources().mobAttack(this), damage + conductiveBonus(victim));
    }

    // Arc from the serpent to the target, then on to up to 3 more players or mobs nearby.
    private void chainLightning(LivingEntity first) {
        ServerLevel server = (ServerLevel) level();
        List<LivingEntity> hit = new ArrayList<>();
        Vec3 from = position().add(0, 1.2, 0);
        LivingEntity current = first;
        for (int jump = 0; jump < 4 && current != null; jump++) {
            Vec3 to = current.position().add(0, current.getBbHeight() / 2, 0);
            drawArc(server, from, to);
            shock(current, jump == 0 ? CHAIN_DAMAGE + 1 : CHAIN_DAMAGE);
            hit.add(current);
            from = to;
            LivingEntity last = current;
            current = server.getEntitiesOfClass(LivingEntity.class, new AABB(last.blockPosition()).inflate(5),
                            e -> e != this && !hit.contains(e) && e.isAlive() && !(e instanceof StormwingEntity)
                                    && !(e instanceof ArcSerpentEntity))
                    .stream().min((a, b) -> Double.compare(a.distanceToSqr(last), b.distanceToSqr(last))).orElse(null);
        }
        server.playSound(null, blockPosition(), ModSounds.STORMWING_ARC.get(), SoundSource.HOSTILE, 1.2f, 1.1f);
    }

    private void drawArc(ServerLevel server, Vec3 a, Vec3 b) {
        Vec3 step = b.subtract(a);
        int points = (int) (step.length() * 4) + 1;
        for (int i = 0; i <= points; i++) {
            Vec3 p = a.add(step.scale((double) i / points));
            double j = 0.15;
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x + (random.nextDouble() - 0.5) * j,
                    p.y + (random.nextDouble() - 0.5) * j, p.z + (random.nextDouble() - 0.5) * j, 1, 0, 0, 0, 0);
        }
        server.sendParticles(ParticleTypes.END_ROD, b.x, b.y, b.z, 4, 0.1, 0.1, 0.1, 0.02);
    }

    // A ring of lightning bursts out once at low health.
    private void lastStand() {
        ServerLevel server = (ServerLevel) level();
        triggerAnim("action", "cast");
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2 * i / 48;
            for (double r = 1; r <= 6; r += 1) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX() + Math.cos(angle) * r, getY() + 0.3,
                        getZ() + Math.sin(angle) * r, 1, 0, 0.1, 0, 0.05);
            }
        }
        server.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, -1), getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6),
                e -> e != this && !(e instanceof StormwingEntity))) {
            shock(e, 8f);
            Vec3 push = e.position().subtract(position()).normalize().scale(1.2);
            e.push(push.x, 0.5, push.z);
        }
        server.playSound(null, blockPosition(), ModSounds.STORMWING_ARC.get(), SoundSource.HOSTILE, 2.0f, 0.7f);
    }

    // ---------- Siphon with an Empty Spark Cell ----------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.EMPTY_SPARK_CELL.get())) return super.mobInteract(player, hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (getHealth() <= SIPHON_FLOOR) {
            player.sendOverlayMessage(Component.literal("The Arc Serpent has no charge left to siphon"));
            return InteractionResult.CONSUME;
        }
        setHealth(Math.max(SIPHON_FLOOR, getHealth() - SIPHON_AMOUNT));
        held.consume(1, player);
        ItemStack charged = new ItemStack(ModItems.SPARK_CELL.get());
        if (!player.getInventory().add(charged)) player.drop(charged, false);
        ServerLevel server = (ServerLevel) level();
        drawArc(server, position().add(0, 1, 0), player.position().add(0, 1, 0));
        server.playSound(null, blockPosition(), ModSounds.STORMWING_ARC.get(), SoundSource.PLAYERS, 0.8f, 1.6f);
        if (!player.getAbilities().instabuild) setTarget(player);
        return InteractionResult.CONSUME;
    }

    // Fades away when the bird comes home (no drops).
    public void fadeAway() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.8, getZ(), 50, 0.8, 0.6, 0.8, 0.2);
            server.playSound(null, blockPosition(), ModSounds.STORMWING_ARC.get(), SoundSource.HOSTILE, 0.8f, 0.6f);
        }
        discard();
    }

    // ---------- Misc ----------

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {}

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GUARDIAN_DEATH;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        CompoundTag tag = new CompoundTag();
        legacySaveArcSerpentEntity(tag, this.registryAccess());
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveArcSerpentEntity(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        
        if (nestPos != null) tag.put("Nest", com.sideflowgames.fulguritefusion.util.NbtBridge.writeBlockPos(nestPos));
        tag.putInt("TicksGuarding", ticksGuarding);
        tag.putBoolean("LastStandUsed", lastStandUsed);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        legacyLoadArcSerpentEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadArcSerpentEntity(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        
        com.sideflowgames.fulguritefusion.util.NbtBridge.readBlockPos(tag, "Nest").ifPresent(this::guard);
        ticksGuarding = tag.getIntOr("TicksGuarding", 0);
        lastStandUsed = tag.getBooleanOr("LastStandUsed", false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 6,
                state -> state.setAndContinue(state.isMoving() ? SLITHER : IDLE)));
        controllers.add(new AnimationController<>("action", 2, state -> PlayState.STOP)
                .triggerableAnim("strike", STRIKE)
                .triggerableAnim("cast", CAST));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static class StrikeGoal extends MeleeAttackGoal {
        StrikeGoal(ArcSerpentEntity serpent) {
            super(serpent, 1.1, true);
        }
    }
}
