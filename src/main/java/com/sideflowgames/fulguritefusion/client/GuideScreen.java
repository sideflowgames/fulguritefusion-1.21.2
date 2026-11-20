package com.sideflowgames.fulguritefusion.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sideflowgames.fulguritefusion.FulguriteFusion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.lwjgl.glfw.GLFW;

// The Fulgurite Fusion Guide: a two-page book. Its contents come from assets/fulguritefusion/guide/guide.json
// (re-read every time the book opens, so a resource reload picks up edits):
//
//   {"intro": {"title": "...", "text": "..."},
//    "chapters": [{"title": "...", "icon": "fulguritefusion:raw_fulgurite",
//                  "pages": [{"title": "...", "items": ["mod:item", ...], "recipe": "mod:recipe_id", "text": "..."}]}]}
//
// Text wraps to the page and runs on to as many pages as it needs; "\n" starts a new line, a blank line a gap.
// Every chapter starts on a fresh spread. Contents are on the first spread; click a chapter to jump to it.
public class GuideScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/gui/guide_book.png");
    private static final ResourceLocation CONTENT = ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "guide/guide.json");
    private static final int BOOK_W = 288, BOOK_H = 184, TEX_W = 512, TEX_H = 256;
    private static final int PAGE_W = 116, PAGE_H = 146, LEFT_X = 18, RIGHT_X = 154, PAGE_Y = 14, LINE = 10;
    private static final int TEXT = 0xFF3B2F2F, TITLE = 0xFF1E4E79, CHAPTER = 0xFF6A1B9A, FAINT = 0xFF8A7A66, LINK = 0xFF1E4E79;
    private static int lastSpread; // reopen where you left off

    private record Chapter(String title, ItemStack icon, int firstPage) {}

    private static final class Page {
        String title;              // section title (first page of a section)
        String chapterTitle;       // big title (first page of a chapter)
        List<ItemStack> items = List.of();
        String recipe;             // recipe id (first page of a section)
        List<FormattedCharSequence> lines = new ArrayList<>();
        boolean contents;          // the table of contents
    }

    private final List<Page> pages = new ArrayList<>();
    private final List<Chapter> chapters = new ArrayList<>();
    private int spread;
    private int left, top;

    private GuideScreen() {
        super(Component.translatable("item.fulguritefusion.guide_book"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GuideScreen());
    }

    // ---------- Building the pages ----------

    @Override
    protected void init() {
        left = (width - BOOK_W) / 2;
        top = Math.max(2, (height - BOOK_H) / 2 - 8);
        if (pages.isEmpty()) build();
        spread = Math.min(lastSpread, spreads() - 1);
        addRenderableWidget(new PageButton(left + BOOK_W - 44, top + BOOK_H - 22, true, b -> turn(1), true));
        addRenderableWidget(new PageButton(left + 20, top + BOOK_H - 22, false, b -> turn(-1), true));
    }

    private void build() {
        JsonObject root = null;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(CONTENT);
            if (res.isPresent()) {
                try (Reader reader = res.get().openAsReader()) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                }
            }
        } catch (Exception e) {
            FulguriteFusion.LOGGER.error("Couldn't read the guide book", e);
        }
        if (root == null) {
            Page p = new Page();
            p.title = "Guide missing";
            p.lines.addAll(wrap("The guide's pages (guide/guide.json) couldn't be loaded."));
            pages.add(p);
            return;
        }

        // Spread 0: introduction on the left, contents on the right.
        JsonObject intro = root.has("intro") ? root.getAsJsonObject("intro") : new JsonObject();
        Page first = new Page();
        first.chapterTitle = str(intro, "title", "Fulgurite Fusion");
        first.lines.addAll(wrap(str(intro, "text", "")));
        pages.add(first);
        Page contents = new Page();
        contents.contents = true;
        contents.title = "Contents";
        pages.add(contents);

        JsonArray chs = root.has("chapters") ? root.getAsJsonArray("chapters") : new JsonArray();
        for (JsonElement ce : chs) {
            JsonObject ch = ce.getAsJsonObject();
            if (pages.size() % 2 == 1) pages.add(new Page()); // chapters start on a fresh spread
            String chTitle = str(ch, "title", "?");
            chapters.add(new Chapter(chTitle, item(str(ch, "icon", "minecraft:book")), pages.size()));
            boolean firstOfChapter = true;
            for (JsonElement pe : ch.has("pages") ? ch.getAsJsonArray("pages") : new JsonArray()) {
                JsonObject pj = pe.getAsJsonObject();
                Page p = new Page();
                if (firstOfChapter) p.chapterTitle = chTitle;
                firstOfChapter = false;
                p.title = pj.has("title") ? pj.get("title").getAsString() : null;
                List<ItemStack> items = new ArrayList<>();
                if (pj.has("items")) for (JsonElement ie : pj.getAsJsonArray("items")) {
                    ItemStack s = item(ie.getAsString());
                    if (!s.isEmpty()) items.add(s);
                }
                p.items = items;
                p.recipe = pj.has("recipe") ? pj.get("recipe").getAsString() : null;
                // Fill this page's space, then run on to more pages.
                List<FormattedCharSequence> lines = wrap(str(pj, "text", ""));
                int room = (PAGE_H - headerHeight(p)) / LINE;
                int i = 0;
                p.lines.addAll(lines.subList(0, Math.min(Math.max(0, room), lines.size())));
                i = p.lines.size();
                pages.add(p);
                int perPage = PAGE_H / LINE;
                while (i < lines.size()) {
                    Page more = new Page();
                    more.lines.addAll(lines.subList(i, Math.min(lines.size(), i + perPage)));
                    i += more.lines.size();
                    pages.add(more);
                }
            }
        }
    }

    private int headerHeight(Page p) {
        int h = 0;
        if (p.chapterTitle != null) h += 20;
        if (p.title != null) h += 13;
        if (!p.items.isEmpty()) h += 20 * ((p.items.size() + 5) / 6);
        if (p.recipe != null) h += 62;
        return h;
    }

    private List<FormattedCharSequence> wrap(String text) {
        List<FormattedCharSequence> out = new ArrayList<>();
        if (text.isEmpty()) return out;
        for (String para : text.split("\n", -1)) {
            if (para.isEmpty()) {
                out.add(FormattedCharSequence.EMPTY);
                continue;
            }
            out.addAll(font.split(Component.literal(para), PAGE_W));
        }
        return out;
    }

    private static String str(JsonObject o, String key, String fallback) {
        return o.has(key) ? o.get(key).getAsString() : fallback;
    }

    private static ItemStack item(String id) {
        try {
            ResourceLocation rl = ResourceLocation.parse(id);
            if (!BuiltInRegistries.ITEM.containsKey(rl)) return ItemStack.EMPTY;
            return new ItemStack(BuiltInRegistries.ITEM.get(rl));
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    // ---------- Navigation ----------

    private int spreads() {
        return Math.max(1, (pages.size() + 1) / 2);
    }

    private void turn(int by) {
        spread = Math.max(0, Math.min(spreads() - 1, spread + by));
        lastSpread = spread;
    }

    private void jumpToPage(int page) {
        spread = page / 2;
        lastSpread = spread;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            turn(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP) {
            turn(-1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_HOME) {
            jumpToPage(0);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy != 0) turn(sy < 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            // "Contents" link under the book.
            if (spread != 0 && overContentsLink(mx, my)) {
                jumpToPage(0);
                return true;
            }
            // Chapter links on the contents page.
            for (int side = 0; side < 2; side++) {
                int index = spread * 2 + side;
                if (index >= pages.size() || !pages.get(index).contents) continue;
                int x = left + (side == 0 ? LEFT_X : RIGHT_X), y = top + PAGE_Y + 15;
                for (int c = 0; c < chapters.size(); c++) {
                    int ry = y + c * 13;
                    if (mx >= x && mx < x + PAGE_W && my >= ry && my < ry + 13) {
                        jumpToPage(chapters.get(c).firstPage());
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean overContentsLink(double mx, double my) {
        int w = font.width("Contents");
        int x = left + BOOK_W / 2 - w / 2, y = top + BOOK_H - 14;
        return mx >= x - 2 && mx <= x + w + 2 && my >= y - 2 && my <= y + 10;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------- Drawing ----------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.blit(TEXTURE, left, top, 0, 0, BOOK_W, BOOK_H, TEX_W, TEX_H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        ItemStack[] hovered = {ItemStack.EMPTY};
        for (int side = 0; side < 2; side++) {
            int index = spread * 2 + side;
            if (index >= pages.size()) continue;
            int x = left + (side == 0 ? LEFT_X : RIGHT_X), y = top + PAGE_Y;
            drawPage(g, pages.get(index), x, y, mouseX, mouseY, hovered);
            String num = String.valueOf(index + 1);
            g.drawString(font, num, x + PAGE_W / 2 - font.width(num) / 2, top + BOOK_H - 24, FAINT, false);
        }
        if (spread != 0) {
            int w = font.width("Contents");
            boolean over = overContentsLink(mouseX, mouseY);
            g.drawString(font, "Contents", left + BOOK_W / 2 - w / 2, top + BOOK_H - 14, over ? LINK : FAINT, false);
        }
        if (!hovered[0].isEmpty()) g.renderTooltip(font, hovered[0], mouseX, mouseY);
    }

    private void drawPage(GuiGraphics g, Page p, int x, int y, int mx, int my, ItemStack[] hovered) {
        if (p.contents) {
            g.drawString(font, "Contents", x, y, CHAPTER, false);
            g.fill(x, y + 10, x + PAGE_W, y + 11, CHAPTER);
            int ry = y + 15;
            for (Chapter c : chapters) {
                boolean over = mx >= x && mx < x + PAGE_W && my >= ry && my < ry + 13;
                g.pose().pushPose();
                g.pose().translate(x, ry - 1, 0);
                g.pose().scale(0.75f, 0.75f, 1);
                g.renderItem(c.icon(), 0, 0);
                g.pose().popPose();
                g.drawString(font, font.plainSubstrByWidth(c.title(), PAGE_W - 16), x + 15, ry + 2, over ? LINK : TEXT, false);
                ry += 13;
            }
            return;
        }
        if (p.chapterTitle != null) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(1.15f, 1.15f, 1);
            g.drawString(font, font.plainSubstrByWidth(p.chapterTitle, (int) (PAGE_W / 1.15f)), 0, 0, CHAPTER, false);
            g.pose().popPose();
            g.fill(x, y + 12, x + PAGE_W, y + 13, CHAPTER);
            y += 20;
        }
        if (p.title != null) {
            g.drawString(font, font.plainSubstrByWidth(p.title, PAGE_W), x, y, TITLE, false);
            y += 13;
        }
        if (!p.items.isEmpty()) {
            for (int i = 0; i < p.items.size(); i++) {
                int ix = x + (i % 6) * 19, iy = y + (i / 6) * 20;
                g.renderItem(p.items.get(i), ix, iy);
                if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) hovered[0] = p.items.get(i);
            }
            y += 20 * ((p.items.size() + 5) / 6);
        }
        if (p.recipe != null) {
            drawRecipe(g, p.recipe, x, y, mx, my, hovered);
            y += 62;
        }
        for (FormattedCharSequence line : p.lines) {
            g.drawString(font, line, x, y, TEXT, false);
            y += LINE;
        }
    }

    // A crafting grid: 3 x 3 ingredients, an arrow, and the result. Ingredients with several options cycle.
    private void drawRecipe(GuiGraphics g, String id, int x, int y, int mx, int my, ItemStack[] hovered) {
        if (minecraft == null || minecraft.level == null) return;
        Optional<RecipeHolder<?>> holder;
        try {
            holder = minecraft.level.getRecipeManager().byKey(ResourceLocation.parse(id));
        } catch (Exception e) {
            holder = Optional.empty();
        }
        if (holder.isEmpty()) {
            g.drawString(font, "(recipe not found)", x, y + 22, FAINT, false);
            return;
        }
        Recipe<?> recipe = holder.get().value();
        NonNullList<Ingredient> ings = recipe.getIngredients();
        int w = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        int gx = x + 4, gy = y + 2;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                slot(g, gx + c * 18, gy + r * 18);
            }
        }
        long tick = minecraft.level.getGameTime() / 20;
        for (int i = 0; i < ings.size(); i++) {
            int r, c;
            if (recipe instanceof ShapedRecipe) {
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
            ItemStack[] options = ings.get(i).getItems();
            if (options.length == 0) continue;
            ItemStack s = options[(int) (tick % options.length)];
            int ix = gx + c * 18 + 1, iy = gy + r * 18 + 1;
            g.renderItem(s, ix, iy);
            if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) hovered[0] = s;
        }
        // Arrow and result.
        int ax = gx + 58, ay = gy + 22;
        g.drawString(font, "→", ax, ay, TEXT, false);
        ItemStack out = recipe.getResultItem(minecraft.level.registryAccess());
        int ox = gx + 72, oy = gy + 17;
        slot(g, ox - 1, oy - 1);
        g.renderItem(out, ox, oy);
        g.renderItemDecorations(font, out, ox, oy);
        if (mx >= ox && mx < ox + 16 && my >= oy && my < oy + 16) hovered[0] = out;
    }

    private static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF8B7B60);
        g.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFF8E8);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFD9CBB0);
    }
}
