package com.sideflowgames.fulguritefusion.forge;

import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.item.FmChargeable;
import com.sideflowgames.fulguritefusion.item.FusionFuelItem;
import com.sideflowgames.fulguritefusion.pairing.Pairings;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

// Storm Forge recipes. Every ingredient sits on the Forge Anvil; when the contents match exactly, the forge
// draws the recipe's Fm (up to 8,192 Fm/t) and then turns them into the result.
// An upgrade keeps the base item's enchantments and custom name. Forged Fm gear comes out half charged.
public final class ForgeRecipes {
    private ForgeRecipes() {}

    public record Part(String name, Predicate<ItemStack> test, int count) {}

    public record Recipe(String name, Part base, List<Part> others, Supplier<Item> result, int fmCost) {
        List<Part> parts() {
            List<Part> all = new ArrayList<>();
            all.add(base);
            all.addAll(others);
            return all;
        }

        public ItemStack make(List<ItemStack> contents) {
            ItemStack out = new ItemStack(result.get());
            for (ItemStack s : contents) {
                if (base.test().test(s)) {
                    if (s.has(DataComponents.ENCHANTMENTS)) out.set(DataComponents.ENCHANTMENTS, s.get(DataComponents.ENCHANTMENTS));
                    if (s.has(DataComponents.CUSTOM_NAME)) out.set(DataComponents.CUSTOM_NAME, s.get(DataComponents.CUSTOM_NAME));
                    if (s.has(DataComponents.CONTAINER)) out.set(DataComponents.CONTAINER, s.get(DataComponents.CONTAINER));
                    copy(s, out, com.sideflowgames.fulguritefusion.ModDataComponents.BACKPACK_UPGRADES.get());
                    copy(s, out, com.sideflowgames.fulguritefusion.ModDataComponents.FILTER.get());
                    copy(s, out, com.sideflowgames.fulguritefusion.ModDataComponents.FILTER_MODE.get());
                    break;
                }
            }
            if (FmChargeable.is(out)) FmChargeable.set(out, FmChargeable.max(out) / 2);
            return out;
        }
    }

    private static <T> void copy(ItemStack from, ItemStack to, net.minecraft.core.component.DataComponentType<T> type) {
        if (from.has(type)) to.set(type, from.get(type));
    }

    public static final int STORMFORGED_COST = 500_000;
    public static final int UPGRADE_COST = 100_000;
    public static final int BACKPACK_COST = 250_000;

    private static Part item(Item item, int count) {
        return new Part(item.getDescription().getString(), s -> s.is(item), count);
    }

    // A Fusion Fuel capsule of Strong or Jackpot tier.
    private static final Part STRONG_FUEL = new Part("Strong or Jackpot Fusion Fuel", s -> {
        if (!(s.getItem() instanceof FusionFuelItem)) return false;
        Pairings.Pairing p = FusionFuelItem.result(s);
        return p != null && (p.tier() == Pairings.Tier.STRONG || p.tier() == Pairings.Tier.JACKPOT);
    }, 1);

    private static List<Recipe> all;

