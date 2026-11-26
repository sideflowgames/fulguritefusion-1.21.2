package com.sideflowgames.fulguritefusion.titan;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.item.FmChargeable;
import com.sideflowgames.fulguritefusion.item.TitanArmorItem;
import com.sideflowgames.fulguritefusion.prime.PrimeEvents;
import com.sideflowgames.fulguritefusion.stormreach.Stormreach;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Titan armor abilities (on top of everything Prime and Stormforged armor do; see TitanArmorItem).
//   Each piece: +5 max health (2.5 hearts).
//   Full Titan set: regeneration while storming, the conduit aura, no knockback, fire and lava immunity, and the
//   static dash (the client moves the player; the server checks the cooldown, takes the Fm and hurts what's in the
//   way, see TitanNetwork and client/TitanClient).
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class TitanArmorEvents {
    private TitanArmorEvents() {}

    public static final int DASH_COOLDOWN = 60;
    public static final int DASH_FM = 20_000;
    public static final int AURA_FM = 2_000;
    private static final Identifier HEALTH_ID = Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "titan_health");
    private static final Identifier KB_ID = Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "titan_knockback");
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, Long> lastDash = new HashMap<>();

    public static boolean fullTitanSet(LivingEntity e) {
        for (EquipmentSlot slot : SLOTS) {
            ItemStack s = e.getItemBySlot(slot);
            if (!(s.getItem() instanceof TitanArmorItem) || !FmChargeable.hasCharge(s)) return false;
        }
        return true;
    }

    // Titan Prime pieces add another 1.5 hearts each on top of the Titan 2.5.
    private static int primePieces(LivingEntity e) {
        int n = 0;
        for (EquipmentSlot slot : SLOTS) if (e.getItemBySlot(slot).getItem() instanceof com.sideflowgames.fulguritefusion.item.TitanPrimeArmorItem) n++;
        return n;
    }

    private static int pieces(LivingEntity e) {
        int n = 0;
        for (EquipmentSlot slot : SLOTS) if (e.getItemBySlot(slot).getItem() instanceof TitanArmorItem) n++;
        return n;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;
        setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, pieces(player) * 5.0 + primePieces(player) * 3.0);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        boolean full = fullTitanSet(player);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KB_ID, full ? 1.0 : 0);
        if (!full) return;
        if (player.isOnFire()) player.clearFire();
        long time = level.getGameTime();
        boolean storming = Stormreach.is(level) || level.isThundering();
        if (storming && time % 40 == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0f);
            level.sendParticles(new DustParticleOptions(PrimeEvents.VIOLET, 1.0f), player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.3, 0.5, 0.3, 0);
        }
        if (time % 40 == 20) aura(level, player);
    }

    // Arcs of lightning into every monster within 6 blocks.
    private static void aura(ServerLevel level, Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean any = false;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6),
                e -> e instanceof Enemy && e.isAlive())) {
            if (FmChargeable.get(chest) < AURA_FM) break;
            FmChargeable.use(chest, AURA_FM);
            e.hurtOrSimulate(level.damageSources().lightningBolt(), 4.0f);
            Vec3 from = player.position().add(0, 1.2, 0), to = e.position().add(0, e.getBbHeight() * 0.5, 0);
            int steps = (int) Math.max(4, from.distanceTo(to) * 3);
            for (int i = 0; i <= steps; i++) {
                Vec3 p = from.lerp(to, i / (double) steps);
                level.sendParticles(new DustParticleOptions(PrimeEvents.VIOLET, 0.9f), p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0);
            }
            any = true;
        }
        if (any) level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.3f, 2.0f);
    }

    // Fire and lava immunity.
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && fullTitanSet(event.getEntity())) event.setCanceled(true);
    }

    // Server side of the static dash: the client already moved the player along (dx, dz).
    public static void dashed(ServerPlayer player, float dx, float dz) {
        if (!fullTitanSet(player)) return;
        long now = player.level().getGameTime();
        Long last = lastDash.get(player.getUUID());
        if (last != null && now - last < DASH_COOLDOWN - 5) return;
        lastDash.put(player.getUUID(), now);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        FmChargeable.use(chest, DASH_FM);
        ServerLevel level = player.level();
        Vec3 dir = new Vec3(dx, 0, dz);
        if (dir.lengthSqr() < 1e-4) return;
        dir = dir.normalize();
        Vec3 start = player.position(), end = start.add(dir.scale(6));
        AABB path = new AABB(start, end).inflate(1.2, 1.5, 1.2);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, path, e -> e instanceof Enemy && e.isAlive())) {
            e.hurtOrSimulate(level.damageSources().playerAttack(player), 8.0f);
        }
        for (int i = 0; i <= 18; i++) {
            Vec3 p = start.lerp(end, i / 18.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.6, p.z, 3, 0.15, 0.3, 0.15, 0.05);
            level.sendParticles(new DustParticleOptions(PrimeEvents.VIOLET, 1.2f), p.x, p.y + 0.4, p.z, 1, 0.1, 0.2, 0.1, 0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    private static void setModifier(Player player, Holder<Attribute> attribute, Identifier id, double amount) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier current = inst.getModifier(id);
        if (amount == 0) {
            if (current != null) inst.removeModifier(id);
        } else if (current == null || current.amount() != amount) {
            inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
