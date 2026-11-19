package com.sideflowgames.fulguritefusion.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

// The layout of a multiblock machine: a cube (2x2x2 or 3x3x3) with the controller on the front face, bottom row.
// Grid coordinates (r, u, b): r = to the right (as you face the controller's front), u = up, b = toward the back.
//   - 2x2x2: controller at the front-left bottom.  3x3x3: controller at the front-center bottom.
//   - Power port: on the top layer, at the back. A Machine Connector goes on top of it.
//   - Input port: on the top layer, at the front. A hopper above it fills the machine.
//   - Output port: on the bottom layer, at the back. A hopper below (or a pipe beside) empties the machine.
public enum MachineShape {
    SMALL(2, 0, new int[]{1, 1, 1}, new int[]{0, 1, 0}, new int[]{1, 0, 1}),
    LARGE(3, 1, new int[]{1, 2, 2}, new int[]{1, 2, 0}, new int[]{1, 0, 2});

    public enum Role { NONE, POWER, INPUT, OUTPUT }

    public final int size;
    private final int controllerR;
    private final int[] power, input, output;

    MachineShape(int size, int controllerR, int[] power, int[] input, int[] output) {
        this.size = size;
        this.controllerR = controllerR;
        this.power = power;
        this.input = input;
        this.output = output;
    }

    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public BlockPos toWorld(BlockPos controller, Direction facing, int r, int u, int b) {
        return controller.relative(right(facing), r - controllerR).above(u).relative(facing.getOpposite(), b);
    }

    public boolean isController(int r, int u, int b) {
        return r == controllerR && u == 0 && b == 0;
    }

    public Role roleAt(int r, int u, int b) {
        if (matches(power, r, u, b)) return Role.POWER;
        if (matches(input, r, u, b)) return Role.INPUT;
        if (matches(output, r, u, b)) return Role.OUTPUT;
        return Role.NONE;
    }

    private static boolean matches(int[] p, int r, int u, int b) {
        return p[0] == r && p[1] == u && p[2] == b;
    }

    // Every casing position (everything but the controller), with its role.
    public List<Part> parts(BlockPos controller, Direction facing) {
        List<Part> list = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int u = 0; u < size; u++) {
                for (int b = 0; b < size; b++) {
                    if (isController(r, u, b)) continue;
                    list.add(new Part(toWorld(controller, facing, r, u, b), roleAt(r, u, b)));
                }
            }
        }
        return list;
    }

    // How far the structure's center sits from the controller block's center: blocks to the right, and to the back.
    public double centerRight() {
        return (size - 1) / 2.0 - controllerR;
    }

    public double centerBack() {
        return (size - 1) / 2.0;
    }

    public record Part(BlockPos pos, Role role) {}
}
