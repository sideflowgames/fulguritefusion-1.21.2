package com.sideflowgames.fulguritefusion.farm;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

// Shock Farm upgrade cards. They go in the upgrade slots of the Shock Farm Controller menu
// (2 slots on a 5x5 farm, 4 on a 7x7). Effects from every card in an unlocked slot add up.
//
//   Blank       crafting base, does nothing in a slot
//   Mob Filter  right-click a mob with it to set it; the farm then only spawns the filtered mobs (they must spawn in the biome)
//   Speed       +50% spawn rate per tier, but each spawn costs +25% Fm per tier
//   Efficiency  -15% Fm per tier on spawns and zaps (at most -75% in total)
//   Looting     +1 Looting per tier on kills (max Looting III) and +25% XP per tier
public class FarmUpgradeItem extends Item {
    public enum Kind { BLANK, MOB_FILTER, SPEED, EFFICIENCY, LOOTING }

    public final Kind kind;
    public final int tier;

    public FarmUpgradeItem(Kind kind, int tier, Properties properties) {
        super(properties);
        this.kind = kind;
        this.tier = tier;
    }

    // ---------- mob filter ----------

    @Nullable
    public static EntityType<?> filterMob(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("Mob")) return null;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Mob"));
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return null;
        return BuiltInRegistries.ENTITY_TYPE.get(id);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (kind != Kind.MOB_FILTER) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        EntityType<?> type = target.getType();
        if (!(target instanceof Mob) || !ShockFarmControllerBlockEntity.allowed(type)) {
            player.displayClientMessage(Component.literal("The Shock Farm can't spawn " + type.getDescription().getString() + ".")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("Mob", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        ItemStack held = player.getItemInHand(hand);
        held.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        player.displayClientMessage(Component.literal("Mob Filter set: " + type.getDescription().getString())
                .withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.CONSUME;
    }

    // Sneak-use in the air clears a Mob Filter.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.MOB_FILTER && player.isShiftKeyDown() && filterMob(stack) != null) {
            if (!level.isClientSide) {
                stack.remove(DataComponents.CUSTOM_DATA);
                player.displayClientMessage(Component.literal("Mob Filter cleared").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return kind == Kind.MOB_FILTER && filterMob(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        ChatFormatting c = ChatFormatting.GRAY;
        switch (kind) {
            case BLANK -> lines.add(Component.literal("Crafting base for Shock Farm upgrades").withStyle(c));
            case MOB_FILTER -> {
                EntityType<?> mob = filterMob(stack);
                if (mob == null) {
                    lines.add(Component.literal("Right-click a mob with it to set the filter").withStyle(c));
                } else {
                    lines.add(Component.literal("Only spawns: ").withStyle(c)
                            .append(mob.getDescription().copy().withStyle(ChatFormatting.AQUA)));
                    lines.add(Component.literal("Sneak + use to clear").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            case SPEED -> {
                lines.add(Component.literal("+" + (50 * tier) + "% spawn rate").withStyle(ChatFormatting.GREEN));
                lines.add(Component.literal("+" + (25 * tier) + "% Fm per spawn").withStyle(ChatFormatting.RED));
            }
            case EFFICIENCY -> lines.add(Component.literal("-" + (15 * tier) + "% Fm per spawn and zap").withStyle(ChatFormatting.GREEN));
            case LOOTING -> {
                lines.add(Component.literal("+" + tier + " Looting on kills").withStyle(ChatFormatting.GREEN));
                lines.add(Component.literal("+" + (25 * tier) + "% XP").withStyle(ChatFormatting.GREEN));
            }
        }
        if (kind != Kind.BLANK) lines.add(Component.literal("Shock Farm upgrade").withStyle(ChatFormatting.DARK_GRAY));
    }
}
