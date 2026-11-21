package com.sideflowgames.fulguritefusion.backpack;

// The kinds of backpack upgrade, and the smallest backpack each fits. Every kind also has a Stormforged
// version (made in the Storm Forge) that only fits the Stormforged Backpack and gets stronger while the
// pack has Fm.
//   Pickup: items you pick up go into the backpack first.
//   Magnet: pulls nearby items to you (6 blocks; Stormforged 12 while powered).
//   Void: destroys filtered items as you pick them up.
//   Compacting: turns 2x2 sets into their block (Stormforged also 3x3 while powered). Only recipes that
//   craft back into the same items, so nothing is lost (honey, iron, diamonds, wheat to hay, and so on).
//   Feeding: eats food from the pack when you're hungry (skips harmful food unless you allow-list it).
//   Refill: tops up hotbar stacks from the pack, and refills a slot when you use its last few items.
//   Restock / Deposit: buttons beside an open container move items between it and the pack.
//   Tool Swapper: swaps the best tool from the pack into your hand when you start mining.
public enum UpgradeKind {
    PICKUP("pickup", BackpackTier.COPPER, false),
    MAGNET("magnet", BackpackTier.IRON, false),
    VOID("void", BackpackTier.COPPER, true),
    COMPACTING("compacting", BackpackTier.FULGURITE, false),
    FEEDING("feeding", BackpackTier.COPPER, false),
    REFILL("refill", BackpackTier.COPPER, false),
    RESTOCK("restock", BackpackTier.IRON, false),
    DEPOSIT("deposit", BackpackTier.IRON, false),
    TOOL_SWAPPER("tool_swapper", BackpackTier.FULGURITE, false);

    public final String id;
    public final BackpackTier minTier;
    // Void starts as an allow list (only what you list is destroyed); the others start as a block list
    // (empty = everything).
    public final boolean defaultAllow;

    UpgradeKind(String id, BackpackTier minTier, boolean defaultAllow) {
        this.id = id;
        this.minTier = minTier;
        this.defaultAllow = defaultAllow;
    }
}
