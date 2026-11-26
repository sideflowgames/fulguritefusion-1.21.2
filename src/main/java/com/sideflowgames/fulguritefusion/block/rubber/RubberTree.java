package com.sideflowgames.fulguritefusion.block.rubber;

import com.sideflowgames.fulguritefusion.FulguriteFusion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

// Rubber trees: tall, slim trees that grow in jungles and swamps (data/fulguritefusion/worldgen/...).
// Saplings drop from their leaves and wandering traders sell them. Put a Tree Tap on the trunk to collect latex.
public final class RubberTree {
    public static final ResourceKey<ConfiguredFeature<?, ?>> FEATURE = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "rubber_tree"));

    public static final TreeGrower GROWER = new TreeGrower(FulguriteFusion.MODID + ":rubber",
            Optional.empty(), Optional.of(FEATURE), Optional.empty());

    // Rubber Log and Rubber Wood: what a Tree Tap can sit on.
    public static final TagKey<Block> LOGS = BlockTags.create(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "rubber_logs"));

    private RubberTree() {}
}
