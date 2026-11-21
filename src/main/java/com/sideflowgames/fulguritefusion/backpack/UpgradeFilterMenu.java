package com.sideflowgames.fulguritefusion.backpack;

import com.sideflowgames.fulguritefusion.ModDataComponents;
import com.sideflowgames.fulguritefusion.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

// Setting a backpack upgrade's filter. The 9 filter slots only hold copies: click one with an item on the
// cursor to list it, click it empty-handed to clear it, or shift-click an item in your inventory to add it.
// Nothing real ever goes into these slots. Buttons: allow/block list (0), void XP (1, Storm Void only).
public class UpgradeFilterMenu extends AbstractContainerMenu {
    public static final int FILTER_Y = 38, INV_Y = 84;

    private final ItemStack upgrade; // server: the real stack; client: empty
    private final int handSlot;
    public final boolean voidXpButton;
    private final SimpleContainer ghosts = new SimpleContainer(BackpackUpgradeItem.FILTER_SLOTS);
    private final DataSlot mode = DataSlot.standalone();

    // Client side.
    public UpgradeFilterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, ItemStack.EMPTY, buf.readVarInt(), buf.readBoolean());
    }

    public UpgradeFilterMenu(int id, Inventory inventory, ItemStack upgrade, int handSlot, boolean voidXpButton) {
        super(ModMenus.UPGRADE_FILTER.get(), id);
        this.upgrade = upgrade;
        this.handSlot = handSlot;
        this.voidXpButton = voidXpButton;
        if (!upgrade.isEmpty()) {
            var items = net.minecraft.core.NonNullList.withSize(BackpackUpgradeItem.FILTER_SLOTS, ItemStack.EMPTY);
            upgrade.getOrDefault(ModDataComponents.FILTER.get(), ItemContainerContents.EMPTY).copyInto(items);
            for (int i = 0; i < items.size(); i++) ghosts.setItem(i, items.get(i));
            mode.set(BackpackUpgradeItem.mode(upgrade));
        }
        for (int i = 0; i < BackpackUpgradeItem.FILTER_SLOTS; i++) {
            addSlot(new Slot(ghosts, i, 8 + i * 18, FILTER_Y) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) addSlot(lockable(inventory, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        }
        for (int c = 0; c < 9; c++) addSlot(lockable(inventory, c, 8 + c * 18, INV_Y + 58));
        addDataSlot(mode);
    }

    private Slot lockable(Inventory inv, int index, int x, int y) {
        return new Slot(inv, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return index != handSlot;
            }
        };
    }

    public boolean allowList() {
        return (mode.get() & BackpackUpgradeItem.MODE_ALLOW) != 0;
    }

    public boolean voidXp() {
        return (mode.get() & BackpackUpgradeItem.MODE_VOID_XP) != 0;
    }

    private void save() {
        if (upgrade.isEmpty()) return;
        upgrade.set(ModDataComponents.FILTER.get(), ItemContainerContents.fromItems(ghosts.getItems()));
        upgrade.set(ModDataComponents.FILTER_MODE.get(), mode.get());
    }

    private void setGhost(int i, ItemStack from) {
        ghosts.setItem(i, from.isEmpty() ? ItemStack.EMPTY : from.copyWithCount(1));
        save();
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < BackpackUpgradeItem.FILTER_SLOTS) {
            if (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE) setGhost(slotId, getCarried());
            return;
        }
        if (type == ClickType.SWAP && button == handSlot) return;
        super.clicked(slotId, button, type, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != ghosts;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) mode.set(mode.get() ^ BackpackUpgradeItem.MODE_ALLOW);
        else if (id == 1 && voidXpButton) mode.set(mode.get() ^ BackpackUpgradeItem.MODE_VOID_XP);
        else return false;
        save();
        return true;
    }

    // Shift-clicking an inventory item lists it (the item stays where it is).
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < BackpackUpgradeItem.FILTER_SLOTS) return ItemStack.EMPTY;
        ItemStack stack = slots.get(index).getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (int i = 0; i < BackpackUpgradeItem.FILTER_SLOTS; i++) {
            if (ItemStack.isSameItem(ghosts.getItem(i), stack)) return ItemStack.EMPTY;
        }
        for (int i = 0; i < BackpackUpgradeItem.FILTER_SLOTS; i++) {
            if (ghosts.getItem(i).isEmpty()) {
                setGhost(i, stack);
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (upgrade.isEmpty()) return true; // client
        return player.isAlive() && player.getInventory().getItem(handSlot) == upgrade;
    }
}
