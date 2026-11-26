package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sideflowgames.fulguritefusion.item.ChainBolt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

// Draws Storm/Prime Blade chain lightning: a jagged, flickering bolt between each pair of enemies in the chain,
// with small side branches, fading out over about a third of a second. Storm bolts are blue-white, Prime violet.
public final class ChainBolts {
    private ChainBolts() {}

    private static final int LIFE = 7; // ticks
    private static final List<Arc> ARCS = new ArrayList<>();
    private static final Random SEEDS = new Random();

    private record Arc(List<Vec3> points, boolean prime, long seed, int[] age) {}

    public static void init() {
        ChainBolt.clientHandler = (points, prime) -> {
            ARCS.add(new Arc(points, prime, SEEDS.nextLong(), new int[]{0}));
            if (ARCS.size() > 64) ARCS.remove(0);
        };
        NeoForge.EVENT_BUS.addListener(ChainBolts::render);
    }

    // Called every client tick.
    public static void tick() {
        Iterator<Arc> it = ARCS.iterator();
        while (it.hasNext()) {
            Arc a = it.next();
            if (++a.age()[0] > LIFE) it.remove();
        }
    }

    private static void render(RenderLevelStageEvent.AfterTranslucentParticles event) {
        if (ARCS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer c = buffers.getBuffer(RenderTypes.lightning());
        for (Arc a : ARCS) {
            float strength = 1f - a.age()[0] / (float) (LIFE + 1);
            // Re-jag every other tick so the bolt flickers.
            Random r = new Random(a.seed() + a.age()[0] / 2);
            for (int i = 0; i + 1 < a.points().size(); i++) {
                bolt(c, m, a.points().get(i), a.points().get(i + 1), r, strength, a.prime());
            }
        }
        buffers.endBatch(RenderTypes.lightning());
        ps.popPose();
    }

    private static void bolt(VertexConsumer c, Matrix4f m, Vec3 a, Vec3 b, Random r, float strength, boolean prime) {
        double len = a.distanceTo(b);
        int n = Math.max(6, (int) (len * 2.5));
        float jag = (float) Math.min(0.6, 0.12 + len * 0.06);
        int gr = prime ? 190 : 120, gg = prime ? 110 : 190, gb = 255;      // glow
        int cr = prime ? 240 : 225, cg = prime ? 215 : 242, cb = 255;      // core
        int glowA = (int) (90 * strength), coreA = (int) (255 * strength);
        float glowW = prime ? 0.09f : 0.07f, coreW = prime ? 0.022f : 0.016f;
        float[][] pts = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float off = (i == 0 || i == n) ? 0 : jag;
            pts[i] = new float[]{(float) (a.x + (b.x - a.x) * t) + (r.nextFloat() - 0.5f) * off,
                    (float) (a.y + (b.y - a.y) * t) + (r.nextFloat() - 0.5f) * off,
                    (float) (a.z + (b.z - a.z) * t) + (r.nextFloat() - 0.5f) * off};
        }
        for (int i = 0; i < n; i++) {
            ColliderRenderer.ribbon(c, m, pts[i], pts[i + 1], glowW, gr, gg, gb, glowA);
            ColliderRenderer.ribbon(c, m, pts[i], pts[i + 1], coreW, cr, cg, cb, coreA);
        }
        // One or two short forks.
        int forks = 1 + r.nextInt(2);
        for (int f = 0; f < forks; f++) {
            float[] prev = pts[1 + r.nextInt(n - 1)];
            for (int k = 0; k < 3; k++) {
                float[] next = {prev[0] + (r.nextFloat() - 0.5f) * jag * 1.8f, prev[1] + (r.nextFloat() - 0.6f) * jag * 1.4f,
                        prev[2] + (r.nextFloat() - 0.5f) * jag * 1.8f};
                ColliderRenderer.ribbon(c, m, prev, next, glowW * 0.5f, gr, gg, gb, glowA);
                ColliderRenderer.ribbon(c, m, prev, next, coreW * 0.6f, cr, cg, cb, (int) (coreA * 0.7f));
                prev = next;
            }
        }
    }
}
