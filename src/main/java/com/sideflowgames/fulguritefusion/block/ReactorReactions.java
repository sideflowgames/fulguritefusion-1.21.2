package com.sideflowgames.fulguritefusion.block;

import java.util.List;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.pairing.Pairings.Pairing;
import com.sideflowgames.fulguritefusion.power.FmSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

// Every pairing's own reaction when the Fusion Reactor burns it (on top of its tier's general effect and the Fm).
// Effects that destroy or replace blocks (fires, shattering, rot, sculk, burying snow, Chamber Breach) only happen
// with the blockDamage setting on; Ring Melt needs ringMelt; Warden Call and Hellfire's Wither need bossSpawns.
// Growth effects (plants, coral, frost flowers, ice) always happen.
public final class ReactorReactions {
    private ReactorReactions() {}

    private record Ctx(ServerLevel level, FusionColliderBlockEntity reactor, BlockPos c, RandomSource rand) {
        Vec3 centre() {
            return Vec3.atCenterOf(c);
        }
    }

    public static void react(ServerLevel level, FusionColliderBlockEntity reactor, Pairing result) {
        Ctx x = new Ctx(level, reactor, reactor.getBlockPos(), level.random);
        boolean damage = Config.BLOCK_DAMAGE.get();
        switch (result.id()) {
            // ---------- Aurora Dust ----------
            case 1, 2, 3, 19, 24, 53, 59, 65, 66 -> calm(x); // steady, safe burns: a soft shimmer
            case 4 -> particles(x, ParticleTypes.SNOWFLAKE, 200, 6); // Polar Glow
            case 5 -> { hum(x); particles(x, ParticleTypes.NOTE, 40, 4); } // Night Chorus
            case 6 -> { if (x.rand.nextFloat() < 0.3f) x.reactor.overheat(1200); } // Torn Sky
            case 7 -> aurora(x); // Storm Aurora
            case 8, 29 -> fizzle(x); // Snuffed, Dead Calm
            case 9 -> grow(x, 10, Blocks.GLOW_LICHEN, 24); // Bloomlight
            case 10 -> { x.reactor.overheat(600); burn(x, 6, 4); particles(x, ParticleTypes.FLAME, 150, 3); } // Flare
            case 11 -> { particles(x, ParticleTypes.WHITE_ASH, 400, 12); effect(x, 12, MobEffects.WITHER, 160, 0); } // Ashfall
            // ---------- Sunmote ----------
            case 13 -> x.reactor.overheat(1200); // Twin Suns
            case 15 -> breach(x, 4, 8); // Thermal Shock: Chamber Breach
            case 16 -> { blast(x, 3f); if (damage) spread(x, 10, Blocks.SCULK, 24); } // Solar Shriek
            case 17 -> { if (damage) fires(x, 12, 10); } // Sunstorm
            case 20 -> growCrops(x, 16); // Photosynth
            case 21 -> { fireBlast(x); ringMelt(x); } // Solar Flare
            case 22 -> cloud(x, MobEffects.WITHER, 8, 600); // Scorched Rot
            // ---------- Abyssal Nacre ----------
            case 25 -> freezeWater(x, 12); // Black Ice
            case 26 -> spawn(x, EntityType.DROWNED, 6, 10); // Drowned Choir
            case 27 -> pull(x, 16, 1.2); // Maelstrom
            case 28 -> x.level.setWeatherParameters(0, 6000, true, false); // Thunderhead
            case 30 -> coral(x, 12); // Reef Bloom
            case 31 -> { steam(x); if (damage) breakRing(x, 1, 3); } // Steam Burst
            case 32 -> cloud(x, MobEffects.POISON, 10, 1200); // Black Tide
            // ---------- Soul Frost ----------
            case 34, 37, 38 -> { x.reactor.coolDown(); particles(x, ParticleTypes.SNOWFLAKE, 120, 4); } // Deep Freeze, Frostbolt, Stillness
            case 35 -> { if (damage) shatter(x, 10); } // Shatter Hymn
            case 36 -> { freeze(x, 14, 300); if (damage) spread(x, 12, Blocks.POWDER_SNOW, 60); } // Blizzard
            case 39 -> { grow(x, 10, Blocks.SNOW, 30); grow(x, 10, Blocks.LILY_OF_THE_VALLEY, 12); } // Winter Bloom
            case 40 -> frostburn(x); // Frostburn
            case 41 -> { spawn(x, EntityType.STRAY, 3, 10); spawn(x, EntityType.WITHER_SKELETON, 2, 10); } // Soul Rot
            // ---------- Echo Resin ----------
            case 43 -> wardenCall(x); // Warden Call
            case 44 -> { push(x, 14, 2.0); if (damage) shatter(x, 12); } // Howling Gale
            case 45 -> thunderclap(x); // Thunderclap
            case 46 -> { fizzle(x); effect(x, 16, MobEffects.DARKNESS, 100, 0); } // Silence
            case 47 -> spawn(x, EntityType.ALLAY, 2, 6); // Living Choir
            case 48 -> { blast(x, 5f); effect(x, 20, MobEffects.DARKNESS, 400, 0); breach(x, 3, 6); } // Sonic Detonation
            case 49 -> { effect(x, 24, MobEffects.DARKNESS, 400, 0); effect(x, 24, MobEffects.WITHER, 200, 1); } // Deathknell
            // ---------- Gale Core ----------
            case 51 -> scatterItems(x, 16); // Twin Gyre
            case 52 -> stormSurge(x); // Storm Surge
            case 54 -> { particles(x, ParticleTypes.ITEM_SLIME, 200, 8); grow(x, 10, Blocks.GLOW_LICHEN, 10); } // Spore Wind
            case 55 -> { fireBlast(x); breach(x, 3, 6); if (damage) fires(x, 10, 16); } // Firestorm
            case 56 -> cloud(x, MobEffects.WITHER, 12, 900); // Ash Cyclone
            // ---------- Stormwing Plume ----------
            case 58 -> { if (x.rand.nextFloat() < 0.1f) stormSurge(x); } // Twin Strike
            case 61 -> x.reactor.staticBloom(72000); // Overcharge: Static Bloom for 3 days
            case 62 -> blackLightning(x); // Black Lightning
            // ---------- Null Scale ----------
            case 64 -> voidCollapse(x); // Void Collapse
            case 67 -> drainXp(x, 16, 3); // Hollow Rot
            // ---------- Lumen Gel ----------
            case 69 -> { grow(x, 12, Blocks.MOSS_CARPET, 30); grow(x, 12, Blocks.FLOWERING_AZALEA, 6); } // Bloom
            case 70 -> { if (damage) { fires(x, 10, 10); clear(x, 14); } } // Burnout
            case 71 -> { spawn(x, EntityType.ZOMBIE, 2, 12); spawn(x, EntityType.SKELETON, 2, 12); spawn(x, EntityType.SPIDER, 1, 12); } // Rot Bloom
            // ---------- Cinder Heart ----------
            case 73 -> { breach(x, 5, 9); ringMelt(x); } // Meltdown
            case 74 -> hellfire(x); // Hellfire
            // ---------- Wither Ash ----------
            case 76 -> { if (damage) spread(x, 12, Blocks.SOUL_SOIL, 60, true); } // Necrosis
            default -> {} // jackpots and strong burns: the flash says it all (byproducts come separately)
        }
    }

