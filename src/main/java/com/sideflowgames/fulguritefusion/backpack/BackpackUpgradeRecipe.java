package com.sideflowgames.fulguritefusion.backpack;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

// A normal shaped crafting recipe that moves the old backpack's contents, upgrades and name into the new one.
// JSON: the same as "minecraft:crafting_shaped", with "type": "fulguritefusion:backpack_upgrade".
public class BackpackUpgradeRecipe implements CraftingRecipe {
    private final ShapedRecipe inner;

    public BackpackUpgradeRecipe(ShapedRecipe inner) {
        this.inner = inner;
    }

    public ShapedRecipe inner() {
        return inner;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return inner.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack out = inner.assemble(input, registries);
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.getItem() instanceof BackpackItem) {
                if (s.has(DataComponents.CONTAINER)) out.set(DataComponents.CONTAINER, s.get(DataComponents.CONTAINER));
                var ups = com.sideflowgames.fulguritefusion.ModDataComponents.BACKPACK_UPGRADES.get();
                if (s.has(ups)) out.set(ups, s.get(ups));
                if (s.has(DataComponents.CUSTOM_NAME)) out.set(DataComponents.CUSTOM_NAME, s.get(DataComponents.CUSTOM_NAME));
                break;
            }
        }
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return inner.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return inner.getResultItem(registries);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inner.getIngredients();
    }

    @Override
    public CraftingBookCategory category() {
        return inner.category();
    }

    @Override
    public String getGroup() {
        return inner.getGroup();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return com.sideflowgames.fulguritefusion.ModBlocks.BACKPACK_UPGRADE.get();
    }

    public static class Serializer implements RecipeSerializer<BackpackUpgradeRecipe> {
        private static final MapCodec<BackpackUpgradeRecipe> CODEC =
                RecipeSerializer.SHAPED_RECIPE.codec().xmap(BackpackUpgradeRecipe::new, BackpackUpgradeRecipe::inner);
        private static final StreamCodec<RegistryFriendlyByteBuf, BackpackUpgradeRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPED_RECIPE.streamCodec().map(BackpackUpgradeRecipe::new, BackpackUpgradeRecipe::inner);

        @Override
        public MapCodec<BackpackUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BackpackUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
