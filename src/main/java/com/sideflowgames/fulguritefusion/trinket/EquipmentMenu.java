package com.sideflowgames.fulguritefusion.trinket;

import com.sideflowgames.fulguritefusion.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

// The equipment panel: six trinket slots in a row, the Back slot to their right, the player's inventory below.
// Slot order: trinkets 0-5, Back 6, then the inventory (27 + 9 hotbar).
public class EquipmentMenu extends AbstractContainerMenu {
    public static final int TOP = 22, INV_Y = 54, WIDTH = 176, HEIGHT = 136;

    public static int slotX(int i) {
        return i < Trinkets.BACK ? 19 + i * 18 : 19 + Trinkets.BACK * 18 + 12;
    }

    // Client side.
    public EquipmentMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new ItemStackHandler(Trinkets.SIZE));
    }

    public EquipmentMenu(int id, Inventory inventory, IItemHandler equipment) {
        super(ModMenus.EQUIPMENT.get(), id);
        for (int i = 0; i < Trinkets.SIZE; i++) {
            final int index = i;
            addSlot(new SlotItemHandler(equipment, i, slotX(i), TOP) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return Trinkets.fits(index, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return 1;
                }
            });
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        }
        for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c, 8 + c * 18, INV_Y + 58));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < Trinkets.SIZE) {
            if (!moveItemStackTo(stack, Trinkets.SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < Trinkets.SIZE && !moved; i++) {
                if (Trinkets.fits(i, stack) && !slots.get(i).hasItem()) moved = moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
