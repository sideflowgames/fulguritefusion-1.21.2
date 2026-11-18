package com.sideflowgames.fulguritefusion.pairing;

import java.util.ArrayList;
import java.util.List;

// The 78 exotic-material pairings (every unordered pair of the 12 materials).
// Each pairing has a hand-named result and a tier. The Storm Collider turns a pairing into a Fusion Fuel
// capsule (plus any byproduct); the big reactor will burn that fuel for its energy and side effects.
public final class Pairings {
    public enum Tier {
        FIZZLE(0.0), STEADY(1.0), STRONG(1.5), JACKPOT(2.5), UNSTABLE(1.3), BAD(0.3), CATASTROPHIC(0.5), WILDCARD(1.0);

        public final double fuelMultiplier;

        Tier(double fuelMultiplier) {
            this.fuelMultiplier = fuelMultiplier;
        }

        public String display() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    // Material order matches the design doc. Energy 0 = Struck Spore (random).
    public static final String[] MATERIAL_NAMES = {"Aurora Dust", "Sunmote", "Abyssal Nacre", "Soul Frost", "Echo Resin",
            "Gale Core", "Stormwing Plume", "Null Scale", "Lumen Gel", "Cinder Heart", "Wither Ash", "Struck Spore"};
    public static final int[] ENERGY = {2, 4, 3, 3, 4, 4, 5, 1, 2, 5, 3, 0};
    public static final int STRUCK_SPORE = 11;
    public static final int FM_PER_ENERGY = 2048;

    public record Pairing(int id, int a, int b, String name, Tier tier, String effect) {
        public boolean involves(int material) {
            return a == material || b == material;
        }

        // Fm this fuel will give the big reactor (wildcards are rolled into a real pairing first).
        public int fuelFm() {
            return (int) Math.round((ENERGY[a] + ENERGY[b]) * FM_PER_ENERGY * tier.fuelMultiplier);
        }
    }

    private static final List<Pairing> ALL = new ArrayList<>();
    private static final Pairing[][] BY_MATERIALS = new Pairing[12][12];

