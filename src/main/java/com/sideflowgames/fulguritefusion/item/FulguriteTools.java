package com.sideflowgames.fulguritefusion.item;

import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

import java.util.List;

// Fulgurite tools and sword (early game). They sit between diamond and netherite, use normal durability
// (they can break) and add a small amount of lightning damage to every hit, with a crackle of sparks.
// Later they're upgraded in the Storm Forge into Stormforged gear, which runs on stored Fm instead.
public final class FulguriteTools {
    private FulguriteTools() {}

    //                         diamond   fulgurite   netherite
    // durability              1561      1800        2031
    // mining speed            8.0       8.5         9.0
    // damage bonus            3.0       3.5         4.0
    // enchantability          10        13          15
    public static final Tier TIER = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 8.5f, 3.5f, 13,
            () -> Ingredient.of(ModItems.FULGURITE.get()));

    public static final float SWORD_LIGHTNING = 2.0f; // extra lightning damage per sword hit
    public static final float TOOL_LIGHTNING = 1.0f;  // extra lightning damage per tool hit

    // The crackle on a hit.
    static void zap(LivingEntity target, LivingEntity attacker) {
        if (target.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(),
                    8, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0.15);
            server.playSound(null, target.blockPosition(), SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS,
                    0.5f, 1.6f + server.random.nextFloat() * 0.3f);
        }
    }

    static void tooltip(List<Component> tooltip, float lightning) {
        tooltip.add(Component.literal("+" + (int) lightning + " lightning damage").withStyle(ChatFormatting.AQUA));
    }

    public static class Sword extends SwordItem {
        public Sword(Item.Properties p) {
            super(TIER, p);
        }

        @Override
        public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
            return SWORD_LIGHTNING;
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            zap(target, attacker);
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip(tooltip, SWORD_LIGHTNING);
        }
    }

    public static class Pickaxe extends PickaxeItem {
        public Pickaxe(Item.Properties p) {
            super(TIER, p);
        }

        @Override
        public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
            return TOOL_LIGHTNING;
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            zap(target, attacker);
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip(tooltip, TOOL_LIGHTNING);
        }
    }

    public static class Axe extends AxeItem {
        public Axe(Item.Properties p) {
            super(TIER, p);
        }

        @Override
        public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
            return TOOL_LIGHTNING;
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            zap(target, attacker);
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip(tooltip, TOOL_LIGHTNING);
        }
    }

    public static class Shovel extends ShovelItem {
        public Shovel(Item.Properties p) {
            super(TIER, p);
        }

        @Override
        public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
            return TOOL_LIGHTNING;
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            zap(target, attacker);
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip(tooltip, TOOL_LIGHTNING);
        }
    }

    public static class Hoe extends HoeItem {
        public Hoe(Item.Properties p) {
            super(TIER, p);
        }

        @Override
        public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
            return TOOL_LIGHTNING;
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            zap(target, attacker);
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip(tooltip, TOOL_LIGHTNING);
        }
    }
}
