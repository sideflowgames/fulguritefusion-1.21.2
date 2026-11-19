package com.sideflowgames.fulguritefusion.worldgen;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModEntities;
import com.sideflowgames.fulguritefusion.entity.StormwingEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

// Worldgen: a Stormwing nest on a mountain peak, with its Stormwing living there.
// How often nests appear is set in data/fulguritefusion/worldgen/placed_feature/stormwing_nest.json.
public class StormwingNestFeature extends Feature<NoneFeatureConfiguration> {
    public static final int MIN_HEIGHT = 120;

    public StormwingNestFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, context.origin());
        if (top.getY() < MIN_HEIGHT) return false;
        BlockPos ground = top.below();
        if (level.getFluidState(ground).is(FluidTags.WATER) || level.getBlockState(ground).isAir()) return false;
        // Only on high points: nothing around may stand much taller than the nest.
        for (int dx = -6; dx <= 6; dx += 3) {
            for (int dz = -6; dz <= 6; dz += 3) {
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, top.getX() + dx, top.getZ() + dz);
                if (h > top.getY() + 3) return false;
            }
        }
        BlockPos center = StormwingNests.build(level, ground, random, random.nextFloat() < Config.STORMWING_EGG_CHANCE.get());

        StormwingEntity bird = ModEntities.STORMWING.get().create(level.getLevel());
        if (bird != null) {
            bird.moveTo(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5, random.nextFloat() * 360f, 0f);
            bird.adoptNest(center);
            level.addFreshEntityWithPassengers(bird);
        }
        return true;
    }
}
