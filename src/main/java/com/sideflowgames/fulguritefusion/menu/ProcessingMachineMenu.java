package com.sideflowgames.fulguritefusion.menu;

import com.sideflowgames.fulguritefusion.ModMenus;
import com.sideflowgames.fulguritefusion.block.MachineControllerBlockEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

// Shared menu for all five multiblock machines: 2 inputs, 2 outputs, power, progress and tier.
public class ProcessingMachineMenu extends MachineMenu {
    // Client side.
    public ProcessingMachineMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof MachineControllerBlockEntity be
                        ? be.getItems() : new ItemStackHandler(MachineControllerBlockEntity.SLOTS),
                new SimpleContainerData(MachineControllerBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL, Blocks.AIR);
    }

    // Server side.
    public ProcessingMachineMenu(int containerId, Inventory inventory, MachineControllerBlockEntity be, ContainerData data) {
        this(containerId, inventory, be.getItems(), data, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                be.getBlockState().getBlock());
    }

    private ProcessingMachineMenu(int containerId, Inventory inventory, IItemHandler items, ContainerData data,
                                  ContainerLevelAccess access, Block block) {
        super(ModMenus.PROCESSING_MACHINE.get(), containerId, data, access, block, null, 0, MachineControllerBlockEntity.SLOTS);
        addSlot(new SlotItemHandler(items, 0, 44, 26));
        addSlot(new SlotItemHandler(items, 1, 44, 46));
        addSlot(new OutputSlot(items, 2, 104, 26));
        addSlot(new OutputSlot(items, 3, 104, 46));
        addPlayerInventory(inventory);
    }

    @Override
    protected int inputSlotCount() {
        return 2;
    }

    public int getFm() { return data.get(0); }
    public int getMaxFm() { return MachineControllerBlockEntity.MAX_FM; }
    public int getProgress() { return data.get(1); }
    public int getNeeded() { return data.get(2); }
    public int getTier() { return data.get(3); }
    public int getRate() { return data.get(4); }
    public boolean isFormed() { return data.get(5) == 1; }

    // Players can take from output slots but not put items in.
    private static class OutputSlot extends SlotItemHandler {
        OutputSlot(IItemHandler items, int index, int x, int y) {
            super(items, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
