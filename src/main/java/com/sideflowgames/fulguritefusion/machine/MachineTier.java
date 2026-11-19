package com.sideflowgames.fulguritefusion.machine;

// Arc Casing tiers. A machine runs at the tier of its LOWEST casing.
// Each tier doubles speed and draws a bit more Fm per tick, so every item costs less energy.
//   Tier 1 Crude:    1x speed, 16 Fm/t (100% energy per item)
//   Tier 2 Tempered: 2x speed, 24 Fm/t (75%)
//   Tier 3 Charged:  4x speed, 40 Fm/t (62.5%)
//   Tier 4 Prime:    8x speed, 64 Fm/t (50%)
public final class MachineTier {
    public static final String[] NAMES = {"", "Crude", "Tempered", "Charged", "Prime"};
    private static final int[] SPEED = {0, 1, 2, 4, 8};
    private static final int[] FM_PER_TICK = {0, 16, 24, 40, 64};

    private MachineTier() {}

    public static int speed(int tier) {
        return SPEED[Math.max(0, Math.min(4, tier))];
    }

    public static int fmPerTick(int tier) {
        return FM_PER_TICK[Math.max(0, Math.min(4, tier))];
    }

    public static int ticksPerItem(MachineType type, int tier) {
        return Math.max(1, type.baseTicks / Math.max(1, speed(tier)));
    }
}
