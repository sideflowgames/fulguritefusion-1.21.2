package com.sideflowgames.fulguritefusion;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import com.sideflowgames.fulguritefusion.item.FusionFuelItem;
import com.sideflowgames.fulguritefusion.item.WireRollItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// All of Fulgurite Fusion's items and its creative tab live here.
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FulguriteFusion.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FulguriteFusion.MODID);

    // Clear grey glass vial, crafted from glass, copper and redstone. Stacks to 16.
    public static final DeferredItem<Item> EMPTY_SPARK_CELL = ITEMS.registerSimpleItem("empty_spark_cell",
            new Item.Properties().stacksTo(16));

    // Charged, crystal-blue vial. The enchantment glint gives it the sparkle. Stacks to 16.
    // No crafting recipe: it will be filled by the Fulgurite Charger.
    public static final DeferredItem<Item> SPARK_CELL = ITEMS.registerSimpleItem("spark_cell",
            new Item.Properties().stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    // Dropped when mining Raw Fulgurite (4-8 per block, boosted by Fortune).
    public static final DeferredItem<Item> FULGURITE_SHARD = ITEMS.registerSimpleItem("fulgurite_shard",
            new Item.Properties());

    // Fulgurite: 9 shards in a crafting grid make 2.
    public static final DeferredItem<Item> FULGURITE = ITEMS.registerSimpleItem("fulgurite",
            new Item.Properties());

    // Copper Sheet: used for wire. (Temporary recipe: 2 copper ingots; a processing machine will make these later.)
    public static final DeferredItem<Item> COPPER_SHEET = ITEMS.registerSimpleItem("copper_sheet",
            new Item.Properties());

    // Wire Rolls: string hanging wires between Machine Connectors.
    public static final DeferredItem<WireRollItem> RAW_WIRE = ITEMS.registerItem("raw_wire",
            props -> new WireRollItem(false, props), new Item.Properties());
    public static final DeferredItem<WireRollItem> INSULATED_WIRE = ITEMS.registerItem("insulated_wire",
            props -> new WireRollItem(true, props), new Item.Properties());

    // ---------- Storm Collider materials and products ----------

    // The 12 exotic materials, in design-doc order (index = material number - 1).
    // How each is gathered in survival comes later; for now they are in the creative tab.
    public static final DeferredItem<Item> AURORA_DUST = ITEMS.registerSimpleItem("aurora_dust", new Item.Properties());
    public static final DeferredItem<Item> SUNMOTE = ITEMS.registerSimpleItem("sunmote", new Item.Properties());
    public static final DeferredItem<Item> ABYSSAL_NACRE = ITEMS.registerSimpleItem("abyssal_nacre", new Item.Properties());
    public static final DeferredItem<Item> SOUL_FROST = ITEMS.registerSimpleItem("soul_frost", new Item.Properties());
    public static final DeferredItem<Item> ECHO_RESIN = ITEMS.registerSimpleItem("echo_resin", new Item.Properties());
    public static final DeferredItem<Item> GALE_CORE = ITEMS.registerSimpleItem("gale_core", new Item.Properties());
    public static final DeferredItem<Item> STORMWING_PLUME = ITEMS.registerSimpleItem("stormwing_plume", new Item.Properties());
    public static final DeferredItem<Item> NULL_SCALE = ITEMS.registerSimpleItem("null_scale", new Item.Properties());
    public static final DeferredItem<Item> LUMEN_GEL = ITEMS.registerSimpleItem("lumen_gel", new Item.Properties());
    public static final DeferredItem<Item> CINDER_HEART = ITEMS.registerSimpleItem("cinder_heart", new Item.Properties());
    public static final DeferredItem<Item> WITHER_ASH = ITEMS.registerSimpleItem("wither_ash", new Item.Properties());
    public static final DeferredItem<Item> STRUCK_SPORE = ITEMS.registerSimpleItem("struck_spore", new Item.Properties());

    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item>[] MATERIALS = new DeferredItem[]{AURORA_DUST, SUNMOTE, ABYSSAL_NACRE, SOUL_FROST, ECHO_RESIN, GALE_CORE, STORMWING_PLUME, NULL_SCALE, LUMEN_GEL, CINDER_HEART, WITHER_ASH, STRUCK_SPORE};

    // Pairing byproducts.
    public static final DeferredItem<Item> RESONANT_SHARD = ITEMS.registerSimpleItem("resonant_shard", new Item.Properties());
    public static final DeferredItem<Item> PEARL_EMBER = ITEMS.registerSimpleItem("pearl_ember", new Item.Properties());
    public static final DeferredItem<Item> LIVING_SPARK = ITEMS.registerSimpleItem("living_spark", new Item.Properties());
    public static final DeferredItem<Item> PRIME_SPARK_FRAGMENT = ITEMS.registerSimpleItem("prime_spark_fragment",
            new Item.Properties().rarity(Rarity.RARE));

    // Storm Collider crafting products.
    public static final DeferredItem<Item> PRIME_SPARK = ITEMS.registerSimpleItem("prime_spark",
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<Item> REFINED_FULGURITE = ITEMS.registerSimpleItem("refined_fulgurite", new Item.Properties());
    public static final DeferredItem<Item> CHARGED_SPARK_CELL = ITEMS.registerSimpleItem("charged_spark_cell",
            new Item.Properties().stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<Item> OVERCHARGED_SPARK_CELL = ITEMS.registerSimpleItem("overcharged_spark_cell",
            new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    // Dropped by the Arc Serpent (coming later). Creative tab only for now.
    public static final DeferredItem<Item> ARC_CORE = ITEMS.registerSimpleItem("arc_core", new Item.Properties().rarity(Rarity.RARE));

    // Spawn eggs (creative tab).
    public static final DeferredItem<DeferredSpawnEggItem> LUMEN_SLUG_SPAWN_EGG = ITEMS.registerItem("lumen_slug_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntities.LUMEN_SLUG, 0x2E7D6B, 0xA8FFC4, props), new Item.Properties());
    public static final DeferredItem<DeferredSpawnEggItem> VOIDMOTH_SPAWN_EGG = ITEMS.registerItem("voidmoth_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntities.VOIDMOTH, 0x2B1B3A, 0xC8A8F0, props), new Item.Properties());
    public static final DeferredItem<DeferredSpawnEggItem> STORMWING_SPAWN_EGG = ITEMS.registerItem("stormwing_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntities.STORMWING, 0x5A6170, 0xBFE6FF, props), new Item.Properties());

    // Fusion Fuel capsule: one per pairing, fuel for the big reactor.
    public static final DeferredItem<FusionFuelItem> FUSION_FUEL = ITEMS.registerItem("fusion_fuel",
            FusionFuelItem::new, new Item.Properties().stacksTo(16));

    // Which exotic material (0-11) a stack is, or -1.
    public static int materialIndex(ItemStack stack) {
        for (int i = 0; i < MATERIALS.length; i++) {
            if (stack.is(MATERIALS[i].get())) return i;
        }
        return -1;
    }

    // The mod's own creative tab, shown after the Combat tab.
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fulguritefusion"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> SPARK_CELL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.RAW_FULGURITE_ITEM.get());
                        output.accept(FULGURITE_SHARD.get());
                        output.accept(FULGURITE.get());
                        output.accept(EMPTY_SPARK_CELL.get());
                        output.accept(SPARK_CELL.get());
                        output.accept(ModBlocks.STORM_FURNACE_ITEM.get());
                        output.accept(ModBlocks.ENHANCED_STORM_FURNACE_ITEM.get());
                        output.accept(ModBlocks.CHARGED_STORM_FURNACE_ITEM.get());
                        output.accept(ModBlocks.FULGURITE_CHARGER_ITEM.get());
                        output.accept(ModBlocks.STORM_BATTERY_ITEM.get());
                        output.accept(COPPER_SHEET.get());
                        output.accept(RAW_WIRE.get());
                        output.accept(INSULATED_WIRE.get());
                        output.accept(ModBlocks.MACHINE_CONNECTOR_ITEM.get());
                        output.accept(ModBlocks.FE_CONVERTER_ITEM.get());
                        output.accept(ModBlocks.EU_CONVERTER_ITEM.get());
                        output.accept(ModBlocks.FULGURITE_RING_ITEM.get());
                        output.accept(ModBlocks.COLLIDER_INPUT_ITEM.get());
                        output.accept(ModBlocks.COLLIDER_INJECTOR_ITEM.get());
                        output.accept(ModBlocks.MINI_COLLIDER_ITEM.get());
                        output.accept(ModBlocks.STORM_COLLIDER_ITEM.get());
                        for (DeferredItem<Item> material : MATERIALS) output.accept(material.get());
                        output.accept(RESONANT_SHARD.get());
                        output.accept(PEARL_EMBER.get());
                        output.accept(LIVING_SPARK.get());
                        output.accept(PRIME_SPARK_FRAGMENT.get());
                        output.accept(ARC_CORE.get());
                        output.accept(REFINED_FULGURITE.get());
                        output.accept(CHARGED_SPARK_CELL.get());
                        output.accept(OVERCHARGED_SPARK_CELL.get());
                        output.accept(PRIME_SPARK.get());
                        output.accept(LUMEN_SLUG_SPAWN_EGG.get());
                        output.accept(VOIDMOTH_SPAWN_EGG.get());
                        output.accept(STORMWING_SPAWN_EGG.get());
                        output.accept(ModBlocks.STORMWING_NEST_ITEM.get());
                    })
                    .build());

    private ModItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
