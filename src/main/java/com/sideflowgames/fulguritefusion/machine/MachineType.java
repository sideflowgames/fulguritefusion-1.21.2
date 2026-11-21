package com.sideflowgames.fulguritefusion.machine;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

// The five processing machines: their shape, base speed and recipes.
// baseTicks = time per item at tier 1. Tiers 2/3/4 run 2x/4x/8x faster (see MachineTier).
public enum MachineType {
    PRESS("press", MachineShape.SMALL, 120),
    FURNACE("furnace", MachineShape.SMALL, 150),
    WIRE_EXTRUDER("wire_extruder", MachineShape.SMALL, 100),
    KILN("kiln", MachineShape.LARGE, 200),
    CRUSHER("crusher", MachineShape.LARGE, 160);

    public final String id;
    public final MachineShape shape;
    public final int baseTicks;

    MachineType(String id, MachineShape shape, int baseTicks) {
        this.id = id;
        this.shape = shape;
        this.baseTicks = baseTicks;
    }

    // What one run makes. take0/take1 = items used from input slots 0 and 1. out1 may be empty.
    public record Result(int take0, int take1, ItemStack out0, ItemStack out1) {}

    @Nullable
    public Result findRecipe(Level level, ItemStack in0, ItemStack in1, RandomSource random) {
        return switch (this) {
            case PRESS -> press(in0);
            case WIRE_EXTRUDER -> in0.is(ModItems.COPPER_SHEET.get())
                    ? new Result(1, 0, new ItemStack(ModItems.RAW_WIRE.get(), 2), ItemStack.EMPTY) : null;
            case KILN -> kiln(in0, in1);
            case FURNACE -> smelt(level, in0);
            case CRUSHER -> crush(in0, random);
        };
    }

    // Ingots -> sheets; 4 Fulgurite Shards -> 1 Fulgurite.
    @Nullable
    private static Result press(ItemStack in) {
        if (in.is(ModItems.FULGURITE_SHARD.get())) {
            return in.getCount() >= 4 ? new Result(4, 0, new ItemStack(ModItems.FULGURITE.get()), ItemStack.EMPTY) : null;
        }
        return simple(in, Map.of(
                Items.COPPER_INGOT, () -> new ItemStack(ModItems.COPPER_SHEET.get()),
                Items.IRON_INGOT, () -> new ItemStack(ModItems.IRON_SHEET.get()),
                Items.GOLD_INGOT, () -> new ItemStack(ModItems.GOLD_SHEET.get())), 1);
    }

    // 2 Fulgurite -> Coil Segment (either input slot).
    // Sand + Fulgurite Shard -> 4 Fulgurite Blast Glass; Cobbled Deepslate + Fulgurite Shard -> 4 Reinforced Fulgurite.
    @Nullable
    private static Result kiln(ItemStack a, ItemStack b) {
        if (a.is(ModItems.FULGURITE.get()) && a.getCount() >= 2) return new Result(2, 0, new ItemStack(ModItems.COIL_SEGMENT.get()), ItemStack.EMPTY);
        if (b.is(ModItems.FULGURITE.get()) && b.getCount() >= 2) return new Result(0, 2, new ItemStack(ModItems.COIL_SEGMENT.get()), ItemStack.EMPTY);
        Result r = pair(a, b, Items.SAND, ModItems.FULGURITE_SHARD.get(), new ItemStack(ModBlocks.FULGURITE_BLAST_GLASS_ITEM.get(), 4));
        if (r != null) return r;
        return pair(a, b, Items.COBBLED_DEEPSLATE, ModItems.FULGURITE_SHARD.get(), new ItemStack(ModBlocks.REINFORCED_FULGURITE_ITEM.get(), 4));
    }

    // One of x and one of y, in either slot.
    @Nullable
    private static Result pair(ItemStack a, ItemStack b, Item x, Item y, ItemStack out) {
        if (a.is(x) && b.is(y)) return new Result(1, 1, out, ItemStack.EMPTY);
        if (a.is(y) && b.is(x)) return new Result(1, 1, out, ItemStack.EMPTY);
        return null;
    }

