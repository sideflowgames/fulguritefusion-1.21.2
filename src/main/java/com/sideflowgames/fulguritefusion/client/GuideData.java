package com.sideflowgames.fulguritefusion.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sideflowgames.fulguritefusion.FulguriteFusion;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The guide's contents, read from assets/fulguritefusion/guide/guide.json:
//
//   {"intro": {"title": "...", "text": "...", "pos": [x, y]},
//    "chapters": [{"id": "storm_collider", "title": "...", "icon": "mod:item", "pos": [x, y], "parent": "mini_collider",
//                  "unlock": ["mod:item", ...],          (optional; default: the icon and the mod's items on its pages)
//                  "pages": [{"title": "...", "items": [...], "recipe": "mod:recipe", "text": "...",
//                             "build": "storm_collider"}]}]}         ("build": a 3D build guide, see GuideBuilds)
//
// Each titled page is a section of its topic's screen.
public final class GuideData {
    public static final ResourceLocation CONTENT = ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "guide/guide.json");
    public static final String START = "start";
    // One section of a topic: what the left side shows (items, recipes, or a step-by-step build) and the text
    // for the right side. A page without a title adds its items and recipes to the section before it.
    public static final class Section {
        public String title;
        public final List<ItemStack> items = new ArrayList<>();
        public final List<String> recipes = new ArrayList<>();
        public String text = "";
        public String build;
    }

    public static final class Chapter {
        public String id, title, parent;
        public ItemStack icon = ItemStack.EMPTY;
        public float x, y;
        public final Set<Item> unlock = new HashSet<>();
        public final List<Section> sections = new ArrayList<>();
    }

    public final Map<String, Chapter> chapters = new LinkedHashMap<>();
    public String error;

    private static GuideData cached;

    public static GuideData get() {
        if (cached == null) cached = load();
        return cached;
    }

    // Re-read (the guide is re-read each time it opens, so resource reloads show up).
    public static GuideData reload() {
        cached = load();
        return cached;
    }

    private static GuideData load() {
        GuideData data = new GuideData();
        JsonObject root = null;
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(CONTENT);
            if (res.isPresent()) {
                try (Reader reader = res.get().openAsReader()) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                }
            }
        } catch (Exception e) {
            FulguriteFusion.LOGGER.error("Couldn't read the guide", e);
        }
        Chapter start = new Chapter();
        start.id = START;
        start.icon = new ItemStack(Items.LIGHTNING_ROD);
        data.chapters.put(START, start);
        if (root == null) {
            data.error = "The guide (guide/guide.json) couldn't be loaded.";
            start.title = "Guide missing";
            start.sections.add(section(start.title, data.error));
            return data;
        }
        JsonObject intro = root.has("intro") ? root.getAsJsonObject("intro") : new JsonObject();
        start.title = str(intro, "title", "Fulgurite Fusion");
        float[] sp = pos(intro);
        start.x = sp[0];
        start.y = sp[1];
        start.sections.add(section(start.title, str(intro, "text", "")));

        for (JsonElement ce : root.getAsJsonArray("chapters")) {
            JsonObject cj = ce.getAsJsonObject();
            Chapter c = new Chapter();
            c.title = str(cj, "title", "?");
            c.id = str(cj, "id", c.title.toLowerCase().replaceAll("[^a-z0-9]+", "_"));
            c.parent = str(cj, "parent", START);
            c.icon = item(str(cj, "icon", "minecraft:book"));
            float[] p = pos(cj);
            c.x = p[0];
            c.y = p[1];
            if (cj.has("unlock")) {
                for (JsonElement u : cj.getAsJsonArray("unlock")) {
                    ItemStack s = item(u.getAsString());
                    if (!s.isEmpty()) c.unlock.add(s.getItem());
                }
            } else if (!c.icon.isEmpty()) {
                c.unlock.add(c.icon.getItem());
            }
            for (JsonElement pe : cj.getAsJsonArray("pages")) {
                JsonObject pj = pe.getAsJsonObject();
                Section sec;
                if (pj.has("title") || c.sections.isEmpty()) {
                    sec = new Section();
                    sec.title = str(pj, "title", c.title);
                    c.sections.add(sec);
                } else {
                    sec = c.sections.get(c.sections.size() - 1);
                }
                if (pj.has("items")) for (JsonElement ie : pj.getAsJsonArray("items")) {
                    String id = ie.getAsString();
                    ItemStack s = item(id);
                    if (s.isEmpty()) continue;
                    if (sec.items.stream().noneMatch(o -> o.is(s.getItem()))) sec.items.add(s);
                    if (!cj.has("unlock") && id.startsWith(FulguriteFusion.MODID + ":")) c.unlock.add(s.getItem());
                }
                if (pj.has("recipe")) sec.recipes.add(pj.get("recipe").getAsString());
                String text = str(pj, "text", "");
                if (!text.isEmpty()) sec.text = sec.text.isEmpty() ? text : sec.text + "\n\n" + text;
                if (pj.has("build")) sec.build = pj.get("build").getAsString();
            }
            data.chapters.put(c.id, c);
        }
        return data;
    }

    private static Section section(String title, String text) {
        Section sec = new Section();
        sec.title = title;
        sec.text = text;
        return sec;
    }

    private static float[] pos(JsonObject o) {
        if (!o.has("pos")) return new float[]{0, 0};
        JsonArray a = o.getAsJsonArray("pos");
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat()};
    }

    private static String str(JsonObject o, String key, String fallback) {
        return o.has(key) ? o.get(key).getAsString() : fallback;
    }

    public static ItemStack item(String id) {
        try {
            ResourceLocation rl = ResourceLocation.parse(id);
            if (!BuiltInRegistries.ITEM.containsKey(rl)) return ItemStack.EMPTY;
            return new ItemStack(BuiltInRegistries.ITEM.get(rl));
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
