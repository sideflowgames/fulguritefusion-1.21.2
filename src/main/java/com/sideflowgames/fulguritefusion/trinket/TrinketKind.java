package com.sideflowgames.fulguritefusion.trinket;

// The six trinkets, in the order of their equipment slots (the Back slot comes after them, see Trinkets.BACK).
// curiosSlot is the Curios slot each one goes in when Curios is installed (data/fulguritefusion/curios).
public enum TrinketKind {
    CLOUDSTEP_CHARM("cloudstep_charm"),
    CONDUIT_HEART("conduit_heart"),
    VOID_ARC_BAND("void_arc_band"),
    GROUNDING_CHARM("grounding_charm"),
    CINDER_WARD("cinder_ward"),
    SEERS_RING("seers_ring");

    public final String id;

    TrinketKind(String id) {
        this.id = id;
    }

    public int slot() {
        return ordinal();
    }

    public String curiosSlot() {
        return id;
    }
}
