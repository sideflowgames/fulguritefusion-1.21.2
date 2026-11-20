package com.sideflowgames.fulguritefusion;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import com.sideflowgames.fulguritefusion.block.StormwingEggBlockEntity;
import com.sideflowgames.fulguritefusion.block.StormwingEggBlock;
import com.sideflowgames.fulguritefusion.block.MoltedPlumePileBlock;
import com.sideflowgames.fulguritefusion.block.FulguriteRingBlock;
import com.sideflowgames.fulguritefusion.block.ReactorFacingBlock;
import com.sideflowgames.fulguritefusion.block.ReactorConsoleBlock;
import com.sideflowgames.fulguritefusion.block.ReactorConsoleBlockEntity;
import com.sideflowgames.fulguritefusion.block.ConsoleCasingBlock;
import com.sideflowgames.fulguritefusion.block.ConsolePortBlock;
import com.sideflowgames.fulguritefusion.block.DataCableBlock;
import com.sideflowgames.fulguritefusion.block.CreativePowerSourceBlock;
import com.sideflowgames.fulguritefusion.block.CreativePowerSourceBlockEntity;
import com.sideflowgames.fulguritefusion.block.DataCableBlockEntity;
import com.sideflowgames.fulguritefusion.block.FormableBlock;
import com.sideflowgames.fulguritefusion.block.FormableGlassBlock;
import com.sideflowgames.fulguritefusion.block.FusionColliderBlock;
import com.sideflowgames.fulguritefusion.block.FusionColliderBlockEntity;
import java.util.function.Supplier;

