package com.sideflowgames.fulguritefusion.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.block.FusionColliderBlockEntity;
import com.sideflowgames.fulguritefusion.block.ReactorPath;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Draws the formed rings (inner and outer): smooth, rounded tubes following the channel squares, with a dark shell,
// four see-through window bands (top, bottom, inside, outside) and a glowing channel running through it.
// The geometry is built once; each frame just draws it around the Fusion Collider.
public class FusionColliderRenderer implements BlockEntityRenderer<FusionColliderBlockEntity> {
    private static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/block/storm_battery_gauge.png");
    private static final float RING = FusionColliderBlockEntity.RING;
    private static final float OUTER = FusionColliderBlockEntity.OUTER;
    private static final float TUBE_R = 2.3f;     // tube radius (the ports poke out a little past it)
    private static final float CORE_R = 0.45f;    // glowing channel
    private static final float HALO_R = 0.95f;    // soft glow round it
    private static final int SIDES = 48;          // around the tube
    private static final int SHELL = 0x2B2F3A, LINING = 0x12141A, TRIM = 0x6FD3F5, GLASS = 0x6AB6D6, CORE = 0x8FE8FF;
    private static final int HOUSING = 0x22252E, RECEIVER_GLOW = 0xC88CFF, INJECTOR_GLOW = 0xF0D8FF;
    private static final int TRANSFER_GLOW = 0x7FD8FF, PORT_GLOW = 0xFFB85A;

    // Paths round each ring's square: points + outward offsets (in the XZ plane), relative to the collider centre.
    private static final float[][] INNER_PATH = buildPath(RING, 9);
    private static final float[][] OUTER_PATH = buildPath(OUTER, 22);

    public FusionColliderRenderer(BlockEntityRendererProvider.Context context) {}

