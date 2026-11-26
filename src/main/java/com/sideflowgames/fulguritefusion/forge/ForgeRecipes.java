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
    public static final int PRIME_COST = 20_000_000;
    public static final int PRIME_BACKPACK_COST = 5_000_000;

    private static Part item(Item item, int count) {
        return new Part(item.getName(new ItemStack(item)).getString(), s -> s.is(item), count);
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
            for (var t : List.of(
                    new Object[]{"Cloudstep Charm", ModItems.CLOUDSTEP_CHARM, ModItems.EMPOWERED_CLOUDSTEP_CHARM, ModItems.STORM_EYE},
                    new Object[]{"Conduit Heart", ModItems.CONDUIT_HEART, ModItems.EMPOWERED_CONDUIT_HEART, ModItems.MOLTEN_HEART},
                    new Object[]{"Void Arc Band", ModItems.VOID_ARC_BAND, ModItems.EMPOWERED_VOID_ARC_BAND, ModItems.VOID_SHARD},
                    new Object[]{"Grounding Charm", ModItems.GROUNDING_CHARM, ModItems.EMPOWERED_GROUNDING_CHARM, ModItems.CHARGED_SPARK_CELL},
                    new Object[]{"Cinder Ward", ModItems.CINDER_WARD, ModItems.EMPOWERED_CINDER_WARD, ModItems.CHARGED_SPARK_CELL},
                    new Object[]{"Seer's Ring", ModItems.SEERS_RING, ModItems.EMPOWERED_SEERS_RING, ModItems.CHARGED_SPARK_CELL})) {
                @SuppressWarnings("unchecked") var base = (java.util.function.Supplier<? extends Item>) t[1];
                @SuppressWarnings("unchecked") var result = (java.util.function.Supplier<? extends Item>) t[2];
                @SuppressWarnings("unchecked") var extra = (java.util.function.Supplier<? extends Item>) t[3];
                all.add(new Recipe("Empowered " + t[0], item(base.get(), 1),
                        List.of(item(extra.get(), 1), item(ModItems.REFINED_FULGURITE.get(), 4)), result::get, 2_000_000));
            }
            all.add(new Recipe("Storm Sigil", item(ModItems.STORM_EYE.get(), 1),
                    List.of(item(ModItems.MOLTEN_HEART.get(), 1), item(ModItems.VOID_SHARD.get(), 1), item(ModItems.CHARGED_SPARK_CELL.get(), 1)),
                    ModItems.STORM_SIGIL::get, 5_000_000));
            all.add(new Recipe("Tempest Staff", item(ModItems.TEMPEST_CRYSTAL.get(), 1),
                    List.of(item(ModItems.REFINED_FULGURITE.get(), 2), item(ModItems.STORMQUARTZ.get(), 2), item(ModItems.CHARGED_SPARK_CELL.get(), 1)),
                    ModItems.TEMPEST_STAFF::get, STORMFORGED_COST));
            prime("Prime Blade", ModItems.STORM_BLADE.get(), ModItems.PRIME_BLADE::get, PRIME_COST);
            prime("Prime Spear", ModItems.STORM_SPEAR.get(), ModItems.PRIME_SPEAR::get, PRIME_COST);
            prime("Prime Pickaxe", ModItems.STORMFORGED_PICKAXE.get(), ModItems.PRIME_PICKAXE::get, PRIME_COST);
            prime("Prime Axe", ModItems.STORMFORGED_AXE.get(), ModItems.PRIME_AXE::get, PRIME_COST);
            prime("Prime Shovel", ModItems.STORMFORGED_SHOVEL.get(), ModItems.PRIME_SHOVEL::get, PRIME_COST);
            prime("Prime Hoe", ModItems.STORMFORGED_HOE.get(), ModItems.PRIME_HOE::get, PRIME_COST);
            prime("Prime Helmet", ModItems.STORMFORGED_HELMET.get(), ModItems.PRIME_HELMET::get, PRIME_COST);
            prime("Prime Chestplate", ModItems.STORMFORGED_CHESTPLATE.get(), ModItems.PRIME_CHESTPLATE::get, PRIME_COST);
            prime("Prime Leggings", ModItems.STORMFORGED_LEGGINGS.get(), ModItems.PRIME_LEGGINGS::get, PRIME_COST);
            prime("Prime Boots", ModItems.STORMFORGED_BOOTS.get(), ModItems.PRIME_BOOTS::get, PRIME_COST);
            prime("Prime Backpack", com.sideflowgames.fulguritefusion.ModBlocks.STORMFORGED_BACKPACK_ITEM.get(),
                    () -> com.sideflowgames.fulguritefusion.ModBlocks.PRIME_BACKPACK_ITEM.get(), PRIME_BACKPACK_COST);
        }
        return all;
    }

    private static void stormforged(String name, Item base, Supplier<Item> result) {
        all.add(new Recipe(name, item(base, 1),
                List.of(item(ModItems.REFINED_FULGURITE.get(), 2), STRONG_FUEL), result, STORMFORGED_COST));
    }

    // Prime: the Stormforged item (enchantments and name kept) + a Prime Spark Fragment + a Block of Gold and a
    // Block of Silver.
    private static void prime(String name, Item base, Supplier<Item> result, int cost) {
        all.add(new Recipe(name, item(base, 1),
                List.of(item(ModItems.PRIME_SPARK_FRAGMENT.get(), 1), item(Items.GOLD_BLOCK, 1),
                        item(com.sideflowgames.fulguritefusion.ModBlocks.SILVER_BLOCK_ITEM.get(), 1)), result, cost));
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
        return match(contents, all());
    }

    @Nullable
    public static Recipe match(List<ItemStack> contents, List<Recipe> recipes) {
        if (contents.isEmpty()) return null;
        outer:
        for (Recipe r : recipes) {
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
