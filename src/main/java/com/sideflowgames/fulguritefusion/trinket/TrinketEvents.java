package com.sideflowgames.fulguritefusion.trinket;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.entity.boss.VoidArcEntity;
import com.sideflowgames.fulguritefusion.entity.boss.VoidEchoEntity;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// What the trinkets do (the plain / Empowered numbers):
//   Cloudstep Charm: 1 / 2 extra mid-air jumps (client side, see client/TrinketClient), gravity -30% / -45%,
//     +4 / +10 blocks before fall damage starts.
//   Conduit Heart: immune to fire and lava. Plain: a lightning hit gives Regeneration II for 4 seconds.
//     Empowered: lightning does no damage and gives Regeneration III for 6 seconds.
//   Void Arc Band: the Void Blink key blinks you 6 / 10 blocks the way you're moving (stops at walls),
//     cooldown 5 / 2.5 seconds.
//   Grounding Charm: lightning damage -40% / -70%, and lightning can't set you on fire (any fire is put out for
//     2 seconds after a lightning hit).
//   Cinder Ward: immune to fire (not lava). The first touch of lava starts a window of lava immunity, 3 / 8
//     seconds; it resets once you've been out of lava for 2 seconds.
//   Seer's Ring: invisible monsters and teleporters (endermen, shulkers, the Void Arc and its echoes) within 16 /
//     32 blocks glow; Empowered also makes every monster within 12 blocks glow. Blindness and Darkness are cleared.
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class TrinketEvents {
    private TrinketEvents() {}

    private static final Identifier GRAVITY_ID = Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "cloudstep_gravity");
    private static final Identifier FALL_ID = Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "cloudstep_fall");
    private static final Map<UUID, Long> struck = new HashMap<>();
    private static final Map<UUID, Long> lavaStart = new HashMap<>();
    private static final Map<UUID, Long> lavaLast = new HashMap<>();

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        UUID id = player.getUUID();

        int cloud = Trinkets.level(player, TrinketKind.CLOUDSTEP_CHARM);
        setModifier(player, Attributes.GRAVITY, GRAVITY_ID, cloud == 0 ? 0 : cloud == 3 ? -0.5 : cloud == 2 ? -0.45 : -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.SAFE_FALL_DISTANCE, FALL_ID, cloud == 0 ? 0 : cloud == 3 ? 1024 : cloud == 2 ? 10 : 4, AttributeModifier.Operation.ADD_VALUE);

        boolean heart = Trinkets.level(player, TrinketKind.CONDUIT_HEART) > 0;
        boolean ward = Trinkets.level(player, TrinketKind.CINDER_WARD) > 0;
        if (player.isOnFire() && (heart || (ward && !player.isInLava()))) player.clearFire();
        if (Trinkets.level(player, TrinketKind.GROUNDING_CHARM) > 0) {
            Long t = struck.get(id);
            if (t != null && now - t <= 40 && player.isOnFire()) player.clearFire();
        }
        if (ward) {
            if (player.isInLava()) {
                lavaStart.putIfAbsent(id, now);
                lavaLast.put(id, now);
            } else if (lavaLast.containsKey(id) && now - lavaLast.get(id) > 40) {
                lavaStart.remove(id);
                lavaLast.remove(id);
            }
        }

        int seer = Trinkets.level(player, TrinketKind.SEERS_RING);
        if (seer > 0) {
            if (seer == 3 && player.hasEffect(MobEffects.SLOWNESS)) player.removeEffect(MobEffects.SLOWNESS);
            if (player.hasEffect(MobEffects.BLINDNESS)) player.removeEffect(MobEffects.BLINDNESS);
            if (player.hasEffect(MobEffects.DARKNESS)) player.removeEffect(MobEffects.DARKNESS);
            if (now % 10 == 0) {
                double r = seer == 3 ? 48 : seer == 2 ? 32 : 16;
                for (Mob m : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(r), m -> m instanceof Enemy && m.isAlive())) {
                    boolean hidden = m.isInvisible() || m instanceof EnderMan || m instanceof Shulker
                            || m instanceof VoidArcEntity || m instanceof VoidEchoEntity;
                    if (hidden || seer == 3 || (seer == 2 && m.distanceToSqr(player) < 12 * 12)) {
                        m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        var source = event.getSource();
        int heart = Trinkets.level(player, TrinketKind.CONDUIT_HEART);
        if (source.is(DamageTypeTags.IS_FIRE) && heart > 0) {
            event.setCanceled(true);
            return;
        }
        if (source.is(DamageTypeTags.IS_FIRE) && Trinkets.level(player, TrinketKind.CINDER_WARD) > 0) {
            if (!source.is(DamageTypes.LAVA)) {
                event.setCanceled(true);
                return;
            }
            int wardLevel = Trinkets.level(player, TrinketKind.CINDER_WARD);
            if (wardLevel == 3) {
                event.setCanceled(true);
                return;
            }
            Long start = lavaStart.get(player.getUUID());
            long window = wardLevel == 2 ? 160 : 60;
            if (start != null && player.level().getGameTime() - start <= window) {
                event.setCanceled(true);
                return;
            }
        }
        if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            struck.put(player.getUUID(), player.level().getGameTime());
            if (heart >= 2) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 2));
                event.setCanceled(true);
                return;
            }
            int ground = Trinkets.level(player, TrinketKind.GROUNDING_CHARM);
            if (ground > 0) event.setAmount(event.getAmount() * (ground >= 2 ? 0.3f : 0.6f));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide() && event.getSource().is(DamageTypeTags.IS_LIGHTNING)
                && Trinkets.level(player, TrinketKind.CONDUIT_HEART) == 1) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1));
        }
    }

    // ---- client-started abilities ----

    public static void doubleJumped(ServerPlayer player) {
        if (Trinkets.level(player, TrinketKind.CLOUDSTEP_CHARM) == 0) return;
        player.resetFallDistance();
        player.level().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 12, 0.3, 0.05, 0.3, 0.02);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    public static void blink(ServerPlayer player, float dx, float dz) {
        ItemStack band = Trinkets.worn(player, TrinketKind.VOID_ARC_BAND);
        if (!(band.getItem() instanceof TrinketItem item) || player.getCooldowns().isOnCooldown(band)) return;
        Vec3 dir = new Vec3(dx, 0, dz);
        if (dir.lengthSqr() < 1e-4) return;
        dir = dir.normalize();
        double max = item.relic ? 14 : item.empowered ? 10 : 6;
        ServerLevel level = player.level();
        Vec3 start = player.position(), best = null;
        for (double d = 0.5; d <= max; d += 0.5) {
            Vec3 at = start.add(dir.scale(d));
            if (!level.noCollision(player, player.getBoundingBox().move(at.subtract(start)))) break;
            best = at;
        }
        if (best == null || best.distanceToSqr(start) < 1) return;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y + 1, start.z, 30, 0.3, 0.6, 0.3, 0.05);
        player.teleportTo(best.x, best.y, best.z);
        player.resetFallDistance();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, best.x, best.y + 1, best.z, 30, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.3f);
        player.getCooldowns().addCooldown(band, item.relic ? 30 : item.empowered ? 50 : 100);
    }

    private static void setModifier(Player player, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier current = inst.getModifier(id);
        if (amount == 0) {
            if (current != null) inst.removeModifier(id);
        } else if (current == null || current.amount() != amount) {
            inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
        }
    }
}
