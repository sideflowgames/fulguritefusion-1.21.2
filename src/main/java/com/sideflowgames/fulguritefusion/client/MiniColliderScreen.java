package com.sideflowgames.fulguritefusion.client;

import java.util.List;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.block.ColliderInputBlockEntity;
import com.sideflowgames.fulguritefusion.block.MiniColliderBlockEntity;
import com.sideflowgames.fulguritefusion.menu.MiniColliderMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// Mini Collider: ring map on the left, Spark Cells slot, shot progress, output info and the Fm bar.
public class MiniColliderScreen extends MachineScreen<MiniColliderMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/gui/mini_collider.png");
    static final int RING_X = 8, RING_Y = 18;
    static final int SHOT_X = 86, SHOT_Y = 18, SHOT_W = 4, SHOT_H = 49;
    private static final int TEXT = 0xFF26262C, GOOD = 0xFF1E7A3A, BAD = 0xFFB02020;

    public MiniColliderScreen(MiniColliderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
        inventoryLabelY = 10000; // the status line takes its place
    }

    @Override
    protected boolean hasSideConfig() {
        return false;
    }

    @Override
    protected void renderMachine(GuiGraphics g, float partialTick) {
        ColliderRingWidget.draw(g, leftPos + RING_X, topPos + RING_Y, menu.getRingMask(), menu.getShot(), menu.getShotLength());
        drawShotBar(g, leftPos + SHOT_X, topPos + SHOT_Y, menu.getShot(), menu.getShotLength());
        drawPowerBar(g, menu.getFm(), menu.getMaxFm());
    }

    // Vertical bar that fills while a shot is in flight.
    static void drawShotBar(GuiGraphics g, int x, int y, int shot, int length) {
        g.fill(x - 1, y - 1, x + SHOT_W + 1, y + SHOT_H + 1, 0xFF14233A);
        g.fill(x, y, x + SHOT_W, y + SHOT_H, 0xFF1B2E48);
        if (shot >= 0 && length > 0) {
            int h = Math.min(SHOT_H, shot * SHOT_H / length);
            g.fill(x, y + SHOT_H - h, x + SHOT_W, y + SHOT_H, 0xFF8FE3FF);
        }
    }

    private String stateText() {
        return switch (menu.getState()) {
            case MiniColliderBlockEntity.S_RING -> "Ring incomplete";
            case MiniColliderBlockEntity.S_FLIGHT -> "Cells in flight";
            case MiniColliderBlockEntity.S_RELEASING -> "Releasing power";
            case MiniColliderBlockEntity.S_FULL -> "Full: draw power off the top";
            case MiniColliderBlockEntity.S_CELLS -> "Needs 2 Spark Cells";
            default -> "Ready";
        };
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, menu.getCells() + "/" + ColliderInputBlockEntity.MAX_CELLS, 64, 55, TEXT, false);
        g.drawString(font, menu.getRate() > 0 && menu.getPending() > 0 ? menu.getRate() + " Fm/t" : "-", 96, 22, TEXT, false);
        g.drawString(font, menu.getPending() + " left", 96, 36, TEXT, false);
        int state = menu.getState();
        boolean bad = state == MiniColliderBlockEntity.S_RING || state == MiniColliderBlockEntity.S_CELLS;
        g.drawString(font, stateText(), 8, 72, bad ? BAD : state == MiniColliderBlockEntity.S_IDLE ? GOOD : TEXT, false);
    }

    @Override
    protected void renderExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
        powerTooltip(g, mouseX, mouseY, menu.getFm(), menu.getMaxFm());
        List<Component> ring = ColliderRingWidget.tooltip(mouseX - leftPos - RING_X, mouseY - topPos - RING_Y,
                menu.getRingMask(), menu.isComplete(), "Mini Collider");
        if (ring != null) g.renderComponentTooltip(font, ring, mouseX, mouseY);
        if (isHovering(SHOT_X - 1, SHOT_Y - 1, SHOT_W + 2, SHOT_H + 2, mouseX, mouseY)) {
            g.renderTooltip(font, Component.literal(menu.getShot() >= 0 ? "Shot in flight" : "No shot in flight"), mouseX, mouseY);
        }
        if (isHovering(96, 20, 52, 26, mouseX, mouseY)) {
            g.renderComponentTooltip(font, List.of(Component.literal("Release rate and Fm still to come"),
                    Component.literal("from the last collision").withColor(0xAAAAAA)), mouseX, mouseY);
        }
        if (menu.slots.get(0).getItem().isEmpty() && isHovering(MiniColliderMenu.CELLS_X - 1, MiniColliderMenu.CELLS_Y - 1, 18, 18, mouseX, mouseY)) {
            g.renderTooltip(font, Component.literal("Spark Cells (2 per shot)"), mouseX, mouseY);
        }
    }
}
