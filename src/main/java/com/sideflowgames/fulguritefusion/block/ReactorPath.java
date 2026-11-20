package com.sideflowgames.fulguritefusion.block;

// The geometry of a Fusion Reactor run, shared by the server (timing) and the renderer (where to draw).
// Positions are {x, z} relative to the Fusion Collider's centre, at channel height.
//
// A ring's channel is a square of half-size R. Distance along it, s, starts at corner 0 (R, R) and runs
// through the corners in FusionColliderBlockEntity.CORNERS order: (R, R) -> (-R, R) -> (-R, -R) -> (R, -R).
//
// A run, for particle i (0 or 1):
//   1. starts at the injection corner, fired from Ring Injection Port i; the two go opposite ways round;
//   2. makes 6 laps of the outer ring, then carries on to the channel spot in line with Transfer Portal i;
//   3. jumps straight across the open air to Receiver Portal i on the inner ring;
//   4. makes 6 super-fast laps of the inner ring, same way round, then on to Fusion Injector i;
//   5. is fired straight into the blast chamber and hits the Fusion Collider (the other one comes in from
//      the opposite side).
public final class ReactorPath {
    public static final int OUTER_LAPS = 6, INNER_LAPS = 6;
    // Timeline (ticks from the start of a run).
    public static final int T_JUMP = 600;            // 6 outer laps: 30 seconds
    public static final int T_INNER = T_JUMP + 12;   // the beam across to the inner ring
    public static final int T_INJECT = T_INNER + 60; // 6 inner laps in 3 seconds: super fast
    public static final int T_IMPACT = T_INJECT + 10;
    public static final int T_END = T_IMPACT + 50;   // the flash dies down

    private ReactorPath() {}

    private static int[] corner(int k) {
        return FusionColliderBlockEntity.CORNERS[Math.floorMod(k, 4)];
    }

    public static float perimeter(float r) {
        return 8 * r;
    }

    public static float[] point(float r, float s) {
        float p = perimeter(r);
        s = ((s % p) + p) % p;
        int k = Math.min(3, (int) (s / (2 * r)));
        float f = (s - k * 2 * r) / (2 * r);
        int[] a = corner(k), b = corner(k + 1);
        return new float[]{(a[0] + (b[0] - a[0]) * f) * r, (a[1] + (b[1] - a[1]) * f) * r};
    }

    // s of a point lying on the square.
    public static float param(float r, float x, float z) {
        for (int k = 0; k < 4; k++) {
            int[] a = corner(k), b = corner(k + 1);
            float ax = a[0] * r, az = a[1] * r, bx = b[0] * r, bz = b[1] * r;
            if (ax == bx && Math.abs(x - ax) < 0.01f && between(z, az, bz)) return k * 2 * r + Math.abs(z - az);
            if (az == bz && Math.abs(z - az) < 0.01f && between(x, ax, bx)) return k * 2 * r + Math.abs(x - ax);
        }
        return 0;
    }

    private static boolean between(float v, float a, float b) {
        return v >= Math.min(a, b) - 0.01f && v <= Math.max(a, b) + 0.01f;
    }

    // How far from s0 to s1 going round in 'sense' (+1 / -1).
    public static float dist(float r, float s0, float s1, int sense) {
        float p = perimeter(r);
        return (((sense * (s1 - s0)) % p) + p) % p;
    }

    // Which way round (+1 / -1) particle i goes: Ring Injection Port i fires along the other side's channel.
    public static int sense(int injCorner, int i) {
        int[] c = corner(injCorner), next = corner(injCorner + 1);
        float tx = next[0] - c[0], tz = next[1] - c[1];
        // Port 0 sits off the x side and fires along x (towards -sx); port 1 fires along z (towards -sz).
        float dx = i == 0 ? -c[0] : 0, dz = i == 0 ? 0 : -c[1];
        return dx * tx + dz * tz > 0 ? 1 : -1;
    }

    // ---------- The run, per particle ----------

    public record Run(float outerStart, float outerLength, int sense, float[] jumpFrom, float[] jumpTo,
                      float innerStart, float innerLength, float[] injectFrom) {}

    // How far round the outer laps (0..1 of the whole distance) at u = 0..1 of the 30 seconds:
    // the charges keep speeding up, finishing about 4.7 times faster than they started.
    public static float outerProgress(float u) {
        u = Math.max(0, Math.min(1, u));
        return 0.35f * u + 0.65f * u * u;
    }

    // Speed at u, as a share of the whole outer distance per tick.
    public static float outerSpeed(float u) {
        return (0.35f + 1.3f * Math.max(0, Math.min(1, u))) / T_JUMP;
    }

    public static Run plan(int injCorner, int[] receiver, boolean axisX, int i) {
        float outer = FusionColliderBlockEntity.OUTER, inner = FusionColliderBlockEntity.RING;
        int sense = sense(injCorner, i);
        float s0 = param(outer, corner(injCorner)[0] * outer, corner(injCorner)[1] * outer);

        // The outer channel spot in line with the Transfer Portal, and the inner one by the Receiver Portal.
        boolean alongZ = Math.abs(receiver[1]) > Math.abs(receiver[0]);
        float[] from = alongZ ? new float[]{receiver[0], Math.signum(receiver[1]) * outer}
                : new float[]{Math.signum(receiver[0]) * outer, receiver[1]};
        float[] to = alongZ ? new float[]{receiver[0], Math.signum(receiver[1]) * inner}
                : new float[]{Math.signum(receiver[0]) * inner, receiver[1]};
        float sExit = param(outer, from[0], from[1]);
        float outerLength = OUTER_LAPS * perimeter(outer) + dist(outer, s0, sExit, sense);

        float sIn = param(inner, to[0], to[1]);
        int sign = i == 0 ? 1 : -1;
        float[] inj = axisX ? new float[]{sign * inner, 0} : new float[]{0, sign * inner};
        float sInj = param(inner, inj[0], inj[1]);
        float innerLength = INNER_LAPS * perimeter(inner) + dist(inner, sIn, sInj, sense);
        return new Run(s0, outerLength, sense, from, to, sIn, innerLength, inj);
    }
}