    // ---------- Effects ----------

    private static void calm(Ctx x) {
        particles(x, ParticleTypes.END_ROD, 40, 3);
    }

    private static void fizzle(Ctx x) {
        Vec3 v = x.centre();
        x.level.sendParticles(ParticleTypes.SMOKE, v.x, v.y, v.z, 40, 0.6, 0.6, 0.6, 0.02);
    }

    private static void hum(Ctx x) {
        sound(x, SoundEvents.BEACON_AMBIENT, 3f, 0.6f);
    }

    private static void particles(Ctx x, ParticleOptions p, int count, double spread) {
        Vec3 v = x.centre();
        x.level.sendParticles(p, v.x, v.y + 1, v.z, count, spread, spread / 2, spread, 0.02);
    }

    private static void sound(Ctx x, SoundEvent s, float volume, float pitch) {
        x.level.playSound(null, x.c, s, SoundSource.BLOCKS, volume, pitch);
    }

    private static List<LivingEntity> near(Ctx x, double range) {
        return x.level.getEntitiesOfClass(LivingEntity.class, new AABB(x.c).inflate(range));
    }

    private static void effect(Ctx x, double range, Holder<MobEffect> effect, int ticks, int amp) {
        for (LivingEntity e : near(x, range)) e.addEffect(new MobEffectInstance(effect, ticks, amp));
    }

