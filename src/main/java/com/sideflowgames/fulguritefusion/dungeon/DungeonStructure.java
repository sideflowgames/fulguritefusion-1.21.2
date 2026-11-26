package com.sideflowgames.fulguritefusion.dungeon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

// A Stormreach dungeon (data/fulguritefusion/worldgen/structure/<id>.json). The layout is built by DungeonLayout.
public class DungeonStructure extends Structure {
    public static final MapCodec<DungeonStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            DungeonKind.CODEC.fieldOf("dungeon").forGetter(s -> s.kind)
    ).apply(i, DungeonStructure::new));

    public final DungeonKind kind;

    public DungeonStructure(StructureSettings settings, DungeonKind kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos chunk = ctx.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        int ground = height(ctx, x, z);
        if (kind.tower) {
            // The Stormspire needs room above it: skip sites too high for the whole tower to fit under the build limit.
            int doorGround = height(ctx, x, z - kind.depth * DungeonLayout.CELL / 2 + kind.depth * DungeonLayout.CELL + 3);
            if (doorGround + kind.levels * kind.floorHeight() + 4 > ctx.heightAccessor().getMaxY()) return Optional.empty();
        }
        return Optional.of(new GenerationStub(new BlockPos(x, ground, z), builder -> DungeonLayout.generate(kind, ctx, builder, x, z)));
    }

    static int height(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
    }

    @Override
    public StructureType<?> type() {
        return DungeonRegistry.DUNGEON.get();
    }
}
