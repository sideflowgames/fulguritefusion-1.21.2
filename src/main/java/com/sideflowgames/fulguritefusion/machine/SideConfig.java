package com.sideflowgames.fulguritefusion.machine;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

// Per-face item settings for a machine: None, Pull or Push.
// Faces are stored relative to the machine's front (Top, Bottom, Left, Right, Back),
// so the settings stay correct whichever way the machine is placed. The front is never configurable.
public final class SideConfig {
    public static final int TOP = 0, BOTTOM = 1, LEFT = 2, RIGHT = 3, BACK = 4, COUNT = 5;
    public static final int NONE = 0, PULL = 1, PUSH = 2;
    public static final String[] FACE_NAMES = {"Top", "Bottom", "Left", "Right", "Back"};
    public static final String[] MODE_NAMES = {"None", "Pull", "Push"};

    private final int[] modes;

    public SideConfig(int top, int bottom, int left, int right, int back) {
        modes = new int[] {top, bottom, left, right, back};
    }

    public int get(int face) {
        return modes[face];
    }

    public void cycle(int face) {
        modes[face] = (modes[face] + 1) % 3;
    }

    // Which configurable face a world direction is, or -1 for the front.
    // "Left" and "Right" are as seen by a player looking at the front.
    public static int faceOf(Direction facing, Direction side) {
        if (side == Direction.UP) return TOP;
        if (side == Direction.DOWN) return BOTTOM;
        if (side == facing) return -1;
        if (side == facing.getOpposite()) return BACK;
        if (side == facing.getClockWise()) return LEFT;
        return RIGHT;
    }

    public static Direction directionOf(Direction facing, int face) {
        return switch (face) {
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            default -> facing.getOpposite();
        };
    }

    public int modeFor(Direction facing, Direction side) {
        int face = faceOf(facing, side);
        return face < 0 ? NONE : modes[face];
    }

    public void save(CompoundTag tag) {
        tag.putIntArray("Sides", modes);
    }

    public void load(CompoundTag tag) {
        if (tag.contains("Sides")) {
            int[] saved = tag.getIntArray("Sides");
            if (saved.length == COUNT) System.arraycopy(saved, 0, modes, 0, COUNT);
        }
    }
}
