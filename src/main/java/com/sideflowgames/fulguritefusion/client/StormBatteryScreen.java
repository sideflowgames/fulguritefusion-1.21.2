package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.menu.StormBatteryMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// Shows the whole bank: stored Fm, capacity and how many blocks it has.
public class StormBatteryScreen extends MachineScreen<StormBatteryMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/gui/storm_battery.png");
    private static final int TEXT = 0xFF26262C;

    public StormBatteryScreen(StormBatteryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected boolean hasSideConfig() {
        return false;
    }

    // ---------- Side settings tab (whole bank) ----------
    private static final String[] FACES = {"Down", "Up", "North", "South", "West", "East"};
    private final net.minecraft.client.gui.components.Button[] faceButtons = new net.minecraft.client.gui.components.Button[6];
    private boolean sidesOpen;

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("I/O"), b -> setSidesOpen(!sidesOpen))
                .bounds(leftPos + imageWidth, topPos + 4, 26, 18).build());
        for (int i = 0; i < 6; i++) {
            final int face = i;
            faceButtons[i] = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(faceLabel(i),
                            b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, face))
                    .bounds(leftPos + imageWidth + 4, topPos + 28 + i * 20, 84, 18).build());
        }
        setSidesOpen(sidesOpen);
    }

    private void setSidesOpen(boolean open) {
        sidesOpen = open;
        for (var b : faceButtons) if (b != null) b.visible = open;
    }

    private Component faceLabel(int face) {
        int mode = Math.max(0, Math.min(3, menu.getSideMode(face)));
        return Component.literal(FACES[face] + ": " + com.sideflowgames.fulguritefusion.block.StormBatteryBlockEntity.MODE_NAMES[mode]);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < 6; i++) if (faceButtons[i] != null) faceButtons[i].setMessage(faceLabel(i));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        super.renderBg(g, partialTick, mouseX, mouseY);
        if (sidesOpen) {
            int x = leftPos + imageWidth, y = topPos + 24;
            g.fill(x, y, x + 92, y + 6 + 6 * 20, 0xFF26262C);
            g.fill(x + 1, y + 1, x + 91, y + 5 + 6 * 20, 0xFFA9DCF2);
        }
    }

    @Override
    protected void renderMachine(GuiGraphics g, float partialTick) {
        drawPowerBar(g, menu.getFm(), menu.getCapacity());
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, "Bank: " + menu.getBlockCount() + " / " + menu.getMaxBlocks() + " blocks", 10, 20, TEXT, false);
        g.drawString(font, "Stored: " + menu.getFm() + " Fm", 10, 34, TEXT, false);
        g.drawString(font, "Max: " + menu.getCapacity() + " Fm", 10, 48, TEXT, false);
    }

    @Override
    protected void renderExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
        powerTooltip(g, mouseX, mouseY, menu.getFm(), menu.getCapacity());
    }
}
