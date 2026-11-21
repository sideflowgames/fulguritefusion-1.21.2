package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.ModDataComponents;
import com.sideflowgames.fulguritefusion.event.StormArmorEvents;
import com.sideflowgames.fulguritefusion.item.FmChargeable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// A small HUD panel just left of the XP bar: one tiny icon per worn Fm-charged armor piece (helmet, chest,
// legs, boots), each with its charge bar underneath. Under the helmet a second, thinner bar shows the hydrogen
// tank filling (white, turning red near full). Hidden when no charged armor is worn.
public final class ArmorChargeHud {
    private ArmorChargeHud() {}

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int CELL = 10;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) return;
        ItemStack[] pieces = new ItemStack[4];
        int count = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack s = player.getItemBySlot(SLOTS[i]);
            if (FmChargeable.is(s)) {
                pieces[i] = s;
                count++;
            }
        }
        if (count == 0) return;

        int width = 4 * CELL;
        int x0 = g.guiWidth() / 2 - 91 - 4 - width;
        int y0 = g.guiHeight() - 36;
        g.fill(x0 - 2, y0 - 2, x0 + width, y0 + 13, 0x66000000);
        for (int i = 0; i < 4; i++) {
            int x = x0 + i * CELL;
            ItemStack s = pieces[i];
            if (s == null) {
                g.fill(x + 3, y0 + 3, x + 5, y0 + 5, 0x55FFFFFF); // empty slot dot
                continue;
            }
            g.pose().pushPose();
            g.pose().translate(x, y0, 0);
            g.pose().scale(0.5f, 0.5f, 1f);
            g.renderItem(s, 0, 0);
            g.pose().popPose();
            float f = FmChargeable.fraction(s);
            int fill = Math.round(8 * f);
            int color = f <= 0 ? 0xFF555555 : f < 0.1f ? 0xFFFF4040 : 0xFF40B0FF;
            g.fill(x, y0 + 9, x + 8, y0 + 11, 0xFF101018);
            if (fill > 0) g.fill(x, y0 + 9, x + fill, y0 + 11, color);
            if (SLOTS[i] == EquipmentSlot.HEAD) {
                int h = s.getOrDefault(ModDataComponents.HYDROGEN.get(), 0);
                if (h > 0) {
                    float hf = Math.min(1f, h / (float) StormArmorEvents.HYDROGEN_MAX);
                    g.fill(x, y0 + 11, x + 8, y0 + 12, 0xFF101018);
                    g.fill(x, y0 + 11, x + Math.max(1, Math.round(8 * hf)), y0 + 12, hf > 0.85f ? 0xFFFF5050 : 0xFFE8E8F0);
                }
            }
        }
    }
}
