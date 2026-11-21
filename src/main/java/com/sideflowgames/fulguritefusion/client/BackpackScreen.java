package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.backpack.BackpackMenu;
import com.sideflowgames.fulguritefusion.backpack.BackpackTier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

// The backpack screen, drawn in code (no texture) so every size fits, in the shared menu style (GuiStyle)
// with a strip in the tier's colour.
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.width();
        imageHeight = menu.height();
        inventoryLabelY = menu.playerInventoryY() - 11;
        inventoryLabelX = (imageWidth - 9 * BackpackMenu.SLOT) / 2 + 1;
    }

    // Storm Deposit / Restock: act on the container you were looking at when you opened the pack.
    // Rebuilt whenever the installed upgrades change (they arrive from the server just after the screen opens).
    private final java.util.List<net.minecraft.client.gui.components.Button> remoteButtons = new java.util.ArrayList<>();
    private int remoteKey = -1;

    private int remoteActions() {
        int key = 0;
        for (int i = 0; i < menu.upgrades().getContainerSize(); i++) {
            if (!(menu.upgrades().getItem(i).getItem() instanceof com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem u) || !u.storm) continue;
            if (u.kind == com.sideflowgames.fulguritefusion.backpack.UpgradeKind.DEPOSIT) key |= 1;
            if (u.kind == com.sideflowgames.fulguritefusion.backpack.UpgradeKind.RESTOCK) key |= 2;
        }
        return key;
    }

    private void rebuildRemoteButtons() {
        remoteButtons.forEach(this::removeWidget);
        remoteButtons.clear();
        remoteKey = remoteActions();
        int x = leftPos + imageWidth + 2, y = topPos + 4;
        for (int action : new int[]{2, 3}) {
            if ((remoteKey & (action == 2 ? 1 : 2)) == 0) continue;
            var b = net.minecraft.client.gui.components.Button.builder(Component.literal(action == 2 ? "Deposit" : "Restock"),
                    btn -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new com.sideflowgames.fulguritefusion.backpack.BackpackNetwork.UtilityPayload(action)))
                    .bounds(x, y, 54, 16).build();
            b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                    "Acts on the container you were looking at (8 blocks) when you opened the pack. 500 Fm")));
            remoteButtons.add(addRenderableWidget(b));
            y += 18;
        }
    }

    @Override
    protected void init() {
        super.init();
        remoteButtons.clear();
        rebuildRemoteButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (remoteActions() != remoteKey) rebuildRemoteButtons();
    }

    // Each tier's accent colour: a strip under the top edge (the panel itself is the shared style).
    private int accent() {
        return switch (menu.tier) {
            case COPPER -> 0xFFC9783A;
            case IRON -> 0xFFB8B8C0;
            case FULGURITE -> 0xFF8A6FD8;
            case STORMFORGED -> 0xFF5AB4FF;
        };
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        // Upgrade tab on the left, drawn first so the main panel's edge sits over its join.
        int ups = menu.tier.upgradeSlots;
        int tx = x + BackpackMenu.UPGRADE_X - 5, ty = y + BackpackMenu.TOP - 5;
        GuiStyle.panel(g, tx, ty, x + 4 - tx, ups * BackpackMenu.SLOT + 8);
        GuiStyle.panel(g, x, y, w, h);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, accent());
        if (menu.tier == BackpackTier.STORMFORGED) {
            // Fm gauge under the upgrade tab.
            int gy = ty + ups * BackpackMenu.SLOT + 12, gh = 40;
            g.fill(tx + 6, gy, tx + 18, gy + gh, GuiStyle.DARK);
            int fill = Math.round((gh - 2) * menu.charge());
            g.fill(tx + 7, gy + gh - 1 - fill, tx + 17, gy + gh - 1, 0xFF5AB4FF);
        }
        for (Slot s : menu.slots) GuiStyle.slot(g, x + s.x, y + s.y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - 9 * BackpackMenu.SLOT) / 2 + 1, 6, GuiStyle.TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiStyle.TEXT, false);
    }

    // The upgrade tab sits outside the main panel; clicks there aren't "outside" (which would drop items).
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        int tx = left + BackpackMenu.UPGRADE_X - 5, ty = top + BackpackMenu.TOP - 5;
        int bottom = ty + menu.tier.upgradeSlots * BackpackMenu.SLOT + (menu.tier == BackpackTier.STORMFORGED ? 56 : 8);
        if (mouseX >= tx && mouseX < left && mouseY >= ty && mouseY < bottom) return false;
        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (menu.tier == BackpackTier.STORMFORGED && menu.getCarried().isEmpty()) {
            int tx = leftPos + BackpackMenu.UPGRADE_X - 5;
            int gy = topPos + BackpackMenu.TOP - 5 + menu.tier.upgradeSlots * BackpackMenu.SLOT + 12;
            if (mouseX >= tx + 6 && mouseX < tx + 18 && mouseY >= gy && mouseY < gy + 40) {
                g.renderTooltip(font, Component.literal(Math.round(menu.charge() * 100) + "% charged (Fm for Stormforged upgrades)"), mouseX, mouseY);
            }
        }
    }
}