    // The channel square with clean mitred corners (like the block build). Each entry is {x, z, ox, oz}:
    // (ox, oz) is the sideways offset direction for the tube's cross-section. On a side it's the side's
    // outward normal; at a corner it's the diagonal (1, 1)-style vector, so both meeting sides stay flat.
    private static float[][] buildPath(float ring, int steps) {
        List<float[]> pts = new ArrayList<>();
        float[][] corners = {{ring, ring}, {-ring, ring}, {-ring, -ring}, {ring, -ring}};
        for (int c = 0; c < 4; c++) {
            float[] a = corners[c], b = corners[(c + 1) % 4];
            pts.add(new float[]{a[0], a[1], Math.signum(a[0]), Math.signum(a[1])});
            // Outward normal of this side: the axis where a and b agree.
            float nx = a[0] == b[0] ? Math.signum(a[0]) : 0, nz = a[1] == b[1] ? Math.signum(a[1]) : 0;
            for (int i = 1; i < steps; i++) {
                float t = (float) i / steps;
                pts.add(new float[]{Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1]), nx, nz});
            }
        }
        return pts.toArray(new float[0][]);
    }

    @Override
    public void render(FusionColliderBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (be.getLevel() == null) return;
        boolean ring = be.isFormed(), chamber = be.isChamberFormed();
        if (!ring && !chamber) {
            if (be.isRunning()) drawRun(be, buffers.getBuffer(RenderType.lightning()), poseStack.last(), partialTick, false);
            return;
        }
        int light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above(3));
        float time = be.getLevel().getGameTime() + partialTick;
        PoseStack.Pose pose = poseStack.last();
        float cx = 0.5f, cy = 0.5f, cz = 0.5f;

        float pulse = 0.6f + 0.4f * Mth.sin(time * 0.2f);
        int inj = FusionColliderBlockEntity.INJECTOR;
        List<float[]> ports = new ArrayList<>(); // {x, z, forwardX, forwardZ, colour}
        int[][] receivers = be.receiverSpots();
        for (int i = 0; i < 2; i++) {
            int[] r = receivers[i];
            boolean alongZ = Math.abs(r[1]) > Math.abs(r[0]);
            ports.add(new float[]{r[0], r[1], alongZ ? 0 : Math.signum(r[0]), alongZ ? Math.signum(r[1]) : 0, RECEIVER_GLOW});
            // Its Transfer Portal on the outer ring's inside face, pointing back at it.
            int[] t = FusionColliderBlockEntity.transferFor(r);
            ports.add(new float[]{t[0], t[1], alongZ ? 0 : -Math.signum(t[0]), alongZ ? -Math.signum(t[1]) : 0, TRANSFER_GLOW});
        }
        // Ring Injection Ports: on the outside face at one corner, collars pointing out (where the cables plug in).
        if (be.injectionCorner() >= 0) {
            int[][] spots = FusionColliderBlockEntity.injectionSpots(be.injectionCorner());
            int[] c = FusionColliderBlockEntity.CORNERS[be.injectionCorner()];
            ports.add(new float[]{spots[0][0], spots[0][1], c[0], 0, PORT_GLOW});
            ports.add(new float[]{spots[1][0], spots[1][1], 0, c[1], PORT_GLOW});
        }
        for (int i = 0; i < 2; i++) {
            int sgn = i == 0 ? 1 : -1;
            float px = be.axisX() ? sgn * inj : 0, pz = be.axisX() ? 0 : sgn * inj;
            ports.add(new float[]{px, pz, -Math.signum(px), -Math.signum(pz), INJECTOR_GLOW});
        }

        // 1) Everything solid: the shell bands and the port housings.
        //    (All of one buffer before asking for the next: asking for another closes the first.)
        if (!ring) ports.clear();
        boolean chamberX = be.chamberAxisX();
        int chamberLight = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above(3));

        VertexConsumer solid = buffers.getBuffer(RenderType.entitySolid(WHITE));
        if (ring) {
            tube(solid, pose, INNER_PATH, cx, cy, cz, TUBE_R, light, false, time);
            tube(solid, pose, OUTER_PATH, cx, cy, cz, TUBE_R, light, false, time);
        }
        for (float[] p : ports) fittingSolid(solid, pose, cx + p[0], cy, cz + p[1], p[2], p[3], light);
        if (chamber) chamber(solid, pose, chamberX, CH_SOLID, chamberLight);

        // 2) The dark inside lining, visible from both sides (behind the shell only).
        VertexConsumer lining = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        if (ring) {
            lining(lining, pose, INNER_PATH, cx, cy, cz, light);
            lining(lining, pose, OUTER_PATH, cx, cy, cz, light);
        }
        if (chamber) chamber(lining, pose, chamberX, CH_LINING, chamberLight);

        // 3) A run in progress (glowing, additive): drawn before the windows so it shows through them.
        if (be.isRunning()) drawRun(be, buffers.getBuffer(RenderType.lightning()), pose, partialTick, false);

        // 4) Everything glowing or frosted: the channel, the window strips, the port discs, the portholes.
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        if (ring) {
            for (float[][] path : new float[][][]{INNER_PATH, OUTER_PATH}) {
                core(glow, pose, path, cx, cy, cz, time, CORE_R, 1f);
                core(glow, pose, path, cx, cy, cz, time, HALO_R, 0.25f);
                tube(glow, pose, path, cx, cy, cz, TUBE_R, light, true, time);
            }
        }
        for (float[] p : ports) fittingGlow(glow, pose, cx + p[0], cy, cz + p[1], p[2], p[3], (int) p[4], pulse);
        if (chamber) chamber(glow, pose, chamberX, CH_WINDOWS, chamberLight);

        // 5) The charges lighting up the ring tube from inside as they pass (on top of the windows).
        if (ring && be.isRunning()) drawRun(be, buffers.getBuffer(RenderType.lightning()), pose, partialTick, true);
    }

    // ---------- The blast chamber: an armoured sphere ----------
    // Dark armour plates with seams, four glowing porthole windows (top, bottom and the two sides the beams
    // don't come from) ringed in cyan, and a round port collar on each side where the injectors' beams enter.

    private static final int CH_SOLID = 0, CH_LINING = 1, CH_WINDOWS = 2;
    private static final float SPHERE_R = 2.42f;
    private static final int SPH_LAT = 24, SPH_LON = 48;
    private static final float HOLE_COS = Mth.cos(19 * Mth.DEG_TO_RAD), WINDOW_COS = Mth.cos(24 * Mth.DEG_TO_RAD),
            TRIM_COS = Mth.cos(29 * Mth.DEG_TO_RAD);
    private static final int SEAM = 0x3A404E;

    private static void chamber(VertexConsumer vc, PoseStack.Pose pose, boolean axisX, int pass, int light) {
        float[] c = {0.5f, 0.5f, 0.5f};
        float[] axis = axisX ? new float[]{1, 0, 0} : new float[]{0, 0, 1};
        float[][] windows = axisX
                ? new float[][]{{0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}}
                : new float[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
        float r = pass == CH_LINING ? SPHERE_R - 0.08f : SPHERE_R;
        for (int i = 0; i < SPH_LAT; i++) {
            float t0 = (float) (Math.PI * i / SPH_LAT - Math.PI / 2), t1 = (float) (Math.PI * (i + 1) / SPH_LAT - Math.PI / 2);
            for (int k = 0; k < SPH_LON; k++) {
                float p0 = (float) (Math.PI * 2 * k / SPH_LON), p1 = (float) (Math.PI * 2 * (k + 1) / SPH_LON);
                float[][] n = {sph(t0, p0), sph(t0, p1), sph(t1, p1), sph(t1, p0)};
                float[] mid = norm(new float[]{n[0][0] + n[2][0], n[0][1] + n[2][1], n[0][2] + n[2][2]});
                float along = Math.abs(dot(mid, axis));
                if (along > HOLE_COS) continue; // the beam ports
                float best = 0;
                for (float[] w : windows) best = Math.max(best, dot(mid, w));
                boolean window = best > WINDOW_COS;
                int colour, alpha = 255, l = light;
                if (pass == CH_WINDOWS) {
                    if (!window) continue;
                    colour = GLASS;
                    alpha = 110;
                    l = LightTexture.FULL_BRIGHT;
                } else if (pass == CH_LINING) {
                    if (window) continue;
                    colour = LINING;
                } else {
                    if (window) continue;
                    boolean trim = best > TRIM_COS || along > Mth.cos(23 * Mth.DEG_TO_RAD);
                    colour = trim ? TRIM : (i % 4 == 0 || k % 8 == 0) ? SEAM : SHELL;
                }
                int red = (colour >> 16) & 255, g = (colour >> 8) & 255, b = colour & 255;
                float[][] pts = new float[4][];
                for (int q = 0; q < 4; q++) pts[q] = new float[]{c[0] + n[q][0] * r, c[1] + n[q][1] * r, c[2] + n[q][2] * r};
                // Outward-facing order for the solid shell (back faces are culled there).
                float[] e1 = {pts[1][0] - pts[0][0], pts[1][1] - pts[0][1], pts[1][2] - pts[0][2]};
                float[] e2 = {pts[2][0] - pts[0][0], pts[2][1] - pts[0][1], pts[2][2] - pts[0][2]};
                float[] fn = cross(e1, e2);
                if (dot(fn, mid) == 0) {
                    e1 = new float[]{pts[2][0] - pts[0][0], pts[2][1] - pts[0][1], pts[2][2] - pts[0][2]};
                    e2 = new float[]{pts[3][0] - pts[0][0], pts[3][1] - pts[0][1], pts[3][2] - pts[0][2]};
                    fn = cross(e1, e2);
                }
                int[] order = dot(fn, mid) >= 0 ? new int[]{0, 1, 2, 3} : new int[]{3, 2, 1, 0};
                for (int q : order) raw(vc, pose, pts[q], n[q], red, g, b, alpha, l);
            }
        }
        if (pass == CH_SOLID) {
            // Port collars where the inner ring's beams come in.
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                float fx = axis[0] * sgn, fz = axis[2] * sgn;
                float[] f = {fx, 0, fz}, sd = {fz, 0, -fx}, u = {0, 1, 0};
                cylinder(vc, pose, c, f, sd, u, 1.9f, 2.8f, 0.86f, HOUSING, light, true);
                cylinder(vc, pose, c, f, sd, u, 2.5f, 2.65f, 0.92f, TRIM, light, true);
            }
        }
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    // ---------- Runs ----------

    private static final int TRACE_0 = 0x8FE8FF, TRACE_1 = 0xE0A8FF, BEAM = 0xBFF4FF;
    private static final int[] FLASH = {0xE8FAFF, 0xFFE27A, 0xB6E36A, 0xFF6A3A};

    // surface = false: everything inside (traces, beams, flash); true: the glow the traces cast on the tube's shell.
    private static void drawRun(FusionColliderBlockEntity be, VertexConsumer vc, PoseStack.Pose pose, float partialTick,
                                boolean surface) {
        if (be.injectionCorner() < 0) return;
        float t = be.getLevel().getGameTime() - be.renderStart() + partialTick;
        int mask = be.runMask();
        int[][] receivers = be.receiverSpots();
        float y = 0.5f;
        for (int i = 0; i < 2; i++) {
            if ((mask & (1 << i)) == 0) continue;
            boolean lost = (mask & (4 << i)) != 0;
            int colour = i == 0 ? TRACE_0 : TRACE_1;
            ReactorPath.Run run = ReactorPath.plan(be.injectionCorner(), receivers[i], be.axisX(), i);
            if (t < ReactorPath.T_JUMP) {
                // Outer laps: a bright flowing trace, speeding up lap after lap and stretching out as it goes;
                // it reaches the Transfer Portal's spot exactly as the beam fires.
                float u = t / ReactorPath.T_JUMP;
                float s = run.outerStart() + run.sense() * run.outerLength() * ReactorPath.outerProgress(u);
                float length = Mth.clamp(run.outerLength() * ReactorPath.outerSpeed(u) * 5f, 8f, 40f);
                trail(vc, pose, FusionColliderBlockEntity.OUTER, s, run.sense(), length, y, colour, surface);
            } else if (surface) {
                if (!lost && t >= ReactorPath.T_INNER && t < ReactorPath.T_INJECT) {
                    float f = (t - ReactorPath.T_INNER) / (ReactorPath.T_INJECT - ReactorPath.T_INNER);
                    float s = run.innerStart() + run.sense() * run.innerLength() * f;
                    trail(vc, pose, FusionColliderBlockEntity.RING, s, run.sense(), 26, y, colour, true);
                }
            } else if (t < ReactorPath.T_INNER) {
                // The beam across from the Transfer Portal to the Receiver Portal.
                float bt = t - ReactorPath.T_JUMP;
                float grow = Math.min(1f, bt / 3f) * (lost ? 0.35f : 1f);
                float alpha = 1f - bt / (ReactorPath.T_INNER - ReactorPath.T_JUMP);
                float[] a = {run.jumpFrom()[0] + 0.5f, y, run.jumpFrom()[1] + 0.5f};
                float[] b = {run.jumpTo()[0] + 0.5f, y, run.jumpTo()[1] + 0.5f};
                float[] end = {a[0] + (b[0] - a[0]) * grow, y, a[2] + (b[2] - a[2]) * grow};
                seg(vc, pose, a, end, 0.18f, 0.18f, 0xFFFFFF, (int) (255 * alpha), (int) (255 * alpha));
                seg(vc, pose, a, end, 0.5f, 0.5f, BEAM, (int) (120 * alpha), (int) (120 * alpha));
            } else if (!lost && t < ReactorPath.T_INJECT) {
                // Inner laps: 6 super-fast laps round the small ring.
                float f = (t - ReactorPath.T_INNER) / (ReactorPath.T_INJECT - ReactorPath.T_INNER);
                float s = run.innerStart() + run.sense() * run.innerLength() * f;
                trail(vc, pose, FusionColliderBlockEntity.RING, s, run.sense(), 26, y, colour, false);
            } else if (!lost && t < ReactorPath.T_IMPACT) {
                // Fired from the Fusion Injector into the chamber.
                float f = (t - ReactorPath.T_INJECT) / (ReactorPath.T_IMPACT - ReactorPath.T_INJECT);
                float[] a = {run.injectFrom()[0] + 0.5f, y, run.injectFrom()[1] + 0.5f};
                float[] head = {0.5f + run.injectFrom()[0] * (1 - f), y, 0.5f + run.injectFrom()[1] * (1 - f)};
                seg(vc, pose, a, head, 0.08f, 0.22f, colour, 0, 255);
                seg(vc, pose, a, head, 0.2f, 0.55f, colour, 0, 110);
            }
        }
        if (!surface && t >= ReactorPath.T_IMPACT && be.runSeverity() >= 0) flash(be, vc, pose, t - ReactorPath.T_IMPACT);
    }

    // A glowing trace along a ring's channel: a white-hot head and a long coloured tail fading behind it.
    // On the surface pass it's the light the trace throws on the tube's shell as it passes.
    private static void trail(VertexConsumer vc, PoseStack.Pose pose, float ring, float head, int sense, float length,
                              float y, int colour, boolean surface) {
        int steps = Math.max(6, (int) (length / 0.6f));
        float[] prev = null;
        for (int k = 0; k <= steps; k++) {
            float d = length * k / steps;
            float[] p2 = ReactorPath.point(ring, head - sense * d);
            float[] p = {p2[0] + 0.5f, y, p2[1] + 0.5f};
            if (prev != null) {
                float f0 = 1f - (float) (k - 1) / steps, f1 = 1f - (float) k / steps;
                f0 *= f0;
                f1 *= f1;
                if (surface) {
                    seg(vc, pose, prev, p, TUBE_R + 0.05f, TUBE_R + 0.05f, colour, (int) (140 * f0), (int) (140 * f1));
                } else {
                    seg(vc, pose, prev, p, 0.12f + 0.3f * f0, 0.12f + 0.3f * f1, 0xFFFFFF, (int) (255 * f0), (int) (255 * f1));
                    seg(vc, pose, prev, p, 0.35f + 0.7f * f0, 0.35f + 0.7f * f1, colour, (int) (200 * f0), (int) (200 * f1));
                    seg(vc, pose, prev, p, 0.9f + 0.6f * f0, 0.9f + 0.6f * f1, colour, (int) (70 * f0), (int) (70 * f1));
                }
            }
            prev = p;
        }
        if (!surface) {
            float[] h2 = ReactorPath.point(ring, head);
            sphere(vc, pose, new float[]{h2[0] + 0.5f, y, h2[1] + 0.5f}, 0.55f, 0xFFFFFF, 255);
        }
    }

    // The collision: a burst of light rays shooting out from the collider (like the Ender Dragon's death,
    // much smaller), a white-hot core, and a spray of sparks thrown outward that stop at the chamber walls.
    private static void flash(FusionColliderBlockEntity be, VertexConsumer vc, PoseStack.Pose pose, float ft) {
        float life = ReactorPath.T_END - ReactorPath.T_IMPACT;
        if (ft > life) return;
        int colour = FLASH[Math.min(3, be.runSeverity())];
        float f = ft / life;
        float fade = 1f - f;
        float burst = Math.min(1f, 0.35f + ft / 5f); // rays shoot out in the first quarter second
        float[] c = {0.5f, 0.5f, 0.5f};

        // Light rays: thin triangles fanning out in every direction, white at the root, fading to nothing.
        java.util.Random rays = new java.util.Random(be.runStart() * 31 + 432);
        int r = (colour >> 16) & 255, g = (colour >> 8) & 255, b = colour & 255;
        int rootAlpha = (int) (255 * fade);
        Vector3f v = new Vector3f();
        for (int k = 0; k < 48; k++) {
            Quaternionf q = new Quaternionf()
                    .rotateX(rand(rays)).rotateY(rand(rays)).rotateZ(rand(rays))
                    .rotateX(rand(rays)).rotateY(rand(rays)).rotateZ(rand(rays) + f * 1.6f);
            float len = (0.9f + rays.nextFloat() * 1.2f) * burst; // stays inside the chamber
            float wid = (0.12f + rays.nextFloat() * 0.25f) * burst;
            float[][] tip = {
                    {-0.866f * wid, len, -0.5f * wid},
                    {0.866f * wid, len, -0.5f * wid},
                    {0, len, wid}};
            float[][] pts = new float[3][];
            for (int n = 0; n < 3; n++) {
                q.transform(v.set(tip[n][0], tip[n][1], tip[n][2]));
                pts[n] = new float[]{c[0] + v.x, c[1] + v.y, c[2] + v.z};
            }
            for (int n = 0; n < 3; n++) {
                float[] p1 = pts[n], p2 = pts[(n + 1) % 3];
                ray(vc, pose, c, 255, 255, 255, rootAlpha);
                ray(vc, pose, p1, r, g, b, 0);
                ray(vc, pose, p2, r, g, b, 0);
                ray(vc, pose, p2, r, g, b, 0);
            }
        }

        // White-hot core.
        float core = ft < 3 ? 0.25f + ft * 0.25f : Math.max(0.05f, 1.0f * (1f - (ft - 3) / (life - 3)));
        sphere(vc, pose, c, core, 0xFFFFFF, (int) (255 * fade));
        sphere(vc, pose, c, core * 1.6f, colour, (int) (120 * fade));

        // Sparks, held in by the chamber.
        java.util.Random rand = new java.util.Random(be.runStart());
        float wall = 1.45f; // the inside of the blast chamber
        for (int k = 0; k < 90; k++) {
            float dx = (float) rand.nextGaussian(), dy = (float) rand.nextGaussian(), dz = (float) rand.nextGaussian();
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= len; dy /= len; dz /= len;
            float speed = 0.15f + rand.nextFloat() * 0.35f;
            float far = wall / Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))); // distance to the wall
            float d1 = Math.min(far, ft * speed), d0 = Math.min(far, Math.max(0, ft * speed - 0.8f));
            float[] a = {c[0] + dx * d0, c[1] + dy * d0, c[2] + dz * d0}, e = {c[0] + dx * d1, c[1] + dy * d1, c[2] + dz * d1};
            if (d1 - d0 < 0.05f) { // stuck at the wall: a fading spark
                a = new float[]{e[0] - dx * 0.08f, e[1] - dy * 0.08f, e[2] - dz * 0.08f};
            }
            seg(vc, pose, a, e, 0.015f, 0.035f, colour, (int) (200 * fade), (int) (255 * fade));
        }
    }

    private static float rand(java.util.Random r) {
        return r.nextFloat() * Mth.TWO_PI;
    }

    private static void ray(VertexConsumer vc, PoseStack.Pose pose, float[] p, int r, int g, int b, int a) {
        vc.addVertex(pose, p[0], p[1], p[2]).setColor(r, g, b, a);
    }

    // A glowing tapered tube from a to b (no culling in the translucent type, so winding doesn't matter).
    private static void seg(VertexConsumer vc, PoseStack.Pose pose, float[] a, float[] b, float ra, float rb,
                            int colour, int alphaA, int alphaB) {
        float fx = b[0] - a[0], fy = b[1] - a[1], fz = b[2] - a[2];
        float len = Mth.sqrt(fx * fx + fy * fy + fz * fz);
        if (len < 1e-4f) return;
        float[] f = {fx / len, fy / len, fz / len};
        float[] up = Math.abs(f[1]) < 0.9f ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
        float[] s = norm(cross(f, up)), u = cross(f, s);
        int r = (colour >> 16) & 255, g = (colour >> 8) & 255, bl = colour & 255;
        int sides = 8;
        for (int k = 0; k < sides; k++) {
            float[] d0 = radial(s, u, (float) (Math.PI * 2 * k / sides)), d1 = radial(s, u, (float) (Math.PI * 2 * (k + 1) / sides));
            raw(vc, pose, along(a, f, 0, d0, ra), d0, r, g, bl, alphaA, LightTexture.FULL_BRIGHT);
            raw(vc, pose, along(a, f, 0, d1, ra), d1, r, g, bl, alphaA, LightTexture.FULL_BRIGHT);
            raw(vc, pose, along(b, f, 0, d1, rb), d1, r, g, bl, alphaB, LightTexture.FULL_BRIGHT);
            raw(vc, pose, along(b, f, 0, d0, rb), d0, r, g, bl, alphaB, LightTexture.FULL_BRIGHT);
        }
    }

    private static void sphere(VertexConsumer vc, PoseStack.Pose pose, float[] c, float radius, int colour, int alpha) {
        int r = (colour >> 16) & 255, g = (colour >> 8) & 255, b = colour & 255;
        int lat = 8, lon = 16;
        for (int i = 0; i < lat; i++) {
            float t0 = (float) (Math.PI * i / lat - Math.PI / 2), t1 = (float) (Math.PI * (i + 1) / lat - Math.PI / 2);
            for (int k = 0; k < lon; k++) {
                float p0 = (float) (Math.PI * 2 * k / lon), p1 = (float) (Math.PI * 2 * (k + 1) / lon);
                float[][] n = {sph(t0, p0), sph(t0, p1), sph(t1, p1), sph(t1, p0)};
                for (float[] d : n) {
                    raw(vc, pose, new float[]{c[0] + d[0] * radius, c[1] + d[1] * radius, c[2] + d[2] * radius}, d,
                            r, g, b, alpha, LightTexture.FULL_BRIGHT);
                }
            }
        }
    }

    private static float[] sph(float lat, float lon) {
        float cl = Mth.cos(lat);
        return new float[]{cl * Mth.cos(lon), Mth.sin(lat), cl * Mth.sin(lon)};
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static float[] norm(float[] v) {
        float len = Mth.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new float[]{v[0] / len, v[1] / len, v[2] / len};
    }

    // A port: a round collar sticking out of the tube along F (sideways S = up x F, up U), with a cyan trim
    // ring near its end...
    private static void fittingSolid(VertexConsumer solid, PoseStack.Pose pose, float x, float y, float z,
                                     float fx, float fz, int light) {
        float[] f = {fx, 0, fz}, u = {0, 1, 0}, sd = {fz, 0, -fx};
        float[] base = {x - fx * 1.0f, y, z - fz * 1.0f};
        cylinder(solid, pose, base, f, sd, u, 0f, 1.6f, 0.78f, HOUSING, light, true);
        cylinder(solid, pose, base, f, sd, u, 1.25f, 1.4f, 0.84f, TRIM, light, true);
        // The end face: a dark ring round the glowing disc.
        disc(solid, pose, new float[]{base[0] + fx * 1.6f, y, base[2] + fz * 1.6f}, f, sd, u, 0.78f, HOUSING, 255, light);
    }

    // ...and a glowing disc set into its end.
    private static void fittingGlow(VertexConsumer glow, PoseStack.Pose pose, float x, float y, float z,
                                    float fx, float fz, int glowColour, float pulse) {
        float[] f = {fx, 0, fz}, u = {0, 1, 0}, sd = {fz, 0, -fx};
        float[] c = {x + fx * 0.62f, y, z + fz * 0.62f};
        disc(glow, pose, c, f, sd, u, 0.6f, glowColour, (int) (170 + 85 * pulse), LightTexture.FULL_BRIGHT);
    }

    // Side of a cylinder along f from 'from' to 'to' (distances from base), radius r, facing outward.
    private static void cylinder(VertexConsumer vc, PoseStack.Pose pose, float[] base, float[] f, float[] s, float[] u,
                                 float from, float to, float r, int colour, int light, boolean outward) {
        int segs = 20;
        int red = (colour >> 16) & 255, g = (colour >> 8) & 255, b = colour & 255;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (Math.PI * 2 * i / segs), a1 = (float) (Math.PI * 2 * (i + 1) / segs);
            float[] d0 = radial(s, u, a0), d1 = radial(s, u, a1);
            float[] p00 = along(base, f, from, d0, r), p10 = along(base, f, to, d0, r);
            float[] p01 = along(base, f, from, d1, r), p11 = along(base, f, to, d1, r);
            // d0 -> d1 runs from S toward U; (S x U) = F, so going along f then round winds outward.
            raw(vc, pose, p00, d0, red, g, b, 255, light);
            raw(vc, pose, p01, d1, red, g, b, 255, light);
            raw(vc, pose, p11, d1, red, g, b, 255, light);
            raw(vc, pose, p10, d0, red, g, b, 255, light);
        }
    }

    // A flat disc facing along f.
    private static void disc(VertexConsumer vc, PoseStack.Pose pose, float[] c, float[] f, float[] s, float[] u,
                             float r, int colour, int alpha, int light) {
        int segs = 20;
        int red = (colour >> 16) & 255, g = (colour >> 8) & 255, b = colour & 255;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (Math.PI * 2 * i / segs), a1 = (float) (Math.PI * 2 * (i + 1) / segs);
            float[] p1 = along(c, f, 0, radial(s, u, a0), r), p2 = along(c, f, 0, radial(s, u, a1), r);
            raw(vc, pose, c, f, red, g, b, alpha, light);
            raw(vc, pose, p1, f, red, g, b, alpha, light);
            raw(vc, pose, p2, f, red, g, b, alpha, light);
            raw(vc, pose, p2, f, red, g, b, alpha, light);
        }
    }

    private static float[] radial(float[] s, float[] u, float a) {
        float c = Mth.cos(a), sn = Mth.sin(a);
        return new float[]{s[0] * c + u[0] * sn, s[1] * c + u[1] * sn, s[2] * c + u[2] * sn};
    }

    private static float[] along(float[] base, float[] f, float t, float[] d, float r) {
        return new float[]{base[0] + f[0] * t + d[0] * r, base[1] + f[1] * t + d[1] * r, base[2] + f[2] * t + d[2] * r};
    }

    // A box centred on c with half-sizes along f (a), s (b) and u (h); faces wound outward.
    private static void box(VertexConsumer vc, PoseStack.Pose pose, float[] c, float[] f, float[] s, float[] u,
                            float a, float b, float h, int colour, int light) {
        face(vc, pose, c, f, a, s, b, u, h, colour, light);
        face(vc, pose, c, neg(f), a, u, h, s, b, colour, light);
        face(vc, pose, c, s, b, u, h, f, a, colour, light);
        face(vc, pose, c, neg(s), b, f, a, u, h, colour, light);
        face(vc, pose, c, u, h, f, a, s, b, colour, light);
        face(vc, pose, c, neg(u), h, s, b, f, a, colour, light);
    }

    private static float[] neg(float[] v) {
        return new float[]{-v[0], -v[1], -v[2]};
    }

    // Face with normal n at distance d from c, spanned by e1 (half e1h) and e2 (half e2h); e1 x e2 = n.
    private static void face(VertexConsumer vc, PoseStack.Pose pose, float[] c, float[] n, float d,
                             float[] e1, float e1h, float[] e2, float e2h, int colour, int light) {
        int r = (colour >> 16) & 255, g = (colour >> 8) & 255, b = colour & 255;
        float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (float[] k : corners) {
            float[] p = new float[3];
            for (int i = 0; i < 3; i++) p[i] = c[i] + n[i] * d + e1[i] * e1h * k[0] + e2[i] * e2h * k[1];
            raw(vc, pose, p, n, r, g, b, 255, light);
        }
    }

    // A vertex at an absolute position (already offset to the block centre).
    private static void raw(VertexConsumer vc, PoseStack.Pose pose, float[] p, float[] n, int r, int g, int b, int a, int light) {
        vc.addVertex(pose, p[0], p[1], p[2])
                .setColor(r, g, b, a)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, n[0], n[1], n[2]);
    }

    // Side bands round the tube (48, 7.5 degrees each). Each tip (outside, top, inside, bottom) has a
    // 3-band glass window, with a thin cyan trim band either side; the rest is the dark shell.
    private static boolean window(int k) {
        int m = Math.floorMod(k, 12);
        return m == 0 || m == 1 || m == 11;
    }

    private static boolean trim(int k) {
        int m = Math.floorMod(k, 12);
        return m == 2 || m == 10;
    }

    private static void tube(VertexConsumer vc, PoseStack.Pose pose, float[][] path, float cx, float cy, float cz, float r,
                             int light, boolean windows, float time) {
        int n = path.length;
        for (int i = 0; i < n; i++) {
            float[] a = path[i], b = path[(i + 1) % n];
            for (int k = 0; k < SIDES; k++) {
                boolean isWindow = window(k);
                if (isWindow != windows) continue;
                int colour = isWindow ? GLASS : trim(k) ? TRIM : SHELL;
                // Windows are frosted glass lit from inside: mostly opaque, softly glowing.
                int alpha = isWindow ? 140 : 255;
                float t0 = (float) (Math.PI * 2 * k / SIDES), t1 = (float) (Math.PI * 2 * (k + 1) / SIDES);
                quad(vc, pose, cx, cy, cz, a, b, r, t0, t1, colour, alpha, isWindow ? LightTexture.FULL_BRIGHT : light);
            }
        }
    }

    // The tube's dark inside wall, facing inward: through a window you see the far wall, not the outside world.
    private static void lining(VertexConsumer vc, PoseStack.Pose pose, float[][] path, float cx, float cy, float cz, int light) {
        int n = path.length;
        int r = (LINING >> 16) & 255, g = (LINING >> 8) & 255, b = LINING & 255;
        float rr = TUBE_R - 0.08f;
        for (int i = 0; i < n; i++) {
            float[] a = path[i], c = path[(i + 1) % n];
            for (int k = 0; k < SIDES; k++) {
                if (window(k)) continue; // the windows glow; nothing behind them
                float t0 = (float) (Math.PI * 2 * k / SIDES), t1 = (float) (Math.PI * 2 * (k + 1) / SIDES);
                float[] p00 = point(a, rr, t0), p01 = point(a, rr, t1), p10 = point(c, rr, t0), p11 = point(c, rr, t1);
                float[] n0 = neg(normal(a, t0));
                // Reverse winding so it faces into the tube.
                raw(vc, pose, add(p00, cx, cy, cz), n0, r, g, b, 255, light);
                raw(vc, pose, add(p10, cx, cy, cz), n0, r, g, b, 255, light);
                raw(vc, pose, add(p11, cx, cy, cz), n0, r, g, b, 255, light);
                raw(vc, pose, add(p01, cx, cy, cz), n0, r, g, b, 255, light);
            }
        }
    }

    private static float[] add(float[] p, float x, float y, float z) {
        return new float[]{p[0] + x, p[1] + y, p[2] + z};
    }

    private static void core(VertexConsumer vc, PoseStack.Pose pose, float[][] path, float cx, float cy, float cz, float time,
                             float radius, float strength) {
        int n = path.length;
        for (int i = 0; i < n; i++) {
            float[] a = path[i], b = path[(i + 1) % n];
            // A slow pulse runs round the channel.
            float pulse = 0.55f + 0.45f * Mth.sin(time * 0.15f - i * 0.35f);
            int alpha = (int) ((110 + 120 * pulse) * strength);
            for (int k = 0; k < 12; k++) {
                float t0 = (float) (Math.PI * 2 * k / 12), t1 = (float) (Math.PI * 2 * (k + 1) / 12);
                quad(vc, pose, cx, cy, cz, a, b, radius, t0, t1, CORE, alpha, LightTexture.FULL_BRIGHT);
            }
        }
    }

    // One quad of a tube between path points a and b, from angle t0 to t1 round the tube, facing outward.
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float cx, float cy, float cz, float[] a, float[] b,
                             float r, float t0, float t1, int colour, int alpha, int light) {
        float[] p00 = point(a, r, t0), p01 = point(a, r, t1), p10 = point(b, r, t0), p11 = point(b, r, t1);
        float[] n0 = normal(a, t0), n1 = normal(a, t1), m0 = normal(b, t0), m1 = normal(b, t1);
        int red = (colour >> 16) & 255, green = (colour >> 8) & 255, blue = colour & 255;
        // Going along the path then round the tube winds outward.
        vertex(vc, pose, cx, cy, cz, p00, n0, red, green, blue, alpha, light);
        vertex(vc, pose, cx, cy, cz, p01, n1, red, green, blue, alpha, light);
        vertex(vc, pose, cx, cy, cz, p11, m1, red, green, blue, alpha, light);
        vertex(vc, pose, cx, cy, cz, p10, m0, red, green, blue, alpha, light);
    }

    // Path point + r * (cos t * outward + sin t * up).
    private static float[] point(float[] p, float r, float t) {
        float c = Mth.cos(t), s = Mth.sin(t);
        return new float[]{p[0] + p[2] * c * r, s * r, p[1] + p[3] * c * r};
    }

    // Lighting normal (the mitred corner offset isn't unit length, so normalise it).
    private static float[] normal(float[] p, float t) {
        float c = Mth.cos(t), s = Mth.sin(t);
        float len = Mth.sqrt(p[2] * p[2] + p[3] * p[3]);
        return new float[]{p[2] / len * c, s, p[3] / len * c};
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float cx, float cy, float cz, float[] p, float[] n,
                               int r, int g, int b, int a, int light) {
        vc.addVertex(pose, cx + p[0], cy + p[1], cz + p[2])
                .setColor(r, g, b, a)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, n[0], n[1], n[2]);
    }

    @Override
    public boolean shouldRenderOffScreen(FusionColliderBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 192;
    }

    @Override
    public AABB getRenderBoundingBox(FusionColliderBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(OUTER + 5, 4, OUTER + 5);
    }
}
