package com.sideflowgames.fulguritefusion.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

// The guide's stormy background: a dark gradient, faint drifting dots and dim lightning flickering in the distance.
public final class GuideSky {
    private record Bolt(List<float[]> segs, float born) {}

    private final List<Bolt> bolts = new ArrayList<>();
    private final RandomSource rnd = RandomSource.create();
    private float time, nextBolt = 0.8f, flash;

    // offX/offY: how far the view has moved (screen pixels), for the drifting dots.
    public void draw(GuiGraphics g, int width, int height, float offX, float offY, float dt, boolean lightning) {
        time += dt;
        g.fillGradient(0, 0, width, height, 0xFF060A14, 0xFF101B34);
        int step = 40;
        float ox = Mth.positiveModulo(-offX * 0.35f, step), oy = Mth.positiveModulo(-offY * 0.35f, step);
        for (float x = ox; x < width; x += step) {
            for (float y = oy; y < height; y += step) g.fill((int) x, (int) y, (int) x + 1, (int) y + 1, 0x40486A9A);
        }
        if (!lightning) return;
        if (time >= nextBolt) {
            bolts.add(makeBolt(width, height));
            nextBolt = time + 3f + rnd.nextFloat() * 5f;
            flash = 1;
        }
        flash = Math.max(0, flash - dt * 4);
        if (flash > 0) g.fill(0, 0, width, height, ((int) (flash * 6) << 24) | 0x8FB6D8); // a faint glow, not a white-out
        bolts.removeIf(b -> time - b.born() > 0.6f);
        for (Bolt b : bolts) {
            float age = time - b.born();
            float a = (1 - age / 0.6f) * (age < 0.08f || (age > 0.14f && age < 0.2f) ? 1 : 0.75f);
            for (float[] s : b.segs()) {
                int alpha = (int) (a * (s[4] > 1 ? 90 : 55)); // dim, distant bolts
                line(g, s[0], s[1], s[2], s[3], (int) s[4] + 2, (alpha / 4 << 24) | 0x5AB4FF);
                line(g, s[0], s[1], s[2], s[3], (int) s[4], (alpha << 24) | 0xA8CCEA);
            }
        }
    }

    private Bolt makeBolt(int width, int height) {
        List<float[]> segs = new ArrayList<>();
        float x = rnd.nextFloat() * width, y = -10;
        float end = height * (0.45f + rnd.nextFloat() * 0.4f);
        while (y < end) {
            float nx = x + (rnd.nextFloat() - 0.5f) * 26, ny = y + 8 + rnd.nextFloat() * 14;
            segs.add(new float[]{x, y, nx, ny, 2});
            if (rnd.nextFloat() < 0.14f) {
                float bx = nx, by = ny, dir = rnd.nextBoolean() ? 1 : -1;
                for (int i = 0; i < 3 + rnd.nextInt(4); i++) {
                    float cx = bx + dir * (4 + rnd.nextFloat() * 14), cy = by + 6 + rnd.nextFloat() * 10;
                    segs.add(new float[]{bx, by, cx, cy, 1});
                    bx = cx;
                    by = cy;
                }
            }
            x = nx;
            y = ny;
        }
        return new Bolt(segs, time);
    }

    static void line(GuiGraphics g, float x0, float y0, float x1, float y1, int w, int color) {
        int steps = (int) Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
        for (int i = 0; i <= steps; i++) {
            int x = (int) (x0 + (x1 - x0) * i / steps), y = (int) (y0 + (y1 - y0) * i / steps);
            g.fill(x, y, x + w, y + w, color);
        }
    }
}
