package com.sideflowgames.fulguritefusion.entity;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModEntities;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.ModSounds;
import com.sideflowgames.fulguritefusion.worldgen.StormwingNests;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;
import java.util.function.IntSupplier;

// Stormwing: a storm eagle (3 blocks long, 2 tall, 6-block wingspan) that lives around its nest on the mountains.
//   - Clear or rainy weather: passive; it flies home and sits on its nest.
//   - Thunderstorms: active and aggressive; it circles its nest and attacks players it sees.
//   - Once it fights someone it never gives up: it chases until the player or the bird dies.
//   - Attacks: beak and claw (2 damage each). Below 40% health it adds lightning dives (8 damage),
//     drawn with particles and an electric-arc sound: no thunder, no fire, no block damage.
//   - Hitting it can shake loose a Stormwing Plume (25%, at most once every 10 seconds).
//   - Every 20-30 minutes it flies off to hunt for up to 10 minutes; an Arc Serpent guards the nest until it returns.
//   - While sitting on its nest it sheds a Molted Plume into the bowl now and then.
// Birds raised from eggs are not wild:
//   - LOYAL: follows its owner, sits/follows on the owner's right-click, defends the owner. Never despawns.
//   - FERAL: a strong hostile bird that roams and attacks everything.
// Bonding (Thunderbond Talisman, see tryBond): a wild bird in a thunderstorm, or your own loyal bird, becomes BONDED.
//   - Right-click a bonded bird (empty hand) to ride it; sneak + right-click to make it stay/follow.
//   - Riding: it faces where you look, W/S/A/D to fly, Space climbs, Shift descends.
//     Shift only lets you off once the bird is down near the ground (StormwingRiding stops mid-air dismounts).
//   - R (rebindable): lightning dive. The bird lunges where you look and calls lightning onto what you aimed at.
//   - Bonded birds get +50% health, and in a thunderstorm they fill Empty Spark Cells in the rider's inventory.
public class StormwingEntity extends PathfinderMob implements GeoEntity, StormwingVariant.Holder {
    public enum Mode { WILD, LOYAL, FERAL }

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(StormwingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NESTING = SynchedEntityData.defineId(StormwingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DIVING = SynchedEntityData.defineId(StormwingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BONDED = SynchedEntityData.defineId(StormwingEntity.class, EntityDataSerializers.BOOLEAN);

    // ---------- Riding tuning ----------
    private static final double RIDE_SPEED = 1.15;        // blocks per tick at full speed (about 23 blocks a second)
    private static final double CLIMB_SPEED = 0.55;       // blocks per tick up (Space) or down (Shift)
    private static final double DIVE_SPEED = 2.0;         // lunge speed during a lightning dive
    private static final int DIVE_LUNGE_TICKS = 10;       // how long the lunge lasts
    private static final int DIVE_COOLDOWN = 100;         // 5 seconds between dives
    private static final int DIVE_STRIKE_DELAY = 8;       // the lightning lands this many ticks after R
    private static final double DIVE_RANGE = 48;          // how far you can aim a dive
    private static final double DIVE_RADIUS = 3.5;        // the strike hurts everything this close to where it lands
    private static final float RIDER_DIVE_DAMAGE = 12.0f;
    private static final double SEAT_HEIGHT = 1.15;       // where the rider sits (raise or lower to fit the model)
    private static final double SEAT_BACK = -0.15;        // + moves the rider toward the head
    private static final int RECHARGE_TICKS = 200;        // in a storm: one Empty Spark Cell filled every 10 seconds

    // Set by the client: +1 while Space is held, -1 while Shift is held, 0 otherwise.
    public static IntSupplier riderVertical = () -> 0;

    private long nextRiderDive;          // server: game time the next dive is allowed
    private int strikeDelay;             // server: ticks until a pending dive strike lands (0 = none)
    @Nullable private Vec3 strikePoint;  // server: where it lands if no creature was aimed at
    @Nullable private UUID strikeTarget; // server: the creature that was aimed at
    @Nullable private UUID strikeRider;
    private int clientLungeTicks;        // client: lunge in progress
    private long clientNextDive;         // client: game time the next dive is allowed

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.stormwing.fly");
    private static final RawAnimation NEST = RawAnimation.begin().thenLoop("animation.stormwing.nest");
    private static final RawAnimation DIVE = RawAnimation.begin().thenLoop("animation.stormwing.dive");
    private static final RawAnimation BEAK = RawAnimation.begin().thenPlay("animation.stormwing.beak");
    private static final RawAnimation CLAW = RawAnimation.begin().thenPlay("animation.stormwing.claw");

    public static final int MAX_LOADED = 3;
    private static final float DIVE_HEALTH = 0.4f;
    private static final float DIVE_DAMAGE = 8.0f;
    private static final float PLUME_CHANCE = 0.25f;
    private static final int PLUME_COOLDOWN = 200;
    private static final DustParticleOptions CRACKLE = new DustParticleOptions(new Vector3f(0.6f, 0.85f, 1.0f), 1.0f);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable private BlockPos nestPos;
    private long lastPlumeTime = -PLUME_COOLDOWN;
    // Hunting trips: every 20-30 minutes it leaves the nest for up to 10 (see the config).
    private int exploreCooldown = -1;   // ticks until the next trip (-1 = not rolled yet)
    private int exploreTicksLeft;       // > 0 while away
    @Nullable private UUID serpentId;   // the Arc Serpent guarding the nest while it's away
    private int shedTimer = 6000;       // ticks until it may shed a plume into the nest
    private Mode mode = Mode.WILD;
    @Nullable private UUID owner;
    private boolean sitting;

    public StormwingEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NESTING, false);
        builder.define(DIVING, false);
        builder.define(VARIANT, 0);
        builder.define(BONDED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LightningDiveGoal(this));
        goalSelector.addGoal(2, new BeakAndClawGoal(this));
        goalSelector.addGoal(3, new SitGoal(this));
        goalSelector.addGoal(3, new ExploreGoal(this));
        goalSelector.addGoal(4, new FollowOwnerGoal(this));
        goalSelector.addGoal(4, new ReturnToNestGoal(this));
        goalSelector.addGoal(5, new CircleNestGoal(this));
        // Anyone who hurts it is fought to the death (a loyal bird forgives its owner).
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !(isLoyal() && mob.getLastHurtByMob() != null && mob.getLastHurtByMob().getUUID().equals(owner))
                        && super.canUse();
            }
        });
        targetSelector.addGoal(2, new DefendOwnerGoal(this));
        // Wild: during thunderstorms it hunts players on its own.
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> isWild() && level().isThundering()));
        // Loyal and aggressive: goes after monsters near its owner.
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                m -> isLoyal() && stormwingVariant().aggressive && !sitting));
        // Feral: attacks everything except other Stormwings.
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> mode == Mode.FERAL && !(e instanceof StormwingEntity) && !(e instanceof StormwingHatchlingEntity)));
    }

    // ---------- Raised birds ----------

    public boolean isWild() {
        return mode == Mode.WILD;
    }

    public boolean isLoyal() {
        return mode == Mode.LOYAL;
    }

    public boolean isFeral() {
        return mode == Mode.FERAL;
    }

    @Override
    public StormwingVariant stormwingVariant() {
        return StormwingVariant.byId(entityData.get(VARIANT));
    }

    @Nullable
    private Player ownerPlayer() {
        return owner != null ? level().getPlayerByUUID(owner) : null;
    }

    // A hatchling has grown up: set its variant, loyalty and stats.
    public void raise(StormwingVariant variant, boolean feral, @Nullable UUID owner) {
        entityData.set(VARIANT, variant.ordinal());
        mode = feral ? Mode.FERAL : Mode.LOYAL;
        this.owner = feral ? null : owner;
        nestPos = blockPosition();   // a raised bird has no nest; this is just its home spot
        setPersistenceRequired();
        modify(Attributes.MOVEMENT_SPEED, "variant_speed", variant.speed - 1.0);
        modify(Attributes.FLYING_SPEED, "variant_speed", variant.speed - 1.0);
        modify(Attributes.ATTACK_DAMAGE, "variant_attack", variant.attack - 1.0);
        if (feral) {
            modify(Attributes.MAX_HEALTH, "feral_health", 0.75);
            modify(Attributes.ATTACK_DAMAGE, "feral_attack", 1.5);
            modify(Attributes.ARMOR, "feral_armor", 0.5);
        }
        setHealth(getMaxHealth());
    }

    private void modify(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                        String name, double amount) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null || amount == 0) return;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, name);
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    public void setSitting(boolean sitting) {
        this.sitting = sitting;
    }

    // Worldgen: this bird lives at an existing nest.
    public void adoptNest(BlockPos center) {
        nestPos = center;
        setPersistenceRequired();
    }

    // The owner right-clicks with an empty hand: ride (bonded birds), or sit / follow (sneaking, or not bonded).
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Main hand only: right-click fires once per hand, and two toggles would cancel out.
        if (isLoyal() && player.getUUID().equals(owner) && hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()) {
            if (isBonded() && !player.isSecondaryUseActive()) {
                if (!level().isClientSide && !isVehicle()) {
                    sitting = false;
                    setNesting(false);
                    setTarget(null);
                    getNavigation().stop();
                    player.setYRot(getYRot());
                    player.startRiding(this);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!level().isClientSide) {
                sitting = !sitting;
                setTarget(null);
                getNavigation().stop();
                if (!sitting) setNesting(false);
                player.displayClientMessage(Component.literal(sitting ? "Stormwing: staying" : "Stormwing: following"), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    // ---------- Bonding (Thunderbond Talisman) ----------

    public boolean isBonded() {
        return entityData.get(BONDED);
    }

    // Try to bond this bird to the player. Returns null when it worked, or why it didn't.
    @Nullable
    public String tryBond(Player player) {
        if (isBonded()) return player.getUUID().equals(owner) ? "This Stormwing is already bonded to you." : "This Stormwing is bonded to someone else.";
        if (isFeral()) return "This Stormwing is feral. It will never bond.";
        if (isLoyal() && !player.getUUID().equals(owner)) return "This Stormwing is loyal to someone else.";
        if (isWild()) {
            if (!level().isThundering()) return "The bond only takes during a thunderstorm.";
            if (getTarget() != null || getLastHurtByMob() != null) return "It's too angry to bond. Let it calm down.";
            // It leaves its nest behind for good.
            dismissSerpent();
            exploreTicksLeft = 0;
            nestPos = null;
            mode = Mode.LOYAL;
            owner = player.getUUID();
            setPersistenceRequired();
        }
        entityData.set(BONDED, true);
        sitting = false;
        setTarget(null);
        modify(Attributes.MAX_HEALTH, "bonded_health", 0.5);
        setHealth(getMaxHealth());
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1, getZ(), 80, 1.5, 1.0, 1.5, 0.4);
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 30, 1.0, 1.0, 1.0, 0.05);
            server.playSound(null, blockPosition(), ModSounds.STORMWING_CALL.get(), SoundSource.NEUTRAL, 1.2f, 1.1f);
            server.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 1.0f, 1.3f);
        }
        return null;
    }

    // ---------- Riding ----------

    // The rider steers a bonded bird. (Only its owner can climb on; the owner isn't known on the client,
    // so this doesn't check it.)
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (isBonded() && getFirstPassenger() instanceof Player rider) return rider;
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    // The rider sits on its back.
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0, SEAT_HEIGHT, SEAT_BACK).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    // While ridden, the bird's own goals (wandering, attacking, following) are switched off.
    @Override
    protected void updateControlFlags() {
        super.updateControlFlags();
        if (getControllingPassenger() instanceof Player) {
            goalSelector.setControlFlag(Goal.Flag.MOVE, false);
            goalSelector.setControlFlag(Goal.Flag.JUMP, false);
            goalSelector.setControlFlag(Goal.Flag.LOOK, false);
            targetSelector.setControlFlag(Goal.Flag.TARGET, false);
        }
    }

    // Is the bird down at (or almost at) the ground? The rider can only hop off then.
    public boolean nearGround() {
        return onGround() || !level().noCollision(this, getBoundingBox().move(0, -0.3, 0));
    }

    // Flight while ridden. This runs on the rider's own game, which sends the bird's position to the server.
    @Override
    public void travel(Vec3 input) {
        if (isAlive() && getControllingPassenger() instanceof Player rider) {
            float yaw = rider.getYRot();
            setYRot(yaw);
            yRotO = yaw;
            yBodyRot = yaw;
            yHeadRot = yaw;
            setXRot(rider.getXRot() * 0.5f);

            Vec3 v;
            if (clientLungeTicks > 0) {
                // Lightning dive: lunge where the rider is looking.
                clientLungeTicks--;
                v = rider.getLookAngle().scale(DIVE_SPEED * stormwingVariant().speed);
            } else {
                float strafe = rider.xxa * 0.6f;
                float forward = rider.zza;
                if (forward < 0) forward *= 0.4f; // backing up is slow
                double rad = Math.toRadians(yaw), sin = Math.sin(rad), cos = Math.cos(rad);
                double speed = RIDE_SPEED * stormwingVariant().speed;
                Vec3 want = new Vec3((strafe * cos - forward * sin) * speed, riderVertical.getAsInt() * CLIMB_SPEED,
                        (forward * cos + strafe * sin) * speed);
                Vec3 now = getDeltaMovement();
                // Speeds up smoothly, slows a little faster.
                double ease = want.lengthSqr() > now.lengthSqr() ? 0.08 : 0.15;
                v = now.add(want.subtract(now).scale(ease));
            }
            setDeltaMovement(v);
            move(MoverType.SELF, v);
            calculateEntityAnimation(false);
            return;
        }
        super.travel(input);
    }

    // Client: R was pressed. Returns true if a dive starts (then the client tells the server).
    public boolean clientStartDive() {
        long now = level().getGameTime();
        if (now < clientNextDive) return false;
        clientNextDive = now + DIVE_COOLDOWN;
        clientLungeTicks = DIVE_LUNGE_TICKS;
        return true;
    }

    // Server: the rider pressed R. Aim where they look, and call lightning down a moment later.
    public void riderDive(ServerPlayer rider) {
        if (rider != getControllingPassenger()) return;
        long now = level().getGameTime();
        if (now < nextRiderDive) return;
        nextRiderDive = now + DIVE_COOLDOWN - 5; // a little slack for lag
        Vec3 eye = rider.getEyePosition();
        Vec3 end = eye.add(rider.getLookAngle().scale(DIVE_RANGE));
        BlockHitResult block = level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rider));
        Vec3 reach = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level(), rider, eye, reach, new AABB(eye, reach).inflate(2.0),
                e -> e instanceof LivingEntity && e.isAlive() && e != this && e != rider);
        strikeTarget = hit != null ? hit.getEntity().getUUID() : null;
        strikePoint = reach;
        strikeRider = rider.getUUID();
        strikeDelay = DIVE_STRIKE_DELAY;
        setDiving(true);
        level().playSound(null, blockPosition(), ModSounds.STORMWING_CALL.get(), SoundSource.NEUTRAL, 1.0f, 1.2f);
    }

    private void tickRiderStrike() {
        if (strikeDelay <= 0 || !(level() instanceof ServerLevel server)) return;
        if (--strikeDelay > 0) return;
        setDiving(false);
        Vec3 at = strikePoint;
        if (strikeTarget != null && server.getEntity(strikeTarget) instanceof LivingEntity target && target.isAlive()) {
            at = target.position();
        }
        if (at == null) return;
        Player rider = strikeRider != null ? server.getPlayerByUUID(strikeRider) : null;
        // Everything near the strike is hit, except the bird and its rider.
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(DIVE_RADIUS),
                t -> t != this && t != rider && t.isAlive() && !(t instanceof StormwingEntity b && b.owner != null && b.owner.equals(strikeRider)))) {
            DamageSource source = rider != null ? damageSources().playerAttack(rider) : damageSources().mobAttack(this);
            e.hurt(source, RIDER_DIVE_DAMAGE * (float) stormwingVariant().attack);
        }
        // A jagged bolt from the sky, and an arc from the bird to the spot. No fire, no block damage.
        double x = at.x, z = at.z;
        for (double y = at.y + 18; y > at.y; y -= 0.4) {
            x += (random.nextDouble() - 0.5) * 0.5;
            z += (random.nextDouble() - 0.5) * 0.5;
            server.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
        }
        Vec3 from = position().add(0, 0.8, 0);
        int steps = (int) Math.max(4, from.distanceTo(at) * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(at, i / (double) steps);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.08, 0.08, 0.08, 0.01);
        }
        server.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5, at.z, 80, 1.5, 0.4, 1.5, 0.5);
        server.playSound(null, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 2.0f, 1.0f);
        server.playSound(null, BlockPos.containing(at), ModSounds.STORMWING_ARC.get(), SoundSource.NEUTRAL, 1.6f, 0.8f);
        strikeTarget = null;
        strikePoint = null;
        strikeRider = null;
    }

    // In a thunderstorm, a ridden bird fills one Empty Spark Cell in the rider's inventory every 10 seconds.
    private void tickRecharge() {
        if (!(getControllingPassenger() instanceof ServerPlayer rider) || !level().isThundering() || tickCount % RECHARGE_TICKS != 0) return;
        var inventory = rider.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(ModItems.EMPTY_SPARK_CELL.get())) continue;
            stack.shrink(1);
            ItemStack full = new ItemStack(ModItems.SPARK_CELL.get());
            if (!inventory.add(full)) rider.drop(full, false);
            ((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, rider.getX(), rider.getY() + 1, rider.getZ(), 12, 0.4, 0.5, 0.4, 0.1);
            level().playSound(null, rider.blockPosition(), ModSounds.STORMWING_ARC.get(), SoundSource.NEUTRAL, 0.5f, 1.6f);
            return;
        }
    }

    // Someone took an egg or plumes from a nest: its Stormwing (and the Arc Serpent on guard) go after them.
    public static void alarm(Level level, BlockPos pos, Player player) {
        if (player.isCreative()) return;
        AABB area = new AABB(pos).inflate(64);
        for (StormwingEntity bird : level.getEntitiesOfClass(StormwingEntity.class, area,
                b -> b.isWild() && b.nestPos != null && b.nestPos.closerThan(pos, 4))) {
            bird.setNesting(false);
            bird.setTarget(player);
        }
        for (ArcSerpentEntity serpent : level.getEntitiesOfClass(ArcSerpentEntity.class, new AABB(pos).inflate(16))) {
            serpent.setTarget(player);
        }
    }

    // ---------- State ----------

    public boolean isNesting() {
        return entityData.get(NESTING);
    }

    private void setNesting(boolean nesting) {
        entityData.set(NESTING, nesting);
        setNoGravity(!nesting);
    }

    public boolean isDiving() {
        return entityData.get(DIVING);
    }

    private void setDiving(boolean diving) {
        entityData.set(DIVING, diving);
    }

    private BlockPos home() {
        return nestPos != null ? nestPos : blockPosition();
    }

    private boolean stormy() {
        return level().isThundering();
    }

    public boolean isExploring() {
        return exploreTicksLeft > 0;
    }

    private void rollExploreCooldown() {
        int min = Config.STORMWING_TRIP_MIN_MINUTES.get(), max = Math.max(min, Config.STORMWING_TRIP_MAX_MINUTES.get());
        exploreCooldown = (min + random.nextInt(max - min + 1)) * 1200;
    }

    // Leave the nest to hunt; an Arc Serpent appears to guard it.
    private void startExploring() {
        int away = Math.max(1, Config.STORMWING_AWAY_MAX_MINUTES.get()) * 1200;
        exploreTicksLeft = away / 2 + random.nextInt(away / 2 + 1); // half to all of the maximum
        setNesting(false);
        if (nestPos != null && level() instanceof ServerLevel server) {
            ArcSerpentEntity serpent = ModEntities.ARC_SERPENT.get().create(server);
            if (serpent != null) {
                serpent.moveTo(nestPos.getX() + 0.5, nestPos.getY() + 1, nestPos.getZ() + 0.5, random.nextFloat() * 360f, 0);
                serpent.guard(nestPos);
                server.addFreshEntity(serpent);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, serpent.getX(), serpent.getY() + 0.8, serpent.getZ(), 50, 0.8, 0.6, 0.8, 0.2);
                server.playSound(null, nestPos, ModSounds.STORMWING_ARC.get(), SoundSource.HOSTILE, 1.0f, 0.8f);
                serpentId = serpent.getUUID();
            }
        }
    }

    // Home again: the guardian fades away.
    private void dismissSerpent() {
        if (serpentId != null && level() instanceof ServerLevel server
                && server.getEntity(serpentId) instanceof ArcSerpentEntity serpent) {
            serpent.fadeAway();
        }
        serpentId = null;
    }

    // ---------- Spawning ----------

    // Mountain biomes only (set in the biome modifier), under open sky, and at most 3 loaded at once.
    public static boolean checkSpawn(EntityType<StormwingEntity> type, ServerLevelAccessor level, MobSpawnType reason,
                                     BlockPos pos, RandomSource random) {
        if (!level.canSeeSky(pos) || pos.getY() < 90) return false;
        return level.getLevel().getEntities(ModEntities.STORMWING.get(), StormwingEntity::isWild).size() < MAX_LOADED;
    }

    // A wild Stormwing moves into a free nest nearby, or builds its own where it first appears. It never despawns.
    private void findOrBuildNest() {
        setPersistenceRequired();
        BlockPos found = StormwingNests.findNear(level(), blockPosition(), 16);
        if (found != null && level().getEntitiesOfClass(StormwingEntity.class, new AABB(found).inflate(8),
                b -> b != this && found.equals(b.nestPos)).isEmpty()) {
            nestPos = found;
            return;
        }
        BlockPos ground = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPosition());
        nestPos = StormwingNests.build(level(), ground, random, random.nextFloat() < Config.STORMWING_EGG_CHANCE.get());
        // Hop up out of the nest so it isn't stuck inside it.
        setPos(getX(), ground.getY() + 1.0, getZ());
    }

    // ---------- Combat ----------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            setNesting(false);
            ServerLevel server = (ServerLevel) level();
            // Lightning flares burst from its body.
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1, getZ(), 40, 1.5, 0.6, 1.5, 0.3);
            server.playSound(null, blockPosition(), ModSounds.STORMWING_ARC.get(), SoundSource.HOSTILE, 0.7f, 1.4f);
            // Shake loose a plume now and then.
            if (source.getEntity() instanceof Player && level().getGameTime() - lastPlumeTime >= PLUME_COOLDOWN
                    && random.nextFloat() < PLUME_CHANCE) {
                lastPlumeTime = level().getGameTime();
                spawnAtLocation(new ItemStack(ModItems.STORMWING_PLUME.get()));
            }
        }
        return hurt;
    }

    // Beak or claw, picked at random; both deal the base 2 damage.
    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim("attack", random.nextBoolean() ? "beak" : "claw");
        return super.doHurtTarget(target);
    }

    // Lightning-proof and never takes fall damage.
    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {}

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    private void strike(LivingEntity target) {
        ServerLevel server = (ServerLevel) level();
        target.hurt(damageSources().mobAttack(this), DIVE_DAMAGE * (float) stormwingVariant().attack * (isFeral() ? 2.5f : 1f));
        // Draw a jagged bolt from the sky down to the target with particles. No real lightning: no fire, no thunder.
        Vec3 bottom = target.position();
        double x = bottom.x, z = bottom.z;
        for (double y = bottom.y + 18; y > bottom.y; y -= 0.4) {
            x += (random.nextDouble() - 0.5) * 0.5;
            z += (random.nextDouble() - 0.5) * 0.5;
            server.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
        }
        server.sendParticles(ParticleTypes.FLASH, bottom.x, bottom.y + 1, bottom.z, 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, bottom.x, bottom.y + 0.5, bottom.z, 60, 1.0, 0.3, 1.0, 0.4);
        server.playSound(null, target.blockPosition(), ModSounds.STORMWING_ARC.get(), SoundSource.HOSTILE, 1.6f, 0.9f);
    }

    // ---------- Ticking ----------

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            tickRiderStrike();
            tickRecharge();
            if (isVehicle() && isNesting()) setNesting(false); // gravity off while ridden
            if (isWild()) {
                if (nestPos == null) findOrBuildNest();
                if (exploreCooldown < 0) rollExploreCooldown();
                if (isExploring()) {
                    exploreTicksLeft--;
                } else if (isNesting() && getTarget() == null && --exploreCooldown <= 0) {
                    rollExploreCooldown();
                    startExploring();
                }
                // Shed a plume into the nest now and then (about every 5 minutes, half the time).
                if (isNesting() && --shedTimer <= 0) {
                    shedTimer = 6000;
                    if (random.nextBoolean()) StormwingNests.shedPlume(level(), nestPos, random);
                }
            }
        }
        // Idle crackle across the wings.
        if (level().isClientSide && random.nextInt(isNesting() ? 8 : 3) == 0) {
            double side = (random.nextBoolean() ? 1 : -1) * (0.5 + random.nextDouble() * 2.5);
            double yaw = Math.toRadians(yBodyRot);
            level().addParticle(CRACKLE, getX() + Math.cos(yaw) * side, getY() + 1.1, getZ() + Math.sin(yaw) * side, 0, 0, 0);
            if (random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + Math.cos(yaw) * side, getY() + 1.1,
                        getZ() + Math.sin(yaw) * side, 0, 0.02, 0);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isNesting() ? null : ModSounds.STORMWING_CALL.get();
    }

    // Calls are a few seconds long: call now and then, not constantly.
    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STORMWING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STORMWING_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (nestPos != null) tag.put("Nest", NbtUtils.writeBlockPos(nestPos));
        tag.putBoolean("Nesting", isNesting());
        tag.putInt("ExploreCooldown", exploreCooldown);
        tag.putInt("ExploreTicksLeft", exploreTicksLeft);
        if (serpentId != null) tag.putUUID("Serpent", serpentId);
        tag.putString("Mode", mode.name());
        tag.putInt("Variant", entityData.get(VARIANT));
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("Sitting", sitting);
        tag.putInt("ShedTimer", shedTimer);
        tag.putBoolean("Bonded", isBonded());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nestPos = NbtUtils.readBlockPos(tag, "Nest").orElse(null);
        setNesting(tag.getBoolean("Nesting"));
        exploreCooldown = tag.contains("ExploreCooldown") ? tag.getInt("ExploreCooldown") : -1;
        exploreTicksLeft = tag.getInt("ExploreTicksLeft");
        serpentId = tag.hasUUID("Serpent") ? tag.getUUID("Serpent") : null;
        try {
            mode = tag.contains("Mode") ? Mode.valueOf(tag.getString("Mode")) : Mode.WILD;
        } catch (IllegalArgumentException e) {
            mode = Mode.WILD;
        }
        entityData.set(VARIANT, tag.getInt("Variant"));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        sitting = tag.getBoolean("Sitting");
        if (tag.contains("ShedTimer")) shedTimer = tag.getInt("ShedTimer");
        entityData.set(BONDED, tag.getBoolean("Bonded"));
    }

    // ---------- Animation ----------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 8, state -> {
            if (isDiving()) return state.setAndContinue(DIVE);
            return state.setAndContinue(isNesting() ? NEST : FLY);
        }));
        controllers.add(new AnimationController<>(this, "attack", 2, state -> PlayState.STOP)
                .triggerableAnim("beak", BEAK)
                .triggerableAnim("claw", CLAW));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ---------- Goals ----------

    // Melee with beak and claw.
    private static class BeakAndClawGoal extends MeleeAttackGoal {
        BeakAndClawGoal(StormwingEntity bird) {
            super(bird, 1.2, true);
        }

        @Override
        public boolean canUse() {
            return !((StormwingEntity) mob).isDiving() && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            ((StormwingEntity) mob).setNesting(false);
        }
    }

    // Below 40% health: climb above the target, then dive and strike with lightning.
    private static class LightningDiveGoal extends Goal {
        private final StormwingEntity bird;
        private int phaseTicks;
        private boolean diving;
        private long nextDive;

        LightningDiveGoal(StormwingEntity bird) {
            this.bird = bird;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = bird.getTarget();
            return target != null && target.isAlive() && bird.getHealth() < bird.getMaxHealth() * DIVE_HEALTH
                    && bird.level().getGameTime() >= nextDive;
        }

        @Override
        public boolean canContinueToUse() {
            return bird.getTarget() != null && bird.getTarget().isAlive() && phaseTicks < 140;
        }

        @Override
        public void start() {
            phaseTicks = 0;
            diving = false;
            bird.setNesting(false);
        }

        @Override
        public void stop() {
            bird.setDiving(false);
            nextDive = bird.level().getGameTime() + 160;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = bird.getTarget();
            if (target == null) return;
            phaseTicks++;
            bird.getLookControl().setLookAt(target);
            if (!diving) {
                // Climb to about 10 blocks above the target.
                Vec3 above = target.position().add(0, 10, 0);
                bird.getMoveControl().setWantedPosition(above.x, above.y, above.z, 1.4);
                if (bird.position().distanceToSqr(above) < 9 || phaseTicks > 80) {
                    diving = true;
                    bird.setDiving(true);
                    bird.level().playSound(null, bird.blockPosition(), ModSounds.STORMWING_CALL.get(), SoundSource.HOSTILE, 0.9f, 1.15f);
                }
            } else {
                // Dive straight at the target.
                Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0).subtract(bird.position());
                bird.setDeltaMovement(to.normalize().scale(1.1));
                if (to.lengthSqr() < 6.0 || phaseTicks > 130) {
                    bird.strike(target);
                    phaseTicks = 999; // done
                }
            }
        }
    }

    // On a hunting trip: roam far from the nest, high up, picking a new spot each time it arrives.
    private static class ExploreGoal extends Goal {
        private final StormwingEntity bird;
        @Nullable private Vec3 waypoint;

        ExploreGoal(StormwingEntity bird) {
            this.bird = bird;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return (bird.isExploring() || bird.isFeral()) && bird.getTarget() == null;
        }

        @Override
        public void start() {
            waypoint = null;
            bird.setNesting(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (waypoint == null || bird.position().distanceToSqr(waypoint) < 9) {
                BlockPos home = bird.home();
                double angle = bird.random.nextDouble() * Math.PI * 2;
                double dist = 48 + bird.random.nextDouble() * 48;
                waypoint = new Vec3(home.getX() + Math.cos(angle) * dist, home.getY() + 10 + bird.random.nextInt(20),
                        home.getZ() + Math.sin(angle) * dist);
            }
            bird.getMoveControl().setWantedPosition(waypoint.x, waypoint.y, waypoint.z, 1.0);
        }
    }

    // Out of a thunderstorm and not fighting: fly home and sit on the nest.
    private static class ReturnToNestGoal extends Goal {
        private final StormwingEntity bird;

        ReturnToNestGoal(StormwingEntity bird) {
            this.bird = bird;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return bird.isWild() && !bird.stormy() && bird.getTarget() == null && !bird.isExploring();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            bird.setNesting(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 nest = Vec3.atBottomCenterOf(bird.home()).add(0, 1.0, 0);
            if (bird.position().distanceToSqr(nest) > 2.25) {
                if (bird.isNesting()) bird.setNesting(false);
                bird.getMoveControl().setWantedPosition(nest.x, nest.y, nest.z, 0.8);
            } else if (!bird.isNesting()) {
                bird.setNesting(true);
                bird.setDeltaMovement(Vec3.ZERO);
                bird.dismissSerpent();
            }
        }
    }

    // Loyal bird told to stay: lands where it is and sits.
    private static class SitGoal extends Goal {
        private final StormwingEntity bird;

        SitGoal(StormwingEntity bird) {
            this.bird = bird;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return bird.isLoyal() && bird.sitting;
        }

        @Override
        public void start() {
            bird.getNavigation().stop();
            bird.setNesting(true); // gravity on: it drops to the ground and sits
        }

        @Override
        public void stop() {
            bird.setNesting(false);
        }
    }

    // Loyal bird: stays with its owner, circling overhead; teleports back if left far behind.
    private static class FollowOwnerGoal extends Goal {
        private final StormwingEntity bird;
        private float angle;

        FollowOwnerGoal(StormwingEntity bird) {
            this.bird = bird;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return bird.isLoyal() && !bird.sitting && bird.getTarget() == null && bird.ownerPlayer() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            bird.setNesting(false);
            angle = bird.random.nextFloat() * 360f;
        }

        @Override
        public void tick() {
            Player owner = bird.ownerPlayer();
            if (owner == null) return;
            double dist = bird.distanceToSqr(owner);
            if (dist > 48 * 48) {
                bird.teleportTo(owner.getX(), owner.getY() + 4, owner.getZ());
                bird.getNavigation().stop();
            } else if (dist > 12 * 12) {
                bird.getMoveControl().setWantedPosition(owner.getX(), owner.getY() + 4, owner.getZ(), 1.3);
            } else {
                angle += 2.5f;
                double x = owner.getX() + Math.cos(Math.toRadians(angle)) * 6;
                double z = owner.getZ() + Math.sin(Math.toRadians(angle)) * 6;
                bird.getMoveControl().setWantedPosition(x, owner.getY() + 5, z, 0.8);
            }
        }
    }

    // Loyal bird: attacks whatever hurts its owner, or whatever its owner attacks.
    private static class DefendOwnerGoal extends TargetGoal {
        private final StormwingEntity bird;
        private int lastHurtBy, lastHurt;
        @Nullable private LivingEntity pick;

        DefendOwnerGoal(StormwingEntity bird) {
            super(bird, false);
            this.bird = bird;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!bird.isLoyal() || bird.sitting) return false;
            Player owner = bird.ownerPlayer();
            if (owner == null) return false;
            if (owner.getLastHurtByMobTimestamp() != lastHurtBy && owner.getLastHurtByMob() != null) {
                pick = owner.getLastHurtByMob();
                return canAttack(pick, TargetingConditions.DEFAULT) && pick != bird;
            }
            if (owner.getLastHurtMobTimestamp() != lastHurt && owner.getLastHurtMob() != null) {
                pick = owner.getLastHurtMob();
                return canAttack(pick, TargetingConditions.DEFAULT) && pick != bird;
            }
            return false;
        }

        @Override
        public void start() {
            Player owner = bird.ownerPlayer();
            if (owner != null) {
                lastHurtBy = owner.getLastHurtByMobTimestamp();
                lastHurt = owner.getLastHurtMobTimestamp();
            }
            bird.setTarget(pick);
            super.start();
        }
    }

    // During a thunderstorm with nothing to fight: circle high around the nest.
    private static class CircleNestGoal extends Goal {
        private final StormwingEntity bird;
        private float angle;

        CircleNestGoal(StormwingEntity bird) {
            this.bird = bird;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return bird.isWild() && bird.stormy() && bird.getTarget() == null && !bird.isExploring();
        }

        @Override
        public void start() {
            bird.setNesting(false);
            angle = bird.random.nextFloat() * 360f;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            angle += 1.5f;
            double radius = 14;
            BlockPos home = bird.home();
            double x = home.getX() + 0.5 + Math.cos(Math.toRadians(angle)) * radius;
            double z = home.getZ() + 0.5 + Math.sin(Math.toRadians(angle)) * radius;
            double y = home.getY() + 14 + Math.sin(Math.toRadians(angle * 2)) * 3;
            bird.getMoveControl().setWantedPosition(x, y, z, 0.9);
        }
    }
}
