package com.sideflowgames.fulguritefusion.item;

import net.minecraft.world.InteractionResult;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Arc Magnet (early game): right-click to switch it on or off. While on (it glows), it pulls nearby items and
// XP orbs to whoever carries it. No power needed. Items still waiting to be picked up (just dropped or thrown)
// are left alone, as are items other mods mark with "PreventRemoteMovement".
public class ArcMagnetItem extends Item {
    public static final double RANGE = 6.0;
    private static final double PULL = 0.45; // blocks per tick toward the player

    public ArcMagnetItem(Properties properties) {
        super(properties);
    }

    public static boolean isOn(ItemStack stack) {
        return stack.getOrDefault(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            boolean on = !isOn(stack);
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, on);
            player.sendOverlayMessage(Component.literal(on ? "Arc Magnet on" : "Arc Magnet off")
                    .withStyle(on ? ChatFormatting.AQUA : ChatFormatting.GRAY));
            level.playSound(null, player.blockPosition(), on ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF,
                    SoundSource.PLAYERS, 0.8f, on ? 1.4f : 0.9f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        if (level.isClientSide() || !(entity instanceof Player player) || player.isSpectator() || !isOn(stack)) return;
        if (level.getGameTime() % 2 != 0) return;
        Vec3 target = player.position().add(0, 0.6, 0);
        AABB area = player.getBoundingBox().inflate(RANGE);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.hasPickUpDelay()
                && !e.getPersistentData().getBooleanOr("PreventRemoteMovement", false))) {
            pull(item, target);
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area, Entity::isAlive)) {
            pull(orb, target);
        }
    }

    private static void pull(Entity e, Vec3 target) {
        Vec3 to = target.subtract(e.position());
        if (to.lengthSqr() < 0.25) return;
        e.setDeltaMovement(to.normalize().scale(PULL));
        e.needsSync = true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag flag) {
        java.util.List<Component> tooltip = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut);
        tooltip.add(Component.literal(isOn(stack) ? "On" : "Off").withStyle(isOn(stack) ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click to switch on or off").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Pulls items and XP within " + (int) RANGE + " blocks").withStyle(ChatFormatting.GRAY));
    }
}
