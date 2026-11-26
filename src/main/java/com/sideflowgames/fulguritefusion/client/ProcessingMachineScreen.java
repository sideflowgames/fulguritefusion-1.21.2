package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.machine.MachineTier;
import com.sideflowgames.fulguritefusion.menu.ProcessingMachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Screen for the multiblock machines: 2 inputs, progress arrow, 2 outputs, power bar, and the casing tier.
public class ProcessingMachineScreen extends MachineScreen<ProcessingMachineMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/gui/processing_machine.png");
    private static final int ARROW_X = 70, ARROW_Y = 35, ARROW_W = 24, ARROW_H = 16;
    private static final int TEXT_COLOR = 0xFF26262C;
    private static final int LATEX_X = 24, LATEX_Y = 20, LATEX_W = 8, LATEX_H = 40;

    public ProcessingMachineScreen(ProcessingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected boolean hasSideConfig() {
        return false; // items go in and out through the machine's ports
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor g, float partialTick) {
        drawPowerBar(g, menu.getFm(), menu.getMaxFm());
        int needed = menu.getNeeded();
        int w = needed > 0 ? Math.min(ARROW_W, menu.getProgress() * ARROW_W / needed) : 0;
        if (w > 0) g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, leftPos + ARROW_X, topPos + ARROW_Y, (float) 176, (float) 52, w, ARROW_H, 256, 256);
        // Latex Vulcanizer: a gauge of the latex in its vat, left of the inputs.
        int latex = menu.getLatex();
        if (latex >= 0) {
            int x = leftPos + LATEX_X, y = topPos + LATEX_Y;
            g.fill(x - 1, y - 1, x + LATEX_W + 1, y + LATEX_H + 1, 0xFF26262C);
            g.fill(x, y, x + LATEX_W, y + LATEX_H, 0xFF101014);
            int h = latex * LATEX_H / menu.getLatexCapacity();
            if (h > 0) g.fill(x, y + LATEX_H - h, x + LATEX_W, y + LATEX_H, 0xFFEEEADC);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        int tier = menu.getTier();
        String line = tier > 0 ? MachineTier.NAMES[tier] + " | " + menu.getRate() + " Fm/t" : "Not built";
        g.text(font, line, 8, 63, GuiStyle.opaque(TEXT_COLOR), false);
        int needed = menu.getNeeded();
        if (needed > 0 && menu.getProgress() > 0) {
            String pct = (menu.getProgress() * 100 / needed) + "%";
            g.text(font, pct, ARROW_X + (ARROW_W - font.width(pct)) / 2, ARROW_Y + 18, GuiStyle.opaque(TEXT_COLOR), false);
        }
    }

    @Override
    protected void renderExtraTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        powerTooltip(g, mouseX, mouseY, menu.getFm(), menu.getMaxFm());
        int latex = menu.getLatex();
        if (latex >= 0 && isHovering(LATEX_X - 1, LATEX_Y - 1, LATEX_W + 2, LATEX_H + 2, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, Component.literal(String.format("Latex: %.1f / %d buckets", latex / 1000f, menu.getLatexCapacity() / 1000)),
                    mouseX, mouseY);
        }
    }
}
