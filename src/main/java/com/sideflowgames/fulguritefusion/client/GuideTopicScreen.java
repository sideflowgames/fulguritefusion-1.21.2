package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// One topic of the guide, opened by diving into it on the map. Its sections are tabs across the top.
// Left: what the section is about (its items and blocks, recipes, or a step-by-step build). Right: the text,
// large and easy to read (A- / A+ change the size; scroll if it runs long).
//
// Step-by-step builds play like a little film: each step drops its blocks into place, with a caption saying
// what to place. Play / pause, step back and forward, replay; drag the scene to turn it, scroll to zoom.
public class GuideTopicScreen extends Screen {
    private static final int TITLE = 0xFF1E4E79, FAINT = 0xFF5A6A80, TAB_DARK = 0xFF1C2A44;
    private static final float[] TEXT_SIZES = {0.85f, 1f, 1.25f, 1.5f};
    private static int textSize = 1;
    private static final Map<String, Integer> SECTION = new HashMap<>(); // topic -> last section shown

    private static final class Scene {
        int step;
        boolean playing = true;
        float stepTime, yaw = 225, pitch = 30, zoom = 1;
    }
    private static final Map<String, Scene> SCENES = new HashMap<>();

    private final GuideData.Chapter chapter;
    private final GuideSky sky = new GuideSky();
    private int section;
    private float textScroll;
    private int tabScroll;
    private long lastNanos;
    private float dt, fadeIn = 1;
    private ItemStack hovered = ItemStack.EMPTY;
    private Component hoverLine;
    private boolean rotating;

    // Recipe items you can click: x0, y0, x1, y1 on screen, and the item.
    private record Clickable(float x0, float y0, float x1, float y1, ItemStack item) {}
    private final List<Clickable> clickables = new ArrayList<>();
    // A recipe shown on the left in place of the section's own (when the item isn't covered by the guide).
    private final List<ItemStack> recipeTrail = new ArrayList<>();
    // Topics you jumped from by clicking a recipe item (Back returns to them before the map).
    private static final List<String[]> HISTORY = new ArrayList<>();
    // Item -> crafting recipe id that makes it (or none), and item -> guide topic/section whose recipe makes it.
    private static final Map<net.minecraft.world.item.Item, Optional<Identifier>> RECIPE_FOR = new HashMap<>();
    private static Map<net.minecraft.world.item.Item, String[]> guideRecipes;

    // Layout (worked out in init).
    private int top, leftX, leftW, rightX, rightW, panelH;
    private int viewX, viewY, viewW, viewH;     // build scene
    private int ctlY;                           // build controls row
    private int textMax;                        // how far the text can scroll

    GuideTopicScreen(GuideData.Chapter chapter) {
        super(Component.literal(chapter.title));
        this.chapter = chapter;
        this.section = Mth.clamp(SECTION.getOrDefault(chapter.id, 0), 0, Math.max(0, chapter.sections.size() - 1));
    }

    private GuideData.Section current() {
        return chapter.sections.isEmpty() ? null : chapter.sections.get(section);
    }

    @Override
    protected void init() {
        top = chapter.sections.size() > 1 ? 54 : 36;
        leftX = 8;
        leftW = (int) (width * 0.47f) - 12;
        rightX = leftX + leftW + 6;
        rightW = width - 8 - rightX;
        panelH = height - top - 8;
        viewX = leftX + 6;
        viewY = top + 6;
        viewW = leftW - 12;
        viewH = panelH - 12 - 44;
        ctlY = viewY + viewH + 4;
        addRenderableWidget(Button.builder(Component.literal("◀ Map"), b -> onClose()).bounds(6, 8, 50, 18).build());
        addRenderableWidget(Button.builder(Component.literal("A−"), b -> textSize = Math.max(0, textSize - 1))
                .bounds(width - 52, 8, 22, 18).build());
        addRenderableWidget(Button.builder(Component.literal("A+"), b -> textSize = Math.min(TEXT_SIZES.length - 1, textSize + 1))
                .bounds(width - 28, 8, 22, 18).build());
    }

    @Override
    public void onClose() {
        if (!recipeTrail.isEmpty()) {
            recipeTrail.remove(recipeTrail.size() - 1);
            return;
        }
        if (!HISTORY.isEmpty() && minecraft != null) {
            String[] back = HISTORY.remove(HISTORY.size() - 1);
            GuideData.Chapter c = GuideData.get().chapters.get(back[0]);
            if (c != null) {
                SECTION.put(c.id, Integer.parseInt(back[1]));
                GuideTopicScreen next = new GuideTopicScreen(c);
                next.fadeIn = 0.6f;
                minecraft.setScreen(next);
                return;
            }
        }
        HISTORY.clear();
        GuideMapScreen.returnFrom(chapter);
    }

