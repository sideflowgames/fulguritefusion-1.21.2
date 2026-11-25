package com.sideflowgames.fulguritefusion;

import net.neoforged.neoforge.common.ModConfigSpec;

// Fulgurite Fusion settings. Saved in config/fulguritefusion-common.toml and editable from the Mods menu.
// Grouped into sections: safety, static, power, collider, converters, mobs, ritual.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- Server safety (used by the fusion reactor's failure events, coming later) ---
    public static final ModConfigSpec.BooleanValue BLOCK_DAMAGE;
    public static final ModConfigSpec.IntValue REACTOR_MAX_FM;
    public static final ModConfigSpec.IntValue REACTOR_STARTUP_FM;
    public static final ModConfigSpec.DoubleValue REACTOR_FM_MULTIPLIER;
    public static final ModConfigSpec.IntValue REACTOR_OUTPUT_RATE;
    public static final ModConfigSpec.BooleanValue RING_MELT;
    public static final ModConfigSpec.BooleanValue BOSS_SPAWNS;

    // --- Static discharge from ungrounded Storm Furnaces ---
    public static final ModConfigSpec.BooleanValue STATIC_ENABLED;
    public static final ModConfigSpec.DoubleValue STATIC_DAMAGE;
    public static final ModConfigSpec.DoubleValue STATIC_RANGE;

    // --- Power balance ---
    public static final ModConfigSpec.IntValue WEAK_FURNACE_RATE;
    public static final ModConfigSpec.IntValue ENHANCED_FURNACE_RATE;
    public static final ModConfigSpec.IntValue CHARGED_FURNACE_RATE;
    public static final ModConfigSpec.IntValue FM_PER_COAL;
    public static final ModConfigSpec.IntValue FM_PER_SPARK_CELL;
    public static final ModConfigSpec.IntValue CHARGER_DRAW;
    public static final ModConfigSpec.IntValue BATTERY_CAPACITY;
    public static final ModConfigSpec.IntValue BATTERY_MAX_BANK;
    public static final ModConfigSpec.IntValue BATTERY_OUTPUT;
    public static final ModConfigSpec.IntValue WIRE_RATE;
    public static final ModConfigSpec.IntValue WIRE_MAX_LENGTH;
    public static final ModConfigSpec.IntValue HEAVY_WIRE_MAX_LENGTH;
    public static final ModConfigSpec.IntValue REINFORCED_WIRE_MAX_LENGTH;
    public static final ModConfigSpec.IntValue REINFORCED_WIRE_RATE;
    public static final ModConfigSpec.IntValue HEAVY_WIRE_RATE;
    public static final ModConfigSpec.DoubleValue WIRE_SHOCK_DAMAGE;
    public static final ModConfigSpec.DoubleValue WIRE_SHOCK_RANGE;
    public static final ModConfigSpec.IntValue TREE_TAP_SECONDS;
    public static final ModConfigSpec.IntValue DATA_CABLE_MAX_LENGTH;
    public static final ModConfigSpec.IntValue CHAIN_MAX_MACHINES;
    public static final ModConfigSpec.BooleanValue WATER_ELECTRIFY;
    public static final ModConfigSpec.DoubleValue WATER_RANGE;

    // --- Mini Collider ---
    public static final ModConfigSpec.IntValue COLLIDER_FM_PER_SHOT;
    public static final ModConfigSpec.IntValue COLLIDER_RELEASE_TICKS;
    public static final ModConfigSpec.IntValue STORM_FM_PER_SHOT;

    // --- Converters ---
    public static final ModConfigSpec.IntValue FE_PER_FM;
    public static final ModConfigSpec.IntValue EU_PER_FM;
    public static final ModConfigSpec.IntValue EU_LV;
    public static final ModConfigSpec.IntValue EU_MV;
    public static final ModConfigSpec.IntValue EU_HV;
    public static final ModConfigSpec.IntValue EU_EV;

    // --- Mobs ---
    public static final ModConfigSpec.IntValue STORMWING_TRIP_MIN_MINUTES;
    public static final ModConfigSpec.IntValue STORMWING_TRIP_MAX_MINUTES;
    public static final ModConfigSpec.IntValue STORMWING_AWAY_MAX_MINUTES;
    public static final ModConfigSpec.DoubleValue STORMWING_EGG_CHANCE;
    public static final ModConfigSpec.IntValue EGG_INCUBATION_MIN_MINUTES;
    public static final ModConfigSpec.IntValue EGG_INCUBATION_MAX_MINUTES;
    public static final ModConfigSpec.IntValue EGG_INCUBATION_RADIUS;
    public static final ModConfigSpec.DoubleValue FERAL_CHANCE_BAD;
    public static final ModConfigSpec.DoubleValue FERAL_CHANCE_CATASTROPHIC;
    public static final ModConfigSpec.IntValue HATCHLING_GROW_MINUTES;
    public static final ModConfigSpec.IntValue STORM_CALLER_MINUTES;

    // --- Lightning upgrade ritual ---
    public static final ModConfigSpec.BooleanValue RITUAL_ENABLED;
    public static final ModConfigSpec.IntValue ENHANCED_SPARK_CELLS;
    public static final ModConfigSpec.IntValue ENHANCED_MAGNETITE;
    public static final ModConfigSpec.IntValue CHARGED_SPARK_CELLS;
    public static final ModConfigSpec.IntValue CHARGED_SILVER;

    static {
        BUILDER.comment("Server safety: what reactor failures are allowed to do").push("safety");
        BLOCK_DAMAGE = BUILDER.comment("Bad and Catastrophic reactor results can destroy blocks. Turn off for block-protected servers.")
                .define("blockDamage", true);
        RING_MELT = BUILDER.comment("Ring Melt (Solar Flare, Meltdown) can melt a stretch of the reactor's ring tube back into Raw Fulgurite.")
                .define("ringMelt", true);
        BOSS_SPAWNS = BUILDER.comment("Failures can summon boss mobs: Warden Call spawns a Warden, Hellfire a Wither. Off by default.")
                .define("bossSpawns", false);
        BUILDER.pop();

        BUILDER.comment("Static discharge from ungrounded Storm Furnaces").push("static");
        STATIC_ENABLED = BUILDER.comment("Ungrounded Storm Furnaces shock nearby players.")
                .define("staticEnabled", true);
        STATIC_DAMAGE = BUILDER.comment("Damage every 2 seconds from a Weak furnace (2 = one heart). Doubles for each higher tier.")
                .defineInRange("staticDamage", 1.0, 0.0, 40.0);
        STATIC_RANGE = BUILDER.comment("How far the shock reaches, in blocks.")
                .defineInRange("staticRange", 2.0, 0.0, 16.0);
        WATER_ELECTRIFY = BUILDER.comment("Raw wire carrying power electrifies water it touches.")
                .define("waterElectrify", true);
        WATER_RANGE = BUILDER.comment("How far from the wire creatures in water get shocked, in blocks.")
                .defineInRange("waterRange", 4.0, 0.0, 16.0);
        BUILDER.pop();

        BUILDER.comment("Power balance (Fm = Fulmen, the mod's power)").push("power");
        WEAK_FURNACE_RATE = BUILDER.comment("Fm per tick made by a Weak Storm Furnace.")
                .defineInRange("weakFurnaceRate", 16, 1, 10000);
        ENHANCED_FURNACE_RATE = BUILDER.comment("Fm per tick made by an Enhanced Storm Furnace.")
                .defineInRange("enhancedFurnaceRate", 32, 1, 10000);
        CHARGED_FURNACE_RATE = BUILDER.comment("Fm per tick made by a Charged Storm Furnace.")
                .defineInRange("chargedFurnaceRate", 64, 1, 10000);
        FM_PER_COAL = BUILDER.comment("Total Fm one coal is worth. Other fuels scale with their vanilla burn time.")
                .defineInRange("fmPerCoal", 5120, 1, 1000000);
        FM_PER_SPARK_CELL = BUILDER.comment("Fm needed to charge one Spark Cell.")
                .defineInRange("fmPerSparkCell", 1280, 1, 16000);
        CHARGER_DRAW = BUILDER.comment("Fm per tick the Fulgurite Charger uses while charging.")
                .defineInRange("chargerDraw", 16, 1, 10000);
        BATTERY_CAPACITY = BUILDER.comment("Fm stored by each Storm Battery block.")
                .defineInRange("batteryCapacity", 64000, 1000, 100000000);
        BATTERY_MAX_BANK = BUILDER.comment("Most Storm Battery blocks that can touch to form one bank. More causes an overload.")
                .defineInRange("batteryMaxBank", 6, 1, 64);
        BATTERY_OUTPUT = BUILDER.comment("Fm per tick each Storm Battery block can send to each touching machine.")
                .defineInRange("batteryOutput", 512, 1, 1000000);
        WIRE_RATE = BUILDER.comment("Fm per tick a network of basic (Raw or Insulated) wire can carry (a network carries what its weakest wire can).")
                .defineInRange("wireRate", 256, 1, 1000000);
        WIRE_MAX_LENGTH = BUILDER.comment("Longest hanging wire between two Machine Connectors, in blocks.")
                .defineInRange("wireMaxLength", 16, 2, 64);
        REINFORCED_WIRE_MAX_LENGTH = BUILDER.comment("Longest Reinforced Wire between two Machine Connectors, in blocks.")
                .defineInRange("reinforcedWireMaxLength", 32, 2, 128);
        HEAVY_WIRE_MAX_LENGTH = BUILDER.comment("Longest Heavy Wire between two Machine Connectors, in blocks.")
                .defineInRange("heavyWireSpan", 64, 2, 128);
        REINFORCED_WIRE_RATE = BUILDER.comment("Fm per tick a network can carry when every wire is (Raw) Reinforced or (Raw) Heavy.")
                .defineInRange("reinforcedWireRate", 512, 1, 1000000);
        HEAVY_WIRE_RATE = BUILDER.comment("Fm per tick a network can carry when every wire is (Raw) Heavy.")
                .defineInRange("heavyWireRate", 1024, 1, 1000000);
        WIRE_SHOCK_DAMAGE = BUILDER.comment("Damage from live Raw Wire (2 = one heart). Doubles for Raw Reinforced, and again for Raw Heavy (which also sets you alight).")
                .defineInRange("wireShockDamage", 2.0, 0.0, 40.0);
        WIRE_SHOCK_RANGE = BUILDER.comment("How far live Raw Wire reaches to shock, in blocks. +1 for Raw Reinforced, +2 for Raw Heavy.")
                .defineInRange("wireShockRange", 1.5, 0.0, 16.0);
        TREE_TAP_SECONDS = BUILDER.comment("Seconds for a Tree Tap to add one drip-level of latex to the bucket below it (a bucket holds 4).")
                .defineInRange("treeTapSeconds", 60, 1, 3600);
        DATA_CABLE_MAX_LENGTH = BUILDER.comment("Longest Data Cable run (in cable blocks) from the Reactor Control Console to a Ring Injection Port.")
                .defineInRange("dataCableMaxLength", 128, 4, 1024);
        CHAIN_MAX_MACHINES = BUILDER.comment("Most machines in a daisy chain (machines touching each other) that can pass power along.")
                .defineInRange("chainMaxMachines", 8, 1, 256);
        BUILDER.pop();

        BUILDER.comment("Mini Collider").push("collider");
        COLLIDER_FM_PER_SHOT = BUILDER.comment("Fm released by one collision (two Spark Cells).")
                .defineInRange("colliderFmPerShot", 20480, 1, 1000000);
        COLLIDER_RELEASE_TICKS = BUILDER.comment("Ticks over which a collision's Fm is released (20 ticks = 1 second).")
                .defineInRange("colliderReleaseTicks", 160, 1, 72000);
        STORM_FM_PER_SHOT = BUILDER.comment("Fm the Storm Collider uses for each pairing shot.")
                .defineInRange("stormFmPerShot", 4096, 0, 1000000);
        BUILDER.pop();

        BUILDER.comment("Converters (one-way, from Fm to other mods' power)").push("converters");
        FE_PER_FM = BUILDER.comment("Forge Energy (FE) produced per Fm.")
                .defineInRange("fePerFm", 4, 1, 10000);
        EU_PER_FM = BUILDER.comment("EU produced per Fm.")
                .defineInRange("euPerFm", 1, 1, 10000);
        EU_LV = BUILDER.comment("EU per tick for the LV tier.").defineInRange("euLv", 32, 1, Integer.MAX_VALUE);
        EU_MV = BUILDER.comment("EU per tick for the MV tier.").defineInRange("euMv", 128, 1, Integer.MAX_VALUE);
        EU_HV = BUILDER.comment("EU per tick for the HV tier.").defineInRange("euHv", 512, 1, Integer.MAX_VALUE);
        EU_EV = BUILDER.comment("EU per tick for the EV tier.").defineInRange("euEv", 2048, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.comment("Fusion Reactor").push("reactor");
        REACTOR_MAX_FM = BUILDER.comment("How much Fm the Reactor Control Console can store.")
                .defineInRange("reactorMaxFm", 16_000_000, 10_000, Integer.MAX_VALUE);
        REACTOR_STARTUP_FM = BUILDER.comment("Fm needed to start a fusion run (taken from the console's store, or pulled from a battery behind a power port).")
                .defineInRange("reactorStartupFm", 8_000, 0, 1_000_000);
        REACTOR_FM_MULTIPLIER = BUILDER.comment("Multiplies every fusion run's Fm yield.")
                .defineInRange("reactorFmMultiplier", 1.0, 0.0, 100.0);
        REACTOR_OUTPUT_RATE = BUILDER.comment("Fm per tick each Console Power Port pushes into the block behind it.")
                .defineInRange("reactorOutputRate", 4096, 1, 1_000_000);
        BUILDER.pop();

        BUILDER.comment("Mobs").push("mobs");
        STORMWING_TRIP_MIN_MINUTES = BUILDER.comment("Shortest time (real minutes) a Stormwing stays on its nest between hunting trips.")
                .defineInRange("stormwingTripMinMinutes", 20, 1, 600);
        STORMWING_TRIP_MAX_MINUTES = BUILDER.comment("Longest time (real minutes) a Stormwing stays on its nest between hunting trips.")
                .defineInRange("stormwingTripMaxMinutes", 30, 1, 600);
        STORMWING_AWAY_MAX_MINUTES = BUILDER.comment("Longest a hunting trip lasts (real minutes). The Arc Serpent guards the nest meanwhile.")
                .defineInRange("stormwingAwayMaxMinutes", 10, 1, 600);
        STORMWING_EGG_CHANCE = BUILDER.comment("Chance a new Stormwing nest holds an egg (0.1 = 1 in 10).")
                .defineInRange("stormwingEggChance", 0.1, 0.0, 1.0);
        EGG_INCUBATION_MIN_MINUTES = BUILDER.comment("Shortest incubation (real minutes beside a running Storm Collider).")
                .defineInRange("eggIncubationMinMinutes", 20, 1, 600);
        EGG_INCUBATION_MAX_MINUTES = BUILDER.comment("Longest incubation (real minutes beside a running Storm Collider).")
                .defineInRange("eggIncubationMaxMinutes", 40, 1, 600);
        EGG_INCUBATION_RADIUS = BUILDER.comment("How close (blocks) an egg must be to the Storm Collider's core to incubate.")
                .defineInRange("eggIncubationRadius", 8, 2, 16);
        FERAL_CHANCE_BAD = BUILDER.comment("Chance each Bad pairing fired during incubation makes the hatchling Feral.")
                .defineInRange("feralChanceBad", 0.15, 0.0, 1.0);
        FERAL_CHANCE_CATASTROPHIC = BUILDER.comment("Chance each Catastrophic pairing fired during incubation makes the hatchling Feral.")
                .defineInRange("feralChanceCatastrophic", 0.40, 0.0, 1.0);
        HATCHLING_GROW_MINUTES = BUILDER.comment("Real minutes for a hatchling to grow up (20 = one Minecraft day).")
                .defineInRange("hatchlingGrowMinutes", 20, 1, 600);
        STORM_CALLER_MINUTES = BUILDER.comment("How long (real minutes) a thunderstorm called by the Storm Caller lasts.")
                .defineInRange("stormCallerMinutes", 5, 1, 60);
        BUILDER.pop();

        BUILDER.comment("Lightning upgrade ritual for Storm Furnaces").push("ritual");
        RITUAL_ENABLED = BUILDER.comment("Allow Storm Furnaces to be upgraded by lightning.")
                .define("ritualEnabled", true);
        ENHANCED_SPARK_CELLS = BUILDER.comment("Charged Spark Cells needed for Weak -> Enhanced.")
                .defineInRange("enhancedSparkCells", 2, 0, 64);
        ENHANCED_MAGNETITE = BUILDER.comment("Magnetite Ingots needed for Weak -> Enhanced.")
                .defineInRange("enhancedMagnetite", 4, 0, 64);
        CHARGED_SPARK_CELLS = BUILDER.comment("Charged Spark Cells needed for Enhanced -> Charged.")
                .defineInRange("chargedSparkCells", 6, 0, 64);
        CHARGED_SILVER = BUILDER.comment("Silver Ingots needed for Enhanced -> Charged.")
                .defineInRange("chargedSilver", 6, 0, 64);
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
