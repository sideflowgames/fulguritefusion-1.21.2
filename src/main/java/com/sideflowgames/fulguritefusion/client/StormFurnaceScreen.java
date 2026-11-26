package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.menu.StormFurnaceMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class StormFurnaceScreen extends MachineScreen<StormFurnaceMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/gui/storm_furnace.png");
    // Burn bolt above the fuel slot: 14 x 14. Lit frames at (176, 70), (190, 70), (204, 70).
    private static final int BOLT_X = 57, BOLT_Y = 36, BOLT_SIZE = 14;
    private static final int TEXT_COLOR = 0xFF26262C;

    public StormFurnaceScreen(StormFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor g, float partialTick) {
        drawPowerBar(g, menu.getFm(), menu.getMaxFm());

        // Burning: a flickering lightning bolt that shrinks as the fuel runs down.
        int burn = menu.getFuelFm(), total = menu.getFuelFmTotal();
        if (burn > 0 && total > 0) {
            int h = Mth.ceil(burn * (float) BOLT_SIZE / total);
            long ticks = minecraft.level != null ? minecraft.level.getGameTime() : 0;
            int frame = (int) ((ticks / 2) % 3);
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, leftPos + BOLT_X, topPos + BOLT_Y + BOLT_SIZE - h, (float) 176 + frame * BOLT_SIZE, (float) 70 + BOLT_SIZE - h, BOLT_SIZE, h, 256, 256);
        }

        // Grounded light: green (176, 86) or red (184, 86), 8 x 8.
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, leftPos + 76, topPos + 21, (float) (menu.isGrounded() ? 176 : 184), (float) 86, 8, 8, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        g.text(font, menu.isGrounded() ? "Grounded" : "Ungrounded", 87, 21, GuiStyle.opaque(TEXT_COLOR), false);
        int rate = Math.max(1, menu.getRate());
        int seconds = (menu.getFuelFm() + rate * 20 - 1) / (rate * 20);
        g.text(font, "Burn: " + seconds + "s", 76, 38, GuiStyle.opaque(TEXT_COLOR), false);
        g.text(font, menu.getFm() + " Fm", 76, 55, GuiStyle.opaque(TEXT_COLOR), false);
    }

    @Override
    protected void renderExtraTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovering(BAR_X - 2, BAR_Y - 2, BAR_W + 4, BAR_H + 4, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, Component.literal(menu.getFm() + " / " + menu.getMaxFm() + " Fm  (" + menu.getRate() + " Fm/t)"), mouseX, mouseY);
        }
    }
}