import com.sideflowgames.fulguritefusion.block.ArcCasingBlock;
import com.sideflowgames.fulguritefusion.block.ArcCasingBlockEntity;
import com.sideflowgames.fulguritefusion.block.ColliderInjectorBlock;
import com.sideflowgames.fulguritefusion.block.ColliderInjectorBlockEntity;
import com.sideflowgames.fulguritefusion.block.ColliderInputBlock;
import com.sideflowgames.fulguritefusion.block.ColliderInputBlockEntity;
import com.sideflowgames.fulguritefusion.block.EuConverterBlock;
import com.sideflowgames.fulguritefusion.block.EuConverterBlockEntity;
import com.sideflowgames.fulguritefusion.block.FeConverterBlock;
import com.sideflowgames.fulguritefusion.block.FeConverterBlockEntity;
import com.sideflowgames.fulguritefusion.block.FulguriteChargerBlock;
import com.sideflowgames.fulguritefusion.block.FulguriteChargerBlockEntity;
import com.sideflowgames.fulguritefusion.block.LumenTrailBlock;
import com.sideflowgames.fulguritefusion.block.MachineConnectorBlock;
import com.sideflowgames.fulguritefusion.block.MachineControllerBlock;
import com.sideflowgames.fulguritefusion.block.MachineControllerBlockEntity;
import com.sideflowgames.fulguritefusion.machine.MachineType;
import com.sideflowgames.fulguritefusion.block.MachineConnectorBlockEntity;
import com.sideflowgames.fulguritefusion.block.MiniColliderBlock;
import com.sideflowgames.fulguritefusion.block.MiniColliderBlockEntity;
import com.sideflowgames.fulguritefusion.block.StormColliderBlock;
import com.sideflowgames.fulguritefusion.block.StormColliderBlockEntity;
import com.sideflowgames.fulguritefusion.block.StormBatteryBlock;
import com.sideflowgames.fulguritefusion.block.StormBatteryBlockEntity;
import com.sideflowgames.fulguritefusion.block.StormFurnaceBlock;
import com.sideflowgames.fulguritefusion.block.StormFurnaceBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// All of Fulgurite Fusion's blocks, their block items, and block entities.
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FulguriteFusion.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FulguriteFusion.MODID);

    // Glassy vein left when lightning strikes sand. Mine with a pickaxe for Fulgurite Shards.
    public static final DeferredBlock<Block> RAW_FULGURITE = BLOCKS.registerSimpleBlock("raw_fulgurite",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.0f)
                    .sound(SoundType.GLASS).requiresCorrectToolForDrops());

    // Storm Furnaces: a furnace struck by lightning. Burn fuel for Fm; can no longer smelt.
    // Weak (tier 1) comes from a plain strike; Enhanced and Charged from the lightning upgrade ritual.
    public static final DeferredBlock<StormFurnaceBlock> STORM_FURNACE = BLOCKS.registerBlock("storm_furnace",
            props -> new StormFurnaceBlock(1, props), stormFurnaceProperties());
    public static final DeferredBlock<StormFurnaceBlock> ENHANCED_STORM_FURNACE = BLOCKS.registerBlock("enhanced_storm_furnace",
            props -> new StormFurnaceBlock(2, props), stormFurnaceProperties());
    public static final DeferredBlock<StormFurnaceBlock> CHARGED_STORM_FURNACE = BLOCKS.registerBlock("charged_storm_furnace",
            props -> new StormFurnaceBlock(3, props), stormFurnaceProperties());

    // Fills Empty Spark Cells with Fm.
    public static final DeferredBlock<FulguriteChargerBlock> FULGURITE_CHARGER = BLOCKS.registerBlock("fulgurite_charger",
            FulguriteChargerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL));

    // Turns Fm into Forge Energy for other mods (one-way, 1 Fm = 10 FE).
    public static final DeferredBlock<FeConverterBlock> FE_CONVERTER = BLOCKS.registerBlock("fe_converter",
            FeConverterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL));

    // Universal EU Converter for GregTech CEu and Modern Industrialization (one-way, 1 Fm = 1 EU).
    public static final DeferredBlock<EuConverterBlock> EU_CONVERTER = BLOCKS.registerBlock("eu_converter",
            EuConverterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL));

    // Storm Battery: stores Fm; up to 6 touching blocks form a bank.
    public static final DeferredBlock<StormBatteryBlock> STORM_BATTERY = BLOCKS.registerBlock("storm_battery",
            StormBatteryBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL));

    // Machine Connector: sits on a machine; Wire Rolls string hanging wires between connectors.
    public static final DeferredBlock<MachineConnectorBlock> MACHINE_CONNECTOR = BLOCKS.registerBlock("machine_connector",
            MachineConnectorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f).noOcclusion().sound(SoundType.METAL));

    // Mini Collider multiblock: a 7x7 ring of Fulgurite Ring blocks with one Collider Input,
    // two Collider Injectors and the Mini Collider in the middle.
    // Fulgurite Ring is see-through (like stained glass) so the cells' trails can be watched racing inside it.
    public static final DeferredBlock<FulguriteRingBlock> FULGURITE_RING = BLOCKS.registerBlock("fulgurite_ring",
            FulguriteRingBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.GLASS).lightLevel(state -> 4).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false));
    public static final DeferredBlock<ColliderInjectorBlock> COLLIDER_INJECTOR = BLOCKS.registerBlock("collider_injector",
            ColliderInjectorBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.5f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<ColliderInputBlock> COLLIDER_INPUT = BLOCKS.registerBlock("collider_input",
            ColliderInputBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(3.5f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<MiniColliderBlock> MINI_COLLIDER = BLOCKS.registerBlock("mini_collider",
            MiniColliderBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.5f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(state -> 7));

    // Storm Collider: swap in for the Mini Collider to pair materials instead of making power.
    public static final DeferredBlock<StormColliderBlock> STORM_COLLIDER = BLOCKS.registerBlock("storm_collider",
            StormColliderBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(4.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(state -> 9));

    // Lumen Trail: left by the Lumen Slug; bottle it for Lumen Gel. No item form.
    public static final DeferredBlock<LumenTrailBlock> LUMEN_TRAIL = BLOCKS.registerBlock("lumen_trail",
            LumenTrailBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).noCollission().instabreak()
                    .sound(SoundType.HONEY_BLOCK).lightLevel(state -> 6).replaceable().noLootTable()
                    .pushReaction(PushReaction.DESTROY));

    // Fusion Collider: the main collider block of the big reactor's inner ring / blast chamber.
    // Placeholder: it does nothing yet; the reactor's structure and behaviour come later.
    public static final DeferredBlock<FusionColliderBlock> FUSION_COLLIDER = BLOCKS.registerBlock("fusion_collider",
            FusionColliderBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0f, 3_600_000f)
                    .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> 10));

    // ---------- Fusion Reactor parts (stage 1: blocks only) ----------
    // Explosion-proof: nothing the reactor (or a Catastrophic pairing) throws at them breaks them.
    private static final float REACTOR_BLAST = 3_600_000f;

    // Reinforced Fulgurite: the solid diagonal blocks of both ring tubes. Cheap, needed by the thousand.
    public static final DeferredBlock<FormableBlock> REINFORCED_FULGURITE = BLOCKS.registerBlock("reinforced_fulgurite",
            FormableBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(4.0f, REACTOR_BLAST)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS));
    // Fulgurite Blast Glass: see-through and explosion-proof; the ring tubes' windows and the chamber's faces.
    public static final DeferredBlock<FormableGlassBlock> FULGURITE_BLAST_GLASS = BLOCKS.registerBlock("fulgurite_blast_glass",
            FormableGlassBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.0f, REACTOR_BLAST)
                    .requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false));
    // Blast Chamber Frame: the 12 edges of the 5x5x5 blast chamber.
    public static final DeferredBlock<FormableBlock> BLAST_CHAMBER_FRAME = BLOCKS.registerBlock("blast_chamber_frame",
            FormableBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0f, REACTOR_BLAST)
                    .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK));
    // Transfer Portal (outer ring) fires the charge across open air; Receiver Portal (inner ring) catches it.
    public static final DeferredBlock<ReactorFacingBlock> TRANSFER_PORTAL = BLOCKS.registerBlock("transfer_portal",
            ReactorFacingBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(5.0f, REACTOR_BLAST)
                    .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> 9));
    public static final DeferredBlock<ReactorFacingBlock> RECEIVER_PORTAL = BLOCKS.registerBlock("receiver_portal",
            ReactorFacingBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0f, REACTOR_BLAST)
                    .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> 9));
    // Fusion Injector: on the inner ring's inside face; fires the charge into the blast chamber.
    public static final DeferredBlock<ReactorFacingBlock> FUSION_INJECTOR = BLOCKS.registerBlock("fusion_injector",
            ReactorFacingBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0f, REACTOR_BLAST)
                    .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> 7));
    // Ring Injection Port: replaces a glass tip on the outer ring's corner; fires one fuel down the ring.
    public static final DeferredBlock<ReactorFacingBlock> RING_INJECTION_PORT = BLOCKS.registerBlock("ring_injection_port",
            ReactorFacingBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(5.0f, REACTOR_BLAST)
                    .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> 7));

    // ---------- Reactor Control Console (7 wide, 3 tall, 1 deep) ----------
    private static BlockBehaviour.Properties consoleProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 12.0f)
                .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK);
    }
    public static final DeferredBlock<ReactorConsoleBlock> REACTOR_CONSOLE = BLOCKS.registerBlock("reactor_console",
            ReactorConsoleBlock::new, consoleProperties().lightLevel(state -> 6));
    public static final DeferredBlock<ConsoleCasingBlock> CONSOLE_CASING = BLOCKS.registerBlock("console_casing",
            ConsoleCasingBlock::new, consoleProperties().lightLevel(ConsoleCasingBlock::light));
    public static final DeferredBlock<ConsolePortBlock> CONSOLE_POWER_PORT = BLOCKS.registerBlock("console_power_port",
            ConsolePortBlock::new, consoleProperties());
    public static final DeferredBlock<ConsolePortBlock> CONSOLE_DATA_PORT = BLOCKS.registerBlock("console_data_port",
            ConsolePortBlock::new, consoleProperties());
    public static final DeferredBlock<ConsolePortBlock> CONSOLE_FUEL_INPUT = BLOCKS.registerBlock("console_fuel_input",
            ConsolePortBlock::new, consoleProperties());
    // Data Cable: a free-standing round metal pipe from the console's data ports to the Ring Injection Ports.
    public static final DeferredBlock<DataCableBlock> DATA_CABLE = BLOCKS.registerBlock("data_cable",
            DataCableBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 6.0f)
                    .sound(SoundType.COPPER).noOcclusion().dynamicShape());

    // Creative Power Source: creative only (no recipe, unbreakable in survival). 4,086 Fm/t, endless.
    public static final DeferredBlock<CreativePowerSourceBlock> CREATIVE_POWER_SOURCE = BLOCKS.registerBlock("creative_power_source",
            CreativePowerSourceBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA)
                    .strength(-1.0f, 3_600_000f).noLootTable().sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> 12));

    // Stormwing Nest: the woven floor of a nest (see worldgen/StormwingNests for the full 5x5 bowl).
    public static final DeferredBlock<Block> STORMWING_NEST = BLOCKS.registerSimpleBlock("stormwing_nest",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.GRASS).ignitedByLava());
    // The twig rim and stilts of a nest.
    public static final DeferredBlock<Block> STORMWING_NEST_RIM = BLOCKS.registerSimpleBlock("stormwing_nest_rim",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.GRASS).ignitedByLava());
    // Molted Plumes lying in a nest. No item form; picking it up gives Molted Plumes.
    public static final DeferredBlock<MoltedPlumePileBlock> MOLTED_PLUME_PILE = BLOCKS.registerBlock("molted_plume_pile",
            MoltedPlumePileBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).noCollission().instabreak()
                    .sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.DESTROY));
    // Stormwing Egg: rare, found in wild nests; incubates beside a running Storm Collider.
    public static final DeferredBlock<StormwingEggBlock> STORMWING_EGG = BLOCKS.registerBlock("stormwing_egg",
            StormwingEggBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5f)
                    .sound(SoundType.METAL).noOcclusion().lightLevel(state -> 3).pushReaction(PushReaction.DESTROY));

    // ---------- Multiblock processing machines ----------

    // Arc Casings, tiers 1-4. A machine runs at the tier of its lowest casing.
    public static final DeferredBlock<ArcCasingBlock> CRUDE_ARC_CASING = casing("crude_arc_casing", 1);
    public static final DeferredBlock<ArcCasingBlock> TEMPERED_ARC_CASING = casing("tempered_arc_casing", 2);
    public static final DeferredBlock<ArcCasingBlock> CHARGED_ARC_CASING = casing("charged_arc_casing", 3);
    public static final DeferredBlock<ArcCasingBlock> PRIME_ARC_CASING = casing("prime_arc_casing", 4);

    // Controllers: the block you place first; it shows where the casings go.
    public static final DeferredBlock<MachineControllerBlock> PRESS = controller("press", MachineType.PRESS);
    public static final DeferredBlock<MachineControllerBlock> ARC_FURNACE = controller("arc_furnace", MachineType.FURNACE);
    public static final DeferredBlock<MachineControllerBlock> WIRE_EXTRUDER = controller("wire_extruder", MachineType.WIRE_EXTRUDER);
    public static final DeferredBlock<MachineControllerBlock> KILN = controller("kiln", MachineType.KILN);
    public static final DeferredBlock<MachineControllerBlock> CRUSHER = controller("crusher", MachineType.CRUSHER);

    private static BlockBehaviour.Properties machineProperties() {
        // noOcclusion: when formed the blocks turn invisible and light must reach the machine model.
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f, 6.0f)
                .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion();
    }

    private static DeferredBlock<ArcCasingBlock> casing(String name, int tier) {
        return BLOCKS.registerBlock(name, props -> new ArcCasingBlock(tier, props), machineProperties());
    }

    private static DeferredBlock<MachineControllerBlock> controller(String name, MachineType type) {
        return BLOCKS.registerBlock(name, props -> new MachineControllerBlock(type, props), machineProperties());
    }

    public static final DeferredItem<BlockItem> RAW_FULGURITE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("raw_fulgurite", RAW_FULGURITE);
    public static final DeferredItem<BlockItem> STORM_FURNACE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("storm_furnace", STORM_FURNACE);
    public static final DeferredItem<BlockItem> ENHANCED_STORM_FURNACE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("enhanced_storm_furnace", ENHANCED_STORM_FURNACE);
    public static final DeferredItem<BlockItem> CHARGED_STORM_FURNACE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("charged_storm_furnace", CHARGED_STORM_FURNACE);
    public static final DeferredItem<BlockItem> FULGURITE_CHARGER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fulgurite_charger", FULGURITE_CHARGER);

    public static final DeferredItem<BlockItem> FE_CONVERTER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fe_converter", FE_CONVERTER);
    public static final DeferredItem<BlockItem> STORM_BATTERY_ITEM = ModItems.ITEMS.registerSimpleBlockItem("storm_battery", STORM_BATTERY);
    public static final DeferredItem<BlockItem> MACHINE_CONNECTOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem("machine_connector", MACHINE_CONNECTOR);
    public static final DeferredItem<BlockItem> EU_CONVERTER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("eu_converter", EU_CONVERTER);
    public static final DeferredItem<BlockItem> FULGURITE_RING_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fulgurite_ring", FULGURITE_RING);
    public static final DeferredItem<BlockItem> COLLIDER_INJECTOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem("collider_injector", COLLIDER_INJECTOR);
    public static final DeferredItem<BlockItem> COLLIDER_INPUT_ITEM = ModItems.ITEMS.registerSimpleBlockItem("collider_input", COLLIDER_INPUT);
    public static final DeferredItem<BlockItem> FUSION_COLLIDER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fusion_collider", FUSION_COLLIDER,
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> REINFORCED_FULGURITE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("reinforced_fulgurite", REINFORCED_FULGURITE);
    public static final DeferredItem<BlockItem> FULGURITE_BLAST_GLASS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fulgurite_blast_glass", FULGURITE_BLAST_GLASS);
    public static final DeferredItem<BlockItem> BLAST_CHAMBER_FRAME_ITEM = ModItems.ITEMS.registerSimpleBlockItem("blast_chamber_frame", BLAST_CHAMBER_FRAME);
    public static final DeferredItem<BlockItem> TRANSFER_PORTAL_ITEM = ModItems.ITEMS.registerSimpleBlockItem("transfer_portal", TRANSFER_PORTAL,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> RECEIVER_PORTAL_ITEM = ModItems.ITEMS.registerSimpleBlockItem("receiver_portal", RECEIVER_PORTAL,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> FUSION_INJECTOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fusion_injector", FUSION_INJECTOR,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> RING_INJECTION_PORT_ITEM = ModItems.ITEMS.registerSimpleBlockItem("ring_injection_port", RING_INJECTION_PORT,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> REACTOR_CONSOLE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("reactor_console", REACTOR_CONSOLE,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> CONSOLE_CASING_ITEM = ModItems.ITEMS.registerSimpleBlockItem("console_casing", CONSOLE_CASING);
    public static final DeferredItem<BlockItem> CONSOLE_POWER_PORT_ITEM = ModItems.ITEMS.registerSimpleBlockItem("console_power_port", CONSOLE_POWER_PORT);
    public static final DeferredItem<BlockItem> CONSOLE_DATA_PORT_ITEM = ModItems.ITEMS.registerSimpleBlockItem("console_data_port", CONSOLE_DATA_PORT);
    public static final DeferredItem<BlockItem> CONSOLE_FUEL_INPUT_ITEM = ModItems.ITEMS.registerSimpleBlockItem("console_fuel_input", CONSOLE_FUEL_INPUT);
    public static final DeferredItem<BlockItem> DATA_CABLE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("data_cable", DATA_CABLE);
    public static final DeferredItem<BlockItem> CREATIVE_POWER_SOURCE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("creative_power_source",
            CREATIVE_POWER_SOURCE, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> MINI_COLLIDER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("mini_collider", MINI_COLLIDER);
    public static final DeferredItem<BlockItem> STORMWING_NEST_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormwing_nest", STORMWING_NEST);
    public static final DeferredItem<BlockItem> STORMWING_NEST_RIM_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormwing_nest_rim", STORMWING_NEST_RIM);
    public static final DeferredItem<BlockItem> STORMWING_EGG_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormwing_egg", STORMWING_EGG,
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> CRUDE_ARC_CASING_ITEM = ModItems.ITEMS.registerSimpleBlockItem("crude_arc_casing", CRUDE_ARC_CASING);
    public static final DeferredItem<BlockItem> TEMPERED_ARC_CASING_ITEM = ModItems.ITEMS.registerSimpleBlockItem("tempered_arc_casing", TEMPERED_ARC_CASING);
    public static final DeferredItem<BlockItem> CHARGED_ARC_CASING_ITEM = ModItems.ITEMS.registerSimpleBlockItem("charged_arc_casing", CHARGED_ARC_CASING);
    public static final DeferredItem<BlockItem> PRIME_ARC_CASING_ITEM = ModItems.ITEMS.registerSimpleBlockItem("prime_arc_casing", PRIME_ARC_CASING);
    public static final DeferredItem<BlockItem> PRESS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("press", PRESS);
    public static final DeferredItem<BlockItem> ARC_FURNACE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("arc_furnace", ARC_FURNACE);
    public static final DeferredItem<BlockItem> WIRE_EXTRUDER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("wire_extruder", WIRE_EXTRUDER);
    public static final DeferredItem<BlockItem> KILN_ITEM = ModItems.ITEMS.registerSimpleBlockItem("kiln", KILN);
    public static final DeferredItem<BlockItem> CRUSHER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("crusher", CRUSHER);
    public static final DeferredItem<BlockItem> STORM_COLLIDER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("storm_collider", STORM_COLLIDER);

    public static final Supplier<BlockEntityType<FusionColliderBlockEntity>> FUSION_COLLIDER_BE = BLOCK_ENTITIES.register("fusion_collider",
            () -> BlockEntityType.Builder.of(FusionColliderBlockEntity::new, FUSION_COLLIDER.get()).build(null));
    public static final Supplier<BlockEntityType<ReactorConsoleBlockEntity>> REACTOR_CONSOLE_BE = BLOCK_ENTITIES.register("reactor_console",
            () -> BlockEntityType.Builder.of(ReactorConsoleBlockEntity::new, REACTOR_CONSOLE.get()).build(null));
    public static final Supplier<BlockEntityType<DataCableBlockEntity>> DATA_CABLE_BE = BLOCK_ENTITIES.register("data_cable",
            () -> BlockEntityType.Builder.of(DataCableBlockEntity::new, DATA_CABLE.get()).build(null));
    public static final Supplier<BlockEntityType<CreativePowerSourceBlockEntity>> CREATIVE_POWER_SOURCE_BE = BLOCK_ENTITIES.register("creative_power_source",
            () -> BlockEntityType.Builder.of(CreativePowerSourceBlockEntity::new, CREATIVE_POWER_SOURCE.get()).build(null));
    public static final Supplier<BlockEntityType<StormFurnaceBlockEntity>> STORM_FURNACE_BE = BLOCK_ENTITIES.register("storm_furnace",
            () -> BlockEntityType.Builder.of(StormFurnaceBlockEntity::new,
                    STORM_FURNACE.get(), ENHANCED_STORM_FURNACE.get(), CHARGED_STORM_FURNACE.get()).build(null));

    public static final Supplier<BlockEntityType<FulguriteChargerBlockEntity>> FULGURITE_CHARGER_BE = BLOCK_ENTITIES.register("fulgurite_charger",
            () -> BlockEntityType.Builder.of(FulguriteChargerBlockEntity::new, FULGURITE_CHARGER.get()).build(null));

    public static final Supplier<BlockEntityType<FeConverterBlockEntity>> FE_CONVERTER_BE = BLOCK_ENTITIES.register("fe_converter",
            () -> BlockEntityType.Builder.of(FeConverterBlockEntity::new, FE_CONVERTER.get()).build(null));

    public static final Supplier<BlockEntityType<EuConverterBlockEntity>> EU_CONVERTER_BE = BLOCK_ENTITIES.register("eu_converter",
            () -> BlockEntityType.Builder.of(EuConverterBlockEntity::new, EU_CONVERTER.get()).build(null));

    public static final Supplier<BlockEntityType<StormBatteryBlockEntity>> STORM_BATTERY_BE = BLOCK_ENTITIES.register("storm_battery",
            () -> BlockEntityType.Builder.of(StormBatteryBlockEntity::new, STORM_BATTERY.get()).build(null));

    public static final Supplier<BlockEntityType<MachineConnectorBlockEntity>> MACHINE_CONNECTOR_BE = BLOCK_ENTITIES.register("machine_connector",
            () -> BlockEntityType.Builder.of(MachineConnectorBlockEntity::new, MACHINE_CONNECTOR.get()).build(null));

    public static final Supplier<BlockEntityType<ColliderInputBlockEntity>> COLLIDER_INPUT_BE = BLOCK_ENTITIES.register("collider_input",
            () -> BlockEntityType.Builder.of(ColliderInputBlockEntity::new, COLLIDER_INPUT.get()).build(null));

    public static final Supplier<BlockEntityType<MiniColliderBlockEntity>> MINI_COLLIDER_BE = BLOCK_ENTITIES.register("mini_collider",
            () -> BlockEntityType.Builder.of(MiniColliderBlockEntity::new, MINI_COLLIDER.get()).build(null));

    public static final Supplier<BlockEntityType<ColliderInjectorBlockEntity>> COLLIDER_INJECTOR_BE = BLOCK_ENTITIES.register("collider_injector",
            () -> BlockEntityType.Builder.of(ColliderInjectorBlockEntity::new, COLLIDER_INJECTOR.get()).build(null));

    public static final Supplier<BlockEntityType<StormColliderBlockEntity>> STORM_COLLIDER_BE = BLOCK_ENTITIES.register("storm_collider",
            () -> BlockEntityType.Builder.of(StormColliderBlockEntity::new, STORM_COLLIDER.get()).build(null));

    public static final Supplier<BlockEntityType<ArcCasingBlockEntity>> ARC_CASING_BE = BLOCK_ENTITIES.register("arc_casing",
            () -> BlockEntityType.Builder.of(ArcCasingBlockEntity::new, CRUDE_ARC_CASING.get(), TEMPERED_ARC_CASING.get(),
                    CHARGED_ARC_CASING.get(), PRIME_ARC_CASING.get()).build(null));

    public static final Supplier<BlockEntityType<MachineControllerBlockEntity>> MACHINE_CONTROLLER_BE = BLOCK_ENTITIES.register("machine_controller",
            () -> BlockEntityType.Builder.of(MachineControllerBlockEntity::new, PRESS.get(), ARC_FURNACE.get(),
                    WIRE_EXTRUDER.get(), KILN.get(), CRUSHER.get()).build(null));

    public static final Supplier<BlockEntityType<StormwingEggBlockEntity>> STORMWING_EGG_BE = BLOCK_ENTITIES.register("stormwing_egg",
            () -> BlockEntityType.Builder.of(StormwingEggBlockEntity::new, STORMWING_EGG.get()).build(null));

    private ModBlocks() {}

    private static BlockBehaviour.Properties stormFurnaceProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.5f)
                .requiresCorrectToolForDrops().sound(SoundType.STONE)
                .lightLevel(state -> state.getValue(StormFurnaceBlock.LIT) ? 13 : 0);
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }
}
