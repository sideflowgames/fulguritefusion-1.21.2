package com.sideflowgames.fulguritefusion.entity.stormreach;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.stormreach.Stormreach;
import com.sideflowgames.fulguritefusion.stormreach.StormreachStorm;
import com.sideflowgames.fulguritefusion.titan.TitanArmorEvents;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

// The Stormreach's hostile beasts, built to be very hard without a full Titan set:
//   - every hit is storm damage (ignores normal armor; Titan armor grounds it, see StormDamage)
//   - a static field around them: players nearby without a full charged Titan set take 1 storm damage a second,
//     get Weakness and lose 4,000 Fm from their gear
//   - they spawn at any time of day, but far more often during a Stormreach storm, and a beast born in a storm is
//     Storm-charged: +50% health and +30% damage
//   - lightning heals them instead of hurting them
// Animations: animation.<name>.idle / walk / attack, plus special / special2 / special3 for abilities
// (the server sets the action; it plays on every client).
public abstract class StormBeast extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(StormBeast.class, EntityDataSerializers.INT);
    private static final Identifier CHARGED_HEALTH = Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "storm_charged_health");
    private static final Identifier CHARGED_DAMAGE = Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "storm_charged_damage");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation idle, walk, attack;
    private final RawAnimation[] specials;
    protected double fieldRadius = 5.0;
    private int actionTicks;

    protected StormBeast(EntityType<? extends Monster> type, Level level, String name) {
        super(type, level);
        idle = RawAnimation.begin().thenLoop("animation." + name + ".idle");
        walk = RawAnimation.begin().thenLoop("animation." + name + ".walk");
        attack = RawAnimation.begin().thenLoop("animation." + name + ".attack");
        specials = new RawAnimation[] {
                RawAnimation.begin().thenLoop("animation." + name + ".special"),
                RawAnimation.begin().thenLoop("animation." + name + ".special2"),
                RawAnimation.begin().thenLoop("animation." + name + ".special3")};
        xpReward = 15;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION, 0);
    }

    public int action() {
        return entityData.get(ACTION);
    }

    // Starts ability animation n (1-3) for the given number of ticks.
    protected void setAction(int n, int ticks) {
        entityData.set(ACTION, n);
        actionTicks = ticks;
    }

    protected int actionTicks() {
        return actionTicks;
    }

    protected boolean busy() {
        return action() != 0;
    }

    // Standard ground hunter: swims, chases and melees players, wanders otherwise.
    protected void groundGoals(double chaseSpeed) {
        goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, chaseSpeed, true));
        goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        targetGoals();
    }

    protected void targetGoals() {
        targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this, StormBeast.class).setAlertOthers());
        targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- storm damage ----

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel serverLevel, Entity target) {
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean hit = StormDamage.hit(this, target, damage);
        if (hit) {
            if (target instanceof LivingEntity living) {
                double kb = 0.4 + getAttributeValue(Attributes.ATTACK_KNOCKBACK) * 0.5;
                living.knockback(kb, Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
            }
            setLastHurtMob(target);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(), 14, 0.3, 0.4, 0.3, 0.25);
            }
            onStormHit(target);
        }
        return hit;
    }

    protected void onStormHit(Entity target) {}

    // Players (and other living things) within r of a point, never storm beasts.
    protected List<LivingEntity> victims(Vec3 at, double r) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
                e -> e.isAlive() && !(e instanceof StormBeast) && e.distanceToSqr(at) <= r * r
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
    }

    // A storm-damage blast: hurts everything within r, knocking it away and up.
    protected void shockwave(Vec3 at, double r, float damage, double lift) {
        if (!(level() instanceof ServerLevel server)) return;
        for (LivingEntity e : victims(at, r)) {
            if (StormDamage.hit(this, e, damage)) {
                Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
                away = away.lengthSqr() < 1e-4 ? Vec3.ZERO : away.normalize().scale(0.8);
                e.push(away.x, lift, away.z);
                e.hurtMarked = true;
            }
        }
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI / 20;
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x + Math.cos(a) * r * 0.8, at.y + 0.2, at.z + Math.sin(a) * r * 0.8, 2, 0.2, 0.1, 0.2, 0.05);
        }
        server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 3, r * 0.3, 0.1, r * 0.3, 0);
        server.playSound(null, BlockPos.containing(at), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.7f);
    }

    // A harmless-looking bolt at pos that hurts what stands within r of it.
    protected void callBolt(Vec3 pos, double r, float damage) {
        if (!(level() instanceof ServerLevel server)) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (bolt != null) {
            bolt.snapTo(pos);
            bolt.setVisualOnly(true);
            server.addFreshEntity(bolt);
        }
        for (LivingEntity e : victims(pos, r)) StormDamage.hit(this, e, damage);
    }

    // A particle beam from this beast's chest to the target; storm damage if nothing blocks it.
    protected boolean beam(LivingEntity target, float damage, net.minecraft.core.particles.ParticleOptions particle) {
        if (!(level() instanceof ServerLevel server)) return false;
        Vec3 from = position().add(0, getBbHeight() * 0.7, 0), to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        int steps = (int) Math.max(6, from.distanceTo(to) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            server.sendParticles(particle, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
        }
        return hasLineOfSight(target) && StormDamage.hit(this, target, damage);
    }

    // ---- floating movement (for the beasts that fly) ----

    protected void steer(Vec3 goal, double speed, double steering) {
        Vec3 to = goal.subtract(position());
        Vec3 desired = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(Math.min(speed, to.length())) : Vec3.ZERO;
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.add(desired.subtract(v).scale(steering)));
        Vec3 m = getDeltaMovement();
        LivingEntity target = getTarget();
        if (target != null) {
            getLookControl().setLookAt(target, 30, 30);
            double dx = target.getX() - getX(), dz = target.getZ() - getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
            setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.3f);
        } else if (m.horizontalDistanceSqr() > 1.0E-5) {
            float yaw = (float) (Mth.atan2(m.z, m.x) * Mth.RAD_TO_DEG) - 90.0f;
            setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.2f);
        }
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    // ---- ticking ----

    // Veil Draught: a veiled target can't be picked or kept as a target.
    @Override
    public boolean canAttack(LivingEntity target) {
        return !target.hasEffect(com.sideflowgames.fulguritefusion.brew.ModBrews.STORM_VEIL) && super.canAttack(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            if (actionTicks > 0 && --actionTicks == 0) entityData.set(ACTION, 0);
            if (tickCount % 20 == 0) staticField(server);
            // Veil Draught: storm beasts lose track of you.
            if (getTarget() != null && getTarget().hasEffect(com.sideflowgames.fulguritefusion.brew.ModBrews.STORM_VEIL)) setTarget(null);
        } else if (random.nextInt(5) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.7), getRandomY(), getRandomZ(0.7), 0, 0.04, 0);
        }
    }

    private void staticField(ServerLevel server) {
        for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(fieldRadius),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            if (TitanArmorEvents.fullTitanSet(p)) continue;
            StormDamage.drain(p, 4_000);
            p.hurtOrSimulate(StormDamage.source(this), 1.0f);
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, false, true));
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY(0.5), p.getZ(), 6, 0.3, 0.5, 0.3, 0.1);
        }
    }

    // Lightning feeds them.
    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        heal(getMaxHealth() * 0.25f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.5), getZ(), 30, 0.5, 0.8, 0.5, 0.3);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return !isNoGravity() && super.causeFallDamage(distance, multiplier, source);
    }

    // ---- spawning ----

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (StormreachStorm.isStorming(level.getLevel())) charge();
        return result;
    }

    public void charge() {
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && !hp.hasModifier(CHARGED_HEALTH)) {
            hp.addPermanentModifier(new AttributeModifier(CHARGED_HEALTH, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            setHealth(getMaxHealth());
        }
        var dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && !dmg.hasModifier(CHARGED_DAMAGE)) {
            dmg.addPermanentModifier(new AttributeModifier(CHARGED_DAMAGE, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    // Anywhere in the Stormreach, day or night, out of bright block light. Outside a storm, two of every three
    // tries fail.
    public static <T extends Mob> boolean checkSpawn(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                     BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason) || reason == EntitySpawnReason.SPAWN_ITEM_USE) return true;
        ServerLevel server = level.getLevel();
        if (!Stormreach.is(server) || level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (com.sideflowgames.fulguritefusion.brew.StormWardBlockEntity.warded(server, pos)) return false;
        if (!StormreachStorm.isStorming(server) && random.nextInt(3) != 0) return false;
        return level.getBrightness(LightLayer.BLOCK, pos) < 8
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public float getVoicePitch() {
        return 0.6f + random.nextFloat() * 0.1f;
    }

    // ---- animation ----

    protected boolean moving(com.geckolib.animation.state.AnimationTest<?> s) {
        return s.isMoving();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 4, s -> {
            int a = action();
            if (a > 0 && a <= 3) return s.setAndContinue(specials[a - 1]);
            if (swinging) return s.setAndContinue(attack);
            return s.setAndContinue(moving(s) ? walk : idle);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
