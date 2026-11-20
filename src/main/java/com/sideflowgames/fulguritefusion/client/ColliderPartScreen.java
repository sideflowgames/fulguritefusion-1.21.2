package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.menu.ColliderPartMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// One-slot menu for a Collider Input or Injector that isn't in a finished ring yet.
public class ColliderPartScreen extends MachineScreen<ColliderPartMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/gui/collider_part.png");
    private static final int TEXT = 0xFF26262C;

    public ColliderPartScreen(ColliderPartMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected boolean hasSideConfig() {
        return false;
    }

    @Override
    protected void renderMachine(GuiGraphics g, float partialTick) {}

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        String hint = menu.isInput() ? "Spark Cells (2 per shot)" : "Material for the Storm Collider";
        g.drawString(font, hint, (imageWidth - font.width(hint)) / 2, 58, TEXT, false);
    }
}
