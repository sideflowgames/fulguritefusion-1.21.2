package com.sideflowgames.fulguritefusion.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

// The Fulgurite Fusion Guide's map. Topics sit on a stormy background, joined by traces that branch out from
// the first lightning strike in the order you'll meet them. A topic lights up (and its trace charges) once
// you've held one of its items; "Show all" reveals the rest.
//
// Drag to move, scroll to zoom. Click a topic: the map dives into it and opens its screen (GuideTopicScreen).
public class GuideMapScreen extends Screen {
    private static final float NODE = 30;                // topic size in map units
    private static final float MIN_ZOOM = 0.2f, MAX_ZOOM = 9f;
    private static final float DIVE_TIME = 0.45f;        // seconds of zooming in before the topic opens

    // Kept between openings.
    private static float camX = Float.NaN, camY, zoom = 1;
    private static boolean showAll;
    // Where the map was before diving into a topic (to zoom back out to).
    private static float backX, backY, backZoom;
    private static boolean returning;

    private float tCamX, tCamY, tZoom;
    private GuideData data;
    private long lastNanos;
    private float time, dt;
    private double pressX, pressY;
    private boolean dragging;
    private List<Component> hoverText;
    private Button showAllButton;
    private String diving;      // topic being opened
    private float diveTime;
    private final GuideSky sky = new GuideSky();