    private static void cloud(Ctx x, Holder<MobEffect> effect, float radius, int duration) {
        Vec3 v = x.centre();
        AreaEffectCloud cloud = new AreaEffectCloud(x.level, v.x, v.y - 2, v.z);
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(0);
        cloud.setWaitTime(0);
        cloud.addEffect(new MobEffectInstance(effect, 100, effect == MobEffects.WITHER ? 1 : 0));
        x.level.addFreshEntity(cloud);
    }

    // A blast with no block damage of its own.
    private static void blast(Ctx x, float power) {
        Vec3 v = x.centre();
        x.level.explode(null, v.x, v.y, v.z, power, Level.ExplosionInteraction.NONE);
    }

    private static void fireBlast(Ctx x) {
        Vec3 v = x.centre();
        x.level.explode(null, null, null, v.x, v.y, v.z, 5f, Config.BLOCK_DAMAGE.get(), Level.ExplosionInteraction.NONE);
        burn(x, 10, 8);
    }

    private static void burn(Ctx x, double range, int seconds) {
        for (LivingEntity e : near(x, range)) e.igniteForSeconds(seconds);
    }

    private static void freeze(Ctx x, double range, int ticks) {
        for (LivingEntity e : near(x, range)) if (e.canFreeze()) e.setTicksFrozen(Math.max(e.getTicksFrozen(), ticks));
    }

    // Chamber Breach: tears a few ring and chamber blocks out.
    private static void breach(Ctx x, int min, int max) {
        if (Config.BLOCK_DAMAGE.get()) x.reactor.blastRingBlocks(x.level, min + x.rand.nextInt(max - min + 1));
    }

    private static void breakRing(Ctx x, int min, int max) {
        x.reactor.blastRingBlocks(x.level, min + x.rand.nextInt(max - min + 1));
    }

    private static void ringMelt(Ctx x) {
        if (Config.RING_MELT.get()) x.reactor.meltRing(x.level, 6 + x.rand.nextInt(7));
    }

    // A free spot on the ground near the reactor (outside the chamber), or null.
    private static BlockPos ground(Ctx x, int radius) {
        for (int tries = 0; tries < 8; tries++) {
            int dx = x.rand.nextInt(2 * radius + 1) - radius, dz = x.rand.nextInt(2 * radius + 1) - radius;
            if (Math.abs(dx) <= 3 && Math.abs(dz) <= 3) continue;
            for (int dy = 8; dy >= -12; dy--) {
                BlockPos p = x.c.offset(dx, dy, dz);
                BlockState s = x.level.getBlockState(p);
                if ((s.isAir() || s.canBeReplaced()) && s.getFluidState().isEmpty()
                        && x.level.getBlockState(p.below()).isFaceSturdy(x.level, p.below(), Direction.UP)) {
                    return p;
                }
            }
        }
        return null;
    }

