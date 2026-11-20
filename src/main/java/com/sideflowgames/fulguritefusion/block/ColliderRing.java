package com.sideflowgames.fulguritefusion.block;

// The 24 edge spots of the 7x7 collider ring, in a fixed order shared by the server (which checks them)
// and the menus (which draw them). Local coordinates: u = sideways, v = toward the Collider Input (v = 3).
public final class ColliderRing {
    public static final int RADIUS = ColliderCoreBlockEntity.RADIUS;
    public static final int SPOTS = 24;
    public static final int RING = 0, INPUT = 1, INJECTOR = 2;
    // [index] -> {u, v}
    private static final int[][] SPOT = build();

    private ColliderRing() {}

    private static int[][] build() {
        int[][] s = new int[SPOTS][];
        int i = 0;
        for (int v = RADIUS; v >= -RADIUS; v--) {
            for (int u = -RADIUS; u <= RADIUS; u++) {
                if (Math.max(Math.abs(u), Math.abs(v)) == RADIUS) s[i++] = new int[]{u, v};
            }
        }
        return s;
    }

    public static int u(int index) {
        return SPOT[index][0];
    }

    public static int v(int index) {
        return SPOT[index][1];
    }

    // Which block belongs at a spot.
    public static int kind(int u, int v) {
        if (u == 0 && v == RADIUS) return INPUT;
        if (v == 0) return INJECTOR;
        return RING;
    }

    public static String kindName(int kind) {
        return switch (kind) {
            case INPUT -> "Collider Input";
            case INJECTOR -> "Collider Injector";
            default -> "Fulgurite Ring";
        };
    }
}
