package com.sideflowgames.fulguritefusion.transport;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Filter for one side of a Fulgurite or Stormforged Item Pipe. The 9 filter slots only hold copies: click one with
// an item on the cursor to list it, click it empty-handed to clear it, or shift-click an item in your inventory to
// add it. Button 0 switches between allow list (only these) and block list (everything but these).
// On an Extract side the filter picks what gets pulled out; on an Insert side, what that inventory will take.
public class PipeFilterMenu extends AbstractContainerMenu {
    public static final int FILTER_Y = 38, INV_Y = 84;
    private static final int N = ItemPipeBlockEntity.FILTER_SLOTS;

    @Nullable private final ItemPipeBlockEntity pipe; // server only
    private final BlockPos pos;
    private final Direction side;
    private final SimpleContainer ghosts = new SimpleContainer(N);
    private final DataSlot allow = DataSlot.standalone();

    // Client.
    public PipeFilterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, null, buf.readBlockPos(), Direction.from3DDataValue(buf.readVarInt()));
    }

    // Server.
    public PipeFilterMenu(int id, Inventory inventory, ItemPipeBlockEntity pipe, Direction side) {
        this(id, inventory, pipe, pipe.getBlockPos(), side);
    }

    private PipeFilterMenu(int id, Inventory inventory, @Nullable ItemPipeBlockEntity pipe, BlockPos pos, Direction side) {
        super(TransportBlocks.PIPE_FILTER_MENU.get(), id);
        this.pipe = pipe;
        this.pos = pos;
        this.side = side;
        if (pipe != null) {
            for (int i = 0; i < N; i++) ghosts.setItem(i, pipe.filter(side, i));
            allow.set(pipe.allowList(side) ? 1 : 0);
        }
        for (int i = 0; i < N; i++) {
            addSlot(new Slot(ghosts, i, 8 + i * 18, FILTER_Y) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c, 8 + c * 18, INV_Y + 58));
        addDataSlot(allow);
    }

    public boolean allowList() {
        return allow.get() != 0;
    }

    private void setGhost(int i, ItemStack from) {
        ghosts.setItem(i, from.isEmpty() ? ItemStack.EMPTY : from.copyWithCount(1));
        if (pipe != null) pipe.setFilter(side, i, ghosts.getItem(i));
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < N) {
            if (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE) setGhost(slotId, getCarried());
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != ghosts;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0) return false;
        allow.set(allow.get() == 0 ? 1 : 0);
        if (pipe != null) pipe.setAllowList(side, allow.get() != 0);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < N) return ItemStack.EMPTY;
        ItemStack stack = slots.get(index).getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (int i = 0; i < N; i++) if (ItemStack.isSameItem(ghosts.getItem(i), stack)) return ItemStack.EMPTY;
        for (int i = 0; i < N; i++) {
            if (ghosts.getItem(i).isEmpty()) {
                setGhost(i, stack);
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (pipe != null && pipe.isRemoved()) return false;
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
