package com.sideflowgames.fulguritefusion.power;

// Anything a wire network can draw Fm out of (Storm Furnaces, Storm Batteries).
public interface FmSource {
    // Take up to 'max' Fm out. Returns how much was (or would be, if simulating) taken.
    int extractFm(int max, boolean simulate);
}
