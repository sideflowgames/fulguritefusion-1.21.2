package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.transport.PipeFilterMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

// Item Pipe side filter: 9 filter slots (copies only) and the allow / block list button. Drawn in code in the
// shared menu style (GuiStyle).
public class PipeFilterScreen extends AbstractContainerScreen<PipeFilterMenu> {
    private Button listButton;

    public PipeFilterScreen(PipeFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = PipeFilterMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        listButton = addRenderableWidget(Button.builder(Component.empty(), b -> press(0))
                .bounds(leftPos + 8, topPos + 16, 160, 16).build());
        listButton.setTooltip(Tooltip.create(Component.literal(
                "Allow list: only the listed items. Block list: everything except the listed items.")));
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        listButton.setMessage(Component.literal(menu.allowList() ? "Allow list" : "Block list"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiStyle.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) {
            // Filter slots (copies only) are a lighter blue.
            GuiStyle.slot(g, leftPos + s.x, topPos + s.y, s.index < 9 ? 0xFF3E6A94 : GuiStyle.SLOT_FILL);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, GuiStyle.TEXT, false);
        g.drawString(font, Component.literal("Empty hand clears a slot"), 8, PipeFilterMenu.FILTER_Y + 20, GuiStyle.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiStyle.TEXT, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
