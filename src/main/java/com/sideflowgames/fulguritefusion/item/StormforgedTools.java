package com.sideflowgames.fulguritefusion.item;

import net.minecraft.world.InteractionResult;

import com.sideflowgames.fulguritefusion.ModDataComponents;
import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Stormforged tools and the Storm Blade (mid game, forged from Fulgurite gear in the Storm Forge).
// They never break; they run on stored Fm (1,000,000) and go slow / lose their lightning when empty.
//   Tools: faster than netherite. Sneak + right-click the air to cycle Normal, Arc Smelting, Vein Mining and
//   Smelt + Vein (see event/StormToolEvents for the smelting and the veins).
//   Storm Blade: hits hard, adds lightning damage and chains lightning to up to 5 nearby enemies.
// The charge shows as a blue bar under the item.
public final class StormforgedTools {
    private StormforgedTools() {}

    //                         netherite   stormforged
    // mining speed            9.0         11.0
    // damage bonus            4.0         6.0
    // enchantability          15          18
    public static final net.minecraft.world.item.ToolMaterial TIER = new net.minecraft.world.item.ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4000, 11.0f, 6.0f, 18,
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(com.sideflowgames.fulguritefusion.FulguriteFusion.MODID, "stormforged_tool_materials")));

    public static final int MAX_FM = 1_000_000;
    public static final int MINE_COST = 100;      // Fm per block mined
    public static final int HIT_COST = 500;       // Fm per Storm Blade hit (lightning bonus + chain)
    public static final float BLADE_LIGHTNING = 4.0f;
    public static final float CHAIN_DAMAGE = 4.0f;
    public static final int CHAIN_TARGETS = 5;
    public static final double CHAIN_RANGE = 6.0;
    public static final float EMPTY_SPEED = 1.0f;  // mining speed with no charge left

    public static final int SMELT = 1, VEIN = 2, HAMMER = 4; // HAMMER: Prime pickaxe's 3x3 mode (its own key)
    private static final String[] MODE_NAMES = {"Normal", "Arc Smelting", "Vein Mining", "Arc Smelting + Vein Mining"};

    public static int mode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TOOL_MODE.get(), 0);
    }

    public static boolean smelting(ItemStack stack) {
        return (mode(stack) & SMELT) != 0 && FmChargeable.hasCharge(stack);
    }

    public static boolean veinMining(ItemStack stack) {
        return (mode(stack) & VEIN) != 0 && !hammering(stack) && FmChargeable.hasCharge(stack);
    }

    public static boolean hammering(ItemStack stack) {
        return (mode(stack) & HAMMER) != 0 && FmChargeable.hasCharge(stack);
    }

    // Sneak + right-click the air: next mode. Returns null when it isn't a mode change.
    static InteractionResult cycle(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) return null;
        if (!level.isClientSide()) {
            int next = ((mode(stack) & 3) + 1) % 4;
            stack.set(ModDataComponents.TOOL_MODE.get(), next | (mode(stack) & HAMMER));
            player.sendOverlayMessage(Component.literal("Mode: " + MODE_NAMES[next]).withStyle(ChatFormatting.AQUA));
            level.playSound(null, player.blockPosition(), SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS, 0.6f, 1.3f + next * 0.1f);
        }
        return InteractionResult.SUCCESS;
    }

    static float speed(ItemStack stack, float normal) {
        return FmChargeable.hasCharge(stack) ? normal : Math.min(normal, EMPTY_SPEED);
    }

    static void mined(ItemStack stack, Level level, BlockState state, BlockPos pos) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0) FmChargeable.use(stack, MINE_COST);
    }

    static int barWidth(ItemStack stack) {
        return Math.round(13 * FmChargeable.fraction(stack));
    }

    static int barColor(ItemStack stack) {
        return FmChargeable.fraction(stack) < 0.1f ? 0xFF4040 : Mth.hsvToRgb(0.55f, 0.75f, 1.0f);
    }

    static void tooltip(ItemStack stack, List<Component> tooltip, boolean modes) {
        int fm = FmChargeable.get(stack);
        tooltip.add(Component.literal(String.format("Charge: %,d / %,d Fm", fm, FmChargeable.max(stack)))
                .withStyle(fm > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        if (modes) {
            tooltip.add(Component.literal("Mode: " + MODE_NAMES[mode(stack) & 3]).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Sneak + right-click the air to change mode").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // ---------- Storm Blade ----------

    public static class Blade extends Item implements FmChargeable {
        public Blade(Item.Properties p) {
            this(TIER, p);
        }

        protected Blade(net.minecraft.world.item.ToolMaterial tier, Item.Properties p) {
            super(p);
        }

        protected float lightning() {
            return BLADE_LIGHTNING;
        }

        protected int chainTargets() {
            return CHAIN_TARGETS;
        }

        @Override public int maxFm() { return MAX_FM; }

        @Override
        public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
            return FmChargeable.hasCharge(source.getWeaponItem() != null ? source.getWeaponItem() : ItemStack.EMPTY)
                    ? lightning() : 0;
        }

        @Override
        public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            if (FmChargeable.hasCharge(stack) && target.level() instanceof ServerLevel level) {
                FmChargeable.use(stack, HIT_COST);
                chain(level, target, attacker, chainTargets(), this instanceof PrimeTools.Blade);
            }
            return;
        }

        @Override public boolean isBarVisible(ItemStack stack) { return true; }
        @Override public int getBarWidth(ItemStack stack) { return barWidth(stack); }
        @Override public int getBarColor(ItemStack stack) { return barColor(stack); }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag flag) {
        java.util.List<Component> tooltip = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut);
            tooltip.add(Component.literal("+" + (int) lightning() + " lightning damage; lightning chains to "
                    + chainTargets() + " nearby enemies").withStyle(ChatFormatting.AQUA));
            StormforgedTools.tooltip(stack, tooltip, false);
        }
    }

    // Lightning jumps from the target to up to 5 other enemies nearby (never the attacker, players or pets).
    static void chain(ServerLevel level, LivingEntity target, LivingEntity attacker, int targets, boolean prime) {
        DamageSource lightning = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.LIGHTNING_BOLT), attacker);
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(CHAIN_RANGE),
                e -> e != target && e != attacker && e.isAlive() && !(e instanceof Player)
                        && !(e instanceof OwnableEntity o && o.getOwnerReference() != null));
        near.sort((a, b) -> Double.compare(a.distanceToSqr(target), b.distanceToSqr(target)));
        Vec3 from = target.position().add(0, target.getBbHeight() * 0.6, 0);
        List<Vec3> path = new java.util.ArrayList<>();
        path.add(from);
        for (int i = 0; i < Math.min(targets, near.size()); i++) {
            LivingEntity next = near.get(i);
            Vec3 to = next.position().add(0, next.getBbHeight() * 0.6, 0);
            path.add(to);
            next.hurtOrSimulate(lightning, CHAIN_DAMAGE);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 6, 0.2, 0.2, 0.2, 0.15);
        }
        ChainBolt.send(target, path, prime);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(), 15, 0.3, 0.3, 0.3, 0.3);
        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    // ---------- Tools ----------

    public static class Pickaxe extends Item implements FmChargeable {
        public Pickaxe(Item.Properties p) { super(p); }
        protected Pickaxe(net.minecraft.world.item.ToolMaterial t, Item.Properties p) { super(p); }
        @Override public int maxFm() { return MAX_FM; }
        @Override public InteractionResult use(Level l, Player p, InteractionHand h) {
            var r = cycle(l, p, h); return r != null ? r : super.use(l, p, h);
        }
        @Override public float getDestroySpeed(ItemStack s, BlockState st) { return speed(s, super.getDestroySpeed(s, st)); }
        @Override public boolean mineBlock(ItemStack s, Level l, BlockState st, BlockPos pos, LivingEntity e) { mined(s, l, st, pos); return true; }
        @Override public boolean isBarVisible(ItemStack s) { return true; }
        @Override public int getBarWidth(ItemStack s) { return barWidth(s); }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag f) {
        java.util.List<Component> t = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut); tooltip(s, t, true); }
    }

    public static class Axe extends AxeItem implements FmChargeable {
        public Axe(Item.Properties p) { super(TIER, 5.0f, -3.0f, p); }
        protected Axe(net.minecraft.world.item.ToolMaterial t, Item.Properties p) { super(t, 5.0f, -3.0f, p); }
        @Override public int maxFm() { return MAX_FM; }
        @Override public InteractionResult use(Level l, Player p, InteractionHand h) {
            var r = cycle(l, p, h); return r != null ? r : super.use(l, p, h);
        }
        @Override public float getDestroySpeed(ItemStack s, BlockState st) { return speed(s, super.getDestroySpeed(s, st)); }
        @Override public boolean mineBlock(ItemStack s, Level l, BlockState st, BlockPos pos, LivingEntity e) { mined(s, l, st, pos); return true; }
        @Override public boolean isBarVisible(ItemStack s) { return true; }
        @Override public int getBarWidth(ItemStack s) { return barWidth(s); }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag f) {
        java.util.List<Component> t = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut); tooltip(s, t, true); }
    }

    public static class Shovel extends ShovelItem implements FmChargeable {
        public Shovel(Item.Properties p) { super(TIER, 1.5f, -3.0f, p); }
        protected Shovel(net.minecraft.world.item.ToolMaterial t, Item.Properties p) { super(t, 1.5f, -3.0f, p); }
        @Override public int maxFm() { return MAX_FM; }
        @Override public InteractionResult use(Level l, Player p, InteractionHand h) {
            var r = cycle(l, p, h); return r != null ? r : super.use(l, p, h);
        }
        @Override public float getDestroySpeed(ItemStack s, BlockState st) { return speed(s, super.getDestroySpeed(s, st)); }
        @Override public boolean mineBlock(ItemStack s, Level l, BlockState st, BlockPos pos, LivingEntity e) { mined(s, l, st, pos); return true; }
        @Override public boolean isBarVisible(ItemStack s) { return true; }
        @Override public int getBarWidth(ItemStack s) { return barWidth(s); }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag f) {
        java.util.List<Component> t = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut); tooltip(s, t, true); }
    }

    public static class Hoe extends HoeItem implements FmChargeable {
        public Hoe(Item.Properties p) { super(TIER, -6.0f, 0.0f, p); }
        protected Hoe(net.minecraft.world.item.ToolMaterial t, float damage, Item.Properties p) { super(t, damage, 0.0f, p); }
        @Override public int maxFm() { return MAX_FM; }
        @Override public InteractionResult use(Level l, Player p, InteractionHand h) {
            var r = cycle(l, p, h); return r != null ? r : super.use(l, p, h);
        }
        @Override public float getDestroySpeed(ItemStack s, BlockState st) { return speed(s, super.getDestroySpeed(s, st)); }
        @Override public boolean mineBlock(ItemStack s, Level l, BlockState st, BlockPos pos, LivingEntity e) { mined(s, l, st, pos); return true; }
        @Override public boolean isBarVisible(ItemStack s) { return true; }
        @Override public int getBarWidth(ItemStack s) { return barWidth(s); }
        @Override public int getBarColor(ItemStack s) { return barColor(s); }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag f) {
        java.util.List<Component> t = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut); tooltip(s, t, true); }
    }
}
