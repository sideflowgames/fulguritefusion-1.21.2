package com.sideflowgames.fulguritefusion.entity;

import com.sideflowgames.fulguritefusion.FulguriteFusion;

import net.minecraft.resources.ResourceLocation;

// DEV BUILD ONLY: the hatchling variants. Do not copy this list into the design doc or public pages.
// A hatchling's variant is the exotic material fired most often by the Storm Collider while its egg incubated
// (same order as Pairings.MATERIAL_NAMES). No firing at all hatches a PLAIN storm-gray bird.
// Each variant changes the bird's look plus two stats: flying/walking speed and attack damage.
public enum StormwingVariant {
    PLAIN("plain", 1.00, 1.00, false),
    AURORA("aurora", 1.05, 0.90, false),      // Aurora Dust: teal-green, glows
    SUNMOTE("sunmote", 1.10, 1.10, false),    // Sunmote: gold
    NACRE("nacre", 0.90, 1.15, false),        // Abyssal Nacre: deep pearl blue, heavy hitter
    FROST("frost", 1.00, 1.05, false),        // Soul Frost: icy cyan
    ECHO("echo", 1.00, 1.20, false),          // Echo Resin: dark sculk teal
    GALE("gale", 1.30, 0.85, false),          // Gale Core: pale lilac, fastest
    TEMPEST("tempest", 1.15, 1.15, false),    // Stormwing Plume: electric blue
    NULL("null", 1.10, 1.00, false),          // Null Scale: void purple-black
    LUMEN("lumen", 0.95, 0.90, false),        // Lumen Gel: bright green, glows
    CINDER("cinder", 1.25, 1.30, true),       // Cinder Heart: ember red; faster and more aggressive
    ASH("ash", 1.00, 1.25, false),            // Wither Ash: ashen black
    SPORE("spore", 1.10, 1.10, false);        // Struck Spore: mushroom red

    public final String id;
    public final double speed;
    public final double attack;
    // Aggressive birds also go after hostile mobs near their owner on their own.
    public final boolean aggressive;
    private final ResourceLocation texture;

    StormwingVariant(String id, double speed, double attack, boolean aggressive) {
        this.id = id;
        this.speed = speed;
        this.attack = attack;
        this.aggressive = aggressive;
        this.texture = ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID,
                id.equals("plain") ? "textures/entity/stormwing.png" : "textures/entity/stormwing_" + id + ".png");
    }

    public ResourceLocation texture() {
        return texture;
    }

    // Material index (0-11) -> variant.
    public static StormwingVariant fromMaterial(int material) {
        return material < 0 || material >= 12 ? PLAIN : values()[material + 1];
    }

    public static StormwingVariant byId(int ordinal) {
        return ordinal < 0 || ordinal >= values().length ? PLAIN : values()[ordinal];
    }

    // Anything that shows a variant (adult Stormwing, hatchling).
    public interface Holder {
        StormwingVariant stormwingVariant();
    }
}
