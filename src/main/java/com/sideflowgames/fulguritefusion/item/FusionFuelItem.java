package com.sideflowgames.fulguritefusion.item;

import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.pairing.PairingDiscoveries;
import com.sideflowgames.fulguritefusion.pairing.Pairings;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

// Fusion Fuel: made by the Storm Collider from a pairing of two exotic materials.
// The big reactor (coming later) burns it for the pairing's energy and side effects.
// "Pairing" = which two materials went in (1-78); "Result" = what it actually became
// (the same, unless Struck Spore rolled it into something else).
public class FusionFuelItem extends Item {
    public FusionFuelItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(int pairingId, int resultId) {
        ItemStack stack = new ItemStack(ModItems.FUSION_FUEL.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt("Pairing", pairingId);
        tag.putInt("Result", resultId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static Pairings.Pairing pairing(ItemStack stack) {
        return Pairings.byId(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("Pairing", 0));
    }

    public static Pairings.Pairing result(ItemStack stack) {
        return Pairings.byId(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("Result", 0));
    }

    @Override
    public Component getName(ItemStack stack) {
        Pairings.Pairing result = result(stack);
        if (result == null) return super.getName(stack);
        return Component.translatable(getDescriptionId()).append(": " + result.name());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag flag) {
        java.util.List<Component> tooltip = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut);
        Pairings.Pairing pairing = pairing(stack);
        Pairings.Pairing result = result(stack);
        if (pairing == null || result == null) {
            tooltip.add(Component.literal("Empty capsule").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.literal(Pairings.MATERIAL_NAMES[pairing.a()] + " + " + Pairings.MATERIAL_NAMES[pairing.b()])
                .withStyle(ChatFormatting.GRAY));
        if (result != pairing) {
            tooltip.add(Component.literal("Wild: mutated from " + pairing.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.literal("Tier: " + result.tier().display()).withStyle(tierColor(result.tier())));
        // Yield and side effects stay unknown until this player has burned this result in the big reactor.
        if (PairingDiscoveries.knownOnClient(result.id())) {
            tooltip.add(Component.literal("Reactor yield: " + result.fuelFm() + " Fm").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal(result.effect()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.literal("Reactor yield: ???").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Side effects: ??? (burn it in the reactor to find out)")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static ChatFormatting tierColor(Pairings.Tier tier) {
        return switch (tier) {
            case STEADY -> ChatFormatting.GREEN;
            case STRONG -> ChatFormatting.DARK_GREEN;
            case JACKPOT -> ChatFormatting.GOLD;
            case UNSTABLE -> ChatFormatting.YELLOW;
            case BAD -> ChatFormatting.RED;
            case CATASTROPHIC -> ChatFormatting.DARK_RED;
            default -> ChatFormatting.GRAY;
        };
    }
}
