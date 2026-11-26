package com.sideflowgames.fulguritefusion.client;

import java.util.ArrayList;
import java.util.List;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.block.ReactorConsoleBlockEntity;
import com.sideflowgames.fulguritefusion.block.ReactorPath;
import com.sideflowgames.fulguritefusion.menu.ReactorConsoleMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Reactor Control Console: fuel 1 and fuel 2, Start / Stop / Auto, the run's progress through its phases,
// the console's Fm store (pushed out of the rear power ports) and the last run's result.
public class ReactorConsoleScreen extends MachineScreen<ReactorConsoleMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/gui/reactor_console.png");
    private static final int TEXT = 0xFF26262C, GOOD = 0xFF1E7A3A, BAD = 0xFFB02020;
    private static final int RUN_X = 36, RUN_Y = 20, RUN_W = 110, RUN_H = 10;

    private Button autoButton;

    public ReactorConsoleScreen(ReactorConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
        inventoryLabelY = 10000; // the status lines take its place
    }

    @Override
    protected boolean hasSideConfig() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Start"), b -> press(ReactorConsoleBlockEntity.B_START))
                .bounds(leftPos + 36, topPos + 44, 34, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Stop"), b -> press(ReactorConsoleBlockEntity.B_STOP))
                .bounds(leftPos + 72, topPos + 44, 34, 16).build());
        autoButton = addRenderableWidget(Button.builder(autoLabel(), b -> press(ReactorConsoleBlockEntity.B_AUTO))
                .bounds(leftPos + 108, topPos + 44, 40, 16).build());
    }

    private void press(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private Component autoLabel() {
        return Component.literal(menu.isAuto() ? "Auto: On" : "Auto: Off");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (autoButton != null) autoButton.setMessage(autoLabel());
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor g, float partialTick) {
        int t = menu.getRunTick();
        if (t >= 0) {
            int w = Math.min(RUN_W, (int) ((t + partialTick) * RUN_W / ReactorPath.T_END));
            if (w > 0) g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, leftPos + RUN_X, topPos + RUN_Y, (float) 0, (float) 170, w, RUN_H, 256, 256);
        }
        drawPowerBar(g, menu.getFm(), menu.getMaxFm());
    }

    private static String phase(int t) {
        if (t < 0) return "Idle";
        if (t < ReactorPath.T_JUMP) return "Outer ring: lap " + Math.min(6, 1 + (int) (ReactorPath.outerProgress((float) t / ReactorPath.T_JUMP) * 6)) + " of 6";
        if (t < ReactorPath.T_INNER) return "Portal jump";
        if (t < ReactorPath.T_INJECT) return "Inner ring: lap " + Math.min(6, 1 + (t - ReactorPath.T_INNER) * 6 / 60) + " of 6";
        if (t < ReactorPath.T_IMPACT) return "Injecting";
        return "Collision!";
    }

    private String stateText() {
        return switch (menu.getState()) {
            case ReactorConsoleBlockEntity.S_RUNNING -> "Run in progress";
            case ReactorConsoleBlockEntity.S_CONSOLE -> "Console incomplete";
            case ReactorConsoleBlockEntity.S_REACTOR -> "Reactor not formed / linked";
            case ReactorConsoleBlockEntity.S_FUEL -> "Load fuel 1 and fuel 2";
            case ReactorConsoleBlockEntity.S_POWER -> "Needs " + menu.getStartup() + " Fm to start";
            case ReactorConsoleBlockEntity.S_HEAT -> "Reactor cooling down";
            default -> "Ready";
        };
    }

    private String lastResult() {
        if (minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof ReactorConsoleBlockEntity c) {
            return c.lastResult();
        }
        return "";
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        g.text(font, "1", 4, ReactorConsoleMenu.FUEL1_Y + 4, GuiStyle.opaque(TEXT), false);
        g.text(font, "2", 4, ReactorConsoleMenu.FUEL2_Y + 4, GuiStyle.opaque(TEXT), false);
        g.text(font, phase(menu.getRunTick()), RUN_X, 33, GuiStyle.opaque(TEXT), false);

        int state = menu.getState();
        int colour = state == ReactorConsoleBlockEntity.S_READY ? GOOD : state == ReactorConsoleBlockEntity.S_RUNNING ? TEXT : BAD;
        String status = stateText();
        int cables = menu.getCables();
        if (state != ReactorConsoleBlockEntity.S_CONSOLE && cables != 3) {
            status += cables == 0 ? " | both cables cut" : " | cable " + (cables == 1 ? 2 : 1) + " cut";
            colour = BAD;
        }
        g.text(font, font.plainSubstrByWidth(status, 140), 34, 63, GuiStyle.opaque(colour), false);

        // The last result, small.
        g.pose().pushMatrix();
        g.pose().translate((float) (34), (float) (73));
        g.pose().scale((float) (0.75f), (float) (0.75f));
        g.text(font, font.plainSubstrByWidth("Last: " + lastResult(), 186), 0, 0, GuiStyle.opaque(TEXT), false);
        g.pose().popMatrix();
    }

    @Override
    protected void renderExtraTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovering(BAR_X - 2, BAR_Y - 2, BAR_W + 4, BAR_H + 4, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.literal(String.format("%,d / %,d Fm", menu.getFm(), menu.getMaxFm())),
                    Component.literal("Pushed out of the rear power ports").withColor(0xAAAAAA),
                    Component.literal(String.format("%,d Fm to start a run", menu.getStartup())).withColor(0xAAAAAA)), mouseX, mouseY);
        }
        if (isHovering(34, 72, 140, 9, mouseX, mouseY)) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(lastResult()));
            if (menu.getLastYield() > 0) tip.add(Component.literal(String.format("+%,d Fm", menu.getLastYield())).withColor(0x7FD8FF));
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
        if (isHovering(RUN_X - 1, RUN_Y - 1, RUN_W + 2, RUN_H + 2, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.literal(phase(menu.getRunTick())),
                    Component.literal("6 outer laps (30 s), portal jump, 6 fast inner laps, collision").withColor(0xAAAAAA)), mouseX, mouseY);
        }
        fuelTip(g, 0, ReactorConsoleMenu.FUEL1_Y, "Fuel 1 (data cable 1)", mouseX, mouseY);
        fuelTip(g, 1, ReactorConsoleMenu.FUEL2_Y, "Fuel 2 (data cable 2)", mouseX, mouseY);
        if (menu.slots.get(2).getItem().isEmpty() && isHovering(ReactorConsoleMenu.FUEL_X - 1, ReactorConsoleMenu.BYPRODUCT_Y - 1, 18, 18, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.literal("Byproducts"),
                    Component.literal("Rare drops from the collisions land here").withColor(0xAAAAAA),
                    Component.literal("A hopper under a Fuel Input can empty it").withColor(0xAAAAAA)), mouseX, mouseY);
        }
    }

    private void fuelTip(GuiGraphicsExtractor g, int slot, int y, String name, int mouseX, int mouseY) {
        if (menu.slots.get(slot).getItem().isEmpty() && isHovering(ReactorConsoleMenu.FUEL_X - 1, y - 1, 18, 18, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.literal(name),
                    Component.literal("Fusion Fuel capsule or a raw exotic material").withColor(0xAAAAAA),
                    Component.literal("Two capsules give the most Fm").withColor(0xAAAAAA)), mouseX, mouseY);
        }
    }
}
