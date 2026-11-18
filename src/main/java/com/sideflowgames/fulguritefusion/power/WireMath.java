package com.sideflowgames.fulguritefusion.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

// Shape of a hanging wire: a straight line between two connector caps, sagging in the middle.
public final class WireMath {
    private WireMath() {}

    // Where the wire attaches: the centre of the connector's top cap.
    public static Vec3 attachPoint(BlockPos connector) {
        return new Vec3(connector.getX() + 0.5, connector.getY() + 10.5 / 16.0, connector.getZ() + 0.5);
    }

    // How far the middle hangs down, in blocks.
    public static double sag(Vec3 a, Vec3 b) {
        return 0.15 + a.distanceTo(b) * 0.07;
    }

    // A point along the wire, t from 0 (start) to 1 (end).
    public static Vec3 point(Vec3 a, Vec3 b, double t) {
        double drop = sag(a, b) * 4 * t * (1 - t);
        return new Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t - drop, a.z + (b.z - a.z) * t);
    }
}
