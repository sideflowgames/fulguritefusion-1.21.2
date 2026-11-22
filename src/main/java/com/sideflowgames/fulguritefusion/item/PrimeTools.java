package com.sideflowgames.fulguritefusion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

import java.util.List;

// Prime tools and the Prime Blade (end game: Stormforged gear + a Prime Spark Fragment + a netherite ingot in the
// Storm Forge, about 20,000,000 Fm). Everything the Stormforged versions do (modes, lightning, never break),
// stronger, with an 8,000,000 Fm store and a violet charge bar.
//   Prime Pickaxe: 3x3 hammer mode, toggled with its own key (V by default; see prime/PrimeEvents).
//   Prime Blade: 19 attack damage, +12 lightning, chains to 8 enemies.
public final class PrimeTools {
    private PrimeTools() {}

    //                         netherite   stormforged   prime
    // mining speed            9.0         11.0          14.0
    // damage bonus            4.0         6.0           8.0
    // enchantability          15          18            22
    public static final Tier TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 6000, 14.0f, 8.0f, 22,
            () -> Ingredient.EMPTY);

    public static final int MAX_FM = 8_000_000;

    static int barColor(ItemStack stack) {
        return FmChargeable.fraction(stack) < 0.1f ? 0xFF4040 : Mth.hsvToRgb(0.77f, 0.6f, 1.0f);
    }

    static void primeLine(List<Component> tooltip) {
        tooltip.add(Component.literal("Prime").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
    }

    public static class Blade extends StormforgedTools.Blade {
        public Blade(Properties p) { super(PrimeTools.TIER, p); }
        @Override public int maxFm() { return PrimeTools.MAX_FM; }
        @Override protected float lightning() { return 12.0f; }
        @Override protected int chainTargets() { return 8; }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) {
            primeLine(t);
            super.appendHoverText(s, c, t, f);
        }
    }

    public static class Pickaxe extends StormforgedTools.Pickaxe {
        public Pickaxe(Properties p) { super(PrimeTools.TIER, p); }
        @Override public int maxFm() { return PrimeTools.MAX_FM; }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) {
            primeLine(t);
            super.appendHoverText(s, c, t, f);
            boolean on = (StormforgedTools.mode(s) & StormforgedTools.HAMMER) != 0;
            t.add(Component.literal("3x3 hammer: " + (on ? "on" : "off") + " (toggle with the Prime hammer key, V by default)")
                    .withStyle(on ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY));
        }
    }

    public static class Axe extends StormforgedTools.Axe {
        public Axe(Properties p) { super(PrimeTools.TIER, p); }
        @Override public int maxFm() { return PrimeTools.MAX_FM; }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) {
            primeLine(t);
            super.appendHoverText(s, c, t, f);
        }
    }

    public static class Shovel extends StormforgedTools.Shovel {
        public Shovel(Properties p) { super(PrimeTools.TIER, p); }
        @Override public int maxFm() { return PrimeTools.MAX_FM; }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) {
            primeLine(t);
            super.appendHoverText(s, c, t, f);
        }
    }

    public static class Hoe extends StormforgedTools.Hoe {
        public Hoe(Properties p) { super(PrimeTools.TIER, p); }
        @Override public int maxFm() { return PrimeTools.MAX_FM; }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) {
            primeLine(t);
            super.appendHoverText(s, c, t, f);
        }
    }
}
