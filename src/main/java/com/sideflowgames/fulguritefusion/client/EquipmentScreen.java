package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.trinket.EquipmentMenu;
import com.sideflowgames.fulguritefusion.trinket.TrinketKind;
import com.sideflowgames.fulguritefusion.trinket.Trinkets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// The equipment panel: trinket slots (an empty slot shows a faded picture of what goes there), the Back slot, and
// a button back to the normal inventory.
public class EquipmentScreen extends AbstractContainerScreen<EquipmentMenu> {
    public EquipmentScreen(EquipmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EquipmentMenu.WIDTH, EquipmentMenu.HEIGHT);
        inventoryLabelY = EquipmentMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Inventory"), b -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
                minecraft.setScreen(new InventoryScreen(minecraft.player));
            }
        }).bounds(leftPos + imageWidth - 62, topPos + 4, 56, 12).build());
    }

    private static ItemStack ghost(int i) {
        if (i == Trinkets.BACK) return new ItemStack(ModBlocks.COPPER_BACKPACK_ITEM.get());
        return switch (TrinketKind.values()[i]) {
            case CLOUDSTEP_CHARM -> new ItemStack(ModItems.CLOUDSTEP_CHARM.get());
            case CONDUIT_HEART -> new ItemStack(ModItems.CONDUIT_HEART.get());
            case VOID_ARC_BAND -> new ItemStack(ModItems.VOID_ARC_BAND.get());
            case GROUNDING_CHARM -> new ItemStack(ModItems.GROUNDING_CHARM.get());
            case CINDER_WARD -> new ItemStack(ModItems.CINDER_WARD.get());
            case SEERS_RING -> new ItemStack(ModItems.SEERS_RING.get());
        };
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        GuiStyle.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) GuiStyle.slot(g, leftPos + s.x, topPos + s.y);
        for (int i = 0; i < Trinkets.SIZE; i++) {
            Slot s = menu.slots.get(i);
            if (s.hasItem()) continue;
            int x = leftPos + s.x, y = topPos + s.y;
            g.fakeItem(ghost(i), x, y);
            g.pose().pushMatrix();
            g.nextStratum();
            g.fill(x, y, x + 16, y + 16, 0xB0284664);
            g.pose().popMatrix();
        }
        g.text(font, "Back", leftPos + EquipmentMenu.slotX(Trinkets.BACK) - 1, topPos + EquipmentMenu.TOP + 19, GuiStyle.opaque(GuiStyle.TEXT), false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < Trinkets.SIZE && menu.getCarried().isEmpty()) {
            int i = hoveredSlot.index;
            Component name = i == Trinkets.BACK ? Component.literal("Back: a backpack")
                    : Component.literal("Slot: ").append(ghost(i).getHoverName());
            g.setTooltipForNextFrame(font, name, mouseX, mouseY);
        } else {
        }
    }
}
