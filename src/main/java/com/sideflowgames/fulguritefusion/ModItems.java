package com.sideflowgames.fulguritefusion;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import com.sideflowgames.fulguritefusion.item.FusionFuelItem;
import com.sideflowgames.fulguritefusion.item.WireRollItem;
import com.sideflowgames.fulguritefusion.item.StormCallerItem;
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

    // Copper Sheet: used for wire. Made in the Press from copper ingots.
    public static final DeferredItem<Item> COPPER_SHEET = ITEMS.registerSimpleItem("copper_sheet",
            new Item.Properties());

    // Wire Rolls: string hanging wires between Machine Connectors.
    public static final DeferredItem<WireRollItem> RAW_WIRE = ITEMS.registerItem("raw_wire",
            props -> new WireRollItem(false, props), new Item.Properties());
    public static final DeferredItem<WireRollItem> INSULATED_WIRE = ITEMS.registerItem("insulated_wire",
            props -> new WireRollItem(true, props), new Item.Properties());
    // Heavy Wire: insulated and thick, spans up to 32 blocks (for long power-pole runs).
    public static final DeferredItem<WireRollItem> HEAVY_WIRE = ITEMS.registerItem("heavy_wire",
            props -> new WireRollItem(true, true, props), new Item.Properties());

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
    // Molted Plumes: shed feathers from Stormwing nests; the Arc Serpent drops them too.
    // Storm Caller: calls a thunderstorm from clear skies for 1 Charged Spark Cell.
    public static final DeferredItem<StormCallerItem> STORM_CALLER = ITEMS.registerItem("storm_caller",
            StormCallerItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> MOLTED_PLUME = ITEMS.registerSimpleItem("molted_plume", new Item.Properties());
    // ---------- Fulgurite tools and sword (early; between diamond and netherite) ----------
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.FulguriteTools.Sword> FULGURITE_SWORD =
            ITEMS.registerItem("fulgurite_sword", com.sideflowgames.fulguritefusion.item.FulguriteTools.Sword::new,
                    new Item.Properties().attributes(net.minecraft.world.item.SwordItem.createAttributes(
                            com.sideflowgames.fulguritefusion.item.FulguriteTools.TIER, 3, -2.4f)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.FulguriteTools.Pickaxe> FULGURITE_PICKAXE =
            ITEMS.registerItem("fulgurite_pickaxe", com.sideflowgames.fulguritefusion.item.FulguriteTools.Pickaxe::new,
                    new Item.Properties().attributes(net.minecraft.world.item.DiggerItem.createAttributes(
                            com.sideflowgames.fulguritefusion.item.FulguriteTools.TIER, 1.0f, -2.8f)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.FulguriteTools.Axe> FULGURITE_AXE =
            ITEMS.registerItem("fulgurite_axe", com.sideflowgames.fulguritefusion.item.FulguriteTools.Axe::new,
                    new Item.Properties().attributes(net.minecraft.world.item.DiggerItem.createAttributes(
                            com.sideflowgames.fulguritefusion.item.FulguriteTools.TIER, 5.0f, -3.0f)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.FulguriteTools.Shovel> FULGURITE_SHOVEL =
            ITEMS.registerItem("fulgurite_shovel", com.sideflowgames.fulguritefusion.item.FulguriteTools.Shovel::new,
                    new Item.Properties().attributes(net.minecraft.world.item.DiggerItem.createAttributes(
                            com.sideflowgames.fulguritefusion.item.FulguriteTools.TIER, 1.5f, -3.0f)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.FulguriteTools.Hoe> FULGURITE_HOE =
            ITEMS.registerItem("fulgurite_hoe", com.sideflowgames.fulguritefusion.item.FulguriteTools.Hoe::new,
                    new Item.Properties().attributes(net.minecraft.world.item.DiggerItem.createAttributes(
                            com.sideflowgames.fulguritefusion.item.FulguriteTools.TIER, -3.5f, 0.0f)));
    // ---------- Stormforged armor (mid; made in the Storm Forge, runs on stored Fm) ----------
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedArmorItem> STORMFORGED_HELMET =
            ITEMS.registerItem("stormforged_helmet", p -> new com.sideflowgames.fulguritefusion.item.StormforgedArmorItem(
                    net.minecraft.world.item.ArmorItem.Type.HELMET, p), new Item.Properties().stacksTo(1).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedArmorItem> STORMFORGED_CHESTPLATE =
            ITEMS.registerItem("stormforged_chestplate", p -> new com.sideflowgames.fulguritefusion.item.StormforgedArmorItem(
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, p), new Item.Properties().stacksTo(1).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedArmorItem> STORMFORGED_LEGGINGS =
            ITEMS.registerItem("stormforged_leggings", p -> new com.sideflowgames.fulguritefusion.item.StormforgedArmorItem(
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, p), new Item.Properties().stacksTo(1).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedArmorItem> STORMFORGED_BOOTS =
            ITEMS.registerItem("stormforged_boots", p -> new com.sideflowgames.fulguritefusion.item.StormforgedArmorItem(
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, p), new Item.Properties().stacksTo(1).fireResistant());

    // ---------- Spears ----------
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.SpearItem> FULGURITE_SPEAR = ITEMS.registerItem("fulgurite_spear",
            com.sideflowgames.fulguritefusion.item.SpearItem::new, new Item.Properties().durability(1800)
                    .attributes(com.sideflowgames.fulguritefusion.item.SpearItem.attributes(com.sideflowgames.fulguritefusion.item.SpearItem.FULGURITE_MELEE, -2.9f)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormSpearItem> STORM_SPEAR = ITEMS.registerItem("storm_spear",
            com.sideflowgames.fulguritefusion.item.StormSpearItem::new, new Item.Properties().stacksTo(1).fireResistant()
                    .attributes(com.sideflowgames.fulguritefusion.item.SpearItem.attributes(com.sideflowgames.fulguritefusion.item.StormSpearItem.MELEE, -2.9f)));

    // ---------- Stormforged tools and Storm Blade (mid; forged from Fulgurite gear, run on Fm, never break) ----------
    private static Item.Properties stormforged() {
        return new Item.Properties().fireResistant()
                .component(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(false));
    }
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedTools.Blade> STORM_BLADE = ITEMS.registerItem("storm_blade",
            com.sideflowgames.fulguritefusion.item.StormforgedTools.Blade::new, stormforged().attributes(net.minecraft.world.item.SwordItem.createAttributes(
                    com.sideflowgames.fulguritefusion.item.StormforgedTools.TIER, 4, -2.4f)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedTools.Pickaxe> STORMFORGED_PICKAXE = ITEMS.registerItem("stormforged_pickaxe",
            com.sideflowgames.fulguritefusion.item.StormforgedTools.Pickaxe::new, stormforged().attributes(net.minecraft.world.item.DiggerItem.createAttributes(
                    com.sideflowgames.fulguritefusion.item.StormforgedTools.TIER, 1.0f, -2.8f)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedTools.Axe> STORMFORGED_AXE = ITEMS.registerItem("stormforged_axe",
            com.sideflowgames.fulguritefusion.item.StormforgedTools.Axe::new, stormforged().attributes(net.minecraft.world.item.DiggerItem.createAttributes(
                    com.sideflowgames.fulguritefusion.item.StormforgedTools.TIER, 5.0f, -3.0f)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedTools.Shovel> STORMFORGED_SHOVEL = ITEMS.registerItem("stormforged_shovel",
            com.sideflowgames.fulguritefusion.item.StormforgedTools.Shovel::new, stormforged().attributes(net.minecraft.world.item.DiggerItem.createAttributes(
                    com.sideflowgames.fulguritefusion.item.StormforgedTools.TIER, 1.5f, -3.0f)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedTools.Hoe> STORMFORGED_HOE = ITEMS.registerItem("stormforged_hoe",
            com.sideflowgames.fulguritefusion.item.StormforgedTools.Hoe::new, stormforged().attributes(net.minecraft.world.item.DiggerItem.createAttributes(
                    com.sideflowgames.fulguritefusion.item.StormforgedTools.TIER, -6.0f, 0.0f)));

    // Arc Magnet (early): toggled, no power.
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.ArcMagnetItem> ARC_MAGNET =
            ITEMS.registerItem("arc_magnet", com.sideflowgames.fulguritefusion.item.ArcMagnetItem::new,
                    new Item.Properties().stacksTo(1));

    // Molted Plume Bundle: 4 Molted Plumes tied with string. Goes into the Thunderbond Talisman.
    public static final DeferredItem<Item> MOLTED_PLUME_BUNDLE = ITEMS.registerSimpleItem("molted_plume_bundle", new Item.Properties());
    // Thunderbond Talisman: bonds a Stormwing to its owner so it can be ridden. Used up on bonding.
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.ThunderbondTalismanItem> THUNDERBOND_TALISMAN =
            ITEMS.registerItem("thunderbond_talisman", com.sideflowgames.fulguritefusion.item.ThunderbondTalismanItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    // Dropped (always) by the Arc Serpent.
    public static final DeferredItem<Item> ARC_CORE = ITEMS.registerSimpleItem("arc_core", new Item.Properties().rarity(Rarity.RARE));

    // Spawn eggs (creative tab).
    public static final DeferredItem<DeferredSpawnEggItem> LUMEN_SLUG_SPAWN_EGG = ITEMS.registerItem("lumen_slug_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntities.LUMEN_SLUG, 0x2E7D6B, 0xA8FFC4, props), new Item.Properties());
    public static final DeferredItem<DeferredSpawnEggItem> VOIDMOTH_SPAWN_EGG = ITEMS.registerItem("voidmoth_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntities.VOIDMOTH, 0x2B1B3A, 0xC8A8F0, props), new Item.Properties());
    public static final DeferredItem<DeferredSpawnEggItem> STORMWING_SPAWN_EGG = ITEMS.registerItem("stormwing_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntities.STORMWING, 0x5A6170, 0xBFE6FF, props), new Item.Properties());
    public static final DeferredItem<DeferredSpawnEggItem> ARC_SERPENT_SPAWN_EGG = ITEMS.registerItem("arc_serpent_spawn_egg",
            props -> new DeferredSpawnEggItem(ModEntities.ARC_SERPENT, 0x1B3A6B, 0xE6F7FF, props), new Item.Properties());

    // Processing machine products.
    public static final DeferredItem<Item> IRON_SHEET = ITEMS.registerSimpleItem("iron_sheet", new Item.Properties());
    public static final DeferredItem<Item> GOLD_SHEET = ITEMS.registerSimpleItem("gold_sheet", new Item.Properties());
    public static final DeferredItem<Item> COIL_SEGMENT = ITEMS.registerSimpleItem("coil_segment", new Item.Properties());
    public static final DeferredItem<Item> CRUSHED_IRON = ITEMS.registerSimpleItem("crushed_iron", new Item.Properties());
    public static final DeferredItem<Item> CRUSHED_COPPER = ITEMS.registerSimpleItem("crushed_copper", new Item.Properties());
    public static final DeferredItem<Item> CRUSHED_GOLD = ITEMS.registerSimpleItem("crushed_gold", new Item.Properties());

    // Fulgurite Fusion Guide: the in-game guide book.
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.GuideBookItem> GUIDE_BOOK = ITEMS.registerItem("guide_book",
            com.sideflowgames.fulguritefusion.item.GuideBookItem::new, new Item.Properties().stacksTo(1));
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
                        output.accept(GUIDE_BOOK.get());
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
                        output.accept(ModBlocks.CREATIVE_POWER_SOURCE_ITEM.get());
                        output.accept(COPPER_SHEET.get());
                        output.accept(RAW_WIRE.get());
                        output.accept(INSULATED_WIRE.get());
                        output.accept(HEAVY_WIRE.get());
                        output.accept(ModBlocks.MACHINE_CONNECTOR_ITEM.get());
                        output.accept(ModBlocks.FE_CONVERTER_ITEM.get());
                        output.accept(ModBlocks.EU_CONVERTER_ITEM.get());
                        output.accept(ModBlocks.FULGURITE_RING_ITEM.get());
                        output.accept(ModBlocks.COLLIDER_INPUT_ITEM.get());
                        output.accept(ModBlocks.COLLIDER_INJECTOR_ITEM.get());
                        output.accept(ModBlocks.MINI_COLLIDER_ITEM.get());
                        output.accept(ModBlocks.FUSION_COLLIDER_ITEM.get());
                        output.accept(ModBlocks.REINFORCED_FULGURITE_ITEM.get());
                        output.accept(ModBlocks.FULGURITE_BLAST_GLASS_ITEM.get());
                        output.accept(ModBlocks.BLAST_CHAMBER_FRAME_ITEM.get());
                        output.accept(ModBlocks.TRANSFER_PORTAL_ITEM.get());
                        output.accept(ModBlocks.RECEIVER_PORTAL_ITEM.get());
                        output.accept(ModBlocks.FUSION_INJECTOR_ITEM.get());
                        output.accept(ModBlocks.RING_INJECTION_PORT_ITEM.get());
                        output.accept(ModBlocks.REACTOR_CONSOLE_ITEM.get());
                        output.accept(ModBlocks.CONSOLE_CASING_ITEM.get());
                        output.accept(ModBlocks.CONSOLE_POWER_PORT_ITEM.get());
                        output.accept(ModBlocks.CONSOLE_DATA_PORT_ITEM.get());
                        output.accept(ModBlocks.CONSOLE_FUEL_INPUT_ITEM.get());
                        output.accept(ModBlocks.DATA_CABLE_ITEM.get());
                        output.accept(ModBlocks.STORM_COLLIDER_ITEM.get());
                        for (DeferredItem<Item> material : MATERIALS) output.accept(material.get());
                        output.accept(ModBlocks.SKY_COLLECTOR_ITEM.get());
                        output.accept(ModBlocks.NACRE_CLUSTER_ITEM.get());
                        output.accept(ModBlocks.FROZEN_SOUL_FIRE_ITEM.get());
                        output.accept(RESONANT_SHARD.get());
                        output.accept(PEARL_EMBER.get());
                        output.accept(LIVING_SPARK.get());
                        output.accept(PRIME_SPARK_FRAGMENT.get());
                        output.accept(ARC_CORE.get());
                        output.accept(MOLTED_PLUME_BUNDLE.get());
                        output.accept(THUNDERBOND_TALISMAN.get());
                        output.accept(FULGURITE_SWORD.get());
                        output.accept(FULGURITE_PICKAXE.get());
                        output.accept(FULGURITE_AXE.get());
                        output.accept(FULGURITE_SHOVEL.get());
                        output.accept(FULGURITE_HOE.get());
                        output.accept(ARC_MAGNET.get());
                        output.accept(FULGURITE_SPEAR.get());
                        for (var gear : java.util.List.of(STORM_BLADE, STORM_SPEAR, STORMFORGED_PICKAXE, STORMFORGED_AXE, STORMFORGED_SHOVEL, STORMFORGED_HOE)) {
                            ItemStack full = new ItemStack(gear.get());
                            com.sideflowgames.fulguritefusion.item.FmChargeable.set(full, com.sideflowgames.fulguritefusion.item.StormforgedTools.MAX_FM);
                            output.accept(full);
                        }
                        output.accept(ModBlocks.STORM_FORGE_ITEM.get());
                        output.accept(ModBlocks.FORGE_POWER_PORT_ITEM.get());
                        for (var armor : java.util.List.of(STORMFORGED_HELMET, STORMFORGED_CHESTPLATE, STORMFORGED_LEGGINGS, STORMFORGED_BOOTS)) {
                            ItemStack full = new ItemStack(armor.get());
                            com.sideflowgames.fulguritefusion.item.FmChargeable.set(full, com.sideflowgames.fulguritefusion.item.StormforgedArmorItem.MAX_FM);
                            output.accept(full);
                        }
                        output.accept(REFINED_FULGURITE.get());
                        output.accept(CHARGED_SPARK_CELL.get());
                        output.accept(OVERCHARGED_SPARK_CELL.get());
                        output.accept(PRIME_SPARK.get());
                        output.accept(LUMEN_SLUG_SPAWN_EGG.get());
                        output.accept(VOIDMOTH_SPAWN_EGG.get());
                        output.accept(ModBlocks.CRUDE_ARC_CASING_ITEM.get());
                        output.accept(ModBlocks.TEMPERED_ARC_CASING_ITEM.get());
                        output.accept(ModBlocks.CHARGED_ARC_CASING_ITEM.get());
                        output.accept(ModBlocks.PRIME_ARC_CASING_ITEM.get());
                        output.accept(ModBlocks.PRESS_ITEM.get());
                        output.accept(ModBlocks.ARC_FURNACE_ITEM.get());
                        output.accept(ModBlocks.WIRE_EXTRUDER_ITEM.get());
                        output.accept(ModBlocks.KILN_ITEM.get());
                        output.accept(ModBlocks.CRUSHER_ITEM.get());
                        output.accept(IRON_SHEET.get());
                        output.accept(GOLD_SHEET.get());
                        output.accept(COIL_SEGMENT.get());
                        output.accept(CRUSHED_IRON.get());
                        output.accept(CRUSHED_COPPER.get());
                        output.accept(CRUSHED_GOLD.get());
                        output.accept(STORMWING_SPAWN_EGG.get());
                        output.accept(ARC_SERPENT_SPAWN_EGG.get());
                        output.accept(MOLTED_PLUME.get());
                        output.accept(STORM_CALLER.get());
                        output.accept(ModBlocks.STORMWING_NEST_ITEM.get());
                        output.accept(ModBlocks.STORMWING_NEST_RIM_ITEM.get());
                        output.accept(ModBlocks.STORMWING_EGG_ITEM.get());
                    })
                    .build());

    private ModItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
