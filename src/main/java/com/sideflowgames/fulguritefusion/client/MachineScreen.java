package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.machine.SideConfig;
import com.sideflowgames.fulguritefusion.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// Shared look and parts for every machine screen:
// the custom background, the Fm power bar, and the "I/O" side tab on the right.
public abstract class MachineScreen<M extends MachineMenu> extends AbstractContainerScreen<M> {
    protected static final int TEX_SIZE = 256;
    // Power bar: frame drawn into the background, fill sprite at (176, 0), 12 x 50.
    protected static final int BAR_X = 152, BAR_Y = 18, BAR_W = 12, BAR_H = 50;
    private static final int TAB_PANEL_COLOR = 0xFFA9DCF2;
    private static final int TAB_BORDER_COLOR = 0xFF26262C;

    protected final ResourceLocation texture;
    private boolean sidesOpen;
    private final Button[] sideButtons = new Button[SideConfig.COUNT];

    protected MachineScreen(M menu, Inventory inventory, Component title, ResourceLocation texture) {
        super(menu, inventory, title);
        this.texture = texture;
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    // Machines without item faces (like converters) turn the I/O tab off.
    protected boolean hasSideConfig() {
        return true;
    }

    @Override
    protected void init() {
        super.init();
        if (!hasSideConfig()) return;
        addRenderableWidget(Button.builder(Component.literal("I/O"), b -> setSidesOpen(!sidesOpen))
                .bounds(leftPos + imageWidth, topPos + 4, 26, 18).build());
        for (int i = 0; i < SideConfig.COUNT; i++) {
            final int face = i;
            sideButtons[i] = addRenderableWidget(Button.builder(sideLabel(i),
                            b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, face))
                    .bounds(leftPos + imageWidth + 4, topPos + 28 + i * 20, 84, 18).build());
        }
        setSidesOpen(sidesOpen);
    }

    private void setSidesOpen(boolean open) {
        sidesOpen = open;
        for (Button b : sideButtons) {
            if (b != null) b.visible = open;
        }
    }

    private Component sideLabel(int face) {
        return Component.literal(SideConfig.FACE_NAMES[face] + ": " + SideConfig.MODE_NAMES[menu.getSideMode(face)]);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!hasSideConfig()) return;
        for (int i = 0; i < SideConfig.COUNT; i++) {
            sideButtons[i].setMessage(sideLabel(i));
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (sidesOpen) {
            int x = leftPos + imageWidth, y = topPos + 24;
            g.fill(x, y, x + 92, y + 6 + SideConfig.COUNT * 20, TAB_BORDER_COLOR);
            g.fill(x + 1, y + 1, x + 91, y + 5 + SideConfig.COUNT * 20, TAB_PANEL_COLOR);
        }
        renderMachine(g, partialTick);
    }

    // Draw the machine's moving parts (bars, arrows, animations).
    protected abstract void renderMachine(GuiGraphics g, float partialTick);

    protected void drawPowerBar(GuiGraphics g, int fm, int maxFm) {
        int h = maxFm > 0 ? fm * BAR_H / maxFm : 0;
        if (h > 0) {
            g.blit(texture, leftPos + BAR_X, topPos + BAR_Y + BAR_H - h,
                    176, BAR_H - h, BAR_W, h);
        }
    }

    protected void powerTooltip(GuiGraphics g, int mouseX, int mouseY, int fm, int maxFm) {
        if (isHovering(BAR_X - 2, BAR_Y - 2, BAR_W + 4, BAR_H + 4, mouseX, mouseY)) {
            g.renderTooltip(font, Component.literal(fm + " / " + maxFm + " Fm"), mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        renderExtraTooltips(g, mouseX, mouseY);
    }

    protected void renderExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {}
}
