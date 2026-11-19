package com.sideflowgames.fulguritefusion.power;

import com.sideflowgames.fulguritefusion.block.MachineConnectorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

// Shape of a hanging wire: a straight line between two connector caps, sagging in the middle.
public final class WireMath {
    private WireMath() {}

    // How far the connector's cap sticks out from the face it's attached to (in blocks).
    private static final double CAP = 10.5 / 16.0;

    // Where the wire attaches: the centre of the connector's cap. Connectors can face any direction.
    public static Vec3 attachPoint(BlockPos connector, Direction facing) {
        double out = CAP - 0.5; // from the block's centre toward the cap
        return Vec3.atCenterOf(connector).add(facing.getStepX() * out, facing.getStepY() * out, facing.getStepZ() * out);
    }

    // Same, reading which way the connector at that spot faces (straight up if it can't tell).
    public static Vec3 attachPoint(@Nullable BlockGetter level, BlockPos connector) {
        Direction facing = Direction.UP;
        if (level != null) {
            BlockState state = level.getBlockState(connector);
            if (state.hasProperty(MachineConnectorBlock.FACING)) facing = state.getValue(MachineConnectorBlock.FACING);
        }
        return attachPoint(connector, facing);
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
