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
