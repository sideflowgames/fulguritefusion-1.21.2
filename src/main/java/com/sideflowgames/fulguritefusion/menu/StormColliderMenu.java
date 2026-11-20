package com.sideflowgames.fulguritefusion.menu;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModMenus;
import com.sideflowgames.fulguritefusion.block.ColliderInjectorBlockEntity;
import com.sideflowgames.fulguritefusion.block.ColliderInputBlockEntity;
import com.sideflowgames.fulguritefusion.block.StormColliderBlockEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

// Storm Collider menu: the ring diagram, Fm, shot progress, the Spark Cells, both injectors' materials,
// and the 9 output slots (take only).
// Slots: 0 = Spark Cells, 1 = left injector, 2 = right injector, 3-11 = outputs.
public class StormColliderMenu extends MachineMenu {
    public static final int SLOT_X = 64, CELLS_Y = 35, INJ_A_Y = 17, INJ_B_Y = 53;
    public static final int OUT_X = 96, OUT_Y = 17;

    public StormColliderMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainerData(StormColliderBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL,
                ColliderSlots.clientCells(), ColliderSlots.clientAny(1), ColliderSlots.clientAny(1),
                ColliderSlots.clientAny(StormColliderBlockEntity.OUTPUT_SLOTS));
        buf.readBlockPos();
    }

    public StormColliderMenu(int containerId, Inventory inventory, StormColliderBlockEntity be, ContainerData data) {
        this(containerId, inventory, data, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                cells(be), injector(be, 1), injector(be, -1), be.getOutput());
    }

    private static IItemHandler cells(StormColliderBlockEntity be) {
        ColliderInputBlockEntity input = be.inputBlock();
        return input != null ? input.getItems() : null;
    }

    private static IItemHandler injector(StormColliderBlockEntity be, int mirror) {
        ColliderInjectorBlockEntity inj = be.injectorBlock(mirror);
        return inj != null ? inj.getItems() : null;
    }

    private StormColliderMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access,
                              IItemHandler cells, IItemHandler injA, IItemHandler injB, IItemHandler output) {
        super(ModMenus.STORM_COLLIDER.get(), containerId, data, access, ModBlocks.STORM_COLLIDER.get(), null, 0,
                3 + StormColliderBlockEntity.OUTPUT_SLOTS);
        addSlot(cells != null ? new SlotItemHandler(cells, 0, SLOT_X, CELLS_Y) : ColliderSlots.disabled(SLOT_X, CELLS_Y));
        addSlot(injA != null ? new SlotItemHandler(injA, 0, SLOT_X, INJ_A_Y) : ColliderSlots.disabled(SLOT_X, INJ_A_Y));
        addSlot(injB != null ? new SlotItemHandler(injB, 0, SLOT_X, INJ_B_Y) : ColliderSlots.disabled(SLOT_X, INJ_B_Y));
        for (int i = 0; i < StormColliderBlockEntity.OUTPUT_SLOTS; i++) {
            addSlot(ColliderSlots.output(output, i, OUT_X + (i % 3) * 18, OUT_Y + (i / 3) * 18));
        }
        addPlayerInventory(inventory);
    }

    @Override
    protected int inputSlotCount() {
        return 3;
    }

    private int big(int index) {
        return (data.get(index) & 0xFFFF) | ((data.get(index + 1) & 0xFFFF) << 16);
    }

    public int getFm() { return big(StormColliderBlockEntity.D_FM); }
    public int getMaxFm() { return big(StormColliderBlockEntity.D_MAX); }
    public int getCost() { return big(StormColliderBlockEntity.D_COST); }
    public int getShot() { return (short) data.get(StormColliderBlockEntity.D_SHOT); }
    public int getShotLength() { return data.get(StormColliderBlockEntity.D_LENGTH); }
    public int getCells() { return data.get(StormColliderBlockEntity.D_CELLS); }
    public int getRingMask() { return (data.get(StormColliderBlockEntity.D_MASK) & 0xFFF) | ((data.get(StormColliderBlockEntity.D_MASK + 1) & 0xFFF) << 12); }
    public boolean isComplete() { return data.get(StormColliderBlockEntity.D_COMPLETE) != 0; }
    public int getState() { return data.get(StormColliderBlockEntity.D_STATE); }
}
