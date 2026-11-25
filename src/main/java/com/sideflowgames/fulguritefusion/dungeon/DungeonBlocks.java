package com.sideflowgames.fulguritefusion.dungeon;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.trinket.TrinketItem;
import com.sideflowgames.fulguritefusion.trinket.TrinketKind;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

// Blocks and items of the Stormreach dungeons. Registered through ModBlocks / ModItems' registers; FulguriteFusion
// calls init() before the registers fire.
public final class DungeonBlocks {
    private DungeonBlocks() {}

    private static final String[][] THEME_IDS = {
            {"spire_bricks", "cracked_spire_bricks", "spire_tiles"},
            {"foundry_bricks", "cracked_foundry_bricks", "foundry_grate"},
            {"sanctum_bricks", "cracked_sanctum_bricks", "sanctum_tiles"},
            {"vault_bricks", "cracked_vault_bricks", "prism_tiles"}};
    private static final MapColor[] THEME_COLORS = {MapColor.COLOR_LIGHT_BLUE, MapColor.COLOR_BLACK, MapColor.SNOW, MapColor.COLOR_PURPLE};
    private static final SoundType[] THEME_SOUNDS = {SoundType.DEEPSLATE_BRICKS, SoundType.NETHER_BRICKS, SoundType.STONE, SoundType.DEEPSLATE_TILES};

    // Three building blocks per dungeon: bricks, cracked bricks, tiles. Index = kind.ordinal() * 3 + n.
    public static final List<DeferredBlock<Block>> THEME = new ArrayList<>();
    public static final List<DeferredItem<BlockItem>> THEME_ITEMS = new ArrayList<>();

