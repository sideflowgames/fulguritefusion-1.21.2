package com.sideflowgames.fulguritefusion.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

// 26.1: clients no longer get every recipe. The server sends the crafting recipes along with its data
// (FulguriteFusion.onDatapackSync asks NeoForge to), and this keeps them for the guide book's recipe grids.
public final class GuideRecipes {
    private GuideRecipes() {}

    private static RecipeMap map = RecipeMap.EMPTY;

    // What a recipe grid shows: each of the 3x3 slots' options (empty list = empty slot) and the result.
    public record Shown(boolean shaped, int width, List<List<ItemStack>> slots, ItemStack result) {}

    public static void received(RecipeMap recipes) {
        map = recipes;
    }

    public static Optional<RecipeHolder<?>> byKey(Identifier id) {
        return Optional.ofNullable(map.byKey(ResourceKey.create(Registries.RECIPE, id)));
    }

    public static Collection<RecipeHolder<CraftingRecipe>> crafting() {
        return map.byType(RecipeType.CRAFTING);
    }

    public static Optional<Shown> show(Recipe<?> recipe, Level level) {
        ContextMap context = SlotDisplayContext.fromLevel(level);
        for (RecipeDisplay display : recipe.display()) {
            if (display instanceof ShapedCraftingRecipeDisplay shaped) {
                return Optional.of(new Shown(true, shaped.width(), resolve(shaped.ingredients(), context), shaped.result().resolveForFirstStack(context)));
            }
            if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
                return Optional.of(new Shown(false, 3, resolve(shapeless.ingredients(), context), shapeless.result().resolveForFirstStack(context)));
            }
        }
        return Optional.empty();
    }

    // The item a recipe makes (empty if it isn't a plain crafting grid).
    public static ItemStack result(Recipe<?> recipe, Level level) {
        return show(recipe, level).map(Shown::result).orElse(ItemStack.EMPTY);
    }

    private static List<List<ItemStack>> resolve(List<SlotDisplay> slots, ContextMap context) {
        List<List<ItemStack>> out = new ArrayList<>();
        for (SlotDisplay s : slots) out.add(s.resolveForStacks(context));
        return out;
    }
}
