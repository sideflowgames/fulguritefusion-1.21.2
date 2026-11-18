package com.sideflowgames.fulguritefusion;

import net.neoforged.neoforge.common.ModConfigSpec;

// Fulgurite Fusion settings. Saved in config/fulguritefusion-common.toml and editable from the Mods menu.
// Grouped into sections: safety, static, power, converters, ritual.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- Server safety (used by the fusion reactor's failure events, coming later) ---
    public static final ModConfigSpec.BooleanValue BLOCK_DAMAGE;
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

    // --- Converters ---
    public static final ModConfigSpec.IntValue FE_PER_FM;
    public static final ModConfigSpec.IntValue EU_PER_FM;
    public static final ModConfigSpec.IntValue EU_LV;
    public static final ModConfigSpec.IntValue EU_MV;
    public static final ModConfigSpec.IntValue EU_HV;
    public static final ModConfigSpec.IntValue EU_EV;

    // --- Lightning upgrade ritual ---
    public static final ModConfigSpec.BooleanValue RITUAL_ENABLED;
    public static final ModConfigSpec.IntValue ENHANCED_SPARK_CELLS;
    public static final ModConfigSpec.IntValue ENHANCED_DIAMONDS;
    public static final ModConfigSpec.IntValue CHARGED_SPARK_CELLS;
    public static final ModConfigSpec.IntValue CHARGED_NETHERITE;

    static {
        BUILDER.comment("Server safety: what reactor failures are allowed to do").push("safety");
        BLOCK_DAMAGE = BUILDER.comment("Bad and Catastrophic reactor results can destroy blocks. Turn off for block-protected servers.")
                .define("blockDamage", true);
        RING_MELT = BUILDER.comment("Failures can melt upgraded coils back into raw fulgurite.")
                .define("ringMelt", true);
        BOSS_SPAWNS = BUILDER.comment("Failures can summon boss mobs (e.g. the Hellfire pairing spawning a Wither).")
                .define("bossSpawns", true);
        BUILDER.pop();

        BUILDER.comment("Static discharge from ungrounded Storm Furnaces").push("static");
        STATIC_ENABLED = BUILDER.comment("Ungrounded Storm Furnaces shock nearby players.")
                .define("staticEnabled", true);
        STATIC_DAMAGE = BUILDER.comment("Damage every 2 seconds from a Weak furnace (2 = one heart). Doubles for each higher tier.")
                .defineInRange("staticDamage", 1.0, 0.0, 40.0);
        STATIC_RANGE = BUILDER.comment("How far the shock reaches, in blocks.")
                .defineInRange("staticRange", 2.0, 0.0, 16.0);
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
        BUILDER.pop();

        BUILDER.comment("Converters (one-way, from Fm to other mods' power)").push("converters");
        FE_PER_FM = BUILDER.comment("Forge Energy (FE) produced per Fm.")
                .defineInRange("fePerFm", 10, 1, 10000);
        EU_PER_FM = BUILDER.comment("EU produced per Fm.")
                .defineInRange("euPerFm", 1, 1, 10000);
        EU_LV = BUILDER.comment("EU per tick for the LV tier.").defineInRange("euLv", 32, 1, Integer.MAX_VALUE);
        EU_MV = BUILDER.comment("EU per tick for the MV tier.").defineInRange("euMv", 128, 1, Integer.MAX_VALUE);
        EU_HV = BUILDER.comment("EU per tick for the HV tier.").defineInRange("euHv", 512, 1, Integer.MAX_VALUE);
        EU_EV = BUILDER.comment("EU per tick for the EV tier.").defineInRange("euEv", 2048, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.comment("Lightning upgrade ritual for Storm Furnaces").push("ritual");
        RITUAL_ENABLED = BUILDER.comment("Allow Storm Furnaces to be upgraded by lightning.")
                .define("ritualEnabled", true);
        ENHANCED_SPARK_CELLS = BUILDER.comment("Charged Spark Cells needed for Weak -> Enhanced.")
                .defineInRange("enhancedSparkCells", 2, 0, 64);
        ENHANCED_DIAMONDS = BUILDER.comment("Diamonds needed for Weak -> Enhanced.")
                .defineInRange("enhancedDiamonds", 1, 0, 64);
        CHARGED_SPARK_CELLS = BUILDER.comment("Charged Spark Cells needed for Enhanced -> Charged.")
                .defineInRange("chargedSparkCells", 6, 0, 64);
        CHARGED_NETHERITE = BUILDER.comment("Netherite ingots needed for Enhanced -> Charged.")
                .defineInRange("chargedNetherite", 6, 0, 64);
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
