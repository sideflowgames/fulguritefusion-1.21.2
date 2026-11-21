package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.backpack.UpgradeFilterMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

// Backpack upgrade filter screen: 9 filter slots (copies only) and the list buttons. Drawn in code in the
// shared menu style (GuiStyle).
public class UpgradeFilterScreen extends AbstractContainerScreen<UpgradeFilterMenu> {
    private Button listButton, xpButton;

    public UpgradeFilterScreen(UpgradeFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = UpgradeFilterMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        listButton = addRenderableWidget(Button.builder(Component.empty(), b -> press(0))
                .bounds(leftPos + 8, topPos + 16, menu.voidXpButton ? 78 : 160, 16).build());
        listButton.setTooltip(Tooltip.create(Component.literal(
                "Allow list: only the listed items. Block list: everything except the listed items.")));
        if (menu.voidXpButton) {
            xpButton = addRenderableWidget(Button.builder(Component.empty(), b -> press(1))
                    .bounds(leftPos + 90, topPos + 16, 78, 16).build());
            xpButton.setTooltip(Tooltip.create(Component.literal("Also remove XP orbs around you (5 Fm each)")));
        }
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        listButton.setMessage(Component.literal(menu.allowList() ? "Allow list" : "Block list"));
        if (xpButton != null) xpButton.setMessage(Component.literal("Void XP: " + (menu.voidXp() ? "On" : "Off")));
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
        g.drawString(font, Component.literal("Empty hand clears a slot"), 8, UpgradeFilterMenu.FILTER_Y + 20, GuiStyle.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiStyle.TEXT, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