    static {
        for (int k = 0; k < 4; k++) {
            for (int n = 0; n < 3; n++) {
                int light = (k == 1 && n == 2) ? 6 : 0;
                DeferredBlock<Block> b = ModBlocks.BLOCKS.registerSimpleBlock(THEME_IDS[k][n], BlockBehaviour.Properties.of()
                        .mapColor(THEME_COLORS[k]).strength(3.0f, 9.0f).requiresCorrectToolForDrops().sound(THEME_SOUNDS[k])
                        .lightLevel(s -> light));
                THEME.add(b);
                THEME_ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(THEME_IDS[k][n], b));
            }
        }
    }

    // Warded Brick: the walls of a treasure vault. Can't be broken until the dungeon's boss dies.
    public static final DeferredBlock<Block> WARDED_BRICK = ModBlocks.BLOCKS.registerSimpleBlock("warded_brick",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1.0f, 3600000.0f).noLootTable()
                    .lightLevel(s -> 5).sound(SoundType.DEEPSLATE_BRICKS).pushReaction(PushReaction.BLOCK));
    // Storm Seal: a crackling barrier in a treasure vault's doorway. Vanishes when the boss dies.
    public static final DeferredBlock<StormSealBlock> STORM_SEAL = ModBlocks.BLOCKS.registerBlock("storm_seal", StormSealBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(-1.0f, 3600000.0f).noLootTable()
                    .lightLevel(s -> 12).sound(SoundType.AMETHYST).noOcclusion().pushReaction(PushReaction.BLOCK)
                    .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    public static final DeferredBlock<ArcTrapBlock> ARC_TRAP = ModBlocks.BLOCKS.registerBlock("arc_trap", ArcTrapBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 9.0f).requiresCorrectToolForDrops().noLootTable()
                    .sound(SoundType.COPPER).lightLevel(s -> s.getValue(ArcTrapBlock.CHARGED) ? 4 : 0));
    public static final DeferredBlock<TeslaCoilBlock> TESLA_COIL = ModBlocks.BLOCKS.registerBlock("tesla_coil", TeslaCoilBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(4.0f, 9.0f).requiresCorrectToolForDrops().noLootTable()
                    .sound(SoundType.COPPER).lightLevel(s -> 7).noOcclusion());
    public static final DeferredBlock<DungeonSpawnerBlock> DUNGEON_SPAWNER = ModBlocks.BLOCKS.registerBlock("storm_cage", DungeonSpawnerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(10.0f, 1200.0f).requiresCorrectToolForDrops().noLootTable()
                    .sound(SoundType.METAL).noOcclusion().lightLevel(s -> s.getValue(DungeonSpawnerBlock.SPENT) ? 0 : 9));
    public static final DeferredBlock<DungeonAltarBlock> DUNGEON_ALTAR = ModBlocks.BLOCKS.registerBlock("dungeon_altar", DungeonAltarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1.0f, 3600000.0f).noLootTable()
                    .lightLevel(s -> 11).sound(SoundType.DEEPSLATE_BRICKS).pushReaction(PushReaction.BLOCK));

    // Signature hazards, one per dungeon (see DungeonHazardBlock).
    public static final DeferredBlock<DungeonHazardBlock> STORM_CONDUCTOR = ModBlocks.BLOCKS.registerBlock("storm_conductor",
            p -> new DungeonHazardBlock(DungeonHazardBlock.CONDUCTOR, p), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(4.0f, 9.0f).requiresCorrectToolForDrops().noLootTable().sound(SoundType.COPPER).lightLevel(s -> 6).noOcclusion());
    public static final DeferredBlock<DungeonHazardBlock> CRUSHER = ModBlocks.BLOCKS.registerBlock("crusher_block",
            p -> new DungeonHazardBlock(DungeonHazardBlock.CRUSHER, p), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(6.0f, 12.0f).requiresCorrectToolForDrops().noLootTable().sound(SoundType.NETHERITE_BLOCK));
    public static final DeferredBlock<DungeonHazardBlock> RIME_TOTEM = ModBlocks.BLOCKS.registerBlock("rime_totem",
            p -> new DungeonHazardBlock(DungeonHazardBlock.TOTEM, p), BlockBehaviour.Properties.of().mapColor(MapColor.ICE)
                    .strength(3.0f, 6.0f).noLootTable().sound(SoundType.GLASS).lightLevel(s -> 8).noOcclusion().friction(0.98f));
    public static final DeferredBlock<DungeonHazardBlock> HANGING_PRISM = ModBlocks.BLOCKS.registerBlock("hanging_prism",
            p -> new DungeonHazardBlock(DungeonHazardBlock.PRISM, p), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK)
                    .strength(1.5f, 3.0f).noLootTable().sound(SoundType.AMETHYST_CLUSTER).lightLevel(s -> 5).noOcclusion());
    public static final DeferredItem<BlockItem> STORM_CONDUCTOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem("storm_conductor", STORM_CONDUCTOR);
    public static final DeferredItem<BlockItem> CRUSHER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("crusher_block", CRUSHER);
    public static final DeferredItem<BlockItem> RIME_TOTEM_ITEM = ModItems.ITEMS.registerSimpleBlockItem("rime_totem", RIME_TOTEM);
    public static final DeferredItem<BlockItem> HANGING_PRISM_ITEM = ModItems.ITEMS.registerSimpleBlockItem("hanging_prism", HANGING_PRISM);
    public static final Supplier<BlockEntityType<DungeonHazardBlockEntity>> HAZARD_BE = ModBlocks.BLOCK_ENTITIES.register("dungeon_hazard",
            () -> BlockEntityType.Builder.of(DungeonHazardBlockEntity::new, STORM_CONDUCTOR.get(), CRUSHER.get(), RIME_TOTEM.get(), HANGING_PRISM.get()).build(null));

    public static final DeferredItem<BlockItem> WARDED_BRICK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("warded_brick", WARDED_BRICK);
    public static final DeferredItem<BlockItem> STORM_SEAL_ITEM = ModItems.ITEMS.registerSimpleBlockItem("storm_seal", STORM_SEAL);
    public static final DeferredItem<BlockItem> ARC_TRAP_ITEM = ModItems.ITEMS.registerSimpleBlockItem("arc_trap", ARC_TRAP);
    public static final DeferredItem<BlockItem> TESLA_COIL_ITEM = ModItems.ITEMS.registerSimpleBlockItem("tesla_coil", TESLA_COIL);
    public static final DeferredItem<BlockItem> DUNGEON_SPAWNER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("storm_cage", DUNGEON_SPAWNER);
    public static final DeferredItem<BlockItem> DUNGEON_ALTAR_ITEM = ModItems.ITEMS.registerSimpleBlockItem("dungeon_altar", DUNGEON_ALTAR);

    public static final Supplier<BlockEntityType<TeslaCoilBlockEntity>> TESLA_COIL_BE = ModBlocks.BLOCK_ENTITIES.register("tesla_coil",
            () -> BlockEntityType.Builder.of(TeslaCoilBlockEntity::new, TESLA_COIL.get()).build(null));
    public static final Supplier<BlockEntityType<DungeonSpawnerBlockEntity>> SPAWNER_BE = ModBlocks.BLOCK_ENTITIES.register("storm_cage",
            () -> BlockEntityType.Builder.of(DungeonSpawnerBlockEntity::new, DUNGEON_SPAWNER.get()).build(null));
    public static final Supplier<BlockEntityType<DungeonAltarBlockEntity>> ALTAR_BE = ModBlocks.BLOCK_ENTITIES.register("dungeon_altar",
            () -> BlockEntityType.Builder.of(DungeonAltarBlockEntity::new, DUNGEON_ALTAR.get()).build(null));

    // One crafting material per dungeon, found only in its chests: Skyforged Plate, Foundry Heartstone, Aurora
    // Essence, Prism Core.
    public static final List<DeferredItem<Item>> MATERIALS = List.of(
            ModItems.ITEMS.registerSimpleItem("skyforged_plate", new Item.Properties().rarity(Rarity.RARE)),
            ModItems.ITEMS.registerSimpleItem("foundry_heartstone", new Item.Properties().rarity(Rarity.RARE).fireResistant()),
            ModItems.ITEMS.registerSimpleItem("aurora_essence", new Item.Properties().rarity(Rarity.RARE)),
            ModItems.ITEMS.registerSimpleItem("prism_core", new Item.Properties().rarity(Rarity.RARE)));

    // Relics: a dungeon-only third tier of a trinket, stronger than Empowered.
    public static final DeferredItem<TrinketItem> SKYRENDER_CHARM = relic("skyrender_charm", TrinketKind.CLOUDSTEP_CHARM);
    public static final DeferredItem<TrinketItem> FOUNDRY_HEART = relic("foundry_heart", TrinketKind.CINDER_WARD);
    public static final DeferredItem<TrinketItem> AURORA_LOOP = relic("aurora_loop", TrinketKind.VOID_ARC_BAND);
    public static final DeferredItem<TrinketItem> PRISM_EYE = relic("prism_eye", TrinketKind.SEERS_RING);

    private static DeferredItem<TrinketItem> relic(String id, TrinketKind kind) {
        return ModItems.ITEMS.registerItem(id, p -> new TrinketItem(kind, p),
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    public static void init() {}

    public static void addToTab(CreativeModeTab.Output output) {
        for (var i : THEME_ITEMS) output.accept(i.get());
        output.accept(WARDED_BRICK_ITEM.get());
        output.accept(STORM_SEAL_ITEM.get());
        output.accept(ARC_TRAP_ITEM.get());
        output.accept(TESLA_COIL_ITEM.get());
        output.accept(DUNGEON_SPAWNER_ITEM.get());
        output.accept(DUNGEON_ALTAR_ITEM.get());
        output.accept(STORM_CONDUCTOR_ITEM.get());
        output.accept(CRUSHER_ITEM.get());
        output.accept(RIME_TOTEM_ITEM.get());
        output.accept(HANGING_PRISM_ITEM.get());
        for (var m : MATERIALS) output.accept(m.get());
        output.accept(SKYRENDER_CHARM.get());
        output.accept(FOUNDRY_HEART.get());
        output.accept(AURORA_LOOP.get());
        output.accept(PRISM_EYE.get());
    }
}
