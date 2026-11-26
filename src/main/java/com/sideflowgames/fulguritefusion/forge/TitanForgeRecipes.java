package com.sideflowgames.fulguritefusion.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Titan Forge recipes: a Prime armor piece + the Titan piece of the same type + Stormreach ores -> the Titan
// Prime piece of that type. Every piece costs 50,000,000 Fm. Aurorite is the backbone of all four; each piece
// adds two more Stormreach ores.
public final class TitanForgeRecipes {
    private TitanForgeRecipes() {}

    public static final int COST = 50_000_000;

    private static List<ForgeRecipes.Recipe> all;

    private static ForgeRecipes.Part item(Item item, int count) {
        return new ForgeRecipes.Part(item.getName(new ItemStack(item)).getString(), s -> s.is(item), count);
    }

    public static List<ForgeRecipes.Recipe> all() {
        if (all == null) {
            all = new ArrayList<>();
            add("Titan Prime Helmet", ModItems.PRIME_HELMET.get(), ModItems.TITAN_HELMET.get(), ModItems.TITAN_PRIME_HELMET::get,
                    item(ModItems.AURORITE_INGOT.get(), 5), item(ModItems.PRISMITE_SHARD.get(), 3), item(ModItems.FOSSIL_AMBER.get(), 2));
            add("Titan Prime Chestplate", ModItems.PRIME_CHESTPLATE.get(), ModItems.TITAN_CHESTPLATE.get(), ModItems.TITAN_PRIME_CHESTPLATE::get,
                    item(ModItems.AURORITE_INGOT.get(), 8), item(ModItems.CINDERITE.get(), 4), item(ModItems.STORMQUARTZ.get(), 4));
            add("Titan Prime Leggings", ModItems.PRIME_LEGGINGS.get(), ModItems.TITAN_LEGGINGS.get(), ModItems.TITAN_PRIME_LEGGINGS::get,
                    item(ModItems.AURORITE_INGOT.get(), 7), item(ModItems.PRISMITE_SHARD.get(), 4), item(ModItems.CINDERITE.get(), 3));
            add("Titan Prime Boots", ModItems.PRIME_BOOTS.get(), ModItems.TITAN_BOOTS.get(), ModItems.TITAN_PRIME_BOOTS::get,
                    item(ModItems.AURORITE_INGOT.get(), 4), item(ModItems.FOSSIL_AMBER.get(), 3), item(ModItems.STORMQUARTZ.get(), 3));
        }
        return all;
    }

    private static void add(String name, Item prime, Item titan, Supplier<Item> result, ForgeRecipes.Part... ores) {
        List<ForgeRecipes.Part> others = new ArrayList<>();
        others.add(item(titan, 1));
        others.addAll(List.of(ores));
        all.add(new ForgeRecipes.Recipe(name, item(prime, 1), others, result, COST));
    }

    // Same exact-match rule as the Storm Forge: every item used, in exactly the right amounts.
    @Nullable
    public static ForgeRecipes.Recipe match(List<ItemStack> contents) {
        return ForgeRecipes.match(contents, all());
    }
}
