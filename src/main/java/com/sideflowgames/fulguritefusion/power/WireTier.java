package com.sideflowgames.fulguritefusion.power;

import com.sideflowgames.fulguritefusion.Config;

// The kinds of hanging wire: three gauges, each bare (raw) or insulated with rubber.
// A network carries as much as its weakest wire can (so upgrade every span).
//   Basic: 256 Fm/t, 16 blocks.   Reinforced: 512 Fm/t, 32 blocks.   Heavy: 1024 Fm/t, 64 blocks, pulled taut.
// Raw wire is cheaper but, while power flows, shocks anyone near it and electrifies water it dips into,
// harder for each gauge: Basic 2 damage / 1.5 blocks, Reinforced 4 / 2.5, Heavy 8 / 3.5 and sets you alight.
// Insulate raw wire in the Wire Extruder with Malleable Rubber. Rates, lengths and shocks are in the config.
public enum WireTier {
    RAW("raw", false, 0),
    INSULATED("insulated", true, 0),
    RAW_REINFORCED("raw_reinforced", false, 1),
    REINFORCED("reinforced", true, 1),
    RAW_HEAVY("raw_heavy", false, 2),
    HEAVY("heavy", true, 2);

    public final String id;
    public final boolean insulated;
    public final int gauge;     // 0 basic, 1 reinforced, 2 heavy
    public final double sag;    // how much it hangs, compared with a basic wire
    public final float radius;  // drawn thickness

    WireTier(String id, boolean insulated, int gauge) {
        this.id = id;
        this.insulated = insulated;
        this.gauge = gauge;
        this.sag = new double[]{1.0, 0.75, 0.4}[gauge];
        this.radius = new float[]{0.035f, 0.05f, 0.07f}[gauge];
    }

    public int rate() {
        return switch (gauge) {
            case 0 -> Config.WIRE_RATE.get();
            case 1 -> Config.REINFORCED_WIRE_RATE.get();
            default -> Config.HEAVY_WIRE_RATE.get();
        };
    }

    public int maxLength() {
        return switch (gauge) {
            case 0 -> Config.WIRE_MAX_LENGTH.get();
            case 1 -> Config.REINFORCED_WIRE_MAX_LENGTH.get();
            default -> Config.HEAVY_WIRE_MAX_LENGTH.get();
        };
    }

    // Raw wire only: damage per shock (doubles each gauge) and how far it reaches (+1 block each gauge).
    public float shockDamage() {
        return (float) (Config.WIRE_SHOCK_DAMAGE.get() * (1 << gauge));
    }

    public double shockRange() {
        return Config.WIRE_SHOCK_RANGE.get() + gauge;
    }

    public boolean shockBurns() {
        return gauge == 2;
    }

    // The bare version of this gauge, and the insulated one.
    public WireTier raw() {
        return values()[gauge * 2];
    }

    public WireTier insulatedVersion() {
        return values()[gauge * 2 + 1];
    }

    public static WireTier byId(String id) {
        for (WireTier t : values()) if (t.id.equals(id)) return t;
        return INSULATED;
    }
}
