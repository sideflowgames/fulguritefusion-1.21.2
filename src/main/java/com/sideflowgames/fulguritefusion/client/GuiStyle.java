package com.sideflowgames.fulguritefusion.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

// The one look shared by every Fulgurite Fusion menu (it matches the machine GUI textures): light storm-blue
// panel with a bevelled edge, navy slots and dark text. Screens drawn in code use these.
public final class GuiStyle {
    private GuiStyle() {}

    public static final int PANEL = 0xFFA9DCF2, LIGHT = 0xFFE1F5FF, SHADOW = 0xFF6EA5C3;
    public static final int SLOT_EDGE = 0xFF142337, SLOT_FILL = 0xFF284664, SLOT_HIGHLIGHT = 0xFF96C8E1;
    public static final int DARK = 0xFF26262C;
    public static final int TEXT = 0xFF26262C, GOOD = 0xFF1E7A3A, BAD = 0xFFB02020;

    // 1.21.1 drew text with no alpha as fully opaque; 26.1 draws it invisible. Same rule as before.
    public static int opaque(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x + 1, y + 1, x + w - 3, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 3, LIGHT);
        g.fill(x + 3, y + h - 3, x + w, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h, SHADOW);
    }

    // x, y: where the slot's item is drawn (the frame is one pixel outside it).
    public static void slot(GuiGraphicsExtractor g, int x, int y, int fill) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        g.fill(x, y, x + 17, y + 17, SLOT_HIGHLIGHT);
        g.fill(x, y, x + 16, y + 16, fill);
    }

    public static void slot(GuiGraphicsExtractor g, int x, int y) {
        slot(g, x, y, SLOT_FILL);
    }
}
