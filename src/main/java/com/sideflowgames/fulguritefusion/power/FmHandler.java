package com.sideflowgames.fulguritefusion.power;

// Anything that can receive and store Fulmen (Fm), the mod's own power.
// Machines implement this; the Storm Furnace only sends, so it does not.
public interface FmHandler {
    // Offer some Fm. Returns how much was actually accepted.
    int receiveFm(int amount, boolean simulate);

    int getFm();

    int getMaxFm();
}
