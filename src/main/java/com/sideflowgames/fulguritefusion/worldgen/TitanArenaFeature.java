package com.sideflowgames.fulguritefusion.worldgen;

import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

// Worldgen (Stormreach): a Titan arena. A round Stormstone Brick floor 23 blocks across, ringed by eight pillars
// crowned with amethyst, with the Titan Altar on a dais at the centre. No loot: players bring their own
// Lightning Rods for the Titan's conduits.
// The ground under the floor is filled in and the air above it cleared so the Titan has room.
// Spacing: the Stormreach is split into regions of REGION x REGION chunks, and each region has exactly one chunk
// (picked from the world seed) where an arena may stand. That chunk is kept MARGIN chunks in from the region's
// edge, so two arenas are always at least 2 * MARGIN chunks apart. If the ground there is unsuitable (water, a
// cliff), that region simply has no arena.
public class TitanArenaFeature extends Feature<NoneFeatureConfiguration> {
    private static final int R = 11;
    private static final int REGION = 28;
    private static final int MARGIN = 4;

    public TitanArenaFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    // True for the one chunk in each region that is allowed an arena.
    public static boolean isArenaChunk(long seed, int cx, int cz) {
        int rx = Math.floorDiv(cx, REGION), rz = Math.floorDiv(cz, REGION);
        java.util.Random r = new java.util.Random(seed ^ (rx * 341873128712L) ^ (rz * 132897987541L) ^ 0x5717A7L);
        int span = REGION - 2 * MARGIN;
        return cx == rx * REGION + MARGIN + r.nextInt(span) && cz == rz * REGION + MARGIN + r.nextInt(span);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!isArenaChunk(level.getSeed(), context.origin().getX() >> 4, context.origin().getZ() >> 4)) return false;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, context.origin());
        BlockPos ground = top.below();
        if (top.getY() < level.getMinY() + 40) return false;
        if (level.getFluidState(ground).is(FluidTags.WATER) || level.getBlockState(ground).isAir()) return false;
        // Not over a crack or on a cliff: the ground across the arena must be roughly level.
        for (int dx = -R; dx <= R; dx += R) {
            for (int dz = -R; dz <= R; dz += R) {
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, top.getX() + dx, top.getZ() + dz);
                if (Math.abs(h - top.getY()) > 8) return false;
            }
        }
        int y0 = ground.getY();
        BlockState bricks = ModBlocks.STORMSTONE_BRICKS.get().defaultBlockState();
        BlockState polished = ModBlocks.POLISHED_STORMSTONE.get().defaultBlockState();
        BlockState stone = ModBlocks.STORMSTONE.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > R + 0.5) continue;
                int x = top.getX() + dx, z = top.getZ() + dz;
                // Floor: bricks, with a polished ring and a polished centre.
                boolean ring = d >= 7.5 && d < 8.5;
                set(level, p.set(x, y0, z), ring || d < 3.0 ? polished : bricks);
                // Fill below until solid ground.
                for (int y = y0 - 1; y > y0 - 24; y--) {
                    p.set(x, y, z);
                    if (level.getBlockState(p).isSolid()) break;
                    set(level, p, stone);
                }
                // Clear the air above for the Titan.
                for (int y = y0 + 1; y <= y0 + 18; y++) {
                    p.set(x, y, z);
                    if (!level.getBlockState(p).isAir()) set(level, p, air);
                }
            }
        }
        // Eight pillars around the edge.
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = top.getX() + (int) Math.round(Math.cos(a) * 10), z = top.getZ() + (int) Math.round(Math.sin(a) * 10);
            for (int y = y0 + 1; y <= y0 + 6; y++) set(level, p.set(x, y, z), y == y0 + 6 ? polished : bricks);
            set(level, p.set(x, y0 + 7, z), Blocks.AMETHYST_CLUSTER.defaultBlockState());
        }
        // Dais and altar.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) set(level, p.set(top.getX() + dx, y0 + 1, top.getZ() + dz), polished);
        }
        BlockPos altar = new BlockPos(top.getX(), y0 + 2, top.getZ());
        set(level, altar, ModBlocks.TITAN_ALTAR.get().defaultBlockState());
        return true;
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }
}
