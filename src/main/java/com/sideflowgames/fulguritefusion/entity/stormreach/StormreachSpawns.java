package com.sideflowgames.fulguritefusion.entity.stormreach;

import com.sideflowgames.fulguritefusion.stormreach.Stormreach;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

// Where the Stormreach's creatures may appear (which biomes is set in each biome's "spawners" list).
//   ground  surface animals: solid ground under open sky
//   air     floaters and fliers: open sky with land somewhere below
//   water   swimmers: inside water
//   cave    the hostile ones: dark caves deep inside the land, never under open sky
public final class StormreachSpawns {
    public static final int CAVE_TOP = 108;

    private StormreachSpawns() {}

    private static boolean inStormreach(ServerLevelAccessor level) {
        return level.getLevel().dimension() == Stormreach.LEVEL;
    }

    public static <T extends Mob> boolean ground(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                 BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason) || reason == EntitySpawnReason.SPAWN_ITEM_USE) return true;
        return inStormreach(level) && level.canSeeSky(pos)
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    public static <T extends Mob> boolean air(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                              BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason) || reason == EntitySpawnReason.SPAWN_ITEM_USE) return true;
        if (!inStormreach(level) || !level.canSeeSky(pos) || !level.getBlockState(pos).isAir()) return false;
        for (int i = 1; i <= 10; i++) {
            if (!level.getBlockState(pos.below(i)).isAir()) return true;
        }
        return false;
    }

    public static <T extends Mob> boolean water(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason) || reason == EntitySpawnReason.SPAWN_ITEM_USE) return true;
        return inStormreach(level) && level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    public static <T extends Monster> boolean cave(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                   BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason) || reason == EntitySpawnReason.SPAWN_ITEM_USE) return true;
        return inStormreach(level) && pos.getY() < CAVE_TOP && !level.canSeeSky(pos)
                && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