    private GuideMapScreen() {
        super(Component.translatable("item.fulguritefusion.guide_book"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GuideMapScreen());
    }

    // Opened from the guide book item (not a return from a topic).
    public static void openFresh() {
        GuideTopicScreen.clearCaches();
        open();
    }

    // Back from a topic: start zoomed in on it, then pull back out to where the map was.
    static void returnFrom(GuideData.Chapter c) {
        camX = c.x;
        camY = c.y;
        zoom = MAX_ZOOM;
        returning = true;
        open();
    }

    @Override
    protected void init() {
        data = GuideData.reload();
        if (Float.isNaN(camX)) {
            overview();
            camX = tCamX;
            camY = tCamY;
            zoom = tZoom;
        } else if (returning) {
            returning = false;
            tCamX = backX;
            tCamY = backY;
            tZoom = backZoom;
        } else {
            tCamX = camX;
            tCamY = camY;
            tZoom = zoom;
        }
        addRenderableWidget(Button.builder(Component.literal("⌂ Overview"), b -> overview()).bounds(6, 6, 74, 16).build());
        showAllButton = addRenderableWidget(Button.builder(Component.empty(), b -> showAll = !showAll)
                .bounds(width - 98, 6, 92, 16).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (showAllButton != null) showAllButton.setMessage(Component.literal("Show all: " + (showAll ? "On" : "Off")));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------- Camera ----------

    private float sx(float mapX) {
        return width / 2f + (mapX - camX) * zoom;
    }

    private float sy(float mapY) {
        return height / 2f + (mapY - camY) * zoom;
    }

    private void overview() {
        float x0 = Float.MAX_VALUE, y0 = x0, x1 = -x0, y1 = -x0;
        for (GuideData.Chapter c : data.chapters.values()) {
            x0 = Math.min(x0, c.x);
            y0 = Math.min(y0, c.y);
            x1 = Math.max(x1, c.x);
            y1 = Math.max(y1, c.y);
        }
        x0 -= 50;
        y0 -= 50;
        x1 += 50;
        y1 += 60;
        tCamX = (x0 + x1) / 2;
        tCamY = (y0 + y1) / 2;
        tZoom = Mth.clamp(Math.min(width * 0.92f / (x1 - x0), (height - 44) * 0.92f / (y1 - y0)), MIN_ZOOM, MAX_ZOOM);
    }

    private void dive(String id) {
        if (diving != null) return;
        GuideData.Chapter c = data.chapters.get(id);
        backX = tCamX;
        backY = tCamY;
        backZoom = tZoom;
        diving = id;
        diveTime = 0;
        tCamX = c.x;
        tCamY = c.y;
        tZoom = MAX_ZOOM * 1.5f;
        GuideUnlocks.NEW.remove(id);
    }

    // ---------- Drawing ----------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        dt = lastNanos == 0 ? 0 : Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        time += dt;
        float k = 1 - (float) Math.pow(diving != null ? 0.00005 : 0.0008, dt);
        camX += (tCamX - camX) * k;
        camY += (tCamY - camY) * k;
        zoom = (float) Math.exp(Math.log(zoom) + (Math.log(tZoom) - Math.log(zoom)) * k);
        hoverText = null;

        sky.draw(g, width, height, camX * zoom, camY * zoom, dt, true);
        drawTraces(g);
        drawNodes(g, mouseX, mouseY);

        if (diving != null) {
            diveTime += dt;
            float f = Mth.clamp(diveTime / DIVE_TIME, 0, 1);
            g.pose().pushMatrix();
            g.nextStratum();
            g.fill(0, 0, width, height, ((int) (f * f * 255) << 24) | 0xE6F6FF);
            g.pose().popMatrix();
            if (diveTime >= DIVE_TIME) {
                GuideData.Chapter c = data.chapters.get(diving);
                diving = null;
                camX = backX;
                camY = backY;
                zoom = backZoom;
                if (c != null && minecraft != null) minecraft.setScreen(new GuideTopicScreen(c));
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (diving != null) return;
        String title = "⚡ Fulgurite Fusion Guide";
        g.text(font, title, (width - font.width(title)) / 2, 10, 0xFFBFE6FF, true);
        String hint = "Drag: move   ·   Scroll: zoom   ·   Click a topic to open it";
        g.text(font, hint, (width - font.width(hint)) / 2, height - 12, 0xFF8FA6C8, false);
        if (hoverText != null) {
            g.pose().pushMatrix();
            g.nextStratum();
            g.setComponentTooltipForNextFrame(font, hoverText, mouseX, mouseY);
            g.pose().popMatrix();
        }
    }

    // A circuit-style trace from a parent topic to a child: across, down, across.
    private static float[][] path(float x0, float y0, float x1, float y1) {
        float mid = (x0 + x1) / 2;
        return new float[][]{{x0, y0}, {mid, y0}, {mid, y1}, {x1, y1}};
    }

    private void drawPath(GuiGraphicsExtractor g, float[][] p, float w, int color) {
        for (int i = 0; i + 1 < p.length; i++) {
            float ax = sx(p[i][0]), ay = sy(p[i][1]), bx = sx(p[i + 1][0]), by = sy(p[i + 1][1]);
            int x0 = (int) (Math.min(ax, bx) - w / 2), x1 = (int) (Math.max(ax, bx) + w / 2 + 1);
            int y0 = (int) (Math.min(ay, by) - w / 2), y1 = (int) (Math.max(ay, by) + w / 2 + 1);
            g.fill(x0, y0, x1, y1, color);
        }
    }

    private void drawTraces(GuiGraphicsExtractor g) {
        float w = Mth.clamp(2 * zoom, 1, 4);
        for (GuideData.Chapter c : data.chapters.values()) {
            GuideData.Chapter p = c.parent == null ? null : data.chapters.get(c.parent);
            if (p == null || p == c) continue;
            float[][] path = path(p.x, p.y, c.x, c.y);
            boolean charged = GuideUnlocks.unlocked(c.id) && GuideUnlocks.unlocked(p.id);
            if (charged) {
                drawPath(g, path, w * 3, 0x305AB4FF);
                drawPath(g, path, w, 0xFF7FD0FF);
                // Sparks running along the trace.
                float len = 0;
                for (int i = 0; i + 1 < path.length; i++) len += Math.abs(path[i + 1][0] - path[i][0]) + Math.abs(path[i + 1][1] - path[i][1]);
                for (int j = 0; j < 2; j++) {
                    float d = (time * 55 + (c.id.hashCode() & 255) + j * len / 2) % Math.max(1, len);
                    for (int i = 0; i + 1 < path.length; i++) {
                        float seg = Math.abs(path[i + 1][0] - path[i][0]) + Math.abs(path[i + 1][1] - path[i][1]);
                        if (d <= seg) {
                            float t = seg == 0 ? 0 : d / seg;
                            float px = sx(Mth.lerp(t, path[i][0], path[i + 1][0])), py = sy(Mth.lerp(t, path[i][1], path[i + 1][1]));
                            g.fill((int) (px - w * 2), (int) (py - w * 2), (int) (px + w * 2), (int) (py + w * 2), 0x60BFE6FF);
                            g.fill((int) (px - w), (int) (py - w), (int) (px + w), (int) (py + w), 0xFFFFFFFF);
                            break;
                        }
                        d -= seg;
                    }
                }
            } else {
                drawPath(g, path, w, 0xFF243049);
            }
            // Solder pads at the bends.
            for (int i = 1; i < 3; i++) {
                float px = sx(path[i][0]), py = sy(path[i][1]);
                g.fill((int) (px - w * 1.5f), (int) (py - w * 1.5f), (int) (px + w * 1.5f), (int) (py + w * 1.5f), charged ? 0xFF5AB4FF : 0xFF2A3348);
            }
        }
    }

    private float nodeSize() {
        return Mth.clamp(NODE * zoom, 16, 56 * Math.max(1, zoom / 2));
    }

    private String nodeAt(double mx, double my) {
        float s = nodeSize();
        for (GuideData.Chapter c : data.chapters.values()) {
            if (Math.abs(mx - sx(c.x)) <= s / 2 + 2 && Math.abs(my - sy(c.y)) <= s / 2 + 2) return c.id;
        }
        return null;
    }

    private boolean canOpen(String id) {
        return showAll || GuideUnlocks.unlocked(id);
    }

    private void drawNodes(GuiGraphicsExtractor g, int mx, int my) {
        float s = nodeSize();
        String over = diving == null ? nodeAt(mx, my) : null;
        for (GuideData.Chapter c : data.chapters.values()) {
            float x = sx(c.x), y = sy(c.y);
            if (x < -s || y < -s || x > width + s || y > height + s) continue;
            boolean open = GuideUnlocks.unlocked(c.id);
            boolean visible = open || showAll;
            int x0 = (int) (x - s / 2), y0 = (int) (y - s / 2), x1 = (int) (x + s / 2), y1 = (int) (y + s / 2);
            if (GuideUnlocks.NEW.contains(c.id)) {
                int a = (int) (90 + 80 * Math.sin(time * 6));
                g.fill(x0 - 5, y0 - 5, x1 + 5, y1 + 5, (a << 24) | 0x7FD0FF);
            } else if (open) {
                g.fill(x0 - 3, y0 - 3, x1 + 3, y1 + 3, 0x405AB4FF);
            }
            int border = c.id.equals(over) && visible ? 0xFFFFFFFF : open ? 0xFF5AB4FF : 0xFF2A3348;
            g.fill(x0, y0, x1, y1, border);
            g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, open ? 0xFF0E1830 : 0xFF0A101E);
            if (visible) {
                float is = s * 0.62f / 16;
                g.pose().pushMatrix();
                g.pose().translate((float) (x), (float) (y));
                g.pose().scale((float) (is), (float) (is));
                g.item(c.icon, -8, -8);
                g.pose().popMatrix();
                if (!open) {
                    g.pose().pushMatrix();
                    g.nextStratum();
                    g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, 0x99060A14);
                    g.pose().popMatrix();
                }
            } else {
                g.text(font, "?", (int) x - font.width("?") / 2, (int) y - 4, 0xFF4A5A74, false);
            }
            String label = visible ? c.title : "???";
            float ts = Mth.clamp(zoom, 0.6f, 1f);
            g.pose().pushMatrix();
            g.pose().translate((float) (x), (float) (y1 + 3));
            g.pose().scale((float) (ts), (float) (ts));
            g.text(font, label, -font.width(label) / 2, 0, GuiStyle.opaque(open ? 0xFFDDEEFF : 0xFF5A6A80), true);
            g.pose().popMatrix();
            if (c.id.equals(over)) {
                hoverText = visible
                        ? List.of(Component.literal(c.title), Component.literal(open ? "Click to open" : "Not unlocked yet").withColor(0xAAAAAA))
                        : List.of(Component.literal("Locked"), Component.literal("Hold one of its items to unlock it").withColor(0xAAAAAA));
            }
        }
    }

    // ---------- Input ----------

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (diving != null) return true;
        if (super.mouseClicked(event, doubleClick)) return true;
        if (button == 0) {
            pressX = mx;
            pressY = my;
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (button != 0 || diving != null) return super.mouseDragged(event, dx, dy);
        if (Math.abs(mx - pressX) + Math.abs(my - pressY) > 3) dragging = true;
        if (dragging) {
            camX -= (float) dx / zoom;
            camY -= (float) dy / zoom;
            tCamX = camX;
            tCamY = camY;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (button == 0 && !dragging && diving == null) {
            String id = nodeAt(mx, my);
            if (id != null && canOpen(id)) dive(id);
        }
        if (button == 0) dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (scrollY == 0 || diving != null) return false;
        float nz = Mth.clamp(tZoom * (float) Math.pow(1.15, scrollY), MIN_ZOOM, MAX_ZOOM);
        float mapX = camX + (float) (mx - width / 2f) / zoom, mapY = camY + (float) (my - height / 2f) / zoom;
        zoom = tZoom = nz;
        camX = tCamX = mapX - (float) (mx - width / 2f) / nz;
        camY = tCamY = mapY - (float) (my - height / 2f) / nz;
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key(), scan = event.scancode(), mods = event.modifiers();
        if (key == GLFW.GLFW_KEY_HOME) {
            overview();
            return true;
        }
        return super.keyPressed(event);
    }
}
