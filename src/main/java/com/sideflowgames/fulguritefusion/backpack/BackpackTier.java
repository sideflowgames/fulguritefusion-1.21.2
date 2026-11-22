package com.sideflowgames.fulguritefusion.backpack;

// Backpack tiers, sized like Sophisticated Backpacks. Stormforged and Prime hold their own Fm (maxFm) to power
// the boosts of Stormforged upgrades.
public enum BackpackTier {
    COPPER("copper", 9, 3, 1, 0),
    IRON("iron", 9, 6, 2, 0),
    FULGURITE("fulgurite", 9, 9, 3, 0),
    STORMFORGED("stormforged", 12, 9, 5, 500_000),
    PRIME("prime", 12, 10, 7, 2_000_000);

    public final String id;
    public final int columns, rows, upgradeSlots, maxFm;

    BackpackTier(String id, int columns, int rows, int upgradeSlots, int maxFm) {
        this.id = id;
        this.columns = columns;
        this.rows = rows;
        this.upgradeSlots = upgradeSlots;
        this.maxFm = maxFm;
    }

    // Holds Fm and takes Stormforged upgrades.
    public boolean powered() {
        return maxFm > 0;
    }

    public int slots() {
        return columns * rows;
    }
}
