package com.sideflowgames.fulguritefusion.transport;

import net.minecraft.util.StringRepresentable;

// What an Item Pipe does on one side: nothing there, another pipe, an inventory it delivers into, or an inventory
// it pulls out of.
public enum PipeSide implements StringRepresentable {
    NONE("none"), PIPE("pipe"), IN("in"), OUT("out");

    private final String name;

    PipeSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
