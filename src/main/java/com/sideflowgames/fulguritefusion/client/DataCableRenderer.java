package com.sideflowgames.fulguritefusion.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.block.DataCableBlock;
import com.sideflowgames.fulguritefusion.block.DataCableBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;

// Draws a Data Cable as a round metal pipe:
//   - two opposite connections: a straight pipe through the block;
//   - two connections at a right angle: a smooth curved elbow (a quarter of a ring, radius half a block);
//   - one, three or more: arms from the middle to each side, with a rounded hub;
//   - none: a short upright stub.
// Where the pipe meets a port (not another cable) it gets a wider flange.
public class DataCableRenderer implements BlockEntityRenderer<DataCableBlockEntity> {
    private static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/block/storm_battery_gauge.png");
    private static final float R = 0.17f;
    private static final int SIDES = 16, ELBOW_STEPS = 10;
    private static final int PIPE = 0x9AA3AE, FLANGE = 0x5C636D, HUB = 0x828B96;

    public DataCableRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(DataCableBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof DataCableBlock) || be.getLevel() == null) return;
        List<Direction> con = new ArrayList<>();
        for (Direction d : Direction.values()) if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d))) con.add(d);

        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(WHITE));
        PoseStack.Pose pose = poseStack.last();
        float[] c = {0.5f, 0.5f, 0.5f};

        if (con.isEmpty()) {
            float[] a = {0.5f, 0.2f, 0.5f}, b = {0.5f, 0.8f, 0.5f};
            pipe(vc, pose, a, b, R, PIPE, light);
            disc(vc, pose, b, new float[]{0, 1, 0}, R, PIPE, light);
            disc(vc, pose, a, new float[]{0, -1, 0}, R, PIPE, light);
        } else if (con.size() == 2 && con.get(0) == con.get(1).getOpposite()) {
            pipe(vc, pose, faceCentre(con.get(0)), faceCentre(con.get(1)), R, PIPE, light);
        } else if (con.size() == 2) {
            elbow(vc, pose, vec(con.get(0)), vec(con.get(1)), light);
        } else {
            for (Direction d : con) pipe(vc, pose, c, faceCentre(d), R, PIPE, light);
            sphere(vc, pose, c, con.size() == 1 ? R : R * 1.35f, con.size() == 1 ? PIPE : HUB, light);
        }

        // Flanges where the pipe plugs into a port.
        for (Direction d : con) {
            BlockState n = be.getLevel().getBlockState(be.getBlockPos().relative(d));
            if (n.getBlock() instanceof DataCableBlock) continue;
            float[] v = vec(d);
            float[] from = {c[0] + v[0] * 0.38f, c[1] + v[1] * 0.38f, c[2] + v[2] * 0.38f};
            float[] to = faceCentre(d);
            pipe(vc, pose, from, to, R + 0.06f, FLANGE, light);
            disc(vc, pose, from, new float[]{-v[0], -v[1], -v[2]}, R + 0.06f, FLANGE, light);
        }
    }

    private static float[] vec(Direction d) {
        return new float[]{d.getStepX(), d.getStepY(), d.getStepZ()};
    }

    private static float[] faceCentre(Direction d) {
        return new float[]{0.5f + d.getStepX() * 0.5f, 0.5f + d.getStepY() * 0.5f, 0.5f + d.getStepZ() * 0.5f};
    }

    // ---------- Shapes ----------

    // Straight pipe from a to b.
    private static void pipe(VertexConsumer vc, PoseStack.Pose pose, float[] a, float[] b, float r, int colour, int light) {
        float[] f = norm(sub(b, a));
        float[] s = perp(f), u = cross(f, s);
        for (int k = 0; k < SIDES; k++) {
            float[] d0 = ring(s, u, k), d1 = ring(s, u, k + 1);
            quad(vc, pose, add(a, d0, r), add(a, d1, r), add(b, d1, r), add(b, d0, r), d0, d1, d1, d0, colour, light);
        }
    }

    // Quarter-ring elbow joining the face centres of sides a and b (at right angles), bending round the corner
    // point C = centre + a/2 + b/2 with radius 1/2: P(t) = C - (b cos t + a sin t) / 2, t from 0 (side a) to 90 deg (side b).
    private static void elbow(VertexConsumer vc, PoseStack.Pose pose, float[] a, float[] b, int light) {
        float[] centre = {0.5f + 0.5f * (a[0] + b[0]), 0.5f + 0.5f * (a[1] + b[1]), 0.5f + 0.5f * (a[2] + b[2])};
        float[] n1 = cross(a, b); // the bend's axis, the same all the way round
        float[][] prevRing = null, prevNorm = null;
        for (int i = 0; i <= ELBOW_STEPS; i++) {
            float t = (float) (Math.PI / 2 * i / ELBOW_STEPS);
            float ct = Mth.cos(t), st = Mth.sin(t);
            float[] radial = {-(b[0] * ct + a[0] * st), -(b[1] * ct + a[1] * st), -(b[2] * ct + a[2] * st)};
            float[] p = add(centre, radial, 0.5f);
            float[][] pts = new float[SIDES][], nrm = new float[SIDES][];
            for (int k = 0; k < SIDES; k++) {
                nrm[k] = ring(n1, radial, k);
                pts[k] = add(p, nrm[k], R);
            }
            if (prevRing != null) {
                for (int k = 0; k < SIDES; k++) {
                    int k1 = (k + 1) % SIDES;
                    quad(vc, pose, prevRing[k], prevRing[k1], pts[k1], pts[k], prevNorm[k], prevNorm[k1], nrm[k1], nrm[k], PIPE, light);
                }
            }
            prevRing = pts;
            prevNorm = nrm;
        }
    }

    private static void sphere(VertexConsumer vc, PoseStack.Pose pose, float[] c, float r, int colour, int light) {
        int lat = 8;
        for (int i = 0; i < lat; i++) {
            float t0 = (float) (Math.PI * i / lat - Math.PI / 2), t1 = (float) (Math.PI * (i + 1) / lat - Math.PI / 2);
            for (int k = 0; k < SIDES; k++) {
                float p0 = (float) (Math.PI * 2 * k / SIDES), p1 = (float) (Math.PI * 2 * (k + 1) / SIDES);
                float[] n00 = sph(t0, p0), n01 = sph(t0, p1), n11 = sph(t1, p1), n10 = sph(t1, p0);
                quad(vc, pose, add(c, n00, r), add(c, n01, r), add(c, n11, r), add(c, n10, r), n00, n01, n11, n10, colour, light);
            }
        }
    }

    private static float[] sph(float lat, float lon) {
        float cl = Mth.cos(lat);
        return new float[]{cl * Mth.cos(lon), Mth.sin(lat), cl * Mth.sin(lon)};
    }

    // Flat disc facing n.
    private static void disc(VertexConsumer vc, PoseStack.Pose pose, float[] c, float[] n, float r, int colour, int light) {
        float[] s = perp(n), u = cross(n, s);
        for (int k = 0; k < SIDES; k++) {
            float[] p1 = add(c, ring(s, u, k), r), p2 = add(c, ring(s, u, k + 1), r);
            quad(vc, pose, c, p1, p2, p2, n, n, n, n, colour, light);
        }
    }

    // ---------- Vertex helpers ----------

    // One quad; its corners are put in the order that makes it face the way its normals point
    // (the solid render type hides back faces).
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float[] p0, float[] p1, float[] p2, float[] p3,
                             float[] n0, float[] n1, float[] n2, float[] n3, int colour, int light) {
        float[] e1 = sub(p1, p0), e2 = sub(p2, p0), e3 = sub(p3, p0);
        float[] fn = add(cross(e1, e2), cross(e2, e3), 1);
        float[] avg = {n0[0] + n1[0] + n2[0] + n3[0], n0[1] + n1[1] + n2[1] + n3[1], n0[2] + n1[2] + n2[2] + n3[2]};
        boolean front = fn[0] * avg[0] + fn[1] * avg[1] + fn[2] * avg[2] >= 0;
        if (front) {
            vertex(vc, pose, p0, n0, colour, light);
            vertex(vc, pose, p1, n1, colour, light);
            vertex(vc, pose, p2, n2, colour, light);
            vertex(vc, pose, p3, n3, colour, light);
        } else {
            vertex(vc, pose, p3, n3, colour, light);
            vertex(vc, pose, p2, n2, colour, light);
            vertex(vc, pose, p1, n1, colour, light);
            vertex(vc, pose, p0, n0, colour, light);
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float[] p, float[] n, int colour, int light) {
        vc.addVertex(pose, p[0], p[1], p[2])
                .setColor((colour >> 16) & 255, (colour >> 8) & 255, colour & 255, 255)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, n[0], n[1], n[2]);
    }

    private static float[] ring(float[] s, float[] u, int k) {
        float a = (float) (Math.PI * 2 * k / SIDES);
        float c = Mth.cos(a), sn = Mth.sin(a);
        return new float[]{s[0] * c + u[0] * sn, s[1] * c + u[1] * sn, s[2] * c + u[2] * sn};
    }

    private static float[] perp(float[] f) {
        float[] other = Math.abs(f[1]) < 0.9f ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
        return norm(cross(f, other));
    }

    private static float[] add(float[] a, float[] d, float k) {
        return new float[]{a[0] + d[0] * k, a[1] + d[1] * k, a[2] + d[2] * k};
    }

    private static float[] sub(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static float[] norm(float[] v) {
        float len = Mth.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return len < 1e-6f ? new float[]{0, 1, 0} : new float[]{v[0] / len, v[1] / len, v[2] / len};
    }
}
