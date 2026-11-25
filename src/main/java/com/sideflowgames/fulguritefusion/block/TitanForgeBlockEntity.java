package com.sideflowgames.fulguritefusion.block;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.forge.ForgeRecipes;
import com.sideflowgames.fulguritefusion.forge.TitanForgeRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Titan Forge: fuses a Prime armor piece with the Titan piece of the same type, plus Stormreach ores, into Titan
// Prime armor (see forge/TitanForgeRecipes). Works like the Storm Forge (set items on the anvil, feed it Fm) but
// bigger and hungrier. The structure (checked every second):
//   - a 7x7 floor under the anvil: the middle 3x3 of Charged Arc Casing, the rest Stormstone Bricks (Forge Power
//     Ports may stand in for any outer-edge brick except the corners),
//   - a Storm Pylon on each floor corner: 2 Stormstone Bricks, a Prismite Block and a Lightning Rod on top.
// The result keeps the Prime piece's name and the best enchantments from both pieces.
public class TitanForgeBlockEntity extends StormForgeBlockEntity {
    public static final int TITAN_DRAW_RATE = 32768;
    public static final int[][] TITAN_CORNERS = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};

    public TitanForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TITAN_FORGE_BE.get(), pos, state);
    }

    public static boolean isOuterEdge(int dx, int dz) {
        return (Math.abs(dx) == 3 || Math.abs(dz) == 3) && !(Math.abs(dx) == 3 && Math.abs(dz) == 3);
    }

    @Override
    protected String title() {
        return "Titan Forge";
    }

    @Override
    protected int drawRate() {
        return TITAN_DRAW_RATE;
    }

    @Override
    protected int buffer() {
        return TITAN_DRAW_RATE * 20;
    }

    @Override
    public boolean acceptsPort(int dx, int dz) {
        return isOuterEdge(dx, dz);
    }

    @Nullable
    @Override
    protected ForgeRecipes.Recipe matchRecipe(List<ItemStack> items) {
        return TitanForgeRecipes.match(items);
    }

    // The Prime piece is the base (its name and enchantments carry over); the Titan piece's enchantments are
    // added on top, keeping the higher level of any the two share.
    @Override
    protected ItemStack craft(ForgeRecipes.Recipe recipe, List<ItemStack> items) {
        ItemStack out = recipe.make(items);
        for (ItemStack s : items) {
            if (s.getItem() instanceof com.sideflowgames.fulguritefusion.item.TitanArmorItem) {
                ItemEnchantments extra = s.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                EnchantmentHelper.updateEnchantments(out, m -> {
                    for (var e : extra.entrySet()) m.upgrade(e.getKey(), e.getIntValue());
                });
            }
        }
        return out;
    }

    @Override
    protected List<Vec3> pylonTips() {
        List<Vec3> out = new ArrayList<>();
        for (int[] c : TITAN_CORNERS) out.add(Vec3.atCenterOf(worldPosition.offset(c[0], 3, c[1])).add(0, 0.4, 0));
        return out;
    }

    @Nullable
    @Override
    protected String checkStructure() {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockState s = level.getBlockState(worldPosition.offset(dx, -1, dz));
                boolean inner = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                boolean ok = inner ? s.is(ModBlocks.CHARGED_ARC_CASING.get())
                        : s.is(ModBlocks.STORMSTONE_BRICKS.get()) || (isOuterEdge(dx, dz) && s.is(ModBlocks.FORGE_POWER_PORT.get()));
                if (!ok) return "floor at " + dx + ", " + dz + (inner ? " needs Charged Arc Casing" : " needs Stormstone Bricks");
            }
        }
        for (int[] c : TITAN_CORNERS) {
            BlockPos base = worldPosition.offset(c[0], 0, c[1]);
            if (!level.getBlockState(base).is(ModBlocks.STORMSTONE_BRICKS.get()) || !level.getBlockState(base.above()).is(ModBlocks.STORMSTONE_BRICKS.get())) {
                return "a corner pylon needs 2 Stormstone Bricks";
            }
            if (!level.getBlockState(base.above(2)).is(ModBlocks.PRISMITE_BLOCK.get())) return "a corner pylon needs a Prismite Block third";
            if (!level.getBlockState(base.above(3)).is(Blocks.LIGHTNING_ROD)) return "a corner pylon needs a Lightning Rod on top";
        }
        return null;
    }
}
