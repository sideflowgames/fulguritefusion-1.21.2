package com.sideflowgames.fulguritefusion.menu;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModMenus;
import com.sideflowgames.fulguritefusion.block.ReactorConsoleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

// Reactor Control Console menu: the two fuel slots, Start / Stop / Auto, the run's progress, the Fm store
// and the last run's result. Slots: 0 = fuel 1, 1 = fuel 2.
public class ReactorConsoleMenu extends MachineMenu {
    public static final int FUEL_X = 12, FUEL1_Y = 22, FUEL2_Y = 50;

    private final ReactorConsoleBlockEntity console; // null on the client
    public final BlockPos pos;

    public ReactorConsoleMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainerData(ReactorConsoleBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL,
                clientFuel(), null, buf.readBlockPos());
    }

    public ReactorConsoleMenu(int containerId, Inventory inventory, ReactorConsoleBlockEntity be, ContainerData data) {
        this(containerId, inventory, data, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()), be.getFuel(), be,
                be.getBlockPos());
    }

    private static IItemHandler clientFuel() {
        return new ItemStackHandler(2) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return ReactorConsoleBlockEntity.isFuel(stack);
            }
        };
    }

    private ReactorConsoleMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access,
                               IItemHandler fuel, ReactorConsoleBlockEntity console, BlockPos pos) {
        super(ModMenus.REACTOR_CONSOLE.get(), containerId, data, access, ModBlocks.REACTOR_CONSOLE.get(), null, 0, 2);
        this.console = console;
        this.pos = pos;
        addSlot(new SlotItemHandler(fuel, 0, FUEL_X, FUEL1_Y));
        addSlot(new SlotItemHandler(fuel, 1, FUEL_X, FUEL2_Y));
        addPlayerInventory(inventory);
    }

    @Override
    protected int inputSlotCount() {
        return 2;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (console != null) console.pressButton(id);
        return true;
    }

    private int big(int index) {
        return (data.get(index) & 0xFFFF) | ((data.get(index + 1) & 0xFFFF) << 16);
    }

    public int getFm() { return big(ReactorConsoleBlockEntity.D_FM); }
    public int getMaxFm() { return big(ReactorConsoleBlockEntity.D_MAX); }
    public int getStartup() { return big(ReactorConsoleBlockEntity.D_STARTUP); }
    public int getRunTick() { return (short) data.get(ReactorConsoleBlockEntity.D_RUN); }
    public int getState() { return data.get(ReactorConsoleBlockEntity.D_STATE); }
    public boolean isAuto() { return data.get(ReactorConsoleBlockEntity.D_AUTO) != 0; }
    public int getCables() { return data.get(ReactorConsoleBlockEntity.D_CABLES); }
    public int getLastYield() { return big(ReactorConsoleBlockEntity.D_LAST); }
}