    private static void grow(Ctx x, int radius, Block block, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos p = ground(x, radius);
            if (p == null || !x.level.getBlockState(p).isAir()) continue;
            BlockState s = block == Blocks.GLOW_LICHEN
                    ? Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true)
                    : block.defaultBlockState();
            if (s.canSurvive(x.level, p)) x.level.setBlock(p, s, Block.UPDATE_ALL);
        }
    }

    // Replace the ground's top block (dirt, grass, stone and the like) with something else.
    private static void spread(Ctx x, int radius, Block block, int count) {
        spread(x, radius, block, count, false);
    }

    private static void spread(Ctx x, int radius, Block block, int count, boolean onlySoil) {
        for (int i = 0; i < count; i++) {
            BlockPos p = ground(x, radius);
            if (p == null) continue;
            if (block == Blocks.POWDER_SNOW) {
                if (x.level.getBlockState(p).isAir()) x.level.setBlock(p, block.defaultBlockState(), Block.UPDATE_ALL);
                continue;
            }
            BlockPos below = p.below();
            BlockState s = x.level.getBlockState(below);
            boolean soil = s.is(BlockTags.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(BlockTags.SAND);
            if (onlySoil ? soil : (soil || s.is(BlockTags.BASE_STONE_OVERWORLD))) {
                x.level.setBlock(below, block.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static void fires(Ctx x, int radius, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos p = ground(x, radius);
            if (p != null && x.level.getBlockState(p).isAir()) x.level.setBlock(p, BaseFireBlock.getState(x.level, p), Block.UPDATE_ALL);
        }
    }

    private static void growCrops(Ctx x, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(x.c.offset(-radius, -6, -radius), x.c.offset(radius, 6, radius))) {
            BlockState s = x.level.getBlockState(p);
            if (s.getBlock() instanceof CropBlock crop) {
                x.level.setBlock(p, crop.getStateForAge(crop.getMaxAge()), Block.UPDATE_CLIENTS);
            } else if (s.is(BlockTags.SAPLINGS) && s.getBlock() instanceof BonemealableBlock b && x.rand.nextInt(3) == 0) {
                b.performBonemeal(x.level, x.rand, p.immutable(), s);
            }
        }
        particles(x, ParticleTypes.HAPPY_VILLAGER, 200, radius);
    }

    private static void freezeWater(Ctx x, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(x.c.offset(-radius, -6, -radius), x.c.offset(radius, 6, radius))) {
            if (x.level.getBlockState(p).is(Blocks.WATER) && x.level.getFluidState(p).isSource()
                    && x.level.getBlockState(p.above()).isAir()) {
                x.level.setBlock(p, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        particles(x, ParticleTypes.SNOWFLAKE, 150, radius);
    }

    private static void coral(Ctx x, int radius) {
        int placed = 0;
        for (int tries = 0; tries < 200 && placed < 12; tries++) {
            BlockPos p = x.c.offset(x.rand.nextInt(2 * radius + 1) - radius, x.rand.nextInt(13) - 8, x.rand.nextInt(2 * radius + 1) - radius);
            if (!x.level.getBlockState(p).is(Blocks.WATER) || !x.level.getFluidState(p).isSource()) continue;
            if (!x.level.getBlockState(p.below()).isFaceSturdy(x.level, p.below(), Direction.UP)) continue;
            BlockState s = x.rand.nextBoolean()
                    ? Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 1 + x.rand.nextInt(4))
                    : List.of(Blocks.TUBE_CORAL, Blocks.BRAIN_CORAL, Blocks.BUBBLE_CORAL, Blocks.FIRE_CORAL, Blocks.HORN_CORAL)
                            .get(x.rand.nextInt(5)).defaultBlockState();
            x.level.setBlock(p, s, Block.UPDATE_ALL);
            placed++;
        }
    }

    private static void steam(Ctx x) {
        particles(x, ParticleTypes.CLOUD, 300, 6);
        sound(x, SoundEvents.FIRE_EXTINGUISH, 3f, 0.5f);
        for (LivingEntity e : near(x, 8)) e.hurt(x.level.damageSources().hotFloor(), 4f);
    }

    // Glass and ice nearby shatter (vanilla glass and ice only: never the reactor's own blast glass).
    private static void shatter(Ctx x, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(x.c.offset(-radius, -radius, -radius), x.c.offset(radius, radius, radius))) {
            BlockState s = x.level.getBlockState(p);
            if (s.is(ModBlocks.FULGURITE_BLAST_GLASS.get())) continue;
            if (s.is(Tags.Blocks.GLASS_BLOCKS) || s.is(Tags.Blocks.GLASS_PANES) || s.is(BlockTags.ICE)) {
                x.level.destroyBlock(p, false);
            }
        }
    }

    private static void push(Ctx x, double range, double strength) {
        Vec3 v = x.centre();
        for (Entity e : x.level.getEntities((Entity) null, new AABB(x.c).inflate(range))) {
            Vec3 d = e.position().subtract(v);
            if (d.lengthSqr() < 0.01) continue;
            Vec3 push = d.normalize().scale(strength * (1 - Math.min(1, d.length() / range)) + 0.3).add(0, 0.4, 0);
            e.push(push.x, push.y, push.z);
            e.hurtMarked = true;
        }
        sound(x, SoundEvents.WARDEN_SONIC_BOOM, 3f, 0.8f);
        Vec3 c = x.centre();
        x.level.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 1, 0, 0, 0, 0);
    }

    private static void pull(Ctx x, double range, double strength) {
        Vec3 v = x.centre();
        for (Entity e : x.level.getEntities((Entity) null, new AABB(x.c).inflate(range))) {
            Vec3 d = v.subtract(e.position());
            if (d.lengthSqr() < 4) continue;
            Vec3 pull = d.normalize().scale(strength);
            e.push(pull.x, pull.y * 0.3 + 0.2, pull.z);
            e.hurtMarked = true;
        }
        particles(x, ParticleTypes.SPLASH, 400, range / 2);
        sound(x, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 3f, 0.6f);
    }

    private static void scatterItems(Ctx x, double range) {
        for (ItemEntity item : x.level.getEntitiesOfClass(ItemEntity.class, new AABB(x.c).inflate(range))) {
            item.setDeltaMovement((x.rand.nextDouble() - 0.5) * 2, 0.6 + x.rand.nextDouble() * 0.5, (x.rand.nextDouble() - 0.5) * 2);
        }
        particles(x, ParticleTypes.GUST, 20, 6);
        sound(x, SoundEvents.PHANTOM_FLAP, 3f, 0.5f);
    }

    private static void spawn(Ctx x, EntityType<? extends Entity> type, int count, int radius) {
        for (int i = 0; i < count; i++) {
            BlockPos p = ground(x, radius);
            if (p == null) continue;
            Entity e = type.create(x.level);
            if (e == null) continue;
            e.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, x.rand.nextFloat() * 360, 0);
            if (e instanceof Mob mob) {
                mob.finalizeSpawn(x.level, x.level.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null);
            }
            x.level.addFreshEntity(e);
        }
    }

    private static void lightning(Ctx x, BlockPos p, boolean visualOnly) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(x.level);
        if (bolt == null) return;
        bolt.moveTo(Vec3.atBottomCenterOf(p));
        bolt.setVisualOnly(visualOnly);
        x.level.addFreshEntity(bolt);
    }

    private static void aurora(Ctx x) {
        Vec3 v = x.centre();
        for (int i = 0; i < 6; i++) {
            x.level.sendParticles(ParticleTypes.GLOW, v.x + (x.rand.nextDouble() - 0.5) * 60, v.y + 30 + x.rand.nextDouble() * 10,
                    v.z + (x.rand.nextDouble() - 0.5) * 60, 80, 12, 2, 1, 0.01);
            x.level.sendParticles(ParticleTypes.END_ROD, v.x + (x.rand.nextDouble() - 0.5) * 60, v.y + 32,
                    v.z + (x.rand.nextDouble() - 0.5) * 60, 40, 10, 3, 1, 0.005);
        }
        sound(x, SoundEvents.AMETHYST_BLOCK_RESONATE, 4f, 0.5f);
    }

    private static void thunderclap(Ctx x) {
        // The boom carries for miles (same reach as natural thunder).
        x.level.playSound(null, x.c, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 10000f, 0.6f);
    }

    private static void stormSurge(Ctx x) {
        boolean real = Config.BLOCK_DAMAGE.get();
        for (int i = 0; i < 12; i++) {
            BlockPos p = ground(x, 24);
            if (p != null) lightning(x, p, !real);
        }
        for (LivingEntity e : near(x, 24)) {
            if (x.rand.nextInt(3) == 0) {
                lightning(x, e.blockPosition(), !real);
                if (!real) e.hurt(x.level.damageSources().lightningBolt(), 5f);
            }
        }
    }

    private static void blackLightning(Ctx x) {
        for (int i = 0; i < 6; i++) {
            BlockPos p = ground(x, 16);
            if (p != null) lightning(x, p, true);
        }
        for (LivingEntity e : near(x, 16)) {
            if (x.rand.nextInt(2) == 0) {
                lightning(x, e.blockPosition(), true);
                e.hurt(x.level.damageSources().lightningBolt(), 4f);
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
            }
        }
    }

    private static void frostburn(Ctx x) {
        freeze(x, 14, 400);
        burn(x, 14, 6);
        particles(x, ParticleTypes.SOUL_FIRE_FLAME, 300, 8);
        blast(x, 4f);
        if (!Config.BLOCK_DAMAGE.get()) return;
        for (int i = 0; i < 16; i++) {
            BlockPos p = ground(x, 12);
            if (p == null || !x.level.getBlockState(p).isAir()) continue;
            x.level.setBlock(p.below(), Blocks.SOUL_SOIL.defaultBlockState(), Block.UPDATE_ALL);
            x.level.setBlock(p, Blocks.SOUL_FIRE.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (int i = 0; i < 20; i++) {
            BlockPos p = ground(x, 12);
            if (p != null && x.level.getBlockState(p).isAir()) x.level.setBlock(p, Blocks.POWDER_SNOW.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void wardenCall(Ctx x) {
        sound(x, SoundEvents.WARDEN_EMERGE, 4f, 1f);
        if (Config.BOSS_SPAWNS.get()) spawn(x, EntityType.WARDEN, 1, 8);
        else effect(x, 24, MobEffects.DARKNESS, 400, 0);
    }

    private static void hellfire(Ctx x) {
        if (Config.BOSS_SPAWNS.get()) {
            BlockPos p = ground(x, 10);
            WitherBoss wither = EntityType.WITHER.create(x.level);
            if (p != null && wither != null) {
                wither.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
                wither.makeInvulnerable();
                x.level.addFreshEntity(wither);
                return;
            }
        }
        // No bosses: the hellfire tears through the chamber instead.
        fireBlast(x);
        breach(x, 4, 8);
        effect(x, 16, MobEffects.WITHER, 300, 1);
    }

    private static void voidCollapse(Ctx x) {
        ReactorConsoleBlockEntity console = x.reactor.consoleEntity();
        if (console != null) console.voidCollapse();
        particles(x, ParticleTypes.REVERSE_PORTAL, 300, 6);
        sound(x, SoundEvents.BEACON_DEACTIVATE, 4f, 0.5f);
    }

    private static void drainXp(Ctx x, double range, int levels) {
        for (Player p : x.level.getEntitiesOfClass(Player.class, new AABB(x.c).inflate(range))) {
            if (p instanceof ServerPlayer sp) sp.giveExperienceLevels(-levels);
        }
        particles(x, ParticleTypes.SCULK_SOUL, 80, 6);
    }

    // Burnout: the Lumen growth around the reactor dies off.
    private static void clear(Ctx x, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(x.c.offset(-radius, -6, -radius), x.c.offset(radius, 6, radius))) {
            BlockState s = x.level.getBlockState(p);
            if (s.is(Blocks.GLOW_LICHEN) || s.is(Blocks.CAVE_VINES) || s.is(Blocks.CAVE_VINES_PLANT)) x.level.destroyBlock(p, false);
        }
    }

    // Used by Void Collapse: anything behind the console's power ports that gives Fm is drained too.
    static void drain(Level level, BlockPos p) {
        if (level.getBlockEntity(p) instanceof FmSource src) src.extractFm(Integer.MAX_VALUE, false);
    }
}
