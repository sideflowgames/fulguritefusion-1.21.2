package com.sideflowgames.fulguritefusion.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Builds and reads Stormwing nests: a 5x5 bowl of woven twigs with raw fulgurite shards in the rim.
//
//   y+1:   . R R R .      R = rim (twigs, sometimes a fulgurite shard)
//          R . . . R      . = open bowl; eggs and shed Molted Plumes sit here
//          R . B . R      B = where the bird sits (center)
//          R . . . R
//          . R R R .
//   y:     R R R R R      F = nest floor (3x3), R = rim
//          R F F F R
//          R F F F R
//          R F F F R
//          R R R R R
//   Twig stilts fill any gap under the nest (up to 4 blocks) so it sits firmly on a rough peak.
public final class StormwingNests {
    public static final float FULGURITE_IN_RIM = 0.25f;
    public static final int MAX_PLUMES = 2;

    private StormwingNests() {}

    // Build a nest with its floor center at 'center'. Returns the center.
    public static BlockPos build(LevelAccessor level, BlockPos center, RandomSource random, boolean withEgg) {
        BlockState floor = ModBlocks.STORMWING_NEST.get().defaultBlockState();
        BlockState rim = ModBlocks.STORMWING_NEST_RIM.get().defaultBlockState();
        BlockState shard = ModBlocks.RAW_FULGURITE.get().defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean ring = Math.max(Math.abs(dx), Math.abs(dz)) == 2;
                boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                BlockPos base = center.offset(dx, 0, dz);
                // Stilts down to the ground.
                for (int down = 1; down <= 4; down++) {
                    BlockPos p = base.below(down);
                    if (!level.getBlockState(p).canBeReplaced()) break;
                    level.setBlock(p, rim, 2);
                }
                level.setBlock(base, ring ? rim : floor, 2);
                BlockPos wall = base.above();
                if (ring && !corner) {
                    level.setBlock(wall, random.nextFloat() < FULGURITE_IN_RIM ? shard : rim, 2);
                } else {
                    level.setBlock(wall, Blocks.AIR.defaultBlockState(), 2);
                }
                // Clear snow and plants above.
                for (int up = 2; up <= 4; up++) {
                    BlockPos p = base.above(up);
                    if (!level.getBlockState(p).isAir() && level.getBlockState(p).canBeReplaced()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        List<BlockPos> spots = openSpots(level, center);
        int plumes = 1 + random.nextInt(2);
        for (int i = 0; i < plumes && !spots.isEmpty(); i++) {
            level.setBlock(spots.remove(random.nextInt(spots.size())), ModBlocks.MOLTED_PLUME_PILE.get().defaultBlockState(), 2);
        }
        if (withEgg && !spots.isEmpty()) {
            level.setBlock(spots.remove(random.nextInt(spots.size())), ModBlocks.STORMWING_EGG.get().defaultBlockState(), 2);
        }
        return center;
    }

    // Empty spots in the bowl (on the floor, around the bird's center spot).
    public static List<BlockPos> openSpots(LevelAccessor level, BlockPos center) {
        List<BlockPos> spots = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos p = center.offset(dx, 1, dz);
                if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(ModBlocks.STORMWING_NEST.get())) {
                    spots.add(p);
                }
            }
        }
        return spots;
    }

    // A nesting bird sheds a Molted Plume into the bowl now and then (at most 2 lying there at once).
    public static void shedPlume(LevelAccessor level, BlockPos center, RandomSource random) {
        int lying = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getBlockState(center.offset(dx, 1, dz)).is(ModBlocks.MOLTED_PLUME_PILE.get())) lying++;
            }
        }
        if (lying >= MAX_PLUMES) return;
        List<BlockPos> spots = openSpots(level, center);
        if (!spots.isEmpty()) {
            level.setBlock(spots.get(random.nextInt(spots.size())), ModBlocks.MOLTED_PLUME_PILE.get().defaultBlockState(), 3);
        }
    }

    // The center of a nest: a floor block with floor on all four sides.
    public static boolean isCenter(LevelAccessor level, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModBlocks.STORMWING_NEST.get())) return false;
        for (BlockPos p : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()}) {
            if (!level.getBlockState(p).is(ModBlocks.STORMWING_NEST.get())) return false;
        }
        return true;
    }

    // Look for an existing nest center near 'origin'.
    public static BlockPos findNear(LevelAccessor level, BlockPos origin, int radius) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    p.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.getBlockState(p).is(ModBlocks.STORMWING_NEST.get()) && isCenter(level, p)) {
                        double d = p.distSqr(origin);
                        if (d < bestDist) {
                            bestDist = d;
                            best = p.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }
}
