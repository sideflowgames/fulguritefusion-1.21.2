package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.menu.FulguriteChargerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FulguriteChargerScreen extends MachineScreen<FulguriteChargerMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/gui/fulgurite_charger.png");
    // Lightning arrow between the slots: empty version drawn in the background, filled sprite at (176, 52), 24 x 16.
    private static final int ARROW_X = 70, ARROW_Y = 35, ARROW_W = 24, ARROW_H = 16;
    private static final int TEXT_COLOR = 0xFF26262C;

    public FulguriteChargerScreen(FulguriteChargerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor g, float partialTick) {
        drawPowerBar(g, menu.getFm(), menu.getMaxFm());
        int cost = menu.getCost();
        int w = cost > 0 ? menu.getProgress() * ARROW_W / cost : 0;
        if (w > 0) {
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, leftPos + ARROW_X, topPos + ARROW_Y, (float) 176, (float) 52, w, ARROW_H, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        int cost = menu.getCost();
        if (cost > 0 && menu.getProgress() > 0) {
            String pct = (menu.getProgress() * 100 / cost) + "%";
            g.text(font, pct, ARROW_X + (ARROW_W - font.width(pct)) / 2, ARROW_Y + 20, GuiStyle.opaque(TEXT_COLOR), false);
        }
    }

    @Override
    protected void renderExtraTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        powerTooltip(g, mouseX, mouseY, menu.getFm(), menu.getMaxFm());
    }
}
