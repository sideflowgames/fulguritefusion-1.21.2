package com.sideflowgames.fulguritefusion.backpack;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Restock and Deposit: moving items between a backpack and a container, either through an open container
// screen's slots or (Storm versions) straight into a container's item handler from a distance.
public final class UtilityTransfer {
    private UtilityTransfer() {}

    public interface Target {
        int size();

        ItemStack peek(int i);          // don't change the returned stack

        ItemStack insert(ItemStack s);  // returns what didn't fit

        ItemStack extract(int i, int n);
    }

    // The container side of an open screen (every slot that isn't the player's own inventory).
    public static Target slots(List<Slot> slots, Player player) {
        List<Slot> list = slots.stream().filter(s -> s.container != player.getInventory() && !(s instanceof ResultSlot)).toList();
        return new Target() {
            @Override public int size() { return list.size(); }
            @Override public ItemStack peek(int i) { return list.get(i).getItem(); }

            @Override
            public ItemStack insert(ItemStack s) {
                for (Slot slot : list) {
                    if (s.isEmpty()) break;
                    if (slot.hasItem() && ItemStack.isSameItemSameComponents(slot.getItem(), s)) s = slot.safeInsert(s);
                }
                for (Slot slot : list) {
                    if (s.isEmpty()) break;
                    if (!slot.hasItem()) s = slot.safeInsert(s);
                }
                return s;
            }

            @Override
            public ItemStack extract(int i, int n) {
                return list.get(i).safeTake(n, n, player);
            }
        };
    }

    public static Target handler(IItemHandler h) {
        return new Target() {
            @Override public int size() { return h.getSlots(); }
            @Override public ItemStack peek(int i) { return h.getStackInSlot(i); }
            @Override public ItemStack insert(ItemStack s) { return ItemHandlerHelper.insertItemStacked(h, s, false); }
            @Override public ItemStack extract(int i, int n) { return h.extractItem(i, n, false); }
        };
    }

    // Pack -> container. Allow list: the listed items. Block list: items the container already holds
    // (minus blocked ones). Returns the number of items moved.
    public static int deposit(Container pack, Target t, ItemStack upgrade) {
        boolean allow = BackpackUpgradeItem.allowList(upgrade);
        Set<Item> has = new HashSet<>();
        for (int j = 0; j < t.size(); j++) if (!t.peek(j).isEmpty()) has.add(t.peek(j).getItem());
        int moved = 0;
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack s = pack.getItem(i);
            if (s.isEmpty() || !BackpackUpgradeItem.matches(upgrade, s)) continue;
            if (!allow && !has.contains(s.getItem())) continue;
            ItemStack rest = t.insert(s.copy());
            int m = s.getCount() - rest.getCount();
            if (m > 0) {
                pack.setItem(i, rest);
                moved += m;
            }
        }
        if (moved > 0) pack.setChanged();
        return moved;
    }

    // Container -> pack. Allow list: the listed items. Block list: items the pack already holds.
    public static int restock(Container pack, Target t, ItemStack upgrade) {
        boolean allow = BackpackUpgradeItem.allowList(upgrade);
        Set<Item> has = new HashSet<>();
        for (int i = 0; i < pack.getContainerSize(); i++) if (!pack.getItem(i).isEmpty()) has.add(pack.getItem(i).getItem());
        int moved = 0;
        for (int j = 0; j < t.size(); j++) {
            ItemStack s = t.peek(j);
            if (s.isEmpty() || !BackpackUpgradeItem.matches(upgrade, s)) continue;
            if (!allow && !has.contains(s.getItem())) continue;
            int room = room(pack, s);
            if (room <= 0) continue;
            ItemStack got = t.extract(j, Math.min(room, s.getCount()));
            if (got.isEmpty()) continue;
            ItemStack rest = BackpackUpgrades.insert(pack, got.copy());
            moved += got.getCount() - rest.getCount();
            if (!rest.isEmpty()) t.insert(rest);
        }
        return moved;
    }

    // How many of this item the container could still take.
    public static int room(Container c, ItemStack s) {
        if (s.getItem() instanceof BackpackItem || !s.getItem().canFitInsideContainerItems()) return 0;
        int room = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack in = c.getItem(i);
            if (in.isEmpty()) room += c.getMaxStackSize(s);
            else if (ItemStack.isSameItemSameComponents(in, s)) room += Math.max(0, c.getMaxStackSize(in) - in.getCount());
        }
        return room;
    }
}
