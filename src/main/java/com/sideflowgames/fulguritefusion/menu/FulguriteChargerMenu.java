package com.sideflowgames.fulguritefusion.menu;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModMenus;
import com.sideflowgames.fulguritefusion.block.FulguriteChargerBlockEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class FulguriteChargerMenu extends MachineMenu {
    // Client side: opened from the packet the server sends.
    public FulguriteChargerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof FulguriteChargerBlockEntity be ? be.getItems() : new ItemStackHandler(2),
                new SimpleContainerData(FulguriteChargerBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL, null);
    }

    // Server side.
    public FulguriteChargerMenu(int containerId, Inventory inventory, FulguriteChargerBlockEntity be, ContainerData data) {
        this(containerId, inventory, be.getItems(), data, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()), be);
    }

    private FulguriteChargerMenu(int containerId, Inventory inventory, IItemHandler items, ContainerData data,
                                 ContainerLevelAccess access, FulguriteChargerBlockEntity be) {
        super(ModMenus.FULGURITE_CHARGER.get(), containerId, data, access, ModBlocks.FULGURITE_CHARGER.get(), be, 4, 2);
        addSlot(new SlotItemHandler(items, FulguriteChargerBlockEntity.INPUT, 44, 35));
        addSlot(new SlotItemHandler(items, FulguriteChargerBlockEntity.OUTPUT, 104, 35));
        addPlayerInventory(inventory);
    }

    @Override
    protected int inputSlotCount() {
        return 1;
    }

    public int getFm() { return data.get(0); }
    public int getMaxFm() { return data.get(1); }
    public int getProgress() { return data.get(2); }
    public int getCost() { return data.get(3); }
}