    public static List<Recipe> all() {
        if (all == null) {
            all = new ArrayList<>();
            stormforged("Stormforged Helmet", Items.DIAMOND_HELMET, ModItems.STORMFORGED_HELMET::get);
            stormforged("Stormforged Chestplate", Items.DIAMOND_CHESTPLATE, ModItems.STORMFORGED_CHESTPLATE::get);
            stormforged("Stormforged Leggings", Items.DIAMOND_LEGGINGS, ModItems.STORMFORGED_LEGGINGS::get);
            stormforged("Stormforged Boots", Items.DIAMOND_BOOTS, ModItems.STORMFORGED_BOOTS::get);
            all.add(new Recipe("Stormforged Backpack", item(com.sideflowgames.fulguritefusion.ModBlocks.FULGURITE_BACKPACK_ITEM.get(), 1),
                    List.of(item(ModItems.REFINED_FULGURITE.get(), 2), STRONG_FUEL),
                    () -> com.sideflowgames.fulguritefusion.ModBlocks.STORMFORGED_BACKPACK_ITEM.get(), BACKPACK_COST));
            stormUpgrade("Storm Pickup Upgrade", ModItems.PICKUP_UPGRADE.get(), ModItems.STORM_PICKUP_UPGRADE::get);
            stormUpgrade("Storm Magnet Upgrade", ModItems.MAGNET_UPGRADE.get(), ModItems.STORM_MAGNET_UPGRADE::get);
            stormUpgrade("Storm Void Upgrade", ModItems.VOID_UPGRADE.get(), ModItems.STORM_VOID_UPGRADE::get);
            stormUpgrade("Storm Compacting Upgrade", ModItems.COMPACTING_UPGRADE.get(), ModItems.STORM_COMPACTING_UPGRADE::get);
            stormUpgrade("Storm Feeding Upgrade", ModItems.FEEDING_UPGRADE.get(), ModItems.STORM_FEEDING_UPGRADE::get);
            stormUpgrade("Storm Refill Upgrade", ModItems.REFILL_UPGRADE.get(), ModItems.STORM_REFILL_UPGRADE::get);
            stormUpgrade("Storm Restock Upgrade", ModItems.RESTOCK_UPGRADE.get(), ModItems.STORM_RESTOCK_UPGRADE::get);
            stormUpgrade("Storm Deposit Upgrade", ModItems.DEPOSIT_UPGRADE.get(), ModItems.STORM_DEPOSIT_UPGRADE::get);
            stormUpgrade("Storm Tool Swapper Upgrade", ModItems.TOOL_SWAPPER_UPGRADE.get(), ModItems.STORM_TOOL_SWAPPER_UPGRADE::get);
            stormforged("Storm Blade", ModItems.FULGURITE_SWORD.get(), ModItems.STORM_BLADE::get);
            stormforged("Storm Spear", ModItems.FULGURITE_SPEAR.get(), ModItems.STORM_SPEAR::get);
            stormforged("Stormforged Pickaxe", ModItems.FULGURITE_PICKAXE.get(), ModItems.STORMFORGED_PICKAXE::get);
            stormforged("Stormforged Axe", ModItems.FULGURITE_AXE.get(), ModItems.STORMFORGED_AXE::get);
            stormforged("Stormforged Shovel", ModItems.FULGURITE_SHOVEL.get(), ModItems.STORMFORGED_SHOVEL::get);
            stormforged("Stormforged Hoe", ModItems.FULGURITE_HOE.get(), ModItems.STORMFORGED_HOE::get);
        }
        return all;
    }

    private static void stormforged(String name, Item base, Supplier<Item> result) {
        all.add(new Recipe(name, item(base, 1),
                List.of(item(ModItems.REFINED_FULGURITE.get(), 2), STRONG_FUEL), result, STORMFORGED_COST));
    }

    // Stormforged backpack upgrade: the basic upgrade (its filter is kept) + Refined Fulgurite + an
    // Overcharged Spark Cell.
    private static void stormUpgrade(String name, Item base, Supplier<Item> result) {
        all.add(new Recipe(name, item(base, 1),
                List.of(item(ModItems.REFINED_FULGURITE.get(), 1), item(ModItems.OVERCHARGED_SPARK_CELL.get(), 1)), result, UPGRADE_COST));
    }

    // The recipe the anvil's contents make, or null. Every item must be used, in exactly the right amounts.
    @Nullable
    public static Recipe match(List<ItemStack> contents) {
        if (contents.isEmpty()) return null;
        outer:
        for (Recipe r : all()) {
            int[] have = new int[r.parts().size()];
            for (ItemStack s : contents) {
                boolean used = false;
                for (int i = 0; i < have.length; i++) {
                    if (r.parts().get(i).test().test(s)) {
                        have[i] += s.getCount();
                        used = true;
                        break;
                    }
                }
                if (!used) continue outer;
            }
            for (int i = 0; i < have.length; i++) if (have[i] != r.parts().get(i).count()) continue outer;
            return r;
        }
        return null;
    }
}
