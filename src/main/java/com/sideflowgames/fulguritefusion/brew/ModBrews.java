package com.sideflowgames.fulguritefusion.brew;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.dungeon.DungeonBlocks;
import com.sideflowgames.fulguritefusion.item.FmChargeable;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Stormreach brews: potions brewed at an ordinary brewing stand from an Awkward Potion and a mob part or dungeon
// material. Splash and lingering versions work the usual way (gunpowder, dragon's breath).
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class ModBrews {
    private ModBrews() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, FulguriteFusion.MODID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, FulguriteFusion.MODID);

    // ---- effects ----

    // Storm Resistance: storm damage taken is cut by 25% (see StormDamage).
    public static final DeferredHolder<MobEffect, MobEffect> STORM_RESISTANCE = EFFECTS.register("storm_resistance",
            () -> new Simple(MobEffectCategory.BENEFICIAL, 0x7CD8FF));
    // Volt Surge (instant): puts 250,000 Fm into the charged gear you're wearing and holding.
    public static final DeferredHolder<MobEffect, MobEffect> VOLT_SURGE = EFFECTS.register("volt_surge", VoltSurge::new);
    // Steadfast: can't be knocked back.
    public static final DeferredHolder<MobEffect, MobEffect> STEADFAST = EFFECTS.register("steadfast",
            () -> new Simple(MobEffectCategory.BENEFICIAL, 0x5C6274).addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE,
                    Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "steadfast"), 1.0, AttributeModifier.Operation.ADD_VALUE));
    // Storm Veil: storm beasts lose track of you (see StormBeast).
    public static final DeferredHolder<MobEffect, MobEffect> STORM_VEIL = EFFECTS.register("storm_veil",
            () -> new Simple(MobEffectCategory.BENEFICIAL, 0x6C5CA8));
    // Frost Ward: you never freeze.
    public static final DeferredHolder<MobEffect, MobEffect> FROST_WARD = EFFECTS.register("frost_ward", FrostWard::new);
    // Prism Sight: every monster within 32 blocks glows.
    public static final DeferredHolder<MobEffect, MobEffect> PRISM_SIGHT = EFFECTS.register("prism_sight", PrismSight::new);

    // ---- potions ----

    public static final DeferredHolder<Potion, Potion> ION_TONIC = POTIONS.register("ion_tonic",
            () -> new Potion("ion_tonic", new MobEffectInstance(STORM_RESISTANCE, 3600)));
    public static final DeferredHolder<Potion, Potion> VOLT_DRAUGHT = POTIONS.register("volt_draught",
            () -> new Potion("volt_draught", new MobEffectInstance(VOLT_SURGE, 1)));
    public static final DeferredHolder<Potion, Potion> HULKS_BREW = POTIONS.register("hulks_brew",
            () -> new Potion("hulks_brew", new MobEffectInstance(MobEffects.RESISTANCE, 1800, 1), new MobEffectInstance(STEADFAST, 1800)));
    public static final DeferredHolder<Potion, Potion> VEIL_DRAUGHT = POTIONS.register("veil_draught",
            () -> new Potion("veil_draught", new MobEffectInstance(MobEffects.INVISIBILITY, 3600), new MobEffectInstance(STORM_VEIL, 3600)));
    public static final DeferredHolder<Potion, Potion> LEECH_TONIC = POTIONS.register("leech_tonic",
            () -> new Potion("leech_tonic", new MobEffectInstance(MobEffects.REGENERATION, 600, 1)));
    public static final DeferredHolder<Potion, Potion> SKYSTEP_DRAUGHT = POTIONS.register("skystep_draught",
            () -> new Potion("skystep_draught", new MobEffectInstance(MobEffects.SLOW_FALLING, 3600), new MobEffectInstance(MobEffects.JUMP_BOOST, 3600, 1)));
    public static final DeferredHolder<Potion, Potion> EMBER_ELIXIR = POTIONS.register("ember_elixir",
            () -> new Potion("ember_elixir", new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 12000)));
    public static final DeferredHolder<Potion, Potion> RIME_DRAUGHT = POTIONS.register("rime_draught",
            () -> new Potion("rime_draught", new MobEffectInstance(FROST_WARD, 9600), new MobEffectInstance(STORM_RESISTANCE, 9600)));
    public static final DeferredHolder<Potion, Potion> PRISM_SIGHT_POTION = POTIONS.register("prism_sight",
            () -> new Potion("prism_sight", new MobEffectInstance(MobEffects.NIGHT_VISION, 6000), new MobEffectInstance(PRISM_SIGHT, 6000)));

    public static void register(IEventBus modEventBus) {
        UtilityBlocks.init();
        com.sideflowgames.fulguritefusion.farm.FarmBlocks.init();
        EFFECTS.register(modEventBus);
        POTIONS.register(modEventBus);
    }

    @SubscribeEvent
    public static void onBrewing(RegisterBrewingRecipesEvent event) {
        var b = event.getBuilder();
        b.addMix(Potions.AWKWARD, ModItems.ION_MOTE.get(), ION_TONIC);
        b.addMix(Potions.AWKWARD, ModItems.VOLT_GLAND.get(), VOLT_DRAUGHT);
        b.addMix(Potions.AWKWARD, ModItems.THUNDERHULK_CORE.get(), HULKS_BREW);
        b.addMix(Potions.AWKWARD, ModItems.SHADE_VEIL.get(), VEIL_DRAUGHT);
        b.addMix(Potions.AWKWARD, ModItems.LEECH_FANG.get(), LEECH_TONIC);
        b.addMix(Potions.AWKWARD, ModItems.TEMPEST_FEATHER.get(), SKYSTEP_DRAUGHT);
        b.addMix(Potions.AWKWARD, DungeonBlocks.MATERIALS.get(1).get(), EMBER_ELIXIR);
        b.addMix(Potions.AWKWARD, DungeonBlocks.MATERIALS.get(2).get(), RIME_DRAUGHT);
        b.addMix(Potions.AWKWARD, DungeonBlocks.MATERIALS.get(3).get(), PRISM_SIGHT_POTION);
    }

    public static boolean has(LivingEntity e, Holder<MobEffect> effect) {
        return e.hasEffect(effect);
    }

    // ---- effect classes ----

    static class Simple extends MobEffect {
        Simple(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    static class VoltSurge extends MobEffect {
        VoltSurge() {
            super(MobEffectCategory.BENEFICIAL, 0xD8FF4C);
        }

        @Override
        public boolean isInstantenous() {
            return true;
        }

        @Override
        public void applyInstantenousEffect(ServerLevel level, @Nullable Entity source, @Nullable Entity indirect, LivingEntity entity, int amplifier, double health) {
            int left = 250_000 * (amplifier + 1);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = entity.getItemBySlot(slot);
                if (left > 0 && FmChargeable.is(stack)) left -= FmChargeable.add(stack, left);
            }
            if (entity.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY(0.5), entity.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
            }
        }
    }

    static class FrostWard extends MobEffect {
        FrostWard() {
            super(MobEffectCategory.BENEFICIAL, 0xBCE8FF);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            if (entity.getTicksFrozen() > 0) entity.setTicksFrozen(0);
            return true;
        }
    }

    static class PrismSight extends MobEffect {
        PrismSight() {
            super(MobEffectCategory.BENEFICIAL, 0xFF9CF0);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 20 == 0;
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            if (entity.level() instanceof ServerLevel server) {
                for (Mob m : server.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(32), m -> m instanceof Enemy && m.isAlive())) {
                    m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
                }
            }
            return true;
        }
    }
}
