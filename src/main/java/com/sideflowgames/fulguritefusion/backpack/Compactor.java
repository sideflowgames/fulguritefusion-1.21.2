package com.sideflowgames.fulguritefusion.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// The Compacting upgrade's crafting: turns a full 2x2 (or 3x3) grid of one item into its block, but only when
// that block crafts straight back into the same items (so nothing is ever lost, and planks never become
// crafting tables). Items with extra data (names, enchantments) are left alone.
public final class Compactor {
    private Compactor() {}

    private static RecipeManager cachedFor;
    private static final Map<Item, Optional<Item>> TWO = new HashMap<>(), THREE = new HashMap<>();

    private static Optional<Item> result(Level level, Item item, int size) {
        RecipeManager rm = ((net.minecraft.server.level.ServerLevel) level).recipeAccess();
        if (rm != cachedFor) {
            cachedFor = rm;
            TWO.clear();
            THREE.clear();
        }
        return (size == 2 ? TWO : THREE).computeIfAbsent(item, i -> find(level, i, size));
    }

    private static Optional<Item> find(Level level, Item item, int size) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < size * size; i++) grid.add(new ItemStack(item));
        CraftingInput in = CraftingInput.of(size, size, grid);
        var recipe = ((net.minecraft.server.level.ServerLevel) level).recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, level);
        if (recipe.isEmpty()) return Optional.empty();
        ItemStack out = recipe.get().value().assemble(in);
        if (out.isEmpty() || out.getCount() != 1 || !out.isComponentsPatchEmpty()) return Optional.empty();
        CraftingInput back = CraftingInput.of(1, 1, List.of(out.copy()));
        var reverse = ((net.minecraft.server.level.ServerLevel) level).recipeAccess().getRecipeFor(RecipeType.CRAFTING, back, level);
        if (reverse.isEmpty()) return Optional.empty();
        ItemStack undo = reverse.get().value().assemble(back);
        if (!undo.is(item) || undo.getCount() != size * size || !undo.isComponentsPatchEmpty()) return Optional.empty();
        return Optional.of(out.getItem());
    }

    // Compacts what the upgrade's filter allows. Returns how many item types were packed 3x3.
    public static int compact(Level level, Container c, ItemStack upgrade, boolean allow3x3) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (!s.isEmpty() && s.isComponentsPatchEmpty() && BackpackUpgradeItem.matches(upgrade, s)) {
                counts.merge(s.getItem(), s.getCount(), Integer::sum);
            }
        }
        int big = 0;
        for (var e : counts.entrySet()) {
            for (int size : allow3x3 ? new int[]{3, 2} : new int[]{2}) {
                int k = size * size;
                if (e.getValue() < k) continue;
                Optional<Item> out = result(level, e.getKey(), size);
                if (out.isEmpty()) continue;
                if (craft(c, e.getKey(), k, out.get(), e.getValue() / k)) {
                    if (size == 3) big++;
                    break;
                }
            }
        }
        return big;
    }

    // Done on a copy first; only applied if every result fits.
    private static boolean craft(Container c, Item in, int k, Item out, int crafts) {
        int n = c.getContainerSize();
        NonNullList<ItemStack> sim = NonNullList.withSize(n, ItemStack.EMPTY);
        for (int i = 0; i < n; i++) sim.set(i, c.getItem(i).copy());
        int remove = crafts * k;
        for (int i = 0; i < n && remove > 0; i++) {
            ItemStack s = sim.get(i);
            if (s.is(in) && s.isComponentsPatchEmpty()) {
                int r = Math.min(remove, s.getCount());
                s.shrink(r);
                remove -= r;
                if (s.isEmpty()) sim.set(i, ItemStack.EMPTY);
            }
        }
        ItemStack add = new ItemStack(out, crafts);
        for (int i = 0; i < n && !add.isEmpty(); i++) {
            ItemStack s = sim.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, add)) {
                int move = Math.min(add.getCount(), c.getMaxStackSize(s) - s.getCount());
                if (move > 0) {
                    s.grow(move);
                    add.shrink(move);
                }
            }
        }
        for (int i = 0; i < n && !add.isEmpty(); i++) {
            if (sim.get(i).isEmpty()) sim.set(i, add.split(Math.min(add.getCount(), c.getMaxStackSize(add))));
        }
        if (!add.isEmpty()) return false;
        for (int i = 0; i < n; i++) {
            if (!ItemStack.matches(sim.get(i), c.getItem(i))) c.setItem(i, sim.get(i));
        }
        c.setChanged();
        return true;
    }
}
