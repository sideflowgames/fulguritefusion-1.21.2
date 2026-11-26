package com.sideflowgames.fulguritefusion.stormreach;

import com.sideflowgames.fulguritefusion.FulguriteFusion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

// The Stormreach's three trees (shapes in data/fulguritefusion/worldgen/configured_feature/*_tree.json):
//   Conductor   tall and thin, copper-tinted bark, glowing pale-blue leaves
//   Thunderoak  broad and twisted, dark wood, violet leaves
//   Sparkbush   a small, bushy tree with brightly glowing leaves
public final class StormFlora {
    private StormFlora() {}

    public static final TreeGrower CONDUCTOR = grower("conductor");
    public static final TreeGrower THUNDEROAK = grower("thunderoak");
    public static final TreeGrower SPARKBUSH = grower("sparkbush");

    private static TreeGrower grower(String name) {
        ResourceKey<ConfiguredFeature<?, ?>> key = ResourceKey.create(Registries.CONFIGURED_FEATURE,
                Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, name + "_tree"));
        return new TreeGrower(FulguriteFusion.MODID + ":" + name, Optional.empty(), Optional.of(key), Optional.empty());
    }
}
