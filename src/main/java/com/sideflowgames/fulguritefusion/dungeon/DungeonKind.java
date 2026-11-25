package com.sideflowgames.fulguritefusion.dungeon;

import com.mojang.serialization.Codec;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModEntities;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// The four Stormreach dungeons. Each is a grid of 15x15x9 rooms ("cells"), with a big boss arena and a sealed
// treasure vault that opens when the boss dies.
//   width/depth/levels: the cell grid. tower: levels stack upward from the ground (Stormspire); otherwise level 0
//   is the top and deeper levels go down. surface: level 0 sits on the ground; otherwise the dungeon is buried and
//   reached by a shaft. bossW/D/H: the arena's size in cells, placed at the far end (tower: on top).
public enum DungeonKind implements StringRepresentable {
    STORMSPIRE("stormspire", 3, 3, 7, true, true, 3, 3, 1, 46),
    ASHEN_FOUNDRY("ashen_foundry", 7, 7, 2, false, false, 2, 2, 2, 36),
    AURORA_SANCTUM("aurora_sanctum", 7, 7, 2, false, true, 2, 2, 2, 36),
    PRISM_VAULT("prism_vault", 7, 7, 2, false, false, 2, 2, 2, 38);

    public static final Codec<DungeonKind> CODEC = StringRepresentable.fromEnum(DungeonKind::values);

    public final String id;
    public final int width, depth, levels;
    public final boolean tower, surface;
    public final int bossW, bossD, bossH;
    public final int rooms;

    DungeonKind(String id, int width, int depth, int levels, boolean tower, boolean surface, int bossW, int bossD, int bossH, int rooms) {
        this.id = id;
        this.width = width;
        this.depth = depth;
        this.levels = levels;
        this.tower = tower;
        this.surface = surface;
        this.bossW = bossW;
        this.bossD = bossD;
        this.bossH = bossH;
        this.rooms = rooms;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static DungeonKind byId(String id) {
        for (DungeonKind k : values()) if (k.id.equals(id)) return k;
        return STORMSPIRE;
    }

    // Height of one floor in blocks. The Stormspire's floors are twice as tall.
    public int floorHeight() {
        return tower ? 17 : 9;
    }

    public String title() {
        return switch (this) {
            case STORMSPIRE -> "Stormspire";
            case ASHEN_FOUNDRY -> "Ashen Foundry";
            case AURORA_SANCTUM -> "Aurora Sanctum";
            case PRISM_VAULT -> "Prism Vault";
        };
    }

    // ---- palette ----

    public BlockState wall() {
        return block(0).defaultBlockState();
    }

    public BlockState cracked() {
        return block(1).defaultBlockState();
    }

    public BlockState tile() {
        return block(2).defaultBlockState();
    }

    private Block block(int i) {
        return DungeonBlocks.THEME.get(ordinal() * 3 + i).get();
    }

    public BlockState floor() {
        return switch (this) {
            case STORMSPIRE -> ModBlocks.POLISHED_STORMSTONE.get().defaultBlockState();
            case ASHEN_FOUNDRY -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case AURORA_SANCTUM -> tile();
            case PRISM_VAULT -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        };
    }

    public BlockState accent() {
        return switch (this) {
            case STORMSPIRE -> ModBlocks.STORMQUARTZ_BLOCK.get().defaultBlockState();
            case ASHEN_FOUNDRY -> ModBlocks.CINDERITE_BLOCK.get().defaultBlockState();
            case AURORA_SANCTUM -> ModBlocks.AURORA_ICE.get().defaultBlockState();
            case PRISM_VAULT -> ModBlocks.PRISMITE_BLOCK.get().defaultBlockState();
        };
    }

    public BlockState light() {
        return switch (this) {
            case STORMSPIRE, AURORA_SANCTUM -> Blocks.SEA_LANTERN.defaultBlockState();
            case ASHEN_FOUNDRY -> Blocks.SHROOMLIGHT.defaultBlockState();
            case PRISM_VAULT -> Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
        };
    }

    public BlockState stairs() {
        return switch (this) {
            case STORMSPIRE -> Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState();
            case ASHEN_FOUNDRY -> Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState();
            case AURORA_SANCTUM -> Blocks.POLISHED_DIORITE_STAIRS.defaultBlockState();
            case PRISM_VAULT -> Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState();
        };
    }

    // ---- inhabitants and rewards ----

    public List<Supplier<? extends EntityType<?>>> mobs() {
        return switch (this) {
            case STORMSPIRE -> List.<Supplier<? extends EntityType<?>>>of(ModEntities.ARC_REAVER, ModEntities.THUNDERHULK, ModEntities.STORMCALLER_SHADE,
                    ModEntities.ION_SWARM, ModEntities.TEMPEST_RAPTOR, ModEntities.STORMHOUND);
            case ASHEN_FOUNDRY -> List.<Supplier<? extends EntityType<?>>>of(ModEntities.THUNDERHULK, ModEntities.VOLTSPITTER, ModEntities.GLOOMARC_SPIDER,
                    ModEntities.STATIC_LEECH, ModEntities.ARC_REAVER);
            case AURORA_SANCTUM -> List.<Supplier<? extends EntityType<?>>>of(ModEntities.STORMCALLER_SHADE, ModEntities.ION_SWARM, ModEntities.ARC_REAVER,
                    ModEntities.STATIC_WRAITH, ModEntities.STATIC_LEECH);
            case PRISM_VAULT -> List.<Supplier<? extends EntityType<?>>>of(ModEntities.GLOOMARC_SPIDER, ModEntities.STATIC_LEECH, ModEntities.VOLTSPITTER,
                    ModEntities.CRYSTAL_STALKER, ModEntities.ARC_REAVER);
        };
    }

    public EntityType<?> boss() {
        return switch (this) {
            case STORMSPIRE -> ModEntities.PEAK_COLOSSUS.get();
            case ASHEN_FOUNDRY -> ModEntities.CINDER_BEHEMOTH.get();
            case AURORA_SANCTUM -> ModEntities.AURORA_WRAITH.get();
            case PRISM_VAULT -> ModEntities.PRISM_SENTINEL.get();
        };
    }

    public Item material() {
        return DungeonBlocks.MATERIALS.get(ordinal()).get();
    }
}
