package com.sideflowgames.fulguritefusion.menu;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModMenus;
import com.sideflowgames.fulguritefusion.block.ColliderInputBlockEntity;
import com.sideflowgames.fulguritefusion.block.MiniColliderBlockEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

// Mini Collider menu: the ring diagram, stored Fm, shot progress, and the Collider Input's Spark Cells.
public class MiniColliderMenu extends MachineMenu {
    public static final int CELLS_X = 64, CELLS_Y = 35;

    public MiniColliderMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainerData(MiniColliderBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL,
                ColliderSlots.clientCells());
        buf.readBlockPos();
    }

    public MiniColliderMenu(int containerId, Inventory inventory, MiniColliderBlockEntity be, ContainerData data) {
        this(containerId, inventory, data, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()), cells(be));
    }

    private static IItemHandler cells(MiniColliderBlockEntity be) {
        ColliderInputBlockEntity input = be.inputBlock();
        return input != null ? input.getItems() : null;
    }

    private MiniColliderMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access, IItemHandler cells) {
        super(ModMenus.MINI_COLLIDER.get(), containerId, data, access, ModBlocks.MINI_COLLIDER.get(), null, 0, 1);
        addSlot(cells != null ? new SlotItemHandler(cells, 0, CELLS_X, CELLS_Y) : ColliderSlots.disabled(CELLS_X, CELLS_Y));
        addPlayerInventory(inventory);
    }

    @Override
    protected int inputSlotCount() {
        return 1;
    }

    private int big(int index) {
        return (data.get(index) & 0xFFFF) | ((data.get(index + 1) & 0xFFFF) << 16);
    }

    public int getFm() { return big(MiniColliderBlockEntity.D_FM); }
    public int getMaxFm() { return big(MiniColliderBlockEntity.D_MAX); }
    public int getPending() { return big(MiniColliderBlockEntity.D_PENDING); }
    public int getRate() { return data.get(MiniColliderBlockEntity.D_RATE); }
    public int getShot() { return (short) data.get(MiniColliderBlockEntity.D_SHOT); }
    public int getShotLength() { return data.get(MiniColliderBlockEntity.D_LENGTH); }
    public int getCells() { return data.get(MiniColliderBlockEntity.D_CELLS); }
    public int getRingMask() { return (data.get(MiniColliderBlockEntity.D_MASK) & 0xFFF) | ((data.get(MiniColliderBlockEntity.D_MASK + 1) & 0xFFF) << 12); }
    public boolean isComplete() { return data.get(MiniColliderBlockEntity.D_COMPLETE) != 0; }
    public int getState() { return data.get(MiniColliderBlockEntity.D_STATE); }
}