    // ---------- Recipe links ----------

    // Recipes can change between worlds and reloads: the map clears these each time the guide opens.
    static void clearCaches() {
        RECIPE_FOR.clear();
        guideRecipes = null;
        HISTORY.clear();
    }

    private Optional<Identifier> recipeFor(ItemStack stack) {
        if (minecraft == null || minecraft.level == null || stack.isEmpty()) return Optional.empty();
        return RECIPE_FOR.computeIfAbsent(stack.getItem(), item -> {
            Identifier found = null;
            for (var h : GuideRecipes.crafting()) {
                ItemStack out;
                try {
                    out = GuideRecipes.result(h.value(), minecraft.level);
                } catch (Exception e) {
                    continue;
                }
                if (!out.is(item)) continue;
                // Prefer this mod's recipes, then the first one found.
                if (found == null || h.id().identifier().getNamespace().equals(com.sideflowgames.fulguritefusion.FulguriteFusion.MODID)) found = h.id().identifier();
            }
            return Optional.ofNullable(found);
        });
    }

    // Which guide topic and section show a recipe for this item, if any.
    private String[] guideFor(ItemStack stack) {
        if (minecraft == null || minecraft.level == null) return null;
        if (guideRecipes == null) {
            guideRecipes = new HashMap<>();
            for (GuideData.Chapter c : GuideData.get().chapters.values()) {
                for (int i = 0; i < c.sections.size(); i++) {
                    for (String id : c.sections.get(i).recipes) {
                        try {
                            var h = GuideRecipes.byKey(Identifier.parse(id));
                            if (h.isPresent()) guideRecipes.putIfAbsent(GuideRecipes.result(h.get().value(), minecraft.level).getItem(), new String[]{c.id, String.valueOf(i)});
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
        return guideRecipes.get(stack.getItem());
    }

    private boolean craftable(ItemStack stack) {
        return guideFor(stack) != null || recipeFor(stack).isPresent();
    }

    // Go to how this item is made: its guide section if the guide covers it, otherwise its recipe on the left.
    private void openRecipe(ItemStack stack) {
        String[] where = guideFor(stack);
        if (where != null) {
            int sec = Integer.parseInt(where[1]);
            if (where[0].equals(chapter.id)) {
                recipeTrail.clear();
                select(sec);
                return;
            }
            GuideData.Chapter c = GuideData.get().chapters.get(where[0]);
            if (c != null && minecraft != null) {
                HISTORY.add(new String[]{chapter.id, String.valueOf(section)});
                SECTION.put(c.id, sec);
                GuideTopicScreen next = new GuideTopicScreen(c);
                next.fadeIn = 0.6f;
                minecraft.setScreen(next);
                return;
            }
        }
        if (recipeFor(stack).isPresent()) {
            if (recipeTrail.size() >= 20) recipeTrail.remove(0);
            recipeTrail.add(stack.copyWithCount(1));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void select(int s) {
        if (chapter.sections.isEmpty()) return;
        section = Mth.clamp(s, 0, chapter.sections.size() - 1);
        SECTION.put(chapter.id, section);
        textScroll = 0;
        recipeTrail.clear();
        // Keep the chosen tab in view.
        if (section < tabScroll) tabScroll = section;
        while (tabScroll < section && tabs().stream().noneMatch(t -> t.index() == section)) tabScroll++;
    }

    // ---------- Drawing ----------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        dt = lastNanos == 0 ? 0 : Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        sky.draw(g, width, height, 0, 0, dt, true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hovered = ItemStack.EMPTY;
        hoverLine = null;
        clickables.clear();
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        // Header: icon and big title.
        g.pose().pushMatrix();
        g.pose().translate((float) (62), (float) (6));
        g.pose().scale((float) (1.4f), (float) (1.4f));
        g.item(chapter.icon, 0, 0);
        g.text(font, chapter.title, 20, 4, 0xFFDDEEFF, true);
        g.pose().popMatrix();

        if (chapter.sections.size() > 1) drawTabs(g, mouseX, mouseY);
        GuideData.Section sec = current();
        GuiStyle.panel(g, leftX, top, leftW, panelH);
        GuiStyle.panel(g, rightX, top, rightW, panelH);
        if (sec != null) {
            if (!recipeTrail.isEmpty()) drawTrail(g, mouseX, mouseY);
            else if (sec.build != null) drawBuild(g, sec.build, mouseX, mouseY);
            else drawShowcase(g, sec, mouseX, mouseY);
            drawText(g, sec, mouseX, mouseY);
        }

        if (fadeIn > 0) {
            g.pose().pushMatrix();
            g.nextStratum();
            g.fill(0, 0, width, height, ((int) (fadeIn * fadeIn * 255) << 24) | 0xE6F6FF);
            g.pose().popMatrix();
            fadeIn = Math.max(0, fadeIn - dt * 3);
        }
        g.pose().pushMatrix();
        g.nextStratum();
        if (!hovered.isEmpty()) {
            List<Component> tip = new ArrayList<>(Screen.getTooltipFromItem(minecraft, hovered));
            if (clickableAt(mouseX, mouseY) != null) tip.add(Component.literal("\u25B8 Click to see how it's made").withColor(0x7FD0FF));
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
        else if (hoverLine != null) g.setTooltipForNextFrame(font, hoverLine, mouseX, mouseY);
        g.pose().popMatrix();
    }

    // ---------- Tabs ----------

    private int tabWidth(int i) {
        // Wide enough for the whole title; the row scrolls (arrows) when the tabs don't all fit.
        return Math.min(Math.max(110, width - 60), font.width(chapter.sections.get(i).title) + 14);
    }

    // Tabs that fit, starting at tabScroll; arrows when there are more.
    private record Tab(int index, int x, int w) {}

    private List<Tab> tabs() {
        List<Tab> out = new ArrayList<>();
        int x = 22, max = width - 22;
        for (int i = tabScroll; i < chapter.sections.size(); i++) {
            int w = tabWidth(i);
            if (x + w > max) break;
            out.add(new Tab(i, x, w));
            x += w + 2;
        }
        return out;
    }

    private void drawTabs(GuiGraphicsExtractor g, int mx, int my) {
        int y = 34;
        List<Tab> tabs = tabs();
        boolean more = tabs.isEmpty() || tabs.get(tabs.size() - 1).index() < chapter.sections.size() - 1;
        if (tabScroll > 0) arrow(g, 6, y, "◀");
        if (more) arrow(g, width - 20, y, "▶");
        for (Tab t : tabs) {
            boolean active = t.index() == section;
            boolean over = mx >= t.x() && mx < t.x() + t.w() && my >= y && my < y + 18;
            g.fill(t.x(), y, t.x() + t.w(), y + 18 + (active ? 2 : 0), active ? GuiStyle.PANEL : over ? 0xFF2C4064 : TAB_DARK);
            String title = font.plainSubstrByWidth(chapter.sections.get(t.index()).title, t.w() - 8);
            g.text(font, title, t.x() + (t.w() - font.width(title)) / 2, y + 5, GuiStyle.opaque(active ? GuiStyle.TEXT : 0xFFBFD6F0), false);
        }
    }

    private void arrow(GuiGraphicsExtractor g, int x, int y, String s) {
        g.fill(x, y, x + 14, y + 18, TAB_DARK);
        g.text(font, s, x + 3, y + 5, 0xFFBFD6F0, false);
    }

    // ---------- Left: items, blocks and recipes ----------

    private void drawShowcase(GuiGraphicsExtractor g, GuideData.Section sec, int mx, int my) {
        int x = leftX + 8, y = top + 8, w = leftW - 16, bottom = top + panelH - 8;
        if (sec.items.isEmpty() && sec.recipes.isEmpty()) {
            // Nothing to show: the topic's icon, big.
            float s = Math.min(w, bottom - y) / 16f * 0.5f;
            g.pose().pushMatrix();
            g.pose().translate((float) (leftX + leftW / 2f - 8 * s), (float) (top + panelH / 2f - 8 * s));
            g.pose().scale((float) (s), (float) (s));
            g.item(chapter.icon, 0, 0);
            g.pose().popMatrix();
            return;
        }
        // Items and blocks along the top: icon and name, in one or two columns.
        int itemsBottom = y;
        if (!sec.items.isEmpty()) {
            int cols = sec.items.size() > 3 && w > 200 ? 2 : 1;
            int colW = w / cols;
            int rows = (sec.items.size() + cols - 1) / cols;
            int room = sec.recipes.isEmpty() ? bottom - y : (bottom - y) / 3;
            float is = rows * 22 <= room ? 1.25f : 1f;
            int rowH = (int) (16 * is) + 4;
            int maxRows = Math.max(1, room / rowH);
            for (int i = 0; i < sec.items.size() && i / cols < maxRows; i++) {
                ItemStack s = sec.items.get(i);
                int ix = x + (i % cols) * colW, iy = y + (i / cols) * rowH;
                g.pose().pushMatrix();
                g.pose().translate((float) (ix), (float) (iy));
                g.pose().scale((float) (is), (float) (is));
                g.item(s, 0, 0);
                g.pose().popMatrix();
                String name = font.plainSubstrByWidth(s.getHoverName().getString(), colW - (int) (20 * is) - 4);
                g.text(font, name, ix + (int) (18 * is) + 2, iy + (int) (8 * is) - 4, GuiStyle.opaque(GuiStyle.TEXT), false);
                if (mx >= ix && mx < ix + colW - 4 && my >= iy && my < iy + 16 * is) hovered = s;
                if (craftable(s) && !madeHere(sec, s)) clickables.add(new Clickable(ix, iy, ix + colW - 4, iy + 16 * is, s));
            }
            itemsBottom = y + Math.min(rows, maxRows) * rowH;
        }
        drawRecipesCentered(g, sec.recipes, x, w, itemsBottom + 6, bottom, mx, my);
    }

    // Whether one of this section's own recipes makes the item (no point linking to where you already are).
    private boolean madeHere(GuideData.Section sec, ItemStack s) {
        String[] where = guideFor(s);
        return where != null && where[0].equals(chapter.id) && Integer.parseInt(where[1]) == section;
    }

    // Recipes as big as fit, centred in the left panel (but never over the item list).
    private void drawRecipesCentered(GuiGraphicsExtractor g, List<String> recipes, int x, int w, int minY, int bottom, int mx, int my) {
        if (recipes.isEmpty()) return;
        int n = recipes.size();
        int perRow = 1;
        float rs = 0;
        // Try 1, 2, 3 per row and keep whichever gives the biggest recipes.
        for (int pr = 1; pr <= Math.min(3, n); pr++) {
            int rows = (n + pr - 1) / pr;
            float s = Math.min(Math.min((w / (float) pr) / 104f, (bottom - minY) / (rows * 66f)), 2.6f);
            if (s > rs + 0.05f) {
                rs = s;
                perRow = pr;
            }
        }
        rs = Math.max(rs, 0.7f);
        int rows = (n + perRow - 1) / perRow;
        float blockH = rows * 66 * rs;
        float panelMid = top + panelH / 2f;
        float ry = Math.max(minY, Math.min(panelMid - blockH / 2, bottom - blockH));
        float cellW = w / (float) perRow;
        for (int i = 0; i < n; i++) {
            int col = i % perRow, row = i / perRow;
            int inRow = Math.min(perRow, n - row * perRow);
            float rowX = x + (w - inRow * cellW) / 2;
            float rx = rowX + col * cellW + (cellW - 98 * rs) / 2;
            drawRecipe(g, recipes.get(i), rx, ry + row * 66 * rs, rs, mx, my);
        }
    }

    // A recipe opened by clicking an item the guide doesn't cover.
    private void drawTrail(GuiGraphicsExtractor g, int mx, int my) {
        ItemStack item = recipeTrail.get(recipeTrail.size() - 1);
        int x = leftX + 8, y = top + 8, w = leftW - 16, bottom = top + panelH - 8;
        button(g, x, y, 40, "\u25C0 Back", mx, my);
        g.item(item, x + 46, y - 1);
        g.text(font, font.plainSubstrByWidth("How to make: " + item.getHoverName().getString(), w - 70), x + 66, y + 3, GuiStyle.opaque(TITLE), false);
        recipeFor(item).ifPresent(id -> drawRecipesCentered(g, List.of(id.toString()), x, w, y + 22, bottom, mx, my));
    }

    private void drawRecipe(GuiGraphicsExtractor g, String id, float x, float y, float scale, int mx, int my) {
        if (minecraft == null || minecraft.level == null) return;
        Optional<RecipeHolder<?>> holder;
        try {
            holder = GuideRecipes.byKey(Identifier.parse(id));
        } catch (Exception e) {
            holder = Optional.empty();
        }
        var ps = g.pose();
        ps.pushMatrix();
        ps.translate(x, y);
        ps.scale(scale, scale);
        float lmx = (mx - x) / scale, lmy = (my - y) / scale;
        if (holder.isEmpty()) {
            g.text(font, "(recipe not found)", 4, 24, GuiStyle.opaque(FAINT), false);
            ps.popMatrix();
            return;
        }
        Recipe<?> recipe = holder.get().value();
        GuideRecipes.Shown shown = GuideRecipes.show(recipe, minecraft.level).orElse(null);
        java.util.List<java.util.List<ItemStack>> ings = shown == null ? java.util.List.of() : shown.slots();
        boolean isShaped = shown != null && shown.shaped();
        int w = shown != null && shown.shaped() ? shown.width() : 3;
        int gx = 4, gy = 2;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) GuiStyle.slot(g, gx + c * 18 + 1, gy + r * 18 + 1);
        long tick = minecraft.level.getGameTime() / 20;
        for (int i = 0; i < ings.size(); i++) {
            int r, c;
            if (isShaped) {
                r = i / w;
                c = i % w;
            } else if (ings.size() == 1) {
                r = 1;
                c = 1;
            } else {
                r = i / 3;
                c = i % 3;
            }
            if (r > 2) break;
            java.util.List<ItemStack> options = ings.get(i);
            if (options.isEmpty()) continue;
            ItemStack s = options.get((int) (tick % options.size()));
            int ix = gx + c * 18 + 1, iy = gy + r * 18 + 1;
            g.item(s, ix, iy);
            if (lmx >= ix && lmx < ix + 16 && lmy >= iy && lmy < iy + 16) hovered = s;
        }
        g.text(font, "→", gx + 60, gy + 22, GuiStyle.opaque(GuiStyle.TEXT), false);
        ItemStack out = shown == null ? ItemStack.EMPTY : shown.result();
        int ox = gx + 74, oy = gy + 19;
        GuiStyle.slot(g, ox, oy);
        g.item(out, ox, oy);
        g.itemDecorations(font, out, ox, oy);
        if (lmx >= ox && lmx < ox + 16 && lmy >= oy && lmy < oy + 16) hovered = out;
        ps.popMatrix();
    }

    // ---------- Left: step-by-step build ----------

    private Scene scene(String id) {
        return SCENES.computeIfAbsent(id, k -> new Scene());
    }

    private static float stepLength(GuideBuilds.Step step) {
        int n = step.blocks().size();
        return (n - 1) * stagger(n) + 0.3f;
    }

    private static float stagger(int n) {
        return Math.min(0.08f, 1.5f / Math.max(1, n));
    }

    private void drawBuild(GuiGraphicsExtractor g, String id, int mx, int my) {
        GuideBuilds.Build b = GuideBuilds.get(id);
        Scene sc = scene(id);
        int steps = b.steps.size();
        sc.step = Mth.clamp(sc.step, 0, steps);
        // Play: each step, then a pause; at the end, hold the finished build and start again.
        sc.stepTime += dt;
        if (sc.playing) {
            boolean done = sc.step >= steps;
            float hold = done ? 4.5f : stepLength(b.steps.get(sc.step)) + 1.4f;
            if (sc.stepTime > hold) {
                sc.step = done ? 0 : sc.step + 1;
                sc.stepTime = 0;
            }
        }
        if (!rotating) sc.yaw += dt * 10;

        g.fill(viewX - 1, viewY - 1, viewX + viewW + 1, viewY + viewH + 1, 0xFF5AB4FF);
        g.fillGradient(viewX, viewY, viewX + viewW, viewY + viewH, 0xFF0C1424, 0xFF16243E);
        render3D(g, b, sc);

        // Caption under the scene.
        Component caption;
        ItemStack icon;
        if (sc.step >= steps) {
            caption = Component.literal(sc.stepTime < FORM_AT ? "All " + b.blocks.size() + " blocks placed..." : "Formed! It's ready to use");
            icon = steps > 0 ? b.steps.get(0).icon() : ItemStack.EMPTY;
        } else {
            caption = b.steps.get(sc.step).caption();
            icon = b.steps.get(sc.step).icon();
        }
        int cy = ctlY;
        g.item(icon, viewX, cy);
        List<FormattedCharSequence> capLines = font.split(caption, viewW - 20);
        for (int i = 0; i < Math.min(2, capLines.size()); i++) g.text(font, capLines.get(i), viewX + 20, cy + (capLines.size() > 1 ? i * 9 : 4), GuiStyle.opaque(GuiStyle.TEXT), false);
        if (mx >= viewX && mx < viewX + 16 && my >= cy && my < cy + 16 && !icon.isEmpty()) hovered = icon;

        // Controls: back, play/pause, forward, replay, and where we are.
        int by = cy + 20;
        button(g, viewX, by, 18, "◀", mx, my);
        button(g, viewX + 20, by, 18, sc.playing ? "∥" : "▶", mx, my);
        button(g, viewX + 40, by, 18, "▶▶", mx, my);
        button(g, viewX + 60, by, 18, "⟲", mx, my);
        String where = sc.step >= steps ? "Done" : "Step " + (sc.step + 1) + " of " + steps;
        g.text(font, where, viewX + 84, by + 4, GuiStyle.opaque(GuiStyle.TEXT), false);
        // Everything it takes, at the right.
        List<ItemStack> mats = b.materials(-1);
        int mx0 = viewX + viewW;
        for (int i = Math.min(6, mats.size()) - 1; i >= 0; i--) {
            ItemStack s = mats.get(i);
            String cnt = "×" + s.getCount();
            int w = 12 + font.width(cnt) + 6;
            mx0 -= w;
            if (mx0 < viewX + 84 + font.width(where) + 6) break;
            g.pose().pushMatrix();
            g.pose().translate((float) (mx0), (float) (by + 1));
            g.pose().scale((float) (0.75f), (float) (0.75f));
            g.item(s, 0, 0);
            g.pose().popMatrix();
            g.text(font, cnt, mx0 + 13, by + 4, GuiStyle.opaque(GuiStyle.TEXT), false);
            if (mx >= mx0 && mx < mx0 + w && my >= by && my < by + 14) hovered = s.copyWithCount(1);
        }
        g.text(font, "Drag to turn \u00B7 Scroll to zoom", viewX + 4, viewY + 4, 0xFF5A7090, false);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, String text, int mx, int my) {
        boolean over = mx >= x && mx < x + w && my >= y && my < y + 14;
        g.fill(x, y, x + w, y + 14, GuiStyle.SLOT_EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + 13, over ? 0xFF3E6A94 : GuiStyle.SLOT_FILL);
        g.text(font, text, x + (w - font.width(text)) / 2 + 1, y + 3, 0xFFDDEEFF, false);
    }

    // How long the finished build waits before forming, and how long the forming flash lasts.
    private static final float FORM_AT = 0.6f, FORM_FLASH = 0.7f;

    // Drawn with a real perspective camera inside the view (a flat camera makes a turning cube look like it's
    // warping). The finished build then forms: blocks switch to their formed look and machines get their model.
    private void render3D(GuiGraphicsExtractor g, GuideBuilds.Build b, Scene sc) {
        if (b.blocks.isEmpty() || minecraft == null) return;
        if (viewW <= 0 || viewH <= 0) return;
        float fov = 30f;
        float bw = b.maxX - b.minX + 1, bh = b.maxY - b.minY + 1, bd = b.maxZ - b.minZ + 1;
        float radius = (float) Math.sqrt(bw * bw + bh * bh + bd * bd) / 2;
        float fit = radius / (float) Math.sin(Math.toRadians(fov / 2)) * Math.max(1f, viewH / (float) viewW);
        float dist = Math.max(radius * 0.3f + 1, fit / sc.zoom);
        boolean finished = sc.step >= b.steps.size();
        boolean formed = finished && sc.stepTime >= FORM_AT && b.formLook;
        List<GuideSceneRenderer.Block> blocks = new ArrayList<>();
        if (finished) {
            for (GuideBuilds.Placed p : b.blocks) {
                BlockState state = formed ? formedState(p.state(), b) : p.state();
                blocks.add(new GuideSceneRenderer.Block(p.pos(), state, 0, formed ? pictureEntity(state) : null));
            }
        } else {
            for (int s = 0; s < sc.step; s++) {
                for (GuideBuilds.Placed p : b.steps.get(s).blocks()) blocks.add(new GuideSceneRenderer.Block(p.pos(), p.state(), 0, null));
            }
            // This step's blocks drop in one after another.
            List<GuideBuilds.Placed> list = b.steps.get(sc.step).blocks();
            float st = stagger(list.size());
            for (int i = 0; i < list.size(); i++) {
                float t = (sc.stepTime - i * st) / 0.3f;
                if (t <= 0) break;
                float e = 1 - (float) Math.pow(1 - Math.min(1, t), 3);
                GuideBuilds.Placed p = list.get(i);
                blocks.add(new GuideSceneRenderer.Block(p.pos(), p.state(), (1 - e) * 2.5f, null));
            }
        }
        g.submitPictureInPictureRenderState(new GuideSceneRenderer.State(blocks, fov, dist, sc.pitch, sc.yaw,
                (b.minX + b.maxX + 1) / 2f, (b.minY + b.maxY + 1) / 2f, (b.minZ + b.maxZ + 1) / 2f,
                viewX, viewY, viewX + viewW, viewY + viewH, null));
        g.nextStratum();

        // The forming flash: builds up as it's about to form, then fades.
        if (finished) {
            float t = sc.stepTime;
            float a = t < FORM_AT ? (t / FORM_AT) * 0.85f : Math.max(0, 0.85f * (1 - (t - FORM_AT) / FORM_FLASH));
            if (a > 0) g.fill(viewX, viewY, viewX + viewW, viewY + viewH, ((int) (a * 255) << 24) | 0xE6F6FF);
            if (t > FORM_AT && t < FORM_AT + FORM_FLASH) {
                // A few sparks around the view.
                java.util.Random r = new java.util.Random((long) (t * 20));
                for (int i = 0; i < 6; i++) {
                    float x0 = viewX + r.nextFloat() * viewW, y0 = viewY + r.nextFloat() * viewH;
                    GuideSky.line(g, x0, y0, x0 + (r.nextFloat() - 0.5f) * 30, y0 + (r.nextFloat() - 0.5f) * 30, 1, 0xFFBFE6FF);
                }
            }
        }
    }

    // A block as it looks once its structure has formed (blocks with a "formed" state).
    private static BlockState formedState(BlockState state, GuideBuilds.Build b) {
        // The Shock Farm controller draws the merged farm for its size.
        if (b.farmSize > 0 && state.getBlock() instanceof com.sideflowgames.fulguritefusion.farm.ShockFarmControllerBlock) {
            return state.setValue(com.sideflowgames.fulguritefusion.farm.ShockFarmControllerBlock.SIZE, b.farmSize);
        }
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("formed") && prop instanceof BooleanProperty bool) return state.setValue(bool, true);
        }
        return state;
    }

    // Formed machines are drawn by their block entity (their big model), so make one just for the picture.
    private static final Map<BlockState, BlockEntity> PICTURE_ENTITIES = new HashMap<>();

    private BlockEntity pictureEntity(BlockState state) {
        if (!(state.getBlock() instanceof com.sideflowgames.fulguritefusion.block.MachineControllerBlock
                || state.getBlock() instanceof com.sideflowgames.fulguritefusion.block.MiniColliderBlock
                || state.getBlock() instanceof com.sideflowgames.fulguritefusion.block.StormColliderBlock
                || state.getBlock() instanceof com.sideflowgames.fulguritefusion.farm.ShockFarmControllerBlock)) return null;
        var block = (net.minecraft.world.level.block.BaseEntityBlock) state.getBlock();
        try {
            BlockEntity be = PICTURE_ENTITIES.computeIfAbsent(state, s -> block.newBlockEntity(BlockPos.ZERO, s));
            if (be == null || minecraft == null) return null;
            if (be.getLevel() == null) be.setLevel(minecraft.level);
            return be;
        } catch (Exception e) {
            return null; // A picture isn't worth a crash.
        }
    }

    // ---------- Right: the text ----------

    private void drawText(GuiGraphicsExtractor g, GuideData.Section sec, int mx, int my) {
        float ts = TEXT_SIZES[textSize];
        int x = rightX + 10, y = top + 10, w = rightW - 24, bottom = top + panelH - 10;
        // Section title.
        g.pose().pushMatrix();
        g.pose().translate((float) (x), (float) (y));
        g.pose().scale((float) (ts * 1.3f), (float) (ts * 1.3f));
        g.text(font, font.plainSubstrByWidth(sec.title, (int) (w / (ts * 1.3f))), 0, 0, GuiStyle.opaque(TITLE), false);
        g.pose().popMatrix();
        y += (int) (14 * ts * 1.3f);
        g.fill(x, y - 3, x + w, y - 2, 0x601E4E79);

        List<FormattedCharSequence> lines = new ArrayList<>();
        int wrapW = (int) (w / ts);
        for (String para : sec.text.split("\n", -1)) {
            if (para.isEmpty()) lines.add(FormattedCharSequence.EMPTY);
            else lines.addAll(font.split(Component.literal(para), wrapW));
        }
        float lineH = 11 * ts;
        int viewTop = y + 2;
        int total = (int) (lines.size() * lineH);
        textMax = Math.max(0, total - (bottom - viewTop));
        textScroll = Mth.clamp(textScroll, 0, textMax);
        g.enableScissor(x, viewTop, x + w + 8, bottom);
        g.pose().pushMatrix();
        g.pose().translate((float) (x), (float) (viewTop - textScroll));
        g.pose().scale((float) (ts), (float) (ts));
        for (int i = 0; i < lines.size(); i++) {
            float ly = i * 11;
            if ((ly + 11) * ts < textScroll || ly * ts > textScroll + (bottom - viewTop)) continue;
            g.text(font, lines.get(i), 0, (int) ly, GuiStyle.opaque(GuiStyle.TEXT), false);
        }
        g.pose().popMatrix();
        g.disableScissor();
        if (textMax > 0) {
            // Scroll bar.
            int barX = rightX + rightW - 8, barH = bottom - viewTop;
            int knob = Math.max(12, barH * barH / total);
            int ky = viewTop + (int) ((barH - knob) * (textScroll / textMax));
            g.fill(barX, viewTop, barX + 3, bottom, 0x30000000);
            g.fill(barX, ky, barX + 3, ky + knob, 0xFF5AB4FF);
        }
        // Next / previous section, at the bottom of the text.
        if (chapter.sections.size() > 1 && section < chapter.sections.size() - 1 && textScroll >= textMax) {
            String next = "Next: " + chapter.sections.get(section + 1).title + " ▶";
            int nx = rightX + rightW - 10 - font.width(next), ny = bottom - 9;
            boolean over = mx >= nx && my >= ny - 2 && my < ny + 10;
            if (total + 14 < bottom - viewTop || textMax > 0) g.text(font, next, nx, ny, GuiStyle.opaque(over ? 0xFF2A6FB0 : FAINT), false);
        }
    }

    // ---------- Input ----------

    private ItemStack clickableAt(double mx, double my) {
        for (Clickable c : clickables) {
            if (mx >= c.x0() && mx < c.x1() && my >= c.y0() && my < c.y1()) return c.item();
        }
        return null;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (super.mouseClicked(event, doubleClick)) return true;
        if (button == 1) {
            onClose();
            return true;
        }
        if (button != 0) return false;
        // Tabs.
        if (chapter.sections.size() > 1 && my >= 34 && my < 52) {
            if (mx < 20 && tabScroll > 0) tabScroll--;
            else if (mx >= width - 20) {
                List<Tab> t = tabs();
                if (!t.isEmpty() && t.get(t.size() - 1).index() < chapter.sections.size() - 1) tabScroll++;
            } else {
                for (Tab t : tabs()) if (mx >= t.x() && mx < t.x() + t.w()) select(t.index());
            }
            return true;
        }
        // Recipe trail: Back, and clickable items.
        if (!recipeTrail.isEmpty() && mx >= leftX + 8 && mx < leftX + 48 && my >= top + 8 && my < top + 22) {
            recipeTrail.remove(recipeTrail.size() - 1);
            return true;
        }
        ItemStack link = clickableAt(mx, my);
        if (link != null) {
            openRecipe(link);
            return true;
        }
        GuideData.Section sec = current();
        if (sec != null && sec.build != null && recipeTrail.isEmpty()) {
            if (mx >= viewX && mx < viewX + viewW && my >= viewY && my < viewY + viewH) {
                rotating = true;
                return true;
            }
            Scene sc = scene(sec.build);
            int steps = GuideBuilds.get(sec.build).steps.size();
            int by = ctlY + 20;
            if (my >= by && my < by + 14) {
                if (mx >= viewX && mx < viewX + 18) {
                    sc.step = Math.max(0, sc.step - 1);
                    sc.stepTime = 0;
                    sc.playing = false;
                } else if (mx >= viewX + 20 && mx < viewX + 38) {
                    sc.playing = !sc.playing;
                    if (sc.playing && sc.step >= steps) sc.step = 0;
                } else if (mx >= viewX + 40 && mx < viewX + 58) {
                    sc.step = Math.min(steps, sc.step + 1);
                    sc.stepTime = 0;
                    sc.playing = false;
                } else if (mx >= viewX + 60 && mx < viewX + 78) {
                    sc.step = 0;
                    sc.stepTime = 0;
                    sc.playing = true;
                }
                return true;
            }
        }
        // "Next: ..." link under the text.
        int bottom = top + panelH - 10;
        if (mx >= rightX && mx < rightX + rightW && my >= bottom - 11 && my < bottom + 2
                && section < chapter.sections.size() - 1 && textScroll >= textMax) {
            select(section + 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        GuideData.Section sec = current();
        if (button == 0 && rotating && sec != null && sec.build != null) {
            Scene sc = scene(sec.build);
            sc.yaw += (float) dx * 1.5f;
            sc.pitch = Mth.clamp(sc.pitch + (float) dy * 1.5f, -89, 89);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (button == 0) rotating = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        GuideData.Section sec = current();
        if (sec != null && sec.build != null && mx >= viewX && mx < viewX + viewW && my >= viewY && my < viewY + viewH) {
            Scene sc = scene(sec.build);
            sc.zoom = Mth.clamp(sc.zoom * (float) Math.pow(1.2, scrollY), 0.5f, 16f);
            return true;
        }
        if (chapter.sections.size() > 1 && my >= 34 && my < 52) {
            tabScroll = Mth.clamp(tabScroll - (int) Math.signum(scrollY), 0, chapter.sections.size() - 1);
            return true;
        }
        textScroll = Mth.clamp(textScroll - (float) scrollY * 22, 0, textMax);
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key(), scan = event.scancode(), mods = event.modifiers();
        switch (key) {
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> {
                select(section + 1);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> {
                select(section - 1);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                textScroll = Mth.clamp(textScroll + 22, 0, textMax);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                textScroll = Mth.clamp(textScroll - 22, 0, textMax);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                onClose();
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }
}