    private static Result simple(ItemStack in, Map<Item, Supplier<ItemStack>> table, int take) {
        Supplier<ItemStack> out = table.get(in.getItem());
        return out != null && in.getCount() >= take ? new Result(take, 0, out.get(), ItemStack.EMPTY) : null;
    }

    // Same rules as a vanilla furnace: any smelting recipe.
    private static Result smelt(Level level, ItemStack in) {
        if (in.isEmpty()) return null;
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(in), level);
        return recipe.map(r -> new Result(1, 0, r.value().getResultItem(level.registryAccess()).copy(), ItemStack.EMPTY))
                .orElse(null);
    }

    // Ore doubling and breaking blocks down into their base parts.
    private static Result crush(ItemStack in, RandomSource random) {
        if (in.isEmpty()) return null;
        Item i = in.getItem();
        // A Raw Fulgurite block -> 10 Fulgurite Shards (mining gives 4-8).
        if (i == ModBlocks.RAW_FULGURITE_ITEM.get()) return one(ModItems.FULGURITE_SHARD.get(), 10);
        // Raw ore -> 2 crushed ore (smelts into ingots).
        if (i == Items.RAW_IRON) return one(ModItems.CRUSHED_IRON.get(), 2);
        if (i == Items.RAW_COPPER) return one(ModItems.CRUSHED_COPPER.get(), 2);
        if (i == Items.RAW_GOLD) return one(ModItems.CRUSHED_GOLD.get(), 2);
        // Ore blocks (silk touch) -> double their normal drop.
        if (i == Items.IRON_ORE || i == Items.DEEPSLATE_IRON_ORE) return one(Items.RAW_IRON, 2);
        if (i == Items.COPPER_ORE || i == Items.DEEPSLATE_COPPER_ORE) return one(Items.RAW_COPPER, 6);
        if (i == Items.GOLD_ORE || i == Items.DEEPSLATE_GOLD_ORE) return one(Items.RAW_GOLD, 2);
        if (i == Items.COAL_ORE || i == Items.DEEPSLATE_COAL_ORE) return one(Items.COAL, 2);
        if (i == Items.REDSTONE_ORE || i == Items.DEEPSLATE_REDSTONE_ORE) return one(Items.REDSTONE, 8);
        if (i == Items.LAPIS_ORE || i == Items.DEEPSLATE_LAPIS_ORE) return one(Items.LAPIS_LAZULI, 8);
        if (i == Items.DIAMOND_ORE || i == Items.DEEPSLATE_DIAMOND_ORE) return one(Items.DIAMOND, 2);
        if (i == Items.EMERALD_ORE || i == Items.DEEPSLATE_EMERALD_ORE) return one(Items.EMERALD, 2);
        if (i == Items.NETHER_QUARTZ_ORE) return one(Items.QUARTZ, 2);
        if (i == Items.NETHER_GOLD_ORE) return one(Items.GOLD_NUGGET, 8);
        if (i == Items.ANCIENT_DEBRIS) return one(Items.NETHERITE_SCRAP, 2);
        // Blocks -> base parts.
        if (i == Items.STONE) return one(Items.COBBLESTONE, 1);
        if (i == Items.COBBLESTONE) return one(Items.GRAVEL, 1);
        if (i == Items.GRAVEL) {
            return new Result(1, 0, new ItemStack(Items.SAND), random.nextFloat() < 0.5f ? new ItemStack(Items.FLINT) : ItemStack.EMPTY);
        }
        if (i == Items.SANDSTONE) return one(Items.SAND, 4);
        if (i == Items.RED_SANDSTONE) return one(Items.RED_SAND, 4);
        return null;
    }

    private static Result one(Item out, int count) {
        return new Result(1, 0, new ItemStack(out, count), ItemStack.EMPTY);
    }
}
