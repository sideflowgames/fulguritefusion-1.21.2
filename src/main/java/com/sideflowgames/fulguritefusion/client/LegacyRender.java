package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// 26.1 port helper. Minecraft 26.1 no longer lets renderers write vertices straight into buffers while drawing;
// they "submit" work that is drawn later. The mod's hand-written drawing code (wires, gauges, bolts, glowing
// markers) still writes vertices the old way. Buffers records those vertices (already moved into place by the
// PoseStack) and submits them, per render type, as custom geometry. The result on screen is the same.
public final class LegacyRender {
    private LegacyRender() {}

    // The collector of the renderer that is drawing right now (renderers draw one at a time on the render thread).
    @Nullable
    private static SubmitNodeCollector current;

    // Runs old-style drawing code: 'draw' gets a MultiBufferSource; everything it writes is submitted afterwards.
    public static void draw(PoseStack poseStack, SubmitNodeCollector collector, java.util.function.Consumer<MultiBufferSource> draw) {
        SubmitNodeCollector before = current;
        current = collector;
        Buffers buffers = new Buffers();
        try {
            draw.accept(buffers);
        } finally {
            current = before;
        }
        buffers.submit(poseStack, collector);
    }

    // The old ItemRenderer.renderStatic: draws an item at the PoseStack's current place.
    public static void item(ItemStack stack, ItemDisplayContext context, int light, PoseStack poseStack, @Nullable Level level, int seed) {
        if (stack.isEmpty() || current == null) return;
        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, context, level, null, seed);
        state.submit(poseStack, current, light, OverlayTexture.NO_OVERLAY, 0);
    }

    // Records vertices per render type, in order.
    public static final class Buffers implements MultiBufferSource {
        private final Map<RenderType, Recorder> recorders = new LinkedHashMap<>();

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return recorders.computeIfAbsent(renderType, t -> new Recorder());
        }

        void submit(PoseStack poseStack, SubmitNodeCollector collector) {
            recorders.forEach((type, rec) -> {
                if (rec.ops.isEmpty()) return;
                collector.submitCustomGeometry(poseStack, type, (pose, out) -> rec.replay(out));
            });
        }
    }

    // A VertexConsumer that remembers every call so it can be played back into a real buffer later.
    private static final class Recorder implements VertexConsumer {
        private final List<Object[]> ops = new ArrayList<>();

        private Recorder add(Object... op) {
            ops.add(op);
            return this;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { return add('v', x, y, z); }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return add('c', r, g, b, a); }
        @Override public VertexConsumer setColor(int color) { return add('C', color); }
        @Override public VertexConsumer setUv(float u, float v) { return add('u', u, v); }
        @Override public VertexConsumer setUv1(int u, int v) { return add('1', u, v); }
        @Override public VertexConsumer setUv2(int u, int v) { return add('2', u, v); }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return add('n', x, y, z); }
        @Override public VertexConsumer setLineWidth(float width) { return add('w', width); }

        void replay(VertexConsumer out) {
            for (Object[] op : ops) {
                switch ((char) op[0]) {
                    case 'v' -> out.addVertex((float) op[1], (float) op[2], (float) op[3]);
                    case 'c' -> out.setColor((int) op[1], (int) op[2], (int) op[3], (int) op[4]);
                    case 'C' -> out.setColor((int) op[1]);
                    case 'u' -> out.setUv((float) op[1], (float) op[2]);
                    case '1' -> out.setUv1((int) op[1], (int) op[2]);
                    case '2' -> out.setUv2((int) op[1], (int) op[2]);
                    case 'n' -> out.setNormal((float) op[1], (float) op[2], (float) op[3]);
                    case 'w' -> out.setLineWidth((float) op[1]);
                    default -> {}
                }
            }
        }
    }
}
