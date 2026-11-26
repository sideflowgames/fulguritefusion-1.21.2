package com.sideflowgames.fulguritefusion.client;

import java.util.List;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.block.StormColliderBlockEntity;
import com.sideflowgames.fulguritefusion.menu.StormColliderMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Storm Collider: ring map, the two injector materials with the Spark Cells between them, shot progress,
// the 3x3 output grid and the Fm bar.
public class StormColliderScreen extends MachineScreen<StormColliderMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/gui/storm_collider.png");
    private static final int TEXT = 0xFF26262C, GOOD = 0xFF1E7A3A, BAD = 0xFFB02020;

    public StormColliderScreen(StormColliderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
        inventoryLabelY = 10000; // the status line takes its place
    }

    @Override
    protected boolean hasSideConfig() {
        return false;
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor g, float partialTick) {
        ColliderRingWidget.draw(g, leftPos + MiniColliderScreen.RING_X, topPos + MiniColliderScreen.RING_Y,
                menu.getRingMask(), menu.getShot(), menu.getShotLength());
        MiniColliderScreen.drawShotBar(g, leftPos + MiniColliderScreen.SHOT_X, topPos + MiniColliderScreen.SHOT_Y,
                menu.getShot(), menu.getShotLength());
        drawPowerBar(g, menu.getFm(), menu.getMaxFm());
    }

    private String stateText() {
        return switch (menu.getState()) {
            case StormColliderBlockEntity.S_RING -> "Ring incomplete";
            case StormColliderBlockEntity.S_FLIGHT -> "Pairing in flight";
            case StormColliderBlockEntity.S_POWER -> "Needs " + menu.getCost() + " Fm per shot";
            case StormColliderBlockEntity.S_LOAD -> "Load both injectors";
            case StormColliderBlockEntity.S_NO_PAIR -> "Those two can't be paired";
            case StormColliderBlockEntity.S_CELLS -> "Needs 2 Spark Cells";
            case StormColliderBlockEntity.S_FULL -> "Output full";
            default -> "Ready";
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        int state = menu.getState();
        boolean ok = state == StormColliderBlockEntity.S_READY || state == StormColliderBlockEntity.S_FLIGHT;
        g.text(font, stateText(), 8, 72, GuiStyle.opaque(state == StormColliderBlockEntity.S_READY ? GOOD : ok ? TEXT : BAD), false);
    }

    @Override
    protected void renderExtraTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovering(BAR_X - 2, BAR_Y - 2, BAR_W + 4, BAR_H + 4, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.literal(menu.getFm() + " / " + menu.getMaxFm() + " Fm"),
                    Component.literal(menu.getCost() + " Fm per shot").withColor(0xAAAAAA)), mouseX, mouseY);
        }
        List<Component> ring = ColliderRingWidget.tooltip(mouseX - leftPos - MiniColliderScreen.RING_X,
                mouseY - topPos - MiniColliderScreen.RING_Y, menu.getRingMask(), menu.isComplete(), "Storm Collider");
        if (ring != null) g.setComponentTooltipForNextFrame(font, ring, mouseX, mouseY);
        if (isHovering(MiniColliderScreen.SHOT_X - 1, MiniColliderScreen.SHOT_Y - 1, MiniColliderScreen.SHOT_W + 2,
                MiniColliderScreen.SHOT_H + 2, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, Component.literal(menu.getShot() >= 0 ? "Pairing in flight" : "No shot in flight"), mouseX, mouseY);
        }
        emptySlotTip(g, 0, StormColliderMenu.CELLS_Y, "Spark Cells (" + menu.getCells() + " loaded, 2 per shot)", mouseX, mouseY);
        emptySlotTip(g, 1, StormColliderMenu.INJ_A_Y, "Left injector: first material", mouseX, mouseY);
        emptySlotTip(g, 2, StormColliderMenu.INJ_B_Y, "Right injector: second material", mouseX, mouseY);
    }

    private void emptySlotTip(GuiGraphicsExtractor g, int slot, int y, String text, int mouseX, int mouseY) {
        if (menu.slots.get(slot).getItem().isEmpty() && isHovering(StormColliderMenu.SLOT_X - 1, y - 1, 18, 18, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, Component.literal(menu.isComplete() ? text : "Finish the ring first"), mouseX, mouseY);
        }
    }
}
