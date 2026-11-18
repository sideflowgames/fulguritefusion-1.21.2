package com.sideflowgames.fulguritefusion.menu;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModMenus;
import com.sideflowgames.fulguritefusion.block.EuConverterBlockEntity;
import com.sideflowgames.fulguritefusion.compat.EuCompat;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

// Information-only menu: no machine slots, just the player inventory and synced numbers.
public class EuConverterMenu extends MachineMenu {
    public EuConverterMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainerData(EuConverterBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL);
        buf.readBlockPos();
    }

    public EuConverterMenu(int containerId, Inventory inventory, EuConverterBlockEntity be, ContainerData data) {
        this(containerId, inventory, data, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()));
    }

    private EuConverterMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.EU_CONVERTER.get(), containerId, data, access, ModBlocks.EU_CONVERTER.get(), null, 0, 0);
        addPlayerInventory(inventory);
    }

    @Override
    protected int inputSlotCount() {
        return 0;
    }

    private int big(int index) {
        return (data.get(index) & 0xFFFF) | ((data.get(index + 1) & 0xFFFF) << 16);
    }

    public int getFm() { return big(EuConverterBlockEntity.D_FM); }
    public int getMaxFm() { return big(EuConverterBlockEntity.D_MAX); }
    public int getVoltage() { return big(EuConverterBlockEntity.D_VOLTAGE); }
    public int getOutput() { return big(EuConverterBlockEntity.D_OUTPUT); }
    public int getStatus() { return data.get(EuConverterBlockEntity.D_STATUS); }

    public EuCompat.Source getSource() {
        int i = data.get(EuConverterBlockEntity.D_SOURCE);
        EuCompat.Source[] all = EuCompat.Source.values();
        return i >= 0 && i < all.length ? all[i] : EuCompat.Source.NONE;
    }

    public String getTierName() {
        int t = data.get(EuConverterBlockEntity.D_TIER) - 1;
        return t >= 0 && t < EuCompat.TIER_NAMES.length ? EuCompat.TIER_NAMES[t] : "-";
    }
}
