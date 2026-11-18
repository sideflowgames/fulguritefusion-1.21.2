package com.sideflowgames.fulguritefusion.menu;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModMenus;
import com.sideflowgames.fulguritefusion.block.StormFurnaceBlockEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class StormFurnaceMenu extends MachineMenu {
    // Client side: opened from the packet the server sends.
    public StormFurnaceMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory,
                inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof StormFurnaceBlockEntity be ? be.getItems() : new ItemStackHandler(1),
                new SimpleContainerData(StormFurnaceBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL, null);
    }

    // Server side.
    public StormFurnaceMenu(int containerId, Inventory inventory, StormFurnaceBlockEntity be, ContainerData data) {
        this(containerId, inventory, be.getItems(), data, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()), be);
    }

    private StormFurnaceMenu(int containerId, Inventory inventory, IItemHandler items, ContainerData data,
                             ContainerLevelAccess access, StormFurnaceBlockEntity be) {
        super(ModMenus.STORM_FURNACE.get(), containerId, data, access, ModBlocks.STORM_FURNACE.get(), be, StormFurnaceBlockEntity.D_SIDES, 1);
        addSlot(new SlotItemHandler(items, 0, 56, 53)); // fuel
        addPlayerInventory(inventory);
    }

    @Override
    protected int inputSlotCount() {
        return 1;
    }

    // Rebuild a number the server sent as two 16-bit halves.
    private int big(int index) {
        return (data.get(index) & 0xFFFF) | ((data.get(index + 1) & 0xFFFF) << 16);
    }

    public int getFm() { return big(StormFurnaceBlockEntity.D_FM); }
    public int getMaxFm() { return big(StormFurnaceBlockEntity.D_MAX); }
    public int getFuelFm() { return big(StormFurnaceBlockEntity.D_FUEL); }
    public int getFuelFmTotal() { return big(StormFurnaceBlockEntity.D_FUEL_TOTAL); }
    public boolean isGrounded() { return data.get(StormFurnaceBlockEntity.D_GROUNDED) == 1; }
    public int getTier() { return data.get(StormFurnaceBlockEntity.D_TIER); }
    public int getRate() { return data.get(StormFurnaceBlockEntity.D_RATE); }
}
