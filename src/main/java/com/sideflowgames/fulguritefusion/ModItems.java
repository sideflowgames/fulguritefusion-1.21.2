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
            () -> new Item.Properties().stacksTo(16));

    // Charged, crystal-blue vial. The enchantment glint gives it the sparkle. Stacks to 16.
    // No crafting recipe: it will be filled by the Fulgurite Charger.
    public static final DeferredItem<Item> SPARK_CELL = ITEMS.registerSimpleItem("spark_cell",
            () -> new Item.Properties().stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    // Tempest Crystal: a rare gem mined only in the Stormreach, kept for end-game recipes.
    public static final DeferredItem<Item> TEMPEST_CRYSTAL = ITEMS.registerSimpleItem("tempest_crystal",
            () -> new Item.Properties().rarity(Rarity.RARE));
    // Developer-only (the player named Dev, development build): travel to the Stormreach and back. Not in any
    // creative tab; get one with /stormreach rift.
    // RELEASE 2.0: taken out for the CurseForge jar. Put it back in the next revision for testing (also the
    // "/stormreach rift" command in StormreachTravel).
    // public static final DeferredItem<com.sideflowgames.fulguritefusion.stormreach.StormreachRiftItem> STORMREACH_RIFT = ITEMS.registerItem("stormreach_rift",
    //         com.sideflowgames.fulguritefusion.stormreach.StormreachRiftItem::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    // Dropped when mining Raw Fulgurite (4-8 per block, boosted by Fortune).
    public static final DeferredItem<Item> FULGURITE_SHARD = ITEMS.registerSimpleItem("fulgurite_shard",
            () -> new Item.Properties());

    // Fulgurite: 9 shards in a crafting grid make 2.
    public static final DeferredItem<Item> FULGURITE = ITEMS.registerSimpleItem("fulgurite",
            () -> new Item.Properties());

    // Copper Sheet: used for wire. Made in the Press from copper ingots.
    public static final DeferredItem<Item> COPPER_SHEET = ITEMS.registerSimpleItem("copper_sheet",
            () -> new Item.Properties());

    // Wire Rolls: string hanging wires between Machine Connectors.
    public static final DeferredItem<WireRollItem> RAW_WIRE = ITEMS.registerItem("raw_wire",
            props -> new WireRollItem(com.sideflowgames.fulguritefusion.power.WireTier.RAW, props), () -> new Item.Properties());
    public static final DeferredItem<WireRollItem> INSULATED_WIRE = ITEMS.registerItem("insulated_wire",
            props -> new WireRollItem(com.sideflowgames.fulguritefusion.power.WireTier.INSULATED, props), () -> new Item.Properties());
    // Reinforced gauge: 512 Fm/t, spans 32 blocks. Raw: Wire Extruder, 2 Raw Wire + an Iron Sheet.
    public static final DeferredItem<WireRollItem> RAW_REINFORCED_WIRE = ITEMS.registerItem("raw_reinforced_wire",
            props -> new WireRollItem(com.sideflowgames.fulguritefusion.power.WireTier.RAW_REINFORCED, props), () -> new Item.Properties());
    public static final DeferredItem<WireRollItem> REINFORCED_WIRE = ITEMS.registerItem("reinforced_wire",
            props -> new WireRollItem(com.sideflowgames.fulguritefusion.power.WireTier.REINFORCED, props), () -> new Item.Properties());
    // Heavy gauge: 1024 Fm/t, spans 64 blocks and hangs taut (for long power-pole runs).
    public static final DeferredItem<WireRollItem> RAW_HEAVY_WIRE = ITEMS.registerItem("raw_heavy_wire",
            props -> new WireRollItem(com.sideflowgames.fulguritefusion.power.WireTier.RAW_HEAVY, props), () -> new Item.Properties());
    public static final DeferredItem<WireRollItem> HEAVY_WIRE = ITEMS.registerItem("heavy_wire",
            props -> new WireRollItem(com.sideflowgames.fulguritefusion.power.WireTier.HEAVY, props), () -> new Item.Properties());

    // ---------- Rubber ----------
    // Latex Bucket: a Tree Tap on a Rubber Log drips into a bucket on the ground below it; break the bucket when full.
    public static final DeferredItem<net.minecraft.world.item.BucketItem> LATEX_BUCKET = ITEMS.registerItem("latex_bucket",
            p -> new net.minecraft.world.item.BucketItem(ModFluids.LATEX.get(), p),
            () -> new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET));
    // Dried Rubber: Latex Bucket in the Arc Furnace (gives the bucket back).
    public static final DeferredItem<Item> DRIED_RUBBER = ITEMS.registerSimpleItem("dried_rubber", () -> new Item.Properties());
    // Malleable Rubber: Dried Rubber kneaded in the Banbury Mixer. Insulates raw wire in the Wire Extruder.
    public static final DeferredItem<Item> MALLEABLE_RUBBER = ITEMS.registerSimpleItem("malleable_rubber", () -> new Item.Properties());

    public static Item wireItem(com.sideflowgames.fulguritefusion.power.WireTier tier) {
        return switch (tier) {
            case RAW -> RAW_WIRE.get();
            case INSULATED -> INSULATED_WIRE.get();
            case RAW_REINFORCED -> RAW_REINFORCED_WIRE.get();
            case REINFORCED -> REINFORCED_WIRE.get();
            case RAW_HEAVY -> RAW_HEAVY_WIRE.get();
            case HEAVY -> HEAVY_WIRE.get();
        };
    }

    // ---------- Storm Collider materials and products ----------

    // The 12 exotic materials, in design-doc order (index = material number - 1).
    // How each is gathered in survival comes later; for now they are in the creative tab.
    public static final DeferredItem<Item> AURORA_DUST = ITEMS.registerSimpleItem("aurora_dust", () -> new Item.Properties());
    public static final DeferredItem<Item> SUNMOTE = ITEMS.registerSimpleItem("sunmote", () -> new Item.Properties());
    public static final DeferredItem<Item> ABYSSAL_NACRE = ITEMS.registerSimpleItem("abyssal_nacre", () -> new Item.Properties());
    public static final DeferredItem<Item> SOUL_FROST = ITEMS.registerSimpleItem("soul_frost", () -> new Item.Properties());
    public static final DeferredItem<Item> ECHO_RESIN = ITEMS.registerSimpleItem("echo_resin", () -> new Item.Properties());
    public static final DeferredItem<Item> GALE_CORE = ITEMS.registerSimpleItem("gale_core", () -> new Item.Properties());
    public static final DeferredItem<Item> STORMWING_PLUME = ITEMS.registerSimpleItem("stormwing_plume", () -> new Item.Properties());
    public static final DeferredItem<Item> NULL_SCALE = ITEMS.registerSimpleItem("null_scale", () -> new Item.Properties());
    public static final DeferredItem<Item> LUMEN_GEL = ITEMS.registerSimpleItem("lumen_gel", () -> new Item.Properties());
    public static final DeferredItem<Item> CINDER_HEART = ITEMS.registerSimpleItem("cinder_heart", () -> new Item.Properties());
    public static final DeferredItem<Item> WITHER_ASH = ITEMS.registerSimpleItem("wither_ash", () -> new Item.Properties());
    public static final DeferredItem<Item> STRUCK_SPORE = ITEMS.registerSimpleItem("struck_spore", () -> new Item.Properties());

    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item>[] MATERIALS = new DeferredItem[]{AURORA_DUST, SUNMOTE, ABYSSAL_NACRE, SOUL_FROST, ECHO_RESIN, GALE_CORE, STORMWING_PLUME, NULL_SCALE, LUMEN_GEL, CINDER_HEART, WITHER_ASH, STRUCK_SPORE};

    // Pairing byproducts.
    public static final DeferredItem<Item> RESONANT_SHARD = ITEMS.registerSimpleItem("resonant_shard", () -> new Item.Properties());
    public static final DeferredItem<Item> PEARL_EMBER = ITEMS.registerSimpleItem("pearl_ember", () -> new Item.Properties());
    public static final DeferredItem<Item> LIVING_SPARK = ITEMS.registerSimpleItem("living_spark", () -> new Item.Properties());
    public static final DeferredItem<Item> PRIME_SPARK_FRAGMENT = ITEMS.registerSimpleItem("prime_spark_fragment",
            () -> new Item.Properties().rarity(Rarity.RARE));

    // Storm Collider crafting products.
    public static final DeferredItem<Item> PRIME_SPARK = ITEMS.registerSimpleItem("prime_spark",
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<Item> REFINED_FULGURITE = ITEMS.registerSimpleItem("refined_fulgurite", () -> new Item.Properties());
    public static final DeferredItem<Item> CHARGED_SPARK_CELL = ITEMS.registerSimpleItem("charged_spark_cell",
            () -> new Item.Properties().stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<Item> OVERCHARGED_SPARK_CELL = ITEMS.registerSimpleItem("overcharged_spark_cell",
            () -> new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    // Molted Plumes: shed feathers from Stormwing nests; the Arc Serpent drops them too.
    // Storm Caller: calls a thunderstorm from clear skies for 1 Charged Spark Cell.
    public static final DeferredItem<StormCallerItem> STORM_CALLER = ITEMS.registerItem("storm_caller",
            StormCallerItem::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    // Tempest Staff: calls lightning where you aim, for stored Fm. Forged at the Storm Forge around a Tempest Crystal.
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.TempestStaffItem> TEMPEST_STAFF = ITEMS.registerItem("tempest_staff",
            com.sideflowgames.fulguritefusion.item.TempestStaffItem::new, () -> new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE));
    public static final DeferredItem<Item> MOLTED_PLUME = ITEMS.registerSimpleItem("molted_plume", () -> new Item.Properties());
    // ---------- Fulgurite tools and sword (early; between diamond and netherite) ----------
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.FulguriteTools.Sword> FULGURITE_SWORD =
            ITEMS.registerItem("fulgurite_sword", com.sideflowgames.fulguritefusion.item.FulguriteTools.Sword::new,
                    () -> new Item.Properties().sword(com.sideflowgames.fulguritefusion.item.FulguriteTools.TIER, 3, -2.4f));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.FulguriteTools.Pickaxe> FULGURITE_PICKAXE =
            ITEMS.registerItem("fulgurite_pickaxe", com.sideflowgames.fulguritefusion.item.FulguriteTools.Pickaxe::new,
                    () -> new Item.Properties().pickaxe(com.sideflowgames.fulguritefusion.item.FulguriteTools.TIER, 1.0f, -2.8f));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.FulguriteTools.Axe> FULGURITE_AXE =
            ITEMS.registerItem("fulgurite_axe", com.sideflowgames.fulguritefusion.item.FulguriteTools.Axe::new,
                    () -> new Item.Properties());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.FulguriteTools.Shovel> FULGURITE_SHOVEL =
            ITEMS.registerItem("fulgurite_shovel", com.sideflowgames.fulguritefusion.item.FulguriteTools.Shovel::new,
                    () -> new Item.Properties());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.FulguriteTools.Hoe> FULGURITE_HOE =
            ITEMS.registerItem("fulgurite_hoe", com.sideflowgames.fulguritefusion.item.FulguriteTools.Hoe::new,
                    () -> new Item.Properties());
    // ---------- Stormforged armor (mid; made in the Storm Forge, runs on stored Fm) ----------
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedArmorItem> STORMFORGED_HELMET =
            ITEMS.registerItem("stormforged_helmet", p -> new com.sideflowgames.fulguritefusion.item.StormforgedArmorItem(
                    net.minecraft.world.item.equipment.ArmorType.HELMET, p), () -> new Item.Properties().stacksTo(1).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedArmorItem> STORMFORGED_CHESTPLATE =
            ITEMS.registerItem("stormforged_chestplate", p -> new com.sideflowgames.fulguritefusion.item.StormforgedArmorItem(
                    net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, p), () -> new Item.Properties().stacksTo(1).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedArmorItem> STORMFORGED_LEGGINGS =
            ITEMS.registerItem("stormforged_leggings", p -> new com.sideflowgames.fulguritefusion.item.StormforgedArmorItem(
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS, p), () -> new Item.Properties().stacksTo(1).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedArmorItem> STORMFORGED_BOOTS =
            ITEMS.registerItem("stormforged_boots", p -> new com.sideflowgames.fulguritefusion.item.StormforgedArmorItem(
                    net.minecraft.world.item.equipment.ArmorType.BOOTS, p), () -> new Item.Properties().stacksTo(1).fireResistant());

    // ---------- Spears ----------
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.SpearItem> FULGURITE_SPEAR = ITEMS.registerItem("fulgurite_spear",
            com.sideflowgames.fulguritefusion.item.SpearItem::new, () -> new Item.Properties().durability(1800).enchantable(13)
                    .repairable(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "fulgurite_tool_materials")))
                    .attributes(com.sideflowgames.fulguritefusion.item.SpearItem.attributes(com.sideflowgames.fulguritefusion.item.SpearItem.FULGURITE_MELEE, -2.9f)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormSpearItem> STORM_SPEAR = ITEMS.registerItem("storm_spear",
            com.sideflowgames.fulguritefusion.item.StormSpearItem::new, () -> new Item.Properties().stacksTo(1).fireResistant().enchantable(18)
                    .attributes(com.sideflowgames.fulguritefusion.item.SpearItem.attributes(com.sideflowgames.fulguritefusion.item.StormSpearItem.MELEE, -2.9f)));

    // ---------- Stormforged tools and Storm Blade (mid; forged from Fulgurite gear, run on Fm, never break) ----------
    private static Item.Properties stormforged() {
        return new Item.Properties().fireResistant()
                .component(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);
    }
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedTools.Blade> STORM_BLADE = ITEMS.registerItem("storm_blade",
            com.sideflowgames.fulguritefusion.item.StormforgedTools.Blade::new, () -> stormforged().sword(com.sideflowgames.fulguritefusion.item.StormforgedTools.TIER, 4, -2.4f));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedTools.Pickaxe> STORMFORGED_PICKAXE = ITEMS.registerItem("stormforged_pickaxe",
            com.sideflowgames.fulguritefusion.item.StormforgedTools.Pickaxe::new, () -> stormforged().pickaxe(com.sideflowgames.fulguritefusion.item.StormforgedTools.TIER, 1.0f, -2.8f));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedTools.Axe> STORMFORGED_AXE = ITEMS.registerItem("stormforged_axe",
            com.sideflowgames.fulguritefusion.item.StormforgedTools.Axe::new, () -> stormforged());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedTools.Shovel> STORMFORGED_SHOVEL = ITEMS.registerItem("stormforged_shovel",
            com.sideflowgames.fulguritefusion.item.StormforgedTools.Shovel::new, () -> stormforged());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormforgedTools.Hoe> STORMFORGED_HOE = ITEMS.registerItem("stormforged_hoe",
            com.sideflowgames.fulguritefusion.item.StormforgedTools.Hoe::new, () -> stormforged());

    // ---------- Prime gear (end; forged from Stormforged gear + Prime Spark Fragment + netherite ingot) ----------
    private static Item.Properties prime() {
        return stormforged().rarity(Rarity.EPIC);
    }
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.PrimeTools.Blade> PRIME_BLADE = ITEMS.registerItem("prime_blade",
            com.sideflowgames.fulguritefusion.item.PrimeTools.Blade::new, () -> prime().sword(com.sideflowgames.fulguritefusion.item.PrimeTools.TIER, 10, -2.4f)); // 1 + 8 (tier) + 10 = 19
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.PrimeTools.Pickaxe> PRIME_PICKAXE = ITEMS.registerItem("prime_pickaxe",
            com.sideflowgames.fulguritefusion.item.PrimeTools.Pickaxe::new, () -> prime().pickaxe(com.sideflowgames.fulguritefusion.item.PrimeTools.TIER, 1.0f, -2.8f));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.PrimeTools.Axe> PRIME_AXE = ITEMS.registerItem("prime_axe",
            com.sideflowgames.fulguritefusion.item.PrimeTools.Axe::new, () -> prime());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.PrimeTools.Shovel> PRIME_SHOVEL = ITEMS.registerItem("prime_shovel",
            com.sideflowgames.fulguritefusion.item.PrimeTools.Shovel::new, () -> prime());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.PrimeTools.Hoe> PRIME_HOE = ITEMS.registerItem("prime_hoe",
            com.sideflowgames.fulguritefusion.item.PrimeTools.Hoe::new, () -> prime());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.PrimeSpearItem> PRIME_SPEAR = ITEMS.registerItem("prime_spear",
            com.sideflowgames.fulguritefusion.item.PrimeSpearItem::new, () -> new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC).enchantable(22)
                    .attributes(com.sideflowgames.fulguritefusion.item.SpearItem.attributes(com.sideflowgames.fulguritefusion.item.PrimeSpearItem.MELEE, -2.9f)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.PrimeArmorItem> PRIME_HELMET = primeArmor("prime_helmet", net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.PrimeArmorItem> PRIME_CHESTPLATE = primeArmor("prime_chestplate", net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.PrimeArmorItem> PRIME_LEGGINGS = primeArmor("prime_leggings", net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.PrimeArmorItem> PRIME_BOOTS = primeArmor("prime_boots", net.minecraft.world.item.equipment.ArmorType.BOOTS);

    // Titan armor: dropped by the Storm Titan.
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.TitanArmorItem> TITAN_HELMET = titanArmor("titan_helmet", net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.TitanArmorItem> TITAN_CHESTPLATE = titanArmor("titan_chestplate", net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.TitanArmorItem> TITAN_LEGGINGS = titanArmor("titan_leggings", net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.TitanArmorItem> TITAN_BOOTS = titanArmor("titan_boots", net.minecraft.world.item.equipment.ArmorType.BOOTS);

    // Titan Prime: a Prime piece and the same Titan piece fused in the Titan Forge.
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.TitanPrimeArmorItem> TITAN_PRIME_HELMET = titanPrimeArmor("titan_prime_helmet", net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.TitanPrimeArmorItem> TITAN_PRIME_CHESTPLATE = titanPrimeArmor("titan_prime_chestplate", net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.TitanPrimeArmorItem> TITAN_PRIME_LEGGINGS = titanPrimeArmor("titan_prime_leggings", net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.TitanPrimeArmorItem> TITAN_PRIME_BOOTS = titanPrimeArmor("titan_prime_boots", net.minecraft.world.item.equipment.ArmorType.BOOTS);

    private static DeferredItem<com.sideflowgames.fulguritefusion.item.TitanPrimeArmorItem> titanPrimeArmor(String name, net.minecraft.world.item.equipment.ArmorType type) {
        return ITEMS.registerItem(name, p -> new com.sideflowgames.fulguritefusion.item.TitanPrimeArmorItem(type, p),
                () -> new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    private static DeferredItem<com.sideflowgames.fulguritefusion.item.TitanArmorItem> titanArmor(String name, net.minecraft.world.item.equipment.ArmorType type) {
        return ITEMS.registerItem(name, p -> new com.sideflowgames.fulguritefusion.item.TitanArmorItem(type, p),
                () -> new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    private static DeferredItem<com.sideflowgames.fulguritefusion.item.PrimeArmorItem> primeArmor(String name, net.minecraft.world.item.equipment.ArmorType type) {
        return ITEMS.registerItem(name, p -> new com.sideflowgames.fulguritefusion.item.PrimeArmorItem(type, p),
                () -> new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    // Arc Magnet (early): toggled, no power.
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.ArcMagnetItem> ARC_MAGNET =
            ITEMS.registerItem("arc_magnet", com.sideflowgames.fulguritefusion.item.ArcMagnetItem::new,
                    () -> new Item.Properties().stacksTo(1));

    // Backpack upgrades (see backpack/UpgradeKind). Stormforged ones are made in the Storm Forge.
    private static DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> upgrade(
            com.sideflowgames.fulguritefusion.backpack.UpgradeKind kind, boolean storm) {
        return ITEMS.registerItem((storm ? "storm_" : "") + kind.id + "_upgrade",
                p -> new com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem(kind, storm, p),
                () -> storm ? new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant() : new Item.Properties().stacksTo(1));
    }
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> PICKUP_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.PICKUP, false);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> MAGNET_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.MAGNET, false);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> VOID_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.VOID, false);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> COMPACTING_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.COMPACTING, false);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> STORM_PICKUP_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.PICKUP, true);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> STORM_MAGNET_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.MAGNET, true);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> STORM_VOID_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.VOID, true);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> STORM_COMPACTING_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.COMPACTING, true);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> FEEDING_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.FEEDING, false);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> STORM_FEEDING_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.FEEDING, true);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> REFILL_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.REFILL, false);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> STORM_REFILL_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.REFILL, true);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> RESTOCK_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.RESTOCK, false);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> STORM_RESTOCK_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.RESTOCK, true);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> DEPOSIT_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.DEPOSIT, false);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> STORM_DEPOSIT_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.DEPOSIT, true);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> TOOL_SWAPPER_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.TOOL_SWAPPER, false);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem> STORM_TOOL_SWAPPER_UPGRADE = upgrade(com.sideflowgames.fulguritefusion.backpack.UpgradeKind.TOOL_SWAPPER, true);

    // Molted Plume Bundle: 4 Molted Plumes tied with string. Goes into the Thunderbond Talisman.
    public static final DeferredItem<Item> MOLTED_PLUME_BUNDLE = ITEMS.registerSimpleItem("molted_plume_bundle", () -> new Item.Properties());
    // Thunderbond Talisman: bonds a Stormwing to its owner so it can be ridden. Used up on bonding.
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.ThunderbondTalismanItem> THUNDERBOND_TALISMAN =
            ITEMS.registerItem("thunderbond_talisman", com.sideflowgames.fulguritefusion.item.ThunderbondTalismanItem::new,
                    () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    // Dropped (always) by the Arc Serpent.
    public static final DeferredItem<Item> ARC_CORE = ITEMS.registerSimpleItem("arc_core", () -> new Item.Properties().rarity(Rarity.RARE));

    // Spawn eggs (creative tab).
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> LUMEN_SLUG_SPAWN_EGG = ITEMS.registerItem("lumen_slug_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.LUMEN_SLUG.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> VOIDMOTH_SPAWN_EGG = ITEMS.registerItem("voidmoth_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.VOIDMOTH.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> STORMWING_SPAWN_EGG = ITEMS.registerItem("stormwing_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.STORMWING.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> ARC_SERPENT_SPAWN_EGG = ITEMS.registerItem("arc_serpent_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.ARC_SERPENT.get()));

    // Mini-boss key pieces: always dropped. Combined at the Storm Forge into the Storm Sigil.
    public static final DeferredItem<Item> STORM_EYE = ITEMS.registerSimpleItem("storm_eye", () -> new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> MOLTEN_HEART = ITEMS.registerSimpleItem("molten_heart", () -> new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<Item> VOID_SHARD = ITEMS.registerSimpleItem("void_shard", () -> new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
    // Trinkets (equipment panel): three from the mini-bosses, three crafted, each with an Empowered version.
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> CLOUDSTEP_CHARM = ITEMS.registerItem("cloudstep_charm",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.CLOUDSTEP_CHARM, false, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> EMPOWERED_CLOUDSTEP_CHARM = ITEMS.registerItem("empowered_cloudstep_charm",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.CLOUDSTEP_CHARM, true, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> CONDUIT_HEART = ITEMS.registerItem("conduit_heart",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.CONDUIT_HEART, false, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> EMPOWERED_CONDUIT_HEART = ITEMS.registerItem("empowered_conduit_heart",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.CONDUIT_HEART, true, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> VOID_ARC_BAND = ITEMS.registerItem("void_arc_band",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.VOID_ARC_BAND, false, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> EMPOWERED_VOID_ARC_BAND = ITEMS.registerItem("empowered_void_arc_band",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.VOID_ARC_BAND, true, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> GROUNDING_CHARM = ITEMS.registerItem("grounding_charm",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.GROUNDING_CHARM, false, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> EMPOWERED_GROUNDING_CHARM = ITEMS.registerItem("empowered_grounding_charm",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.GROUNDING_CHARM, true, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> CINDER_WARD = ITEMS.registerItem("cinder_ward",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.CINDER_WARD, false, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> EMPOWERED_CINDER_WARD = ITEMS.registerItem("empowered_cinder_ward",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.CINDER_WARD, true, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> SEERS_RING = ITEMS.registerItem("seers_ring",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.SEERS_RING, false, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<com.sideflowgames.fulguritefusion.trinket.TrinketItem> EMPOWERED_SEERS_RING = ITEMS.registerItem("empowered_seers_ring",
            p -> new com.sideflowgames.fulguritefusion.trinket.TrinketItem(com.sideflowgames.fulguritefusion.trinket.TrinketKind.SEERS_RING, true, p), () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    // Stormreach (and Overworld) ore materials.
    public static final DeferredItem<Item> RAW_MAGNETITE = ITEMS.registerSimpleItem("raw_magnetite", () -> new Item.Properties());
    public static final DeferredItem<Item> MAGNETITE_INGOT = ITEMS.registerSimpleItem("magnetite_ingot", () -> new Item.Properties());
    public static final DeferredItem<Item> RAW_ARGENTITE = ITEMS.registerSimpleItem("raw_argentite", () -> new Item.Properties());
    public static final DeferredItem<Item> SILVER_INGOT = ITEMS.registerSimpleItem("silver_ingot", () -> new Item.Properties());
    public static final DeferredItem<Item> CINDERITE = ITEMS.registerSimpleItem("cinderite", () -> new Item.Properties());
    public static final DeferredItem<Item> PRISMITE_SHARD = ITEMS.registerSimpleItem("prismite_shard", () -> new Item.Properties());
    public static final DeferredItem<Item> STORMQUARTZ = ITEMS.registerSimpleItem("stormquartz", () -> new Item.Properties());
    public static final DeferredItem<Item> RAW_AURORITE = ITEMS.registerSimpleItem("raw_aurorite", () -> new Item.Properties());
    public static final DeferredItem<Item> AURORITE_INGOT = ITEMS.registerSimpleItem("aurorite_ingot", () -> new Item.Properties());
    public static final DeferredItem<Item> FOSSIL_AMBER = ITEMS.registerSimpleItem("fossil_amber", () -> new Item.Properties());
    // Storm Sigil: forged from the three key pieces, and dropped by the Storm Titan. Opens the Titan Trials and
    // wakes the Titan at its altar.
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.StormSigilItem> STORM_SIGIL = ITEMS.registerItem("storm_sigil",
            com.sideflowgames.fulguritefusion.item.StormSigilItem::new, () -> new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant());
    // Rift Core: given by the Storm Titan when it falls; the heart of a Rift Keystone.
    public static final DeferredItem<Item> RIFT_CORE = ITEMS.registerSimpleItem("rift_core", () -> new Item.Properties().rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> STORM_TITAN_SPAWN_EGG = ITEMS.registerItem("storm_titan_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.STORM_TITAN.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> STORMHOUND_SPAWN_EGG = ITEMS.registerItem("stormhound_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.STORMHOUND.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> VOLTCAP_CRAWLER_SPAWN_EGG = ITEMS.registerItem("voltcap_crawler_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.VOLTCAP_CRAWLER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> THUNDERHEAD_SPAWN_EGG = ITEMS.registerItem("thunderhead_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.THUNDERHEAD.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> MAGMA_CONDUIT_SPAWN_EGG = ITEMS.registerItem("magma_conduit_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.MAGMA_CONDUIT.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> VOID_ARC_SPAWN_EGG = ITEMS.registerItem("void_arc_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.VOID_ARC.get()));

    // ---------- Stormreach creature drops ----------
    public static final DeferredItem<Item> SPARKHORN_SHANK = ITEMS.registerSimpleItem("sparkhorn_shank",
            () -> new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final DeferredItem<Item> COOKED_SPARKHORN_SHANK = ITEMS.registerSimpleItem("cooked_sparkhorn_shank",
            () -> new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build()));
    public static final DeferredItem<Item> HORN_SHARD = ITEMS.registerSimpleItem("horn_shard", () -> new Item.Properties());
    public static final DeferredItem<Item> STATIC_GEL = ITEMS.registerSimpleItem("static_gel", () -> new Item.Properties());
    public static final DeferredItem<Item> ION_SCALE = ITEMS.registerSimpleItem("ion_scale", () -> new Item.Properties());
    public static final DeferredItem<Item> VOLTSHELL = ITEMS.registerSimpleItem("voltshell", () -> new Item.Properties());
    public static final DeferredItem<Item> GLASS_CLAW = ITEMS.registerSimpleItem("glass_claw", () -> new Item.Properties());
    public static final DeferredItem<Item> ARCWURM_HIDE = ITEMS.registerSimpleItem("arcwurm_hide", () -> new Item.Properties());
    public static final DeferredItem<Item> WRAITH_ESSENCE = ITEMS.registerSimpleItem("wraith_essence", () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> SPARKHORN_SPAWN_EGG = ITEMS.registerItem("sparkhorn_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.SPARKHORN.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> PUFFLING_SPAWN_EGG = ITEMS.registerItem("puffling_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.PUFFLING.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> STATIC_JELLY_SPAWN_EGG = ITEMS.registerItem("static_jelly_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.STATIC_JELLY.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> ION_EEL_SPAWN_EGG = ITEMS.registerItem("ion_eel_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.ION_EEL.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> RUBBERBACK_SLOTH_SPAWN_EGG = ITEMS.registerItem("rubberback_sloth_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.RUBBERBACK_SLOTH.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> GLIMMERWING_SPAWN_EGG = ITEMS.registerItem("glimmerwing_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.GLIMMERWING.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> VOLTCAP_SNAIL_SPAWN_EGG = ITEMS.registerItem("voltcap_snail_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.VOLTCAP_SNAIL.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> GLASSCRAB_SPAWN_EGG = ITEMS.registerItem("glasscrab_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.GLASSCRAB.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> ARCWURM_SPAWN_EGG = ITEMS.registerItem("arcwurm_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.ARCWURM.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> CRYSTAL_STALKER_SPAWN_EGG = ITEMS.registerItem("crystal_stalker_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.CRYSTAL_STALKER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> STATIC_WRAITH_SPAWN_EGG = ITEMS.registerItem("static_wraith_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.STATIC_WRAITH.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> SHOCKROACH_SPAWN_EGG = ITEMS.registerItem("shockroach_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.SHOCKROACH.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> ARC_REAVER_SPAWN_EGG = ITEMS.registerItem("arc_reaver_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.ARC_REAVER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> THUNDERHULK_SPAWN_EGG = ITEMS.registerItem("thunderhulk_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.THUNDERHULK.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> STORMCALLER_SHADE_SPAWN_EGG = ITEMS.registerItem("stormcaller_shade_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.STORMCALLER_SHADE.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> STATIC_LEECH_SPAWN_EGG = ITEMS.registerItem("static_leech_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.STATIC_LEECH.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> ION_SWARM_SPAWN_EGG = ITEMS.registerItem("ion_swarm_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.ION_SWARM.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> VOLTSPITTER_SPAWN_EGG = ITEMS.registerItem("voltspitter_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.VOLTSPITTER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> TEMPEST_RAPTOR_SPAWN_EGG = ITEMS.registerItem("tempest_raptor_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.TEMPEST_RAPTOR.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> GLOOMARC_SPIDER_SPAWN_EGG = ITEMS.registerItem("gloomarc_spider_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.GLOOMARC_SPIDER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> PEAK_COLOSSUS_SPAWN_EGG = ITEMS.registerItem("peak_colossus_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.PEAK_COLOSSUS.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> CINDER_BEHEMOTH_SPAWN_EGG = ITEMS.registerItem("cinder_behemoth_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.CINDER_BEHEMOTH.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> AURORA_WRAITH_SPAWN_EGG = ITEMS.registerItem("aurora_wraith_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.AURORA_WRAITH.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> PRISM_SENTINEL_SPAWN_EGG = ITEMS.registerItem("prism_sentinel_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, () -> new Item.Properties().spawnEgg(ModEntities.PRISM_SENTINEL.get()));
    public static final DeferredItem<Item> REAVER_BLADE_SHARD = ITEMS.registerSimpleItem("reaver_blade_shard", () -> new Item.Properties());
    public static final DeferredItem<Item> THUNDERHULK_CORE = ITEMS.registerSimpleItem("thunderhulk_core", () -> new Item.Properties());
    public static final DeferredItem<Item> SHADE_VEIL = ITEMS.registerSimpleItem("shade_veil", () -> new Item.Properties());
    public static final DeferredItem<Item> LEECH_FANG = ITEMS.registerSimpleItem("leech_fang", () -> new Item.Properties());
    public static final DeferredItem<Item> ION_MOTE = ITEMS.registerSimpleItem("ion_mote", () -> new Item.Properties());
    public static final DeferredItem<Item> VOLT_GLAND = ITEMS.registerSimpleItem("volt_gland", () -> new Item.Properties());
    public static final DeferredItem<Item> TEMPEST_FEATHER = ITEMS.registerSimpleItem("tempest_feather", () -> new Item.Properties());
    public static final DeferredItem<Item> GLOOMARC_SILK = ITEMS.registerSimpleItem("gloomarc_silk", () -> new Item.Properties());
    public static final DeferredItem<Item> COLOSSUS_HEART = ITEMS.registerSimpleItem("colossus_heart", () -> new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> BEHEMOTH_EMBER = ITEMS.registerSimpleItem("behemoth_ember", () -> new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> AURORA_SHROUD = ITEMS.registerSimpleItem("aurora_shroud", () -> new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> SENTINEL_LENS = ITEMS.registerSimpleItem("sentinel_lens", () -> new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> CHARGED_ORB = ITEMS.registerSimpleItem("charged_orb", () -> new Item.Properties());

    // Processing machine products.
    public static final DeferredItem<Item> IRON_SHEET = ITEMS.registerSimpleItem("iron_sheet", () -> new Item.Properties());
    public static final DeferredItem<Item> GOLD_SHEET = ITEMS.registerSimpleItem("gold_sheet", () -> new Item.Properties());
    // Pressed from Silver and Magnetite ingots.
    public static final DeferredItem<Item> SILVER_SHEET = ITEMS.registerSimpleItem("silver_sheet", () -> new Item.Properties());
    public static final DeferredItem<Item> MAGNETITE_PLATE = ITEMS.registerSimpleItem("magnetite_plate", () -> new Item.Properties());
    public static final DeferredItem<Item> COIL_SEGMENT = ITEMS.registerSimpleItem("coil_segment", () -> new Item.Properties());
    public static final DeferredItem<Item> CRUSHED_IRON = ITEMS.registerSimpleItem("crushed_iron", () -> new Item.Properties());
    public static final DeferredItem<Item> CRUSHED_COPPER = ITEMS.registerSimpleItem("crushed_copper", () -> new Item.Properties());
    public static final DeferredItem<Item> CRUSHED_GOLD = ITEMS.registerSimpleItem("crushed_gold", () -> new Item.Properties());
    public static final DeferredItem<Item> CRUSHED_MAGNETITE = ITEMS.registerSimpleItem("crushed_magnetite", () -> new Item.Properties());
    public static final DeferredItem<Item> CRUSHED_ARGENTITE = ITEMS.registerSimpleItem("crushed_argentite", () -> new Item.Properties());

    // Fulgurite Fusion Guide: the in-game guide book.
    public static final DeferredItem<com.sideflowgames.fulguritefusion.item.GuideBookItem> GUIDE_BOOK = ITEMS.registerItem("guide_book",
            com.sideflowgames.fulguritefusion.item.GuideBookItem::new, () -> new Item.Properties().stacksTo(1));
    // Fusion Fuel capsule: one per pairing, fuel for the big reactor.
    public static final DeferredItem<FusionFuelItem> FUSION_FUEL = ITEMS.registerItem("fusion_fuel",
            FusionFuelItem::new, () -> new Item.Properties().stacksTo(16));

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
                        output.accept(RAW_REINFORCED_WIRE.get());
                        output.accept(REINFORCED_WIRE.get());
                        output.accept(RAW_HEAVY_WIRE.get());
                        output.accept(HEAVY_WIRE.get());
                        output.accept(ModBlocks.RUBBER_SAPLING_ITEM.get());
                        output.accept(ModBlocks.RUBBER_LOG_ITEM.get());
                        output.accept(ModBlocks.RUBBER_WOOD_ITEM.get());
                        output.accept(ModBlocks.RUBBER_PLANKS_ITEM.get());
                        output.accept(ModBlocks.RUBBER_LEAVES_ITEM.get());
                        output.accept(ModBlocks.TREE_TAP_ITEM.get());
                        output.accept(LATEX_BUCKET.get());
                        output.accept(DRIED_RUBBER.get());
                        output.accept(MALLEABLE_RUBBER.get());
                        output.accept(ModBlocks.DRIED_RUBBER_LOG_ITEM.get());
                        output.accept(ModBlocks.LATEX_HARVESTER_ITEM.get());
                        output.accept(ModBlocks.HARVESTER_TANK_ITEM.get());
                        output.accept(ModBlocks.HARVESTER_POWER_PORT_ITEM.get());
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
                        output.accept(ModBlocks.COPPER_BACKPACK_ITEM.get());
                        output.accept(ModBlocks.IRON_BACKPACK_ITEM.get());
                        output.accept(ModBlocks.FULGURITE_BACKPACK_ITEM.get());
                        output.accept(ModBlocks.STORMFORGED_BACKPACK_ITEM.get());
                        output.accept(ModBlocks.PRIME_BACKPACK_ITEM.get());
                        output.accept(PICKUP_UPGRADE.get());
                        output.accept(MAGNET_UPGRADE.get());
                        output.accept(VOID_UPGRADE.get());
                        output.accept(COMPACTING_UPGRADE.get());
                        output.accept(STORM_PICKUP_UPGRADE.get());
                        output.accept(STORM_MAGNET_UPGRADE.get());
                        output.accept(STORM_VOID_UPGRADE.get());
                        output.accept(STORM_COMPACTING_UPGRADE.get());
                        output.accept(FEEDING_UPGRADE.get());
                        output.accept(STORM_FEEDING_UPGRADE.get());
                        output.accept(REFILL_UPGRADE.get());
                        output.accept(STORM_REFILL_UPGRADE.get());
                        output.accept(RESTOCK_UPGRADE.get());
                        output.accept(STORM_RESTOCK_UPGRADE.get());
                        output.accept(DEPOSIT_UPGRADE.get());
                        output.accept(STORM_DEPOSIT_UPGRADE.get());
                        output.accept(TOOL_SWAPPER_UPGRADE.get());
                        output.accept(STORM_TOOL_SWAPPER_UPGRADE.get());
                        for (var gear : java.util.List.of(STORM_BLADE, STORM_SPEAR, STORMFORGED_PICKAXE, STORMFORGED_AXE, STORMFORGED_SHOVEL, STORMFORGED_HOE)) {
                            ItemStack full = new ItemStack(gear.get());
                            com.sideflowgames.fulguritefusion.item.FmChargeable.set(full, com.sideflowgames.fulguritefusion.item.StormforgedTools.MAX_FM);
                            output.accept(full);
                        }
                        ItemStack staff = new ItemStack(TEMPEST_STAFF.get());
                        com.sideflowgames.fulguritefusion.item.FmChargeable.set(staff, com.sideflowgames.fulguritefusion.item.TempestStaffItem.MAX_FM);
                        output.accept(staff);
                        output.accept(ModBlocks.STORM_FORGE_ITEM.get());
                        output.accept(ModBlocks.FORGE_POWER_PORT_ITEM.get());
                        for (var gear : java.util.List.of(PRIME_BLADE, PRIME_SPEAR, PRIME_PICKAXE, PRIME_AXE, PRIME_SHOVEL, PRIME_HOE,
                                PRIME_HELMET, PRIME_CHESTPLATE, PRIME_LEGGINGS, PRIME_BOOTS)) {
                            ItemStack full = new ItemStack(gear.get());
                            com.sideflowgames.fulguritefusion.item.FmChargeable.set(full, com.sideflowgames.fulguritefusion.item.FmChargeable.max(full));
                            output.accept(full);
                        }
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
                        output.accept(STORM_EYE.get());
                        output.accept(MOLTEN_HEART.get());
                        output.accept(VOID_SHARD.get());
                        for (var t : java.util.List.of(CLOUDSTEP_CHARM, EMPOWERED_CLOUDSTEP_CHARM, CONDUIT_HEART, EMPOWERED_CONDUIT_HEART,
                                VOID_ARC_BAND, EMPOWERED_VOID_ARC_BAND, GROUNDING_CHARM, EMPOWERED_GROUNDING_CHARM, CINDER_WARD,
                                EMPOWERED_CINDER_WARD, SEERS_RING, EMPOWERED_SEERS_RING)) output.accept(t.get());
                        output.accept(STORM_SIGIL.get());
                        output.accept(RIFT_CORE.get());
                        output.accept(ModBlocks.RIFT_FRAME_ITEM.get());
                        output.accept(ModBlocks.RIFT_KEYSTONE_ITEM.get());
                        output.accept(STORM_TITAN_SPAWN_EGG.get());
                        output.accept(ModBlocks.TITAN_ALTAR_ITEM.get());
                        output.accept(ModBlocks.TITAN_FORGE_ITEM.get());
                        for (var armor : java.util.List.of(TITAN_PRIME_HELMET, TITAN_PRIME_CHESTPLATE, TITAN_PRIME_LEGGINGS, TITAN_PRIME_BOOTS)) {
                            ItemStack full = new ItemStack(armor.get());
                            com.sideflowgames.fulguritefusion.item.FmChargeable.set(full, com.sideflowgames.fulguritefusion.item.TitanPrimeArmorItem.MAX_FM);
                            output.accept(full);
                        }
                        for (var armor : java.util.List.of(TITAN_HELMET, TITAN_CHESTPLATE, TITAN_LEGGINGS, TITAN_BOOTS)) {
                            ItemStack full = new ItemStack(armor.get());
                            com.sideflowgames.fulguritefusion.item.FmChargeable.set(full, com.sideflowgames.fulguritefusion.item.TitanArmorItem.MAX_FM);
                            output.accept(full);
                        }
                        output.accept(STORMHOUND_SPAWN_EGG.get());
                        output.accept(VOLTCAP_CRAWLER_SPAWN_EGG.get());
                        output.accept(THUNDERHEAD_SPAWN_EGG.get());
                        output.accept(MAGMA_CONDUIT_SPAWN_EGG.get());
                        output.accept(VOID_ARC_SPAWN_EGG.get());
                        output.accept(SPARKHORN_SHANK.get());
                        output.accept(COOKED_SPARKHORN_SHANK.get());
                        output.accept(HORN_SHARD.get());
                        output.accept(STATIC_GEL.get());
                        output.accept(ION_SCALE.get());
                        output.accept(VOLTSHELL.get());
                        output.accept(GLASS_CLAW.get());
                        output.accept(ARCWURM_HIDE.get());
                        output.accept(WRAITH_ESSENCE.get());
                        output.accept(SPARKHORN_SPAWN_EGG.get());
                        output.accept(PUFFLING_SPAWN_EGG.get());
                        output.accept(STATIC_JELLY_SPAWN_EGG.get());
                        output.accept(ION_EEL_SPAWN_EGG.get());
                        output.accept(RUBBERBACK_SLOTH_SPAWN_EGG.get());
                        output.accept(GLIMMERWING_SPAWN_EGG.get());
                        output.accept(VOLTCAP_SNAIL_SPAWN_EGG.get());
                        output.accept(GLASSCRAB_SPAWN_EGG.get());
                        output.accept(ARCWURM_SPAWN_EGG.get());
                        output.accept(CRYSTAL_STALKER_SPAWN_EGG.get());
                        output.accept(STATIC_WRAITH_SPAWN_EGG.get());
                        output.accept(SHOCKROACH_SPAWN_EGG.get());
                        output.accept(ARC_REAVER_SPAWN_EGG.get());
                        output.accept(THUNDERHULK_SPAWN_EGG.get());
                        output.accept(STORMCALLER_SHADE_SPAWN_EGG.get());
                        output.accept(STATIC_LEECH_SPAWN_EGG.get());
                        output.accept(ION_SWARM_SPAWN_EGG.get());
                        output.accept(VOLTSPITTER_SPAWN_EGG.get());
                        output.accept(TEMPEST_RAPTOR_SPAWN_EGG.get());
                        output.accept(GLOOMARC_SPIDER_SPAWN_EGG.get());
                        output.accept(PEAK_COLOSSUS_SPAWN_EGG.get());
                        output.accept(CINDER_BEHEMOTH_SPAWN_EGG.get());
                        output.accept(AURORA_WRAITH_SPAWN_EGG.get());
                        output.accept(PRISM_SENTINEL_SPAWN_EGG.get());
                        com.sideflowgames.fulguritefusion.dungeon.DungeonBlocks.addToTab(output);
                        com.sideflowgames.fulguritefusion.brew.UtilityBlocks.addToTab(output);
                        com.sideflowgames.fulguritefusion.farm.FarmBlocks.addToTab(output);
                        com.sideflowgames.fulguritefusion.transport.TransportBlocks.addToTab(output);
                        output.accept(ModBlocks.CRUDE_ARC_CASING_ITEM.get());
                        output.accept(ModBlocks.TEMPERED_ARC_CASING_ITEM.get());
                        output.accept(ModBlocks.CHARGED_ARC_CASING_ITEM.get());
                        output.accept(ModBlocks.PRIME_ARC_CASING_ITEM.get());
                        output.accept(ModBlocks.PRESS_ITEM.get());
                        output.accept(ModBlocks.ARC_FURNACE_ITEM.get());
                        output.accept(ModBlocks.WIRE_EXTRUDER_ITEM.get());
                        output.accept(ModBlocks.KILN_ITEM.get());
                        output.accept(ModBlocks.CRUSHER_ITEM.get());
                        output.accept(ModBlocks.BANBURY_MIXER_ITEM.get());
                        output.accept(ModBlocks.LATEX_VULCANIZER_ITEM.get());
                        output.accept(ModBlocks.FLUID_PIPE_ITEM.get());
                        output.accept(ModBlocks.STORMSTONE_ITEM.get());
                        output.accept(ModBlocks.COBBLED_STORMSTONE_ITEM.get());
                        output.accept(ModBlocks.POLISHED_STORMSTONE_ITEM.get());
                        output.accept(ModBlocks.STORMSTONE_BRICKS_ITEM.get());
                        output.accept(ModBlocks.FULGURITE_ORE_ITEM.get());
                        output.accept(ModBlocks.TEMPEST_CRYSTAL_ORE_ITEM.get());
                        output.accept(ModBlocks.DEEP_STORMSTONE_ITEM.get());
                        output.accept(ModBlocks.RIMESTONE_ITEM.get());
                        output.accept(ModBlocks.STORMSLATE_ITEM.get());
                        output.accept(ModBlocks.MAGNETITE_ROCK_ITEM.get());
                        output.accept(ModBlocks.FULGURITE_CRUST_ITEM.get());
                        output.accept(ModBlocks.CINDER_ROCK_ITEM.get());
                        output.accept(ModBlocks.STORMSANDSTONE_ITEM.get());
                        output.accept(ModBlocks.AURORA_ICE_ITEM.get());
                        output.accept(ModBlocks.PETRIFIED_LOG_ITEM.get());
                        output.accept(ModBlocks.ASH_BLOCK_ITEM.get());
                        output.accept(ModBlocks.SCORCHED_SAND_ITEM.get());
                        output.accept(ModBlocks.STORMGLASS_SAND_ITEM.get());
                        output.accept(ModBlocks.STORMSTONE_MAGNETITE_ORE_ITEM.get());
                        output.accept(ModBlocks.STORMSTONE_ARGENTITE_ORE_ITEM.get());
                        output.accept(ModBlocks.CINDERITE_ORE_ITEM.get());
                        output.accept(ModBlocks.PRISMITE_ORE_ITEM.get());
                        output.accept(ModBlocks.STORMQUARTZ_ORE_ITEM.get());
                        output.accept(ModBlocks.AURORITE_ORE_ITEM.get());
                        output.accept(ModBlocks.AMBER_ORE_ITEM.get());
                        output.accept(ModBlocks.MAGNETITE_ORE_ITEM.get());
                        output.accept(ModBlocks.DEEPSLATE_MAGNETITE_ORE_ITEM.get());
                        output.accept(ModBlocks.ARGENTITE_ORE_ITEM.get());
                        output.accept(ModBlocks.DEEPSLATE_ARGENTITE_ORE_ITEM.get());
                        output.accept(ModBlocks.MAGNETITE_BLOCK_ITEM.get());
                        output.accept(ModBlocks.SILVER_BLOCK_ITEM.get());
                        output.accept(ModBlocks.CINDERITE_BLOCK_ITEM.get());
                        output.accept(ModBlocks.PRISMITE_BLOCK_ITEM.get());
                        output.accept(ModBlocks.STORMQUARTZ_BLOCK_ITEM.get());
                        output.accept(ModBlocks.AURORITE_BLOCK_ITEM.get());
                        output.accept(ModBlocks.AMBER_BLOCK_ITEM.get());
                        output.accept(RAW_MAGNETITE.get());
                        output.accept(MAGNETITE_INGOT.get());
                        output.accept(RAW_ARGENTITE.get());
                        output.accept(SILVER_INGOT.get());
                        output.accept(CINDERITE.get());
                        output.accept(PRISMITE_SHARD.get());
                        output.accept(STORMQUARTZ.get());
                        output.accept(RAW_AURORITE.get());
                        output.accept(AURORITE_INGOT.get());
                        output.accept(REAVER_BLADE_SHARD.get());
                        output.accept(THUNDERHULK_CORE.get());
                        output.accept(SHADE_VEIL.get());
                        output.accept(LEECH_FANG.get());
                        output.accept(ION_MOTE.get());
                        output.accept(VOLT_GLAND.get());
                        output.accept(TEMPEST_FEATHER.get());
                        output.accept(GLOOMARC_SILK.get());
                        output.accept(COLOSSUS_HEART.get());
                        output.accept(BEHEMOTH_EMBER.get());
                        output.accept(AURORA_SHROUD.get());
                        output.accept(SENTINEL_LENS.get());
                        output.accept(FOSSIL_AMBER.get());
                        output.accept(ModBlocks.CHARGED_STORMSAND_ITEM.get());
                        output.accept(ModBlocks.FULGURITE_GLASS_ITEM.get());
                        output.accept(ModBlocks.CHARGED_LOAM_ITEM.get());
                        output.accept(ModBlocks.ARCGRASS_BLOCK_ITEM.get());
                        output.accept(ModBlocks.CONDUCTOR_LOG_ITEM.get());
                        output.accept(ModBlocks.CONDUCTOR_PLANKS_ITEM.get());
                        output.accept(ModBlocks.CONDUCTOR_LEAVES_ITEM.get());
                        output.accept(ModBlocks.CONDUCTOR_SAPLING_ITEM.get());
                        output.accept(ModBlocks.THUNDEROAK_LOG_ITEM.get());
                        output.accept(ModBlocks.THUNDEROAK_PLANKS_ITEM.get());
                        output.accept(ModBlocks.THUNDEROAK_LEAVES_ITEM.get());
                        output.accept(ModBlocks.THUNDEROAK_SAPLING_ITEM.get());
                        output.accept(ModBlocks.SPARKBUSH_LOG_ITEM.get());
                        output.accept(ModBlocks.SPARKBUSH_PLANKS_ITEM.get());
                        output.accept(ModBlocks.SPARKBUSH_LEAVES_ITEM.get());
                        output.accept(ModBlocks.SPARKBUSH_SAPLING_ITEM.get());
                        output.accept(ModBlocks.ARCGRASS_ITEM.get());
                        output.accept(ModBlocks.ARC_FERN_ITEM.get());
                        output.accept(ModBlocks.TALL_ARCGRASS_ITEM.get());
                        output.accept(ModBlocks.VOLTBLOOM_ITEM.get());
                        output.accept(ModBlocks.STORMCAP_ITEM.get());
                        output.accept(ModBlocks.AURORA_LILY_ITEM.get());
                        output.accept(ModBlocks.STATIC_REEDS_ITEM.get());
                        output.accept(ModBlocks.CRYSTAL_SPROUTS_ITEM.get());
                        output.accept(ModBlocks.CLOUDMOSS_BLOCK_ITEM.get());
                        output.accept(ModBlocks.CLOUDMOSS_CARPET_ITEM.get());
                        output.accept(ModBlocks.PUFFBLOOM_ITEM.get());
                        output.accept(ModBlocks.VOLTCAP_MYCELIUM_ITEM.get());
                        output.accept(ModBlocks.VOLTCAP_ITEM.get());
                        output.accept(ModBlocks.VOLTCAP_BLOCK_ITEM.get());
                        output.accept(ModBlocks.VOLTCAP_STEM_ITEM.get());
                        output.accept(ModBlocks.ION_BULB_ITEM.get());
                        output.accept(TEMPEST_CRYSTAL.get());
                        output.accept(IRON_SHEET.get());
                        output.accept(GOLD_SHEET.get());
                        output.accept(SILVER_SHEET.get());
                        output.accept(MAGNETITE_PLATE.get());
                        output.accept(COIL_SEGMENT.get());
                        output.accept(CRUSHED_IRON.get());
                        output.accept(CRUSHED_COPPER.get());
                        output.accept(CRUSHED_GOLD.get());
                        output.accept(CRUSHED_MAGNETITE.get());
                        output.accept(CRUSHED_ARGENTITE.get());
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
