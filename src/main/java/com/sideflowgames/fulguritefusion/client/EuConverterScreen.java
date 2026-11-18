package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.block.EuConverterBlockEntity;
import com.sideflowgames.fulguritefusion.compat.EuCompat;
import com.sideflowgames.fulguritefusion.menu.EuConverterMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// Shows which EU system and tier is connected, the EU output, and whether there's enough Fm.
public class EuConverterScreen extends MachineScreen<EuConverterMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/gui/eu_converter.png");
    private static final int TEXT = 0xFF26262C, GOOD = 0xFF1E7A2E, BAD = 0xFFB0281E;

    public EuConverterScreen(EuConverterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected boolean hasSideConfig() {
        return false;
    }

    @Override
    protected void renderMachine(GuiGraphics g, float partialTick) {
        drawPowerBar(g, menu.getFm(), menu.getMaxFm());
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        EuCompat.Source source = menu.getSource();
        if (source == EuCompat.Source.NONE) {
            g.drawString(font, "Type: none found", 10, 20, TEXT, false);
            g.drawString(font, "Connect an LV/MV/HV", 10, 32, TEXT, false);
            g.drawString(font, "cable to the front", 10, 44, TEXT, false);
        } else {
            g.drawString(font, "Type: " + EuCompat.sourceName(source) + " " + menu.getTierName(), 10, 20, TEXT, false);
            g.drawString(font, "Output: " + menu.getOutput() + " EU/t", 10, 32, TEXT, false);
            int euPerFm = Config.EU_PER_FM.get();
            int fmNeeded = (menu.getVoltage() + euPerFm - 1) / euPerFm;
            g.drawString(font, "Needs: " + fmNeeded + " Fm/t", 10, 44, TEXT, false);
        }
        String status;
        int color;
        switch (menu.getStatus()) {
            case EuConverterBlockEntity.STATUS_CONVERTING -> { status = "Converting"; color = GOOD; }
            case EuConverterBlockEntity.STATUS_LOW_FM -> { status = "Not enough Fm"; color = BAD; }
            case EuConverterBlockEntity.STATUS_TARGET_FULL -> { status = "Target full"; color = TEXT; }
            default -> { status = "Idle"; color = TEXT; }
        }
        g.drawString(font, status, 10, 58, color, false);
    }

    @Override
    protected void renderExtraTooltips(GuiGraphics g, int mouseX, int mouseY) {
        powerTooltip(g, mouseX, mouseY, menu.getFm(), menu.getMaxFm());
    }
}
