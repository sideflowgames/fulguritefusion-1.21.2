package com.sideflowgames.fulguritefusion.item;

import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.entity.SpearEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;

// Spears: hold right-click to wind up, let go to throw (like a trident). Pick the spear up again where it lands.
//   Fulgurite Spear (early): durability, repaired with Fulgurite; +2 lightning damage, melee or thrown.
//   Storm Spear (StormSpear, mid): runs on Fm; hits much harder and calls lightning down where it lands.
public class SpearItem extends Item {
    public static final float FULGURITE_MELEE = 8, FULGURITE_THROWN = 9, FULGURITE_LIGHTNING = 2;

    public SpearItem(Properties properties) {
        super(properties);
    }

    // Attack damage (total, including the 1 every hand does) and attack speed for the main hand.
    public static ItemAttributeModifiers attributes(float damage, float speed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage - 1, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public boolean isStorm() {
        return false;
    }

    // Lightning added to melee hits.
    public float lightningBonus(ItemStack stack) {
        return FULGURITE_LIGHTNING;
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        ItemStack weapon = source.getWeaponItem();
        return weapon != null && weapon.getItem() == this ? lightningBonus(weapon) : 0;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (stack.isDamageableItem()) stack.hurtAndBreak(1, attacker, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        return true;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || getUseDuration(stack, entity) - timeLeft < 10) return;
        if (!level.isClientSide) {
            boolean charged = onThrow(stack, player);
            if (stack.isDamageableItem()) stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            SpearEntity spear = new SpearEntity(level, player, stack.copyWithCount(1), isStorm(), charged);
            spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 1.0f);
            if (player.getAbilities().instabuild) spear.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            level.addFreshEntity(spear);
            level.playSound(null, spear, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, isStorm() ? 0.8f : 1.1f);
            if (!player.getAbilities().instabuild) player.getInventory().removeItem(stack);
        }
    }

    // Called as it's thrown; returns whether the throw is charged (Storm Spear: calls lightning).
    protected boolean onThrow(ItemStack stack, Player player) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModItems.FULGURITE.get());
    }

    @Override
    public int getEnchantmentValue() {
        return 13;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("+" + (int) FULGURITE_LIGHTNING + " lightning damage").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Hold right-click, then let go to throw").withStyle(ChatFormatting.GRAY));
    }
}