    static {
        p(1, 0, 0, "Aurora Veil", Tier.STEADY, "Low, very reliable Fm; the beginner fuel");
        p(2, 0, 1, "Daybreak", Tier.STRONG, "High Fm, fully stable");
        p(3, 0, 2, "Tidelight", Tier.STEADY, "Slow, long burn; very safe");
        p(4, 0, 3, "Polar Glow", Tier.STEADY, "Moderate Fm; frost shimmer forms around the chamber");
        p(5, 0, 4, "Night Chorus", Tier.STRONG, "Ring hums; byproduct Resonant Shard (coil upgrade)");
        p(6, 0, 5, "Torn Sky", Tier.UNSTABLE, "Output spikes; chance the ring overheats");
        p(7, 0, 6, "Storm Aurora", Tier.STRONG, "High Fm; a harmless aurora lights the sky");
        p(8, 0, 7, "Snuffed", Tier.FIZZLE, "No reaction");
        p(9, 0, 8, "Bloomlight", Tier.STEADY, "Modest Fm; glowing plants grow around the reactor");
        p(10, 0, 9, "Flare", Tier.UNSTABLE, "Big burst; chamber heat spike");
        p(11, 0, 10, "Ashfall", Tier.BAD, "Ash rains; Wither effect on nearby mobs and players");
        p(12, 0, 11, "Wild Veil", Tier.WILDCARD, "Random Aurora result");
        p(13, 1, 1, "Twin Suns", Tier.STRONG, "Very high Fm; heat builds, needs cooldown breaks");
        p(14, 1, 2, "Sunken Sun", Tier.STRONG, "High, stable Fm; byproduct Pearl Ember");
        p(15, 1, 3, "Thermal Shock", Tier.CATASTROPHIC, "Chamber cracks: Chamber Breach explosion");
        p(16, 1, 4, "Solar Shriek", Tier.UNSTABLE, "Loud blast; sculk spreads around the reactor");
        p(17, 1, 5, "Sunstorm", Tier.UNSTABLE, "Good Fm; fires ignite around the reactor");
        p(18, 1, 6, "Zenith Strike", Tier.JACKPOT, "Huge Fm; byproduct Prime Spark Fragment");
        p(19, 1, 7, "Eclipse", Tier.STEADY, "Dampened, safe, moderate Fm");
        p(20, 1, 8, "Photosynth", Tier.STRONG, "High Fm; nearby crops grow instantly");
        p(21, 1, 9, "Solar Flare", Tier.CATASTROPHIC, "Fire explosion plus Ring Melt");
        p(22, 1, 10, "Scorched Rot", Tier.BAD, "Wither field around the reactor");
        p(23, 1, 11, "Wild Sun", Tier.WILDCARD, "Random Sunmote result");
        p(24, 2, 2, "Deep Pressure", Tier.STEADY, "Slow, extremely safe, longest run");
        p(25, 2, 3, "Black Ice", Tier.STEADY, "Moderate Fm; nearby water freezes");
        p(26, 2, 4, "Drowned Choir", Tier.BAD, "Waves of Drowned spawn");
        p(27, 2, 5, "Maelstrom", Tier.UNSTABLE, "Good Fm; a water vortex pulls in items and mobs");
        p(28, 2, 6, "Thunderhead", Tier.STRONG, "High Fm; it starts raining");
        p(29, 2, 7, "Dead Calm", Tier.FIZZLE, "No reaction");
        p(30, 2, 8, "Reef Bloom", Tier.STEADY, "Moderate Fm; glowing coral grows");
        p(31, 2, 9, "Steam Burst", Tier.BAD, "Scalding steam cloud; ring damage");
        p(32, 2, 10, "Black Tide", Tier.BAD, "Nearby water turns to poisoned blight water");
        p(33, 2, 11, "Wild Tide", Tier.WILDCARD, "Random Nacre result");
        p(34, 3, 3, "Deep Freeze", Tier.STEADY, "Moderate Fm; the reactor runs cooler");
        p(35, 3, 4, "Shatter Hymn", Tier.UNSTABLE, "Good Fm; glass and ice nearby shatter");
        p(36, 3, 5, "Blizzard", Tier.BAD, "Freezing storm buries the area in powder snow");
        p(37, 3, 6, "Frostbolt", Tier.STRONG, "High Fm; cooling bonus");
        p(38, 3, 7, "Stillness", Tier.STEADY, "Low, very safe; a cooldown shot");
        p(39, 3, 8, "Winter Bloom", Tier.STEADY, "Moderate Fm; frost flowers grow");
        p(40, 3, 9, "Frostburn", Tier.CATASTROPHIC, "Cold soul fire spreads, freezing and burning");
        p(41, 3, 10, "Soul Rot", Tier.BAD, "Strays and Wither Skeletons spawn");
        p(42, 3, 11, "Wild Frost", Tier.WILDCARD, "Random Soul Frost result");
        p(43, 4, 4, "Warden Call", Tier.BAD, "A Warden spawns at the reactor, then despawns after a while");
        p(44, 4, 5, "Howling Gale", Tier.BAD, "Sonic shockwave; knockback, glass breaks");
        p(45, 4, 6, "Thunderclap", Tier.STRONG, "High Fm; the boom carries for miles");
        p(46, 4, 7, "Silence", Tier.FIZZLE, "No reaction; the area goes silent briefly");
        p(47, 4, 8, "Living Choir", Tier.STEADY, "Moderate Fm; Allays are drawn to the reactor");
        p(48, 4, 9, "Sonic Detonation", Tier.CATASTROPHIC, "Explosion plus Darkness effect");
        p(49, 4, 10, "Deathknell", Tier.BAD, "Darkness and Wither in a wide radius");
        p(50, 4, 11, "Wild Echo", Tier.WILDCARD, "Random Echo Resin result");
        p(51, 5, 5, "Twin Gyre", Tier.UNSTABLE, "High but erratic Fm; wind scatters items");
        p(52, 5, 6, "Storm Surge", Tier.CATASTROPHIC, "Lightning storm strikes everything nearby");
        p(53, 5, 7, "Eye of the Storm", Tier.STEADY, "Chaos cancelled; stable moderate Fm");
        p(54, 5, 8, "Spore Wind", Tier.UNSTABLE, "Good Fm; glowing slime splatters the area");
        p(55, 5, 9, "Firestorm", Tier.CATASTROPHIC, "Fire tornado and explosion");
        p(56, 5, 10, "Ash Cyclone", Tier.BAD, "A Wither cloud spreads");
        p(57, 5, 11, "Wild Gyre", Tier.WILDCARD, "Random Gale Core result");
        p(58, 6, 6, "Twin Strike", Tier.STRONG, "Very high Fm; small chance of Storm Surge");
        p(59, 6, 7, "Grounded", Tier.STEADY, "Safe, moderate Fm; Null Scale soaks up excess");
        p(60, 6, 8, "Galvanize", Tier.STRONG, "High Fm; byproduct Living Spark");
        p(61, 6, 9, "Overcharge", Tier.UNSTABLE, "Huge Fm; Static Bloom around the reactor");
        p(62, 6, 10, "Black Lightning", Tier.BAD, "Withering lightning strikes the area");
        p(63, 6, 11, "Wild Storm", Tier.WILDCARD, "Random Stormwing result");
        p(64, 7, 7, "Void Collapse", Tier.BAD, "Drains all stored Fm from connected batteries");
        p(65, 7, 8, "Dormancy", Tier.STEADY, "Very low Fm; very stable");
        p(66, 7, 9, "Contained Flare", Tier.STEADY, "Moderate-high Fm; the safe way to burn Cinder Heart");
        p(67, 7, 10, "Hollow Rot", Tier.BAD, "Drains XP from nearby players");
        p(68, 7, 11, "Muted Mutation", Tier.WILDCARD, "Random Null Scale result; never Catastrophic");
        p(69, 8, 8, "Bloom", Tier.STEADY, "Low Fm; lush growth spreads");
        p(70, 8, 9, "Burnout", Tier.BAD, "Fire; Lumen growth around the reactor dies");
        p(71, 8, 10, "Rot Bloom", Tier.BAD, "Hostile mobs spawn from the rot");
        p(72, 8, 11, "Mutation", Tier.WILDCARD, "Random Lumen result; rare Jackpot chance");
        p(73, 9, 9, "Meltdown", Tier.CATASTROPHIC, "Chamber Breach plus Ring Melt");
        p(74, 9, 10, "Hellfire", Tier.CATASTROPHIC, "A Wither spawns");
        p(75, 9, 11, "Wild Ember", Tier.WILDCARD, "Random Cinder Heart result; leans dangerous");
        p(76, 10, 10, "Necrosis", Tier.BAD, "Grass and dirt turn to soul soil");
        p(77, 10, 11, "Wild Rot", Tier.WILDCARD, "Random Wither Ash result; always harmful");
        p(78, 11, 11, "Chain Mutation", Tier.WILDCARD, "Two rolls: Jackpot with a Prime Spark Fragment, or a Catastrophe");
    }

    private Pairings() {}

    private static void p(int id, int a, int b, String name, Tier tier, String effect) {
        Pairing pairing = new Pairing(id, a, b, name, tier, effect);
        ALL.add(pairing);
        BY_MATERIALS[a][b] = pairing;
        BY_MATERIALS[b][a] = pairing;
    }

    public static Pairing of(int a, int b) {
        return BY_MATERIALS[a][b];
    }

    // id is 1-78, as numbered in the design doc.
    public static Pairing byId(int id) {
        return id >= 1 && id <= ALL.size() ? ALL.get(id - 1) : null;
    }

    public static List<Pairing> all() {
        return ALL;
    }
}
