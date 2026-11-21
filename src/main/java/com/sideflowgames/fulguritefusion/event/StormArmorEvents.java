package com.sideflowgames.fulguritefusion.event;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModDataComponents;
import com.sideflowgames.fulguritefusion.item.FmChargeable;
import com.sideflowgames.fulguritefusion.item.StormforgedArmorItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// Stormforged armor abilities. Each one only works while the piece it needs still holds Fm.
//   Full set (all four pieces charged): lightning immunity (a strike recharges every piece), knockback
//   resistance and faster movement. Costs 1 Fm/t from each piece while worn.
//   Helmet: breathe underwater. It splits water into oxygen (breathed) and hydrogen (stored). After 10 minutes
//   underwater the tank is full and breathing stops until it is vented, which happens on its own as soon as
//   your head is out of the water. Venting near fire or lava sets off a small flash explosion.
//   Boots: fall damage cut to a quarter.
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class StormArmorEvents {
    private StormArmorEvents() {}

    public static final int HYDROGEN_MAX = 12_000;      // ticks underwater before the tank is full (10 minutes)
    private static final int HYDROGEN_STEP = 10;        // the tank is updated every 10 ticks
    private static final int SET_DRAIN = 20;            // Fm per piece every 20 ticks while the set bonus runs
    private static final int BREATH_DRAIN = 20;         // Fm from the helmet every 10 ticks underwater
    private static final int FALL_DRAIN = 200;          // Fm from the boots per fall they soften
    private static final int STRIKE_RECHARGE = 50_000;  // Fm added to every piece by a lightning strike
    private static final double KNOCKBACK_RESIST = 0.6;
    private static final double SPEED_BONUS = 0.2;      // +20% movement speed
    private static final float FALL_MULTIPLIER = 0.25f;

    private static final ResourceLocation KB_ID = ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "stormforged_knockback");
    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "stormforged_speed");

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static boolean charged(LivingEntity e, EquipmentSlot slot) {
        ItemStack s = e.getItemBySlot(slot);
        return s.getItem() instanceof StormforgedArmorItem && FmChargeable.hasCharge(s);
    }

    // All four Stormforged pieces worn, each with charge left.
    public static boolean fullSet(LivingEntity e) {
        for (EquipmentSlot slot : SLOTS) if (!charged(e, slot)) return false;
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        long time = player.level().getGameTime();

        // Full set bonus.
        boolean full = fullSet(player);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KB_ID, full ? KNOCKBACK_RESIST : 0,
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, full ? SPEED_BONUS : 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (full && time % 20 == 0) {
            for (EquipmentSlot slot : SLOTS) FmChargeable.use(player.getItemBySlot(slot), SET_DRAIN);
        }

        // Helmet: water breathing and the hydrogen tank.
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof StormforgedArmorItem)) return;
        int hydrogen = helmet.getOrDefault(ModDataComponents.HYDROGEN.get(), 0);
        if (player.isEyeInFluid(FluidTags.WATER)) {
            if (hydrogen < HYDROGEN_MAX && FmChargeable.hasCharge(helmet)) {
                player.setAirSupply(player.getMaxAirSupply());
                if (time % HYDROGEN_STEP == 0) {
                    hydrogen = Math.min(HYDROGEN_MAX, hydrogen + HYDROGEN_STEP);
                    helmet.set(ModDataComponents.HYDROGEN.get(), hydrogen);
                    FmChargeable.use(helmet, BREATH_DRAIN);
                    if (hydrogen >= HYDROGEN_MAX) {
                        player.displayClientMessage(Component.literal("Hydrogen tank full: surface to vent it!")
                                .withStyle(ChatFormatting.RED), true);
                    } else if (hydrogen == HYDROGEN_MAX - 60 * 20) {
                        player.displayClientMessage(Component.literal("Hydrogen tank: 1 minute left")
                                .withStyle(ChatFormatting.YELLOW), true);
                    }
                }
            }
        } else if (hydrogen > 0) {
            vent(player, helmet, hydrogen);
        }
    }

    private static void setModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount,
                                    AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        boolean has = inst.getModifier(id) != null;
        if (amount != 0 && !has) inst.addTransientModifier(new AttributeModifier(id, amount, op));
        else if (amount == 0 && has) inst.removeModifier(id);
    }

    // Let the hydrogen out: a hiss and a cloud. Near fire or lava it goes off with a flash.
    private static void vent(Player player, ItemStack helmet, int hydrogen) {
        helmet.set(ModDataComponents.HYDROGEN.get(), 0);
        if (!(player.level() instanceof ServerLevel level)) return;
        double x = player.getX(), y = player.getEyeY(), z = player.getZ();
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.6f);
        level.sendParticles(ParticleTypes.CLOUD, x, y + 0.2, z, 12 + hydrogen / 400, 0.25, 0.2, 0.25, 0.04);
        if (nearFlame(level, player.blockPosition())) {
            float power = 1.0f + 1.5f * Math.min(1f, hydrogen / (float) HYDROGEN_MAX);
            level.explode(null, x, y + 0.3, z, power, false, Level.ExplosionInteraction.NONE);
        }
    }

    private static boolean nearFlame(Level level, BlockPos center) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 3, 3))) {
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.FIRE) || s.is(Blocks.LAVA) || s.is(Blocks.MAGMA_BLOCK)
                    || (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT))) return true;
        }
        return false;
    }

    // Boots: a quarter of the fall damage.
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || event.getDistance() <= 3 || !charged(e, EquipmentSlot.FEET)) return;
        event.setDamageMultiplier(event.getDamageMultiplier() * FALL_MULTIPLIER);
        FmChargeable.use(e.getItemBySlot(EquipmentSlot.FEET), FALL_DRAIN);
    }

    // Full set: lightning can't hurt you, and every strike tops up all four pieces.
    @SubscribeEvent
    public static void onStruck(EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof LivingEntity e) || !fullSet(e)) return;
        event.setCanceled(true);
        for (EquipmentSlot slot : SLOTS) FmChargeable.add(e.getItemBySlot(slot), STRIKE_RECHARGE);
        if (e.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY(1.0), e.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        }
    }

    // Other lightning damage (lightning attacks, static) is blocked too.
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_LIGHTNING) && fullSet(event.getEntity())) event.setCanceled(true);
    }
}
