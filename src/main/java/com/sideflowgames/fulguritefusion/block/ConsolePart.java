package com.sideflowgames.fulguritefusion.block;

import net.minecraft.util.StringRepresentable;

// What a Console Casing shows once the Reactor Control Console forms (NONE = loose casing).
public enum ConsolePart implements StringRepresentable {
    NONE("none"),
    FRAME("frame"),
    GAUGE_TOP("gauge_top"),
    GAUGE_BOTTOM("gauge_bottom"),
    DISPLAY("display"),
    FUEL_SCREEN_TOP("fuel_screen_top"),
    FUEL_SCREEN_BOTTOM("fuel_screen_bottom");

    private final String name;

    ConsolePart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
