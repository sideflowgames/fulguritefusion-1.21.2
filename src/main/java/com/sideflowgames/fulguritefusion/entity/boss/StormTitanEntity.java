package com.sideflowgames.fulguritefusion.entity.boss;

import java.util.ArrayList;
import java.util.List;

import com.sideflowgames.fulguritefusion.ModEntities;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.item.FmChargeable;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

// The Storm Titan: the final boss, a 14-block giant of storm cloud and violet plasma. Summoned at a Titan Altar in
// the Stormreach (the three key pieces the first time, then the Storm Sigil it drops).
// 800 health, heavy armor. Its hits are made for players in Prime gear (on Normal, a full Prime set takes about
// 4 hearts from a slam and 2-3 from the rest; Hard hits half again as hard); without it, a slam is lethal.
//   - Slam (close): both fists down in front of it, 32 damage.
//   - Pulse (right underneath it): crackles around its feet, then bursts outward, 22 damage, thrown clear.
//     Jumping doesn't help; get out from under it.
//   - Stomp (mid range): a shockwave along the ground, 24 damage and thrown upward. Jump to avoid it.
//   - Plasma strikes (any range): marks 4 spots (6 below half health) and strikes them a second later, 26 damage
//     and 2 seconds of Slowness II. These aren't lightning, so Prime's lightning immunity doesn't stop them.
//   - Below half health it attacks faster and calls Stormhounds every 20 seconds (2, then 3). They hunt for
//     60 seconds anywhere, storm or not.
//   - Regeneration cycles at 3/4, 1/2 and 1/4 health: it stands still, channels, and raises 4 Titan Conduits that
//     heal it and arc into players. It can't be hurt while any conduit stands. Ground each conduit with a Lightning
//     Rod, then break it (broken conduits burst, hurting players nearby and blowing away nearby Lightning Rods).
//     Break all four within 20 seconds and the Titan staggers for 6 seconds, taking half again as much damage.
//     After 30 seconds any conduits left crumble. While channelling it still calls weaker strikes (10 damage).
//   - It walks itself straight at you (no pathfinding), stepping up ledges up to 2.5 blocks and hopping higher ones.
//     Attack animations are triggered the moment an attack starts and each hit lands on its animation's impact.
// Drops one Titan armor piece (90% the sacrificed type, if an armor piece was offered at the altar) and a Storm Sigil.
public class StormTitanEntity extends StormBossEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.storm_titan.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.storm_titan.walk");
    private static final RawAnimation SLAM = RawAnimation.begin().thenPlay("animation.storm_titan.slam");
    private static final RawAnimation STOMP = RawAnimation.begin().thenPlay("animation.storm_titan.stomp");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("animation.storm_titan.cast");
    private static final RawAnimation CHANNEL = RawAnimation.begin().thenLoop("animation.storm_titan.channel");
    private static final RawAnimation STAGGER = RawAnimation.begin().thenLoop("animation.storm_titan.stagger");

    public static final int IDLE_STATE = 0, SLAM_STATE = 1, STOMP_STATE = 2, CAST_STATE = 3, CHANNEL_STATE = 4, STAGGER_STATE = 5, PULSE_STATE = 6;
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(StormTitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> WALKING = SynchedEntityData.defineId(StormTitanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final float[] CYCLES = {0.75f, 0.5f, 0.25f};
    private static final Vector3f VIOLET = new Vector3f(0.72f, 0.42f, 1.0f);

    private int actionTimer = 60;  // until the next attack is chosen
    private int windup;             // ticks until the current attack lands
    private int cycleTicks;         // ticks since the current regeneration cycle started
    private int stagger;            // ticks of stagger left
    private int houndTimer = 400;
    private final List<Vec3> marked = new ArrayList<>();
    private int markTimer;
    private float markDamage;
    private EquipmentSlot targetSlot; // armor type offered at the altar, if any
    private net.minecraft.core.BlockPos home;  // centre of its arena: it won't stray far from it
    private int partySize = 1;                 // most players seen fighting it at once
    private static final net.minecraft.resources.ResourceLocation PARTY_HEALTH =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.sideflowgames.fulguritefusion.FulguriteFusion.MODID, "titan_party");

    public StormTitanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 800.0).add(Attributes.ATTACK_DAMAGE, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 20.0).add(Attributes.ARMOR_TOUGHNESS, 12.0).add(Attributes.STEP_HEIGHT, 2.5);
    }

    public void setHome(net.minecraft.core.BlockPos pos) {
        home = pos.immutable();
    }

    // More challengers, a stronger Titan: for each player past the first, +60% health, one more conduit each
    // regeneration cycle, one more Stormhound each wave, and attacks come 12% sooner (down to 60% of normal).
    private void updateParty(ServerLevel server) {
        Vec3 c = home != null ? Vec3.atCenterOf(home) : position();
        int n = 0;
        for (Player p : server.players()) {
            if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(c) < 48 * 48) n++;
        }
        if (n <= partySize) return;
        float oldMax = getMaxHealth();
        partySize = n;
        applyPartyHealth();
        setHealth(getHealth() * getMaxHealth() / oldMax);
        tellNearby(server, "The Titan swells with power against " + n + " challengers!");
    }

    private void applyPartyHealth() {
        var inst = getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        inst.removeModifier(PARTY_HEALTH);
        if (partySize > 1) {
            inst.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(PARTY_HEALTH, 0.6 * (partySize - 1),
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    private int scaledDelay(int ticks) {
        return Math.max((int) (ticks * 0.6), Math.round(ticks * (1 - 0.12f * (partySize - 1))));
    }

    public void setTargetSlot(EquipmentSlot slot) {
        targetSlot = slot;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, IDLE_STATE);
        builder.define(WALKING, false);
    }

    public int state() {
        return entityData.get(STATE);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount % 40 == 0) updateParty(server);
        // Kept to its arena: pulled back if it wanders off the edge or falls.
        if (home != null && (getY() < home.getY() - 8 || horizontalDistanceSqr(home) > 26 * 26)) {
            teleportTo(home.getX() + 0.5, home.getY() + 3, home.getZ() + 0.5);
            setDeltaMovement(Vec3.ZERO);
        }
        tickMarks(server);
        LivingEntity target = getTarget();

        entityData.set(WALKING, false);
        if (stagger > 0) {
            stagger--;
            entityData.set(STATE, STAGGER_STATE);
            getNavigation().stop();
            return;
        }
        if (isShielded()) {
            // Regeneration cycle: rooted and channelling, still calling weaker strikes.
            cycleTicks++;
            entityData.set(STATE, CHANNEL_STATE);
            getNavigation().stop();
            if (cycleTicks == 600) {
                tellNearby(server, "The Titan's cycle completes.");
                clearAnchors();
            }
            if (target != null && cycleTicks % 50 == 25 && marked.isEmpty()) mark(server, target, 1, 10.0f);
            return;
        }
        if (target == null || !target.isAlive()) {
            entityData.set(STATE, IDLE_STATE);
            return;
        }
        getLookControl().setLookAt(target, 20, 20);
        double dist = Math.sqrt(distanceToSqr(target));

        if (windup > 0) {
            getNavigation().stop();
            if (state() == PULSE_STATE && windup % 2 == 0) {
                for (int i = 0; i < 24; i++) {
                    double a = Math.PI * 2 * i / 24, r = 2 + random.nextDouble() * 4;
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX() + Math.cos(a) * r, getY() + 0.3, getZ() + Math.sin(a) * r, 1, 0, 0.1, 0, 0.05);
                }
            }
            if (--windup == 0) land(server, target);
            return;
        }
        entityData.set(STATE, IDLE_STATE);
        if (dist > 7) walkTowards(target);

        if (enraged() && --houndTimer <= 0) {
            houndTimer = 400;
            int hounds = (getHealth() < getMaxHealth() * 0.25f ? 3 : 2) + partySize - 1;
            for (int i = 0; i < hounds; i++) {
                com.sideflowgames.fulguritefusion.entity.stormreach.StormhoundEntity hound = ModEntities.STORMHOUND.get().create(server);
                if (hound == null) continue;
                Vec3 at = groundNear(server, inArena(position().add(random.nextInt(9) - 4, 0, random.nextInt(9) - 4), 18));
                hound.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
                hound.setTarget(target);
                hound.setSummoned(1200);
                server.addFreshEntity(hound);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5, at.z, 30, 0.4, 0.6, 0.4, 0.2);
            }
            server.playSound(null, blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 2.0f, 0.5f);
        }

        if (--actionTimer > 0) return;
        actionTimer = scaledDelay(enraged() ? 22 : 32);
        // What it does depends on range, with plasma strikes mixed in everywhere:
        //   under it: pulse 50%, slam 30%, strikes 20%   close: slam 60%, strikes 40%
        //   mid: stomp 40%, strikes 60%                  far: strikes
        int roll = random.nextInt(10);
        boolean canCast = marked.isEmpty();
        if (dist < 5 && roll < 5) {
            start(PULSE_STATE, 14);
            server.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0f, 1.6f);
        } else if (dist < 8 && (roll < 6 || !canCast)) {
            start(SLAM_STATE, 21);
            server.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0f, 0.5f);
        } else if (dist >= 8 && dist < 16 && (roll < 4 || !canCast)) {
            start(STOMP_STATE, 17);
            server.playSound(null, blockPosition(), SoundEvents.WARDEN_ANGRY, SoundSource.HOSTILE, 3.0f, 0.6f);
        } else if (canCast) {
            start(CAST_STATE, 7);
        }
    }

    private void start(int state, int ticks) {
        entityData.set(STATE, state);
        windup = ticks;
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        triggerAnim("main", state == SLAM_STATE ? "slam" : state == STOMP_STATE ? "stomp" : "cast");
    }

    // Its own walking: turn toward the target and stride straight at it. Hop anything too tall to step up.
    private void walkTowards(LivingEntity target) {
        Vec3 to = target.position().subtract(position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1e-4) return;
        float want = (float) (net.minecraft.util.Mth.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90.0f;
        float yaw = net.minecraft.util.Mth.approachDegrees(getYRot(), want, 8.0f);
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        double speed = enraged() ? 0.28 : 0.22;
        Vec3 dir = to.normalize();
        // Never walks off its arena.
        if (home != null) {
            Vec3 next = position().add(dir.scale(1.5));
            if ((next.x - home.getX() - 0.5) * (next.x - home.getX() - 0.5) + (next.z - home.getZ() - 0.5) * (next.z - home.getZ() - 0.5) > 19 * 19) {
                dir = Vec3.ZERO;
            }
        }
        double vy = getDeltaMovement().y;
        if (horizontalCollision && onGround()) vy = 0.6;
        setDeltaMovement(dir.x * speed, vy, dir.z * speed);
        entityData.set(WALKING, true);
    }

    private void land(ServerLevel server, LivingEntity target) {
        int state = state();
        entityData.set(STATE, IDLE_STATE);
        if (state == SLAM_STATE) {
            Vec3 look = Vec3.directionFromRotation(0, getYRot());
            Vec3 at = position().add(look.scale(4.5));
            for (LivingEntity e : victims(new AABB(at, at).inflate(5, 4, 5))) {
                if (e.hurt(damageSources().mobAttack(this), 32.0f)) {
                    Vec3 push = e.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.6);
                    e.push(push.x, 0.5, push.z);
                    e.hurtMarked = true;
                }
            }
            server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 6, 2.0, 0.5, 2.0, 0);
            server.sendParticles(new DustParticleOptions(VIOLET, 2.0f), at.x, at.y + 0.5, at.z, 60, 2.5, 0.5, 2.5, 0);
            server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0f, 0.5f);
        } else if (state == STOMP_STATE) {
            for (LivingEntity e : victims(getBoundingBox().inflate(10, 2, 10))) {
                if (!e.onGround()) continue; // jumped over the wave
                if (e.hurt(damageSources().mobAttack(this), 24.0f)) {
                    Vec3 push = e.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.8);
                    e.push(push.x, 0.9, push.z);
                    e.hurtMarked = true;
                }
            }
            for (int i = 0; i < 48; i++) {
                double a = Math.PI * 2 * i / 48;
                for (double r = 3; r <= 10; r += 3.5) {
                    server.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(a) * r, getY() + 0.2, getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0.02);
                }
            }
            server.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0f, 0.5f);
        } else if (state == CAST_STATE) {
            mark(server, target, enraged() ? 6 : 4, 26.0f);
        } else if (state == PULSE_STATE) {
            for (LivingEntity e : victims(getBoundingBox().inflate(6, 3, 6))) {
                if (e.hurt(damageSources().mobAttack(this), 22.0f)) {
                    Vec3 push = e.position().subtract(position()).multiply(1, 0, 1).normalize().scale(2.0);
                    e.push(push.x, 0.6, push.z);
                    e.hurtMarked = true;
                }
            }
            server.sendParticles(new DustParticleOptions(VIOLET, 2.5f), getX(), getY() + 1, getZ(), 120, 4.0, 1.0, 4.0, 0);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1, getZ(), 80, 4.0, 1.0, 4.0, 0.5);
            server.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 3.0f, 0.7f);
        }
    }

    // Mark spots around the target; a second later each one is struck.
    private void mark(ServerLevel server, LivingEntity target, int count, float damage) {
        marked.clear();
        marked.add(target.position());
        for (int i = 1; i < count; i++) {
            marked.add(groundNear(server, target.position().add(random.nextInt(13) - 6, 0, random.nextInt(13) - 6)));
        }
        markTimer = 22;
        markDamage = damage;
        server.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    private void tickMarks(ServerLevel server) {
        if (marked.isEmpty()) return;
        if (markTimer % 2 == 0) {
            float shrink = 0.6f + 0.4f * markTimer / 22f; // the ring tightens as the strike gets close
            for (Vec3 m : marked) {
                for (int i = 0; i < 28; i++) {
                    double a = Math.PI * 2 * i / 28;
                    server.sendParticles(new DustParticleOptions(VIOLET, 2.5f), m.x + Math.cos(a) * 2.5 * shrink, m.y + 0.2, m.z + Math.sin(a) * 2.5 * shrink, 1, 0, 0, 0, 0);
                }
                server.sendParticles(ParticleTypes.END_ROD, m.x, m.y + 0.3, m.z, 2, 0.2, 0.6, 0.2, 0.05);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.x, m.y + 0.2, m.z, 4, 1.0, 0.1, 1.0, 0.1);
            }
        }
        if (markTimer % 8 == 0) {
            for (Vec3 m : marked) server.playSound(null, net.minecraft.core.BlockPos.containing(m), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5f, 0.6f);
        }
        if (--markTimer > 0) return;
        for (Vec3 m : marked) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
            if (bolt != null) {
                bolt.moveTo(m.x, m.y, m.z);
                bolt.setVisualOnly(true);
                server.addFreshEntity(bolt);
            }
            for (LivingEntity e : victims(new AABB(m, m).inflate(2.5, 3, 2.5))) {
                if (e.hurt(damageSources().mobAttack(this), markDamage) && markDamage > 20) {
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1), this);
                }
            }
            server.sendParticles(new DustParticleOptions(VIOLET, 2.0f), m.x, m.y + 0.5, m.z, 30, 1.0, 0.5, 1.0, 0);
        }
        marked.clear();
    }

    private List<LivingEntity> victims(AABB box) {
        return level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this && e.isAlive() && !(e instanceof StormBossEntity) && !(e instanceof BossAnchorEntity)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
    }

    // ---- regeneration cycles (built on the mini-boss shield system, but it can still be hurt) ----

    @Override
    protected float[] shieldThresholds() {
        return CYCLES;
    }

    @Override
    protected String shieldLabel() {
        return " - Regenerating";
    }

    @Override
    protected List<Entity> spawnAnchors(ServerLevel level, int shieldNumber) {
        List<Entity> out = new ArrayList<>();
        windup = 0;
        marked.clear();
        cycleTicks = 0;
        double start = random.nextDouble() * Math.PI * 2;
        int count = 3 + partySize;
        Vec3 center = home != null ? new Vec3(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5) : position();
        for (int i = 0; i < count; i++) {
            TitanConduitEntity conduit = ModEntities.TITAN_CONDUIT.get().create(level);
            if (conduit == null) continue;
            double a = start + Math.PI * 2 * i / count;
            Vec3 at = groundNear(level, center.add(Math.cos(a) * 12, 0, Math.sin(a) * 12));
            conduit.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
            conduit.setOwner(this);
            level.addFreshEntity(conduit);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(at.x, at.y, at.z);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            out.add(conduit);
        }
        return out;
    }

    @Override
    protected String shieldHint() {
        return "The Titan is regenerating! Ground its conduits with Lightning Rods, then break them.";
    }

    @Override
    protected ParticleOptions shieldParticle() {
        return new DustParticleOptions(VIOLET, 1.5f);
    }

    @Override
    protected void onShieldDropped() {
        if (cycleTicks <= 400 && level() instanceof ServerLevel server) {
            stagger = 120;
            tellNearby(server, "The Titan staggers! Strike now!");
            server.playSound(null, blockPosition(), SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 2.0f, 0.4f);
        }
        cycleTicks = 0;
    }

    // Staggered: half again as much damage.
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, stagger > 0 ? amount * 1.5f : amount);
    }

    // Keeps a point within `radius` blocks of the arena centre (when it has one).
    private Vec3 inArena(Vec3 at, double radius) {
        if (home == null) return at;
        Vec3 c = new Vec3(home.getX() + 0.5, at.y, home.getZ() + 0.5);
        Vec3 off = at.subtract(c);
        return off.length() <= radius ? at : c.add(off.normalize().scale(radius));
    }

    private double horizontalDistanceSqr(net.minecraft.core.BlockPos pos) {
        double dx = getX() - pos.getX() - 0.5, dz = getZ() - pos.getZ() - 0.5;
        return dx * dx + dz * dz;
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !dead;
        super.die(source);
        if (first && dead && level() instanceof ServerLevel server) com.sideflowgames.fulguritefusion.titan.TitanTrials.victory(server, this);
    }

    // ---- drops ----

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        EquipmentSlot slot = targetSlot != null && random.nextFloat() < 0.9f ? targetSlot : slots[random.nextInt(4)];
        Item piece = switch (slot) {
            case HEAD -> ModItems.TITAN_HELMET.get();
            case CHEST -> ModItems.TITAN_CHESTPLATE.get();
            case LEGS -> ModItems.TITAN_LEGGINGS.get();
            default -> ModItems.TITAN_BOOTS.get();
        };
        ItemStack armor = new ItemStack(piece);
        FmChargeable.set(armor, FmChargeable.max(armor) / 2);
        spawnAtLocation(armor);
        spawnAtLocation(new ItemStack(ModItems.STORM_SIGIL.get()));
    }

    // ---- the rest ----

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.CLOUD, getRandomX(0.8), getY() + getBbHeight() * random.nextDouble(), getRandomZ(0.8), 0, 0.02, 0);
            }
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.9), getY() + getBbHeight() * random.nextDouble(), getRandomZ(0.9), 0, 0, 0);
            }
            if (state() == CHANNEL_STATE) {
                level().addParticle(new DustParticleOptions(VIOLET, 2.0f), getRandomX(1.2), getY() + getBbHeight() * random.nextDouble(), getRandomZ(1.2), 0, 0.1, 0);
            }
        } else if (tickCount % 120 == 0 && level() instanceof ServerLevel server) {
            server.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0f, 0.6f);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetSlot != null) tag.putString("TargetSlot", targetSlot.getName());
        tag.putInt("CycleTicks", cycleTicks);
        tag.putInt("PartySize", partySize);
        if (home != null) tag.putLong("Home", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TargetSlot")) targetSlot = EquipmentSlot.byName(tag.getString("TargetSlot"));
        cycleTicks = tag.getInt("CycleTicks");
        partySize = Math.max(1, tag.getInt("PartySize"));
        if (tag.contains("Home")) home = net.minecraft.core.BlockPos.of(tag.getLong("Home"));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.4f;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0f;
    }

    // Loops come from the synced state; the attacks are triggered from the server the moment they start
    // (triggerAnim), so they always restart and line up with the hit.
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, s -> {
            switch (state()) {
                case CHANNEL_STATE: return s.setAndContinue(CHANNEL);
                case STAGGER_STATE: return s.setAndContinue(STAGGER);
                default: return s.setAndContinue(entityData.get(WALKING) ? WALK : IDLE);
            }
        }).triggerableAnim("slam", SLAM).triggerableAnim("stomp", STOMP).triggerableAnim("cast", CAST));
    }
}
