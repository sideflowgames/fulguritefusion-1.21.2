package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.block.EuConverterBlockEntity;
import com.sideflowgames.fulguritefusion.compat.EuCompat;
import com.sideflowgames.fulguritefusion.menu.EuConverterMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Shows which EU system and tier is connected, the EU output, and whether there's enough Fm.
public class EuConverterScreen extends MachineScreen<EuConverterMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/gui/eu_converter.png");
    private static final int TEXT = 0xFF26262C, GOOD = 0xFF1E7A2E, BAD = 0xFFB0281E;

    public EuConverterScreen(EuConverterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected boolean hasSideConfig() {
        return false;
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor g, float partialTick) {
        drawPowerBar(g, menu.getFm(), menu.getMaxFm());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        EuCompat.Source source = menu.getSource();
        if (source == EuCompat.Source.NONE) {
            g.text(font, "Type: none found", 10, 20, GuiStyle.opaque(TEXT), false);
            g.text(font, "Connect an LV/MV/HV", 10, 32, GuiStyle.opaque(TEXT), false);
            g.text(font, "cable to the front", 10, 44, GuiStyle.opaque(TEXT), false);
        } else {
            g.text(font, "Type: " + EuCompat.sourceName(source) + " " + menu.getTierName(), 10, 20, GuiStyle.opaque(TEXT), false);
            g.text(font, "Output: " + menu.getOutput() + " EU/t", 10, 32, GuiStyle.opaque(TEXT), false);
            int euPerFm = Config.EU_PER_FM.get();
            int fmNeeded = (menu.getVoltage() + euPerFm - 1) / euPerFm;
            g.text(font, "Needs: " + fmNeeded + " Fm/t", 10, 44, GuiStyle.opaque(TEXT), false);
        }
        String status;
        int color;
        switch (menu.getStatus()) {
            case EuConverterBlockEntity.STATUS_CONVERTING -> { status = "Converting"; color = GOOD; }
            case EuConverterBlockEntity.STATUS_LOW_FM -> { status = "Not enough Fm"; color = BAD; }
            case EuConverterBlockEntity.STATUS_TARGET_FULL -> { status = "Target full"; color = TEXT; }
            default -> { status = "Idle"; color = TEXT; }
        }
        g.text(font, status, 10, 58, GuiStyle.opaque(color), false);
    }

    @Override
    protected void renderExtraTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        powerTooltip(g, mouseX, mouseY, menu.getFm(), menu.getMaxFm());
    }
}
