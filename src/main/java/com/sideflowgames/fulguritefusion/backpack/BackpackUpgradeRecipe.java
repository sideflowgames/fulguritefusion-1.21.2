package com.sideflowgames.fulguritefusion.backpack;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

// A normal shaped crafting recipe that moves the old backpack's contents, upgrades and name into the new one.
// JSON: the same as "minecraft:crafting_shaped", with "type": "fulguritefusion:backpack_upgrade".
public class BackpackUpgradeRecipe extends NormalCraftingRecipe {
    public static final MapCodec<BackpackUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
            ).apply(i, BackpackUpgradeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
            ItemStackTemplate.STREAM_CODEC, o -> o.result,
            BackpackUpgradeRecipe::new);
    public static final RecipeSerializer<BackpackUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;
    private final ShapedRecipe inner;

    public BackpackUpgradeRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo);
        this.pattern = pattern;
        this.result = result;
        this.inner = new ShapedRecipe(commonInfo, bookInfo, pattern, result);
    }

    public ShapedRecipe inner() {
        return inner;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return inner.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack out = inner.assemble(input);
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
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(pattern.ingredients());
    }

    @Override
    public List<RecipeDisplay> display() {
        return inner.display();
    }

    @Override
    public RecipeSerializer<BackpackUpgradeRecipe> getSerializer() {
        return com.sideflowgames.fulguritefusion.ModBlocks.BACKPACK_UPGRADE.get();
    }
}
