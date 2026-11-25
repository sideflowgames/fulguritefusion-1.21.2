package com.sideflowgames.fulguritefusion.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

// A backpack's contents (or its upgrade slots) while it's open from an item: read from the stack's
// component, written back on every change.
public class BackpackItemContainer extends SimpleContainer {
    private final ItemStack backpack;
    private final DataComponentType<ItemContainerContents> component;

    public BackpackItemContainer(ItemStack backpack, int size) {
        this(backpack, size, DataComponents.CONTAINER);
    }

    public BackpackItemContainer(ItemStack backpack, int size, DataComponentType<ItemContainerContents> component) {
        super(size);
        this.backpack = backpack;
        this.component = component;
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        backpack.getOrDefault(component, ItemContainerContents.EMPTY).copyInto(items);
        for (int i = 0; i < size; i++) setItem(i, items.get(i));
    }

    public ItemStack backpack() {
        return backpack;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        backpack.set(component, ItemContainerContents.fromItems(getItems()));
    }

    // Only while this exact backpack is still on the player (inventory, armor, offhand or a Curios slot).
    // If the stack was moved, dropped or replaced, the screen closes, so the contents can't be split between
    // two copies of the backpack.
    @Override
    public boolean stillValid(Player player) {
        if (backpack.isEmpty() || !player.isAlive()) return false;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i) == backpack) return true;
        }
        if (com.sideflowgames.fulguritefusion.trinket.Trinkets.back(player) == backpack) return true;
        return CuriosCompat.find(player, s -> s == backpack) == backpack;
    }
}
