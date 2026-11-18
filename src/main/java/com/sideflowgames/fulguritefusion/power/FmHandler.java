package com.sideflowgames.fulguritefusion.power;

// Anything that can receive and store Fulmen (Fm), the mod's own power.
// Machines implement this; the Storm Furnace only sends, so it does not.
public interface FmHandler {
    // Offer some Fm. Returns how much was actually accepted.
    int receiveFm(int amount, boolean simulate);

    int getFm();

    int getMaxFm();

    // True for storage blocks (Storm Battery). Machines don't share their power back into storage,
    // so power flows furnace -> battery -> machines instead of bouncing back and forth.
    default boolean isStorage() {
        return false;
    }

    // Daisy chains (see DaisyChain): a machine passing power to a touching machine tells it its position in
    // the chain. Every machine type follows this; batteries keep the defaults below and don't take part.
    default void touchChain(int depth) {}

    default int receiveFmFromChain(int amount, int depth) {
        return receiveFm(amount, false);
    }
}
