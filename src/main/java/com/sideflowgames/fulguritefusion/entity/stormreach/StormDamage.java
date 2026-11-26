package com.sideflowgames.fulguritefusion.entity.stormreach;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.item.FmChargeable;
import com.sideflowgames.fulguritefusion.item.TitanArmorItem;
import com.sideflowgames.fulguritefusion.titan.TitanArmorEvents;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// Storm damage: what the Stormreach's hostile beasts hit with. It goes straight through normal armor
// (data/fulguritefusion/damage_type/storm.json is tagged minecraft:bypasses_armor), but Titan armor grounds it:
// each Titan piece takes 12% off, and a full charged Titan set takes 75% off.
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class StormDamage {
    private StormDamage() {}

    public static final ResourceKey<DamageType> STORM =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "storm"));
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    // Storm damage with no attacker (traps).
    public static DamageSource trap(net.minecraft.world.level.Level level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(STORM));
    }

    public static DamageSource source(Entity attacker) {
        return source(attacker, attacker);
    }

    public static DamageSource source(Entity direct, @Nullable Entity owner) {
        return new DamageSource(direct.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(STORM), direct, owner);
    }

    // Hurts with storm damage; never hits other storm beasts.
    public static boolean hit(Entity attacker, Entity target, float amount) {
        if (target instanceof StormBeast || target == attacker) return false;
        return target.hurtOrSimulate(source(attacker), amount);
    }

    public static boolean hit(Entity direct, @Nullable Entity owner, Entity target, float amount) {
        if (target instanceof StormBeast || target == owner) return false;
        return target.hurtOrSimulate(source(direct, owner), amount);
    }

    // Takes up to `amount` Fm from the charged gear the entity is wearing or holding. Returns what was taken.
    public static int drain(LivingEntity target, int amount) {
        int left = amount;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            if (left > 0 && FmChargeable.hasCharge(stack)) left -= FmChargeable.use(stack, left);
        }
        if (left < amount && target.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(), 8, 0.3, 0.5, 0.3, 0.1);
        }
        return amount - left;
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(STORM)) return;
        LivingEntity e = event.getEntity();
        // Ion Tonic / Rime Draught
        if (e.hasEffect(com.sideflowgames.fulguritefusion.brew.ModBrews.STORM_RESISTANCE)) event.setAmount(event.getAmount() * 0.75f);
        if (TitanArmorEvents.fullTitanSet(e)) {
            event.setAmount(event.getAmount() * 0.25f);
            return;
        }
        int pieces = 0;
        for (EquipmentSlot slot : ARMOR) if (e.getItemBySlot(slot).getItem() instanceof TitanArmorItem) pieces++;
        if (pieces > 0) event.setAmount(event.getAmount() * (1 - 0.12f * pieces));
    }
}
