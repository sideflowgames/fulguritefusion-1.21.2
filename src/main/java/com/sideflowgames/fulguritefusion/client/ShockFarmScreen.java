package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.farm.ShockFarmControllerBlockEntity;
import com.sideflowgames.fulguritefusion.farm.ShockFarmMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

// Shock Farm Controller: power bar, status, farm size, mobs on the pad, the On/Off button, and the mobs this
// biome can spawn here (with their share of the spawns), the upgrade slots and the player's inventory.
// Mobs left out by a Mob Filter card are greyed out in the spawn list.
public class ShockFarmScreen extends AbstractContainerScreen<ShockFarmMenu> {
    private static final int W = 220, H = 262;
    private static final int BAR_X = 198, BAR_Y = 20, BAR_W = 12, BAR_H = 62;
    private static final int LIST_Y = 96, ROW = 20, ROWS = 3;
    private Button toggle;

    public ShockFarmScreen(ShockFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        toggle = addRenderableWidget(Button.builder(label(), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + 8, topPos + 68, 90, 18).build());
    }

    private Component label() {
        return Component.literal(menu.enabled() ? "Farm: ON" : "Farm: OFF");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (toggle != null) toggle.setMessage(label());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        GuiStyle.panel(g, leftPos, topPos, imageWidth, imageHeight);
        // Power bar.
        int x = leftPos + BAR_X, y = topPos + BAR_Y;
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, GuiStyle.SLOT_EDGE);
        g.fill(x, y, x + BAR_W, y + BAR_H, GuiStyle.SLOT_FILL);
        int cap = Math.max(1, menu.capacity());
        int h = (int) ((long) menu.fm() * BAR_H / cap);
        if (h > 0) {
            g.fill(x, y + BAR_H - h, x + BAR_W, y + BAR_H, 0xFF4CC8F0);
            g.fill(x + 2, y + BAR_H - h, x + 4, y + BAR_H, 0xFFB4ECFF);
        }
        // Upgrade slots; locked ones (5x5 farm) are dimmed.
        for (int i = 0; i < ShockFarmMenu.UPGRADE_POS.length; i++) {
            int sx = leftPos + ShockFarmMenu.UPGRADE_POS[i][0], sy = topPos + ShockFarmMenu.UPGRADE_POS[i][1];
            if (menu.isLocked(i)) {
                GuiStyle.slot(g, sx, sy, 0xFF2A2C34);
                if (menu.upgrade(i).isEmpty()) {
                    g.fill(sx + 5, sy + 7, sx + 11, sy + 12, 0xFF6A6E7A);
                    g.fill(sx + 6, sy + 4, sx + 10, sy + 5, 0xFF6A6E7A);
                    g.fill(sx + 6, sy + 4, sx + 7, sy + 7, 0xFF6A6E7A);
                    g.fill(sx + 9, sy + 4, sx + 10, sy + 7, 0xFF6A6E7A);
                }
            } else {
                GuiStyle.slot(g, sx, sy);
            }
        }
        // Player inventory.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) GuiStyle.slot(g, leftPos + ShockFarmMenu.INV_X + col * 18, topPos + ShockFarmMenu.INV_Y + row * 18);
        }
        for (int col = 0; col < 9; col++) GuiStyle.slot(g, leftPos + ShockFarmMenu.INV_X + col * 18, topPos + ShockFarmMenu.INV_Y + 58);
        // Spawn list slots.
        List<ShockFarmMenu.SpawnEntry> spawns = menu.spawns();
        int shown = Math.min(spawns.size(), ROWS * 2);
        for (int i = 0; i < shown; i++) {
            GuiStyle.slot(g, leftPos + 9 + (i % 2) * 104, topPos + LIST_Y + 12 + (i / 2) * ROW);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, 8, 6, GuiStyle.opaque(GuiStyle.TEXT), false);
        int status = menu.status();
        String text = switch (status) {
            case ShockFarmControllerBlockEntity.ST_RUNNING -> "Running";
            case ShockFarmControllerBlockEntity.ST_OFF -> "Switched off";
            case ShockFarmControllerBlockEntity.ST_NO_POWER -> "Needs power";
            case ShockFarmControllerBlockEntity.ST_FULL -> "Collector full";
            case ShockFarmControllerBlockEntity.ST_NOTHING -> "Nothing spawns here";
            case ShockFarmControllerBlockEntity.ST_FILTER -> "Filter mob not here";
            default -> "Not formed";
        };
        int color = status == ShockFarmControllerBlockEntity.ST_RUNNING ? GuiStyle.GOOD
                : status == ShockFarmControllerBlockEntity.ST_OFF ? GuiStyle.TEXT : GuiStyle.BAD;
        g.text(font, "Status: ", 8, 20, GuiStyle.opaque(GuiStyle.TEXT), false);
        g.text(font, text, 8 + font.width("Status: "), 20, GuiStyle.opaque(color), false);
        if (menu.tier() > 0) {
            g.text(font, "Size: " + (menu.tier() == 2 ? "7x7" : "5x5"), 8, 32, GuiStyle.opaque(GuiStyle.TEXT), false);
            g.text(font, "Mobs on pad: " + menu.mobs() + " / " + menu.mobCap(), 8, 44, GuiStyle.opaque(GuiStyle.TEXT), false);
            g.text(font, menu.spawnCost() + " Fm / spawn, " + String.format("%.1f", menu.spawnInterval() / 20f) + "s", 8, 56, GuiStyle.opaque(GuiStyle.TEXT), false);
        } else if (!menu.problem().isEmpty()) {
            List<FormattedCharSequence> lines = font.split(Component.literal(menu.problem()), 132);
            for (int i = 0; i < Math.min(3, lines.size()); i++) g.text(font, lines.get(i), 8, 32 + i * 10, GuiStyle.opaque(GuiStyle.BAD), false);
        }

        g.text(font, "Upgrades", 148, 8, GuiStyle.opaque(GuiStyle.TEXT), false);
        g.text(font, playerInventoryTitle, ShockFarmMenu.INV_X - 1, ShockFarmMenu.INV_Y - 11, GuiStyle.opaque(GuiStyle.TEXT), false);
        g.text(font, "Spawns in this biome:", 8, LIST_Y, GuiStyle.opaque(GuiStyle.TEXT), false);
        List<ShockFarmMenu.SpawnEntry> spawns = menu.spawns();
        if (spawns.isEmpty()) {
            g.text(font, "Nothing the farm can spawn.", 8, LIST_Y + 16, GuiStyle.opaque(GuiStyle.BAD), false);
            return;
        }
        java.util.Set<EntityType<?>> filter = filters();
        int total = 0;
        for (var e : spawns) {
            if (filter.isEmpty() || filter.contains(BuiltInRegistries.ENTITY_TYPE.getValue(e.id()))) total += e.weight();
        }
        int shown = Math.min(spawns.size(), ROWS * 2);
        for (int i = 0; i < shown; i++) {
            var e = spawns.get(i);
            int x = 9 + (i % 2) * 104, y = LIST_Y + 12 + (i / 2) * ROW;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(e.id());
            ItemStack egg = egg(type);
            if (!egg.isEmpty()) g.item(egg, x, y);
            boolean on = filter.isEmpty() || filter.contains(type);
            String name = type.getDescription().getString();
            int pct = on ? Math.round(e.weight() * 100f / Math.max(1, total)) : 0;
            String line = font.plainSubstrByWidth(name, 64);
            g.text(font, line, x + 20, y + 1, GuiStyle.opaque(on ? GuiStyle.TEXT : 0xFF8A8E96), false);
            g.text(font, on ? pct + "%" : "filtered out", x + 20, y + 10, GuiStyle.opaque(on ? 0xFF3A5A70 : 0xFF8A8E96), false);
            if (!on) g.fill(x, y, x + 16, y + 16, 0x90C6C6C6);
        }
        if (spawns.size() > shown) {
            g.text(font, "+" + (spawns.size() - shown) + " more", 150, LIST_Y, GuiStyle.opaque(GuiStyle.TEXT), false);
        }
    }

    // Mobs set on the Mob Filter cards in unlocked slots.
    private java.util.Set<EntityType<?>> filters() {
        java.util.Set<EntityType<?>> out = new java.util.HashSet<>();
        for (int i = 0; i < menu.unlockedSlots(); i++) {
            ItemStack st = menu.upgrade(i);
            if (st.getItem() instanceof com.sideflowgames.fulguritefusion.farm.FarmUpgradeItem u
                    && u.kind == com.sideflowgames.fulguritefusion.farm.FarmUpgradeItem.Kind.MOB_FILTER) {
                EntityType<?> t = com.sideflowgames.fulguritefusion.farm.FarmUpgradeItem.filterMob(st);
                if (t != null) out.add(t);
            }
        }
        return out;
    }

    private static ItemStack egg(EntityType<?> type) {
        return SpawnEggItem.byId(type).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (isHovering(BAR_X - 1, BAR_Y - 1, BAR_W + 2, BAR_H + 2, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, Component.literal(menu.fm() + " / " + menu.capacity() + " Fm"), mouseX, mouseY);
        }
        List<ShockFarmMenu.SpawnEntry> spawns = menu.spawns();
        int shown = Math.min(spawns.size(), ROWS * 2);
        for (int i = 0; i < shown; i++) {
            int x = 9 + (i % 2) * 104, y = LIST_Y + 12 + (i / 2) * ROW;
            if (isHovering(x, y, 96, 18, mouseX, mouseY)) {
                g.setTooltipForNextFrame(font, BuiltInRegistries.ENTITY_TYPE.getValue(spawns.get(i).id()).getDescription(), mouseX, mouseY);
            }
        }
        for (int i = 0; i < ShockFarmMenu.UPGRADE_POS.length; i++) {
            if (menu.isLocked(i) && menu.upgrade(i).isEmpty()
                    && isHovering(ShockFarmMenu.UPGRADE_POS[i][0], ShockFarmMenu.UPGRADE_POS[i][1], 16, 16, mouseX, mouseY)) {
                g.setTooltipForNextFrame(font, Component.literal("Unlocks on a 7x7 farm"), mouseX, mouseY);
            }
        }
    }
}
