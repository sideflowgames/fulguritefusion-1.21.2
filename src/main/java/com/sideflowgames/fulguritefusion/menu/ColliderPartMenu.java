package com.sideflowgames.fulguritefusion.menu;

import com.sideflowgames.fulguritefusion.ModMenus;
import com.sideflowgames.fulguritefusion.block.ColliderInjectorBlockEntity;
import com.sideflowgames.fulguritefusion.block.ColliderInputBlockEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

// Small one-slot menu for a Collider Input (Spark Cells) or Collider Injector (a material) that isn't
// part of a finished ring yet. (Once the ring is built, clicking it opens the collider's own menu.)
public class ColliderPartMenu extends MachineMenu {
    public static final int SLOT_X = 80, SLOT_Y = 35;
    private final boolean input;

    public ColliderPartMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, ContainerLevelAccess.NULL, readInput(buf), null, null);
    }

    private static boolean readInput(RegistryFriendlyByteBuf buf) {
        buf.readBlockPos();
        return buf.readBoolean();
    }

    public ColliderPartMenu(int containerId, Inventory inventory, BlockEntity be) {
        this(containerId, inventory, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                be instanceof ColliderInputBlockEntity, be.getBlockState().getBlock(),
                be instanceof ColliderInputBlockEntity in ? in.getItems() : ((ColliderInjectorBlockEntity) be).getItems());
    }

    private ColliderPartMenu(int containerId, Inventory inventory, ContainerLevelAccess access, boolean input,
                             Block block, IItemHandler items) {
        super(ModMenus.COLLIDER_PART.get(), containerId, new SimpleContainerData(0), access, block, null, 0, 1);
        this.input = input;
        IItemHandler handler = items != null ? items : input ? ColliderSlots.clientCells() : ColliderSlots.clientAny(1);
        addSlot(new SlotItemHandler(handler, 0, SLOT_X, SLOT_Y));
        addPlayerInventory(inventory);
    }

    public boolean isInput() {
        return input;
    }

    @Override
    protected int inputSlotCount() {
        return 1;
    }
}
