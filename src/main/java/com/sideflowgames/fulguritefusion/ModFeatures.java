package com.sideflowgames.fulguritefusion;

import java.util.function.Supplier;

import com.sideflowgames.fulguritefusion.worldgen.StormwingNestFeature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

// Worldgen features. Where and how often they appear is set in data/fulguritefusion/worldgen
// and data/fulguritefusion/neoforge/biome_modifier.
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, FulguriteFusion.MODID);

    public static final Supplier<Feature<NoneFeatureConfiguration>> STORMWING_NEST =
            FEATURES.register("stormwing_nest", StormwingNestFeature::new);
    public static final Supplier<Feature<NoneFeatureConfiguration>> TITAN_ARENA =
            FEATURES.register("titan_arena", com.sideflowgames.fulguritefusion.worldgen.TitanArenaFeature::new);

    private ModFeatures() {}

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
