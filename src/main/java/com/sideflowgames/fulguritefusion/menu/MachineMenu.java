package com.sideflowgames.fulguritefusion.menu;

import com.sideflowgames.fulguritefusion.machine.SideConfig;
import com.sideflowgames.fulguritefusion.machine.SideConfigurable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

// Shared parts of every machine menu: the player inventory, synced numbers, and the side-config buttons.
public abstract class MachineMenu extends AbstractContainerMenu {
    protected final ContainerData data;
    private final ContainerLevelAccess access;
    private final Block block;
    private final SideConfigurable machine; // null on the client
    private final int sideDataStart;
    protected final int machineSlots;

    protected MachineMenu(MenuType<?> type, int containerId, ContainerData data, ContainerLevelAccess access,
                          Block block, SideConfigurable machine, int sideDataStart, int machineSlots) {
        super(type, containerId);
        this.data = data;
        this.access = access;
        this.block = block;
        this.machine = machine;
        this.sideDataStart = sideDataStart;
        this.machineSlots = machineSlots;
        addDataSlots(data);
    }

    protected void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public int getSideMode(int face) {
        return data.get(sideDataStart + face);
    }

    // Buttons 0-4 on the side tab cycle Top/Bottom/Left/Right/Back between None, Pull and Push.
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < SideConfig.COUNT && machine != null) {
            machine.cycleSide(id);
            return true;
        }
        return false;
    }

    // Shift-click: machine slots go to the player; player items go into the machine's input slots.
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, inputSlotCount(), false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    // How many machine slots (from the start) players can shift-click items into.
    protected abstract int inputSlotCount();

    @Override
    public boolean stillValid(Player player) {
        // Any block of the same kind counts (e.g. a Storm Furnace that was upgraded while the menu was open).
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock().getClass() == block.getClass()
                && player.isWithinBlockInteractionRange(pos, 4.0), true);
    }
}
