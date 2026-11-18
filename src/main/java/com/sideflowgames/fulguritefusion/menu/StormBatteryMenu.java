package com.sideflowgames.fulguritefusion.menu;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModMenus;
import com.sideflowgames.fulguritefusion.block.StormBatteryBlockEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

// Information-only menu for a battery bank: total stored Fm, capacity, and bank size.
public class StormBatteryMenu extends MachineMenu {
    public StormBatteryMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainerData(StormBatteryBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL);
        buf.readBlockPos();
    }

    public StormBatteryMenu(int containerId, Inventory inventory, StormBatteryBlockEntity be, ContainerData data) {
        this(containerId, inventory, data, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()));
    }

    private StormBatteryMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.STORM_BATTERY.get(), containerId, data, access, ModBlocks.STORM_BATTERY.get(), null, 0, 0);
        addPlayerInventory(inventory);
    }

    @Override
    protected int inputSlotCount() {
        return 0;
    }

    private int big(int index) {
        return (data.get(index) & 0xFFFF) | ((data.get(index + 1) & 0xFFFF) << 16);
    }

    public int getFm() { return big(StormBatteryBlockEntity.D_FM); }
    public int getCapacity() { return big(StormBatteryBlockEntity.D_CAPACITY); }
    public int getBlockCount() { return data.get(StormBatteryBlockEntity.D_COUNT); }
    public int getMaxBlocks() { return data.get(StormBatteryBlockEntity.D_MAX); }
}
