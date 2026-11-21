package com.sideflowgames.fulguritefusion.backpack;

// Backpack tiers, sized like Sophisticated Backpacks (the Prime tier, 120 slots, comes with the Prime gear).
// Upgrade slots are reserved for the backpack upgrades still to come.
public enum BackpackTier {
    COPPER("copper", 9, 3, 1),
    IRON("iron", 9, 6, 2),
    FULGURITE("fulgurite", 9, 9, 3),
    STORMFORGED("stormforged", 12, 9, 5);

    public final String id;
    public final int columns, rows, upgradeSlots;

    BackpackTier(String id, int columns, int rows, int upgradeSlots) {
        this.id = id;
        this.columns = columns;
        this.rows = rows;
        this.upgradeSlots = upgradeSlots;
    }

    public int slots() {
        return columns * rows;
    }
}
