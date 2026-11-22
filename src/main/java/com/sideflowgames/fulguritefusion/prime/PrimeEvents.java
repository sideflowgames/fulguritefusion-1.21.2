package com.sideflowgames.fulguritefusion.prime;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.item.FmChargeable;
import com.sideflowgames.fulguritefusion.item.PrimeArmorItem;
import com.sideflowgames.fulguritefusion.item.PrimeTools;
import com.sideflowgames.fulguritefusion.item.StormforgedTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

// Prime gear abilities (on top of everything Stormforged gear does):
//   Prime boots: no fall damage (500 Fm per fall).
//   Full Prime set: elytra-style gliding (400 Fm/s from the chestplate, see PrimeArmorItem) and a boost while
//   holding jump (client side, see PrimeClient; 100 Fm per tick of boost); static discharge: anything that hurts
//   you takes 8 lightning damage and is thrown back (2,000 Fm from the chestplate a time, no cooldown).
//   Prime pickaxe, hammer mode on: breaks a 3x3 square facing you (120 Fm per extra block).
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class PrimeEvents {
    private PrimeEvents() {}

    public static final int GLIDE_FM_PER_SECOND = 400;
    public static final int BOOST_FM_PER_TICK = 100;
    public static final int FALL_FM = 500;
    public static final int STATIC_FM = 2000;
    public static final float STATIC_DAMAGE = 8.0f;
    public static final int HAMMER_FM = 120;
    public static final Vector3f VIOLET = new Vector3f(0.72f, 0.42f, 1.0f);

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static boolean hammering; // stops the extra blocks hammering in turn

    public static boolean fullPrimeSet(LivingEntity e) {
        for (EquipmentSlot slot : SLOTS) {
            ItemStack s = e.getItemBySlot(slot);
            if (!(s.getItem() instanceof PrimeArmorItem) || !FmChargeable.hasCharge(s)) return false;
        }
        return true;
    }

    // Prime boots: no fall damage at all (runs after the Stormforged quarter, so it wins).
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFall(LivingFallEvent event) {
        LivingEntity e = event.getEntity();
        ItemStack boots = e.getItemBySlot(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof PrimeArmorItem) || !FmChargeable.hasCharge(boots)) return;
        event.setDamageMultiplier(0);
        if (!e.level().isClientSide && event.getDistance() > 3) FmChargeable.use(boots, FALL_FM);
    }

    // Static discharge: whoever hurts a full Prime set gets blasted back.
    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !fullPrimeSet(victim)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim || !attacker.isAlive()) return;
        ItemStack chest = victim.getItemBySlot(EquipmentSlot.CHEST);
        if (FmChargeable.get(chest) < STATIC_FM) return;
        FmChargeable.use(chest, STATIC_FM);
        DamageSource zap = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT), victim);
        attacker.hurt(zap, STATIC_DAMAGE);
        Vec3 away = attacker.position().subtract(victim.position()).multiply(1, 0, 1);
        if (away.lengthSqr() > 1e-4) attacker.knockback(1.2, -away.x, -away.z);
        // A crackling arc from you to them.
        Vec3 from = victim.position().add(0, victim.getBbHeight() * 0.6, 0), to = attacker.position().add(0, attacker.getBbHeight() * 0.5, 0);
        int steps = (int) Math.max(4, from.distanceTo(to) * 4);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(new DustParticleOptions(VIOLET, 1.0f), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 20, 0.3, 0.3, 0.3, 0.3);
        level.playSound(null, victim.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.8f);
    }

    // Prime pickaxe hammer mode: also break the 8 blocks around the one mined, in the plane facing you.
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (hammering || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof PrimeTools.Pickaxe) || !StormforgedTools.hammering(tool)) return;
        BlockState center = event.getState();
        if (!tool.isCorrectToolForDrops(center)) return;
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0f, false);
        Direction face = hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK ? b.getDirection() : Direction.UP;
        float centerHardness = center.getDestroySpeed(level, event.getPos());
        List<BlockPos> around = new ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos p = switch (face.getAxis()) {
                    case X -> event.getPos().offset(0, a, b);
                    case Y -> event.getPos().offset(a, 0, b);
                    case Z -> event.getPos().offset(a, b, 0);
                };
                around.add(p);
            }
        }
        hammering = true;
        try {
            for (BlockPos p : around) {
                BlockState s = level.getBlockState(p);
                float hardness = s.getDestroySpeed(level, p);
                // Only what this pickaxe could mine anyway, and nothing much harder than the block you hit.
                if (s.isAir() || hardness < 0 || hardness > centerHardness + 5 || !tool.isCorrectToolForDrops(s)) continue;
                if (FmChargeable.get(tool) < HAMMER_FM || player.getMainHandItem() != tool) break;
                FmChargeable.use(tool, HAMMER_FM);
                player.gameMode.destroyBlock(p);
            }
        } finally {
            hammering = false;
        }
    }

    // Server side of the boost: the client applies the push (it moves the player) and reports the ticks spent.
    public static void boosted(ServerPlayer player, int ticks) {
        if (!fullPrimeSet(player) || !player.isFallFlying()) return;
        ticks = Math.max(0, Math.min(40, ticks));
        FmChargeable.use(player.getItemBySlot(EquipmentSlot.CHEST), ticks * BOOST_FM_PER_TICK);
        player.serverLevel().sendParticles(new DustParticleOptions(VIOLET, 1.4f), player.getX(), player.getY() + 0.4, player.getZ(),
                4, 0.2, 0.2, 0.2, 0);
    }

    // The hammer key: toggle 3x3 on the Prime pickaxe in hand.
    public static void toggleHammer(ServerPlayer player) {
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof PrimeTools.Pickaxe)) return;
        int mode = StormforgedTools.mode(tool) ^ StormforgedTools.HAMMER;
        tool.set(com.sideflowgames.fulguritefusion.ModDataComponents.TOOL_MODE.get(), mode);
        boolean on = (mode & StormforgedTools.HAMMER) != 0;
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("3x3 hammer: " + (on ? "on" : "off"))
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS, 0.6f, on ? 1.6f : 1.0f);
    }
}
