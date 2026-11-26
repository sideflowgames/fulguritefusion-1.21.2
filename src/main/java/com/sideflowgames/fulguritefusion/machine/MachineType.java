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

// The processing machines: their shape, base speed and recipes.
// baseTicks = time per item at tier 1. Tiers 2/3/4 run 2x/4x/8x faster (see MachineTier).
public enum MachineType {
    PRESS("press", MachineShape.SMALL, 120),
    FURNACE("furnace", MachineShape.SMALL, 150),
    WIRE_EXTRUDER("wire_extruder", MachineShape.SMALL, 100),
    KILN("kiln", MachineShape.LARGE, 200),
    CRUSHER("crusher", MachineShape.LARGE, 160),
    BANBURY_MIXER("banbury_mixer", MachineShape.SMALL, 140),
    // Takes liquid latex (pipes into any casing face but the front, or buckets) and cures it: 1 bucket -> 3 Dried Rubber.
    LATEX_VULCANIZER("latex_vulcanizer", MachineShape.SMALL, 160);

    public final String id;
    public final MachineShape shape;
    public final int baseTicks;

    MachineType(String id, MachineShape shape, int baseTicks) {
        this.id = id;
        this.shape = shape;
        this.baseTicks = baseTicks;
    }

    public boolean usesLatex() {
        return this == LATEX_VULCANIZER;
    }

    // What one run makes. take0/take1 = items used from input slots 0 and 1. out1 may be empty.
    public record Result(int take0, int take1, ItemStack out0, ItemStack out1) {}

    @Nullable
    public Result findRecipe(Level level, ItemStack in0, ItemStack in1, RandomSource random) {
        return switch (this) {
            case PRESS -> press(in0);
            case WIRE_EXTRUDER -> extrude(in0, in1);
            case KILN -> kiln(in0, in1);
            case FURNACE -> smelt(level, in0);
            case CRUSHER -> crush(in0, random);
            case BANBURY_MIXER -> in0.is(ModItems.DRIED_RUBBER.get()) ? one(ModItems.MALLEABLE_RUBBER.get(), 1)
                    : in1.is(ModItems.DRIED_RUBBER.get()) ? new Result(0, 1, new ItemStack(ModItems.MALLEABLE_RUBBER.get()), ItemStack.EMPTY) : null;
            case LATEX_VULCANIZER -> null; // runs on its latex tank (MachineControllerBlockEntity)
        };
    }

    // Copper Sheet -> 2 Raw Wire.
    // 2 Raw Wire + an Iron Sheet -> 2 Raw Reinforced Wire.
    // Raw wire + Malleable Rubber -> the insulated wire of the same gauge: up to 4 basic, 2 reinforced or 1 heavy per rubber.
    // Two-item recipes take their items in either slot.
    @Nullable
    private static Result extrude(ItemStack in0, ItemStack in1) {
        Result r = insulate(in0, in1, false);
        if (r != null) return r;
        r = insulate(in1, in0, true);
        if (r != null) return r;
        if (in0.is(ModItems.RAW_WIRE.get()) && in1.is(ModItems.IRON_SHEET.get()) && in0.getCount() >= 2) {
            return new Result(2, 1, new ItemStack(ModItems.RAW_REINFORCED_WIRE.get(), 2), ItemStack.EMPTY);
        }
        if (in1.is(ModItems.RAW_WIRE.get()) && in0.is(ModItems.IRON_SHEET.get()) && in1.getCount() >= 2) {
            return new Result(1, 2, new ItemStack(ModItems.RAW_REINFORCED_WIRE.get(), 2), ItemStack.EMPTY);
        }
        if (in0.is(ModItems.COPPER_SHEET.get())) return new Result(1, 0, new ItemStack(ModItems.RAW_WIRE.get(), 2), ItemStack.EMPTY);
        if (in1.is(ModItems.COPPER_SHEET.get())) return new Result(0, 1, new ItemStack(ModItems.RAW_WIRE.get(), 2), ItemStack.EMPTY);
        return null;
    }

    @Nullable
    private static Result insulate(ItemStack wire, ItemStack rubber, boolean swapped) {
        if (!rubber.is(ModItems.MALLEABLE_RUBBER.get())) return null;
        if (!(wire.getItem() instanceof com.sideflowgames.fulguritefusion.item.WireRollItem roll) || roll.tier().insulated) return null;
        int n = Math.min(wire.getCount(), new int[]{4, 2, 1}[roll.tier().gauge]);
        ItemStack out = new ItemStack(ModItems.wireItem(roll.tier().insulatedVersion()), n);
        return swapped ? new Result(1, n, out, ItemStack.EMPTY) : new Result(n, 1, out, ItemStack.EMPTY);
    }

    // Ingots -> sheets (copper, iron, gold, silver; magnetite makes a plate); 4 Fulgurite Shards -> 1 Fulgurite.
    @Nullable
    private static Result press(ItemStack in) {
        if (in.is(ModItems.FULGURITE_SHARD.get())) {
            return in.getCount() >= 4 ? new Result(4, 0, new ItemStack(ModItems.FULGURITE.get()), ItemStack.EMPTY) : null;
        }
        return simple(in, Map.of(
                Items.COPPER_INGOT, () -> new ItemStack(ModItems.COPPER_SHEET.get()),
                Items.IRON_INGOT, () -> new ItemStack(ModItems.IRON_SHEET.get()),
                Items.GOLD_INGOT, () -> new ItemStack(ModItems.GOLD_SHEET.get()),
                ModItems.SILVER_INGOT.get(), () -> new ItemStack(ModItems.SILVER_SHEET.get()),
                ModItems.MAGNETITE_INGOT.get(), () -> new ItemStack(ModItems.MAGNETITE_PLATE.get())), 1);
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

    // Same rules as a vanilla furnace: any smelting recipe. Also dries a Latex Bucket into rubber (bucket comes back).
    private static Result smelt(Level level, ItemStack in) {
        if (in.isEmpty()) return null;
        if (in.is(ModItems.LATEX_BUCKET.get())) {
            return new Result(1, 0, new ItemStack(ModItems.DRIED_RUBBER.get(), 2), new ItemStack(Items.BUCKET));
        }
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return null;
        SingleRecipeInput input = new SingleRecipeInput(in);
        Optional<RecipeHolder<SmeltingRecipe>> recipe = server.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, server);
        return recipe.map(r -> new Result(1, 0, r.value().assemble(input).copy(), ItemStack.EMPTY))
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
        if (i == ModItems.RAW_MAGNETITE.get()) return one(ModItems.CRUSHED_MAGNETITE.get(), 2);
        if (i == ModItems.RAW_ARGENTITE.get()) return one(ModItems.CRUSHED_ARGENTITE.get(), 2);
        if (i == ModBlocks.MAGNETITE_ORE_ITEM.get() || i == ModBlocks.DEEPSLATE_MAGNETITE_ORE_ITEM.get()
                || i == ModBlocks.STORMSTONE_MAGNETITE_ORE_ITEM.get()) return one(ModItems.RAW_MAGNETITE.get(), 2);
        if (i == ModBlocks.ARGENTITE_ORE_ITEM.get() || i == ModBlocks.DEEPSLATE_ARGENTITE_ORE_ITEM.get()
                || i == ModBlocks.STORMSTONE_ARGENTITE_ORE_ITEM.get()) return one(ModItems.RAW_ARGENTITE.get(), 2);
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
