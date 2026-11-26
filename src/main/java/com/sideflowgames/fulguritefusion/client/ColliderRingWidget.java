package com.sideflowgames.fulguritefusion.client;

import java.util.ArrayList;
import java.util.List;

import com.sideflowgames.fulguritefusion.block.ColliderRing;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

// Draws the 7x7 collider ring as a little map: the Collider Input at the bottom, the collider in the middle.
// Pieces in place are lit in their colour; missing pieces are red. Hover a square to see what it is.
public final class ColliderRingWidget {
    public static final int CELL = 7;            // pixels per square (6 + 1 gap)
    public static final int SIZE = CELL * 7;     // 49
    private static final int RING = 0xFF7FD8FF, INPUT = 0xFF7CFF9E, INJECTOR = 0xFFB58CFF, CORE = 0xFFFFD45A;
    private static final int MISSING = 0xFFE04848, INSIDE = 0xFF1B2E48, FRAME = 0xFF14233A, TRAIL = 0xFFFFFFFF;

    private ColliderRingWidget() {}

    private static int sx(int u) {
        return (u + ColliderRing.RADIUS) * CELL;
    }

    // v = +3 (the input side) is drawn at the bottom.
    private static int sy(int v) {
        return (ColliderRing.RADIUS - v) * CELL;
    }

    // shot/shotLength: progress of the cells in flight (shot < 0 = none), drawn as a pulse moving round the ring.
    public static void draw(GuiGraphicsExtractor g, int x, int y, int mask, int shot, int shotLength) {
        g.fill(x - 1, y - 1, x + SIZE, y + SIZE, FRAME);
        g.fill(x, y, x + SIZE - 1, y + SIZE - 1, INSIDE);
        for (int i = 0; i < ColliderRing.SPOTS; i++) {
            int u = ColliderRing.u(i), v = ColliderRing.v(i);
            boolean ok = (mask & (1 << i)) != 0;
            int colour = !ok ? MISSING : switch (ColliderRing.kind(u, v)) {
                case ColliderRing.INPUT -> INPUT;
                case ColliderRing.INJECTOR -> INJECTOR;
                default -> RING;
            };
            int px = x + sx(-u), py = y + sy(-v);
            g.fill(px, py, px + CELL - 1, py + CELL - 1, colour);
        }
        int c = x + sx(0), cy = y + sy(0);
        g.fill(c, cy, c + CELL - 1, cy + CELL - 1, CORE);
        if (shot >= 0 && shotLength > 0) {
            // Two pulses leave the input in opposite directions and go once round the ring.
            float f = Math.min(1f, shot / (float) shotLength);
            int step = (int) (f * 24);
            for (int mirror : new int[]{1, -1}) {
                int[] p = lapSpot(step, mirror);
                int px = x + sx(-p[0]), py = y + sy(-p[1]);
                g.fill(px + 1, py + 1, px + CELL - 2, py + CELL - 2, TRAIL);
            }
        }
    }

    // Spot 'step' (0-24) along a lap starting at the input, in one direction.
    private static int[] lapSpot(int step, int mirror) {
        int r = ColliderRing.RADIUS;
        int s = Math.floorMod(step, 24);
        int u, v;
        if (s <= 3) { u = s; v = r; }
        else if (s <= 9) { u = r; v = r - (s - 3); }
        else if (s <= 15) { u = r - (s - 9); v = -r; }
        else if (s <= 21) { u = -r; v = -r + (s - 15); }
        else { u = -r + (s - 21); v = r; }
        return new int[]{u * mirror, v};
    }

    // Tooltip for the square under the mouse (mx/my relative to the widget's corner), or null.
    public static List<Component> tooltip(int mx, int my, int mask, boolean complete, String coreName) {
        if (mx < 0 || my < 0 || mx >= SIZE || my >= SIZE) return null;
        int col = mx / CELL, row = my / CELL;
        int u = -(col - ColliderRing.RADIUS), v = -(ColliderRing.RADIUS - row);
        List<Component> lines = new ArrayList<>();
        if (u == 0 && v == 0) {
            lines.add(Component.literal(coreName));
            lines.add(Component.literal(complete ? "Ring complete" : "Ring incomplete").withColor(complete ? 0x7CFF9E : 0xE04848));
            return lines;
        }
        if (Math.max(Math.abs(u), Math.abs(v)) != ColliderRing.RADIUS) {
            lines.add(Component.literal("Inside the ring (leave empty or decorate)").withColor(0xAAAAAA));
            return lines;
        }
        for (int i = 0; i < ColliderRing.SPOTS; i++) {
            if (ColliderRing.u(i) == u && ColliderRing.v(i) == v) {
                boolean ok = (mask & (1 << i)) != 0;
                lines.add(Component.literal(ColliderRing.kindName(ColliderRing.kind(u, v))));
                lines.add(Component.literal(ok ? "In place" : "Missing").withColor(ok ? 0x7CFF9E : 0xE04848));
                return lines;
            }
        }
        return null;
    }
}
