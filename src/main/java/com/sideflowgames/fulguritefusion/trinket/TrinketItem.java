package com.sideflowgames.fulguritefusion.trinket;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

// A trinket: worn in its own slot of the equipment panel (or its Curios slot), never in the hand. The Empowered
// version (Storm Forge) does the same thing, stronger. What each one does lives in TrinketEvents and TrinketClient.
public class TrinketItem extends Item {
    public final TrinketKind kind;
    public final boolean empowered;
    // Relic: the dungeon-only third tier (Stormreach dungeons). Counts as Empowered, and stronger still.
    public final boolean relic;

    public TrinketItem(TrinketKind kind, boolean empowered, Properties properties) {
        super(properties);
        this.kind = kind;
        this.empowered = empowered;
        this.relic = false;
    }

    public TrinketItem(TrinketKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.empowered = true;
        this.relic = true;
    }

    // 1 normal, 2 Empowered, 3 Relic.
    public int tier() {
        return relic ? 3 : empowered ? 2 : 1;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return empowered;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Trinket: wear it in the equipment panel").withStyle(ChatFormatting.DARK_GRAY));
        for (String line : relic ? describeRelic(kind) : describe(kind, empowered)) tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        if (relic) tooltip.add(Component.literal("Relic of the Stormreach").withStyle(ChatFormatting.LIGHT_PURPLE));
        else if (empowered) tooltip.add(Component.literal("Empowered").withStyle(ChatFormatting.GOLD));
    }

    public static List<String> describeRelic(TrinketKind kind) {
        return switch (kind) {
            case CLOUDSTEP_CHARM -> List.of("Jump three times more in mid-air", "Never take fall damage");
            case CINDER_WARD -> List.of("Immune to fire and lava", "Swim through lava for as long as you like");
            case VOID_ARC_BAND -> List.of("Press the Void Blink key to blink the way you're moving", "14 blocks, 1.5 second cooldown");
            case SEERS_RING -> List.of("Every monster within 48 blocks glows", "Immune to Blindness, Darkness and Slowness");
            default -> describe(kind, true);
        };
    }

    public static List<String> describe(TrinketKind kind, boolean e) {
        return switch (kind) {
            case CLOUDSTEP_CHARM -> List.of(e ? "Jump twice more in mid-air" : "Jump once more in mid-air",
                    e ? "Fall much slower, take far less fall damage" : "Fall slower, take less fall damage");
            case CONDUIT_HEART -> List.of("Immune to fire and lava",
                    e ? "Lightning can't hurt you: it heals you instead" : "After a lightning hit, regenerate for a few seconds");
            case VOID_ARC_BAND -> List.of("Press the Void Blink key to blink the way you're moving",
                    e ? "10 blocks, 2.5 second cooldown" : "6 blocks, 5 second cooldown");
            case GROUNDING_CHARM -> List.of(e ? "Lightning deals 70% less damage" : "Lightning deals 40% less damage",
                    "Lightning can't set you on fire");
            case CINDER_WARD -> List.of("Immune to fire (not lava)",
                    e ? "Touching lava: 8 seconds of lava immunity" : "Touching lava: 3 seconds of lava immunity");
            case SEERS_RING -> List.of(e ? "Invisible and teleporting monsters within 32 blocks glow; every monster within 12"
                    : "Invisible and teleporting monsters within 16 blocks glow", "Immune to Blindness and Darkness");
        };
    }
}
