package com.sideflowgames.fulguritefusion;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import com.sideflowgames.fulguritefusion.block.StormwingEggBlockEntity;
import com.sideflowgames.fulguritefusion.block.StormForgeBlock;
import com.sideflowgames.fulguritefusion.block.StormForgeBlockEntity;
import com.sideflowgames.fulguritefusion.block.ForgePowerPortBlock;
import com.sideflowgames.fulguritefusion.block.ForgePowerPortBlockEntity;
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
import com.sideflowgames.fulguritefusion.block.SkyCollectorBlock;
import com.sideflowgames.fulguritefusion.block.SkyCollectorBlockEntity;
import com.sideflowgames.fulguritefusion.block.NacreClusterBlock;
import com.sideflowgames.fulguritefusion.block.FrozenSoulFireBlock;
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

    // Fluid Pipe: copper with a glass window; carries latex from a Latex Harvester to a Latex Vulcanizer.
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.pipe.FluidPipeBlock> FLUID_PIPE = BLOCKS.registerBlock("fluid_pipe",
            com.sideflowgames.fulguritefusion.pipe.FluidPipeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.0f, 4.0f).sound(SoundType.COPPER).noOcclusion().dynamicShape());

    // ---------- Stormreach ----------
    // The Stormreach's stone and its variants.
    public static final DeferredBlock<Block> STORMSTONE = BLOCKS.registerSimpleBlock("stormstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> COBBLED_STORMSTONE = BLOCKS.registerSimpleBlock("cobbled_stormstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> POLISHED_STORMSTONE = BLOCKS.registerSimpleBlock("polished_stormstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.POLISHED_DEEPSLATE));
    public static final DeferredBlock<Block> STORMSTONE_BRICKS = BLOCKS.registerSimpleBlock("stormstone_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS));
    // Fulgurite Ore: veins in Stormstone; drops Fulgurite Shards.
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> FULGURITE_ORE = BLOCKS.registerBlock("fulgurite_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(1, 3), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).lightLevel(s -> 3));
    // Tempest Crystal Ore: rare; only found in the Stormreach. Needs a diamond pickaxe.
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> TEMPEST_CRYSTAL_ORE = BLOCKS.registerBlock("tempest_crystal_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(4.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s -> 6));
    // Charged Stormsand: glowing sand on the high ground; lightning fuses it into Fulgurite Glass.
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.stormreach.ChargedStormsandBlock> CHARGED_STORMSAND = BLOCKS.registerBlock("charged_stormsand",
            com.sideflowgames.fulguritefusion.stormreach.ChargedStormsandBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.6f).sound(SoundType.SAND).lightLevel(s -> 5));
    public static final DeferredBlock<net.minecraft.world.level.block.TransparentBlock> FULGURITE_GLASS = BLOCKS.registerBlock("fulgurite_glass",
            net.minecraft.world.level.block.TransparentBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.3f).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((s, l, p, t) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));

    // ---------- Stormreach lands: underground layers, biome blocks and ores ----------
    // Biome stones (each biome's subsoil) and Deep Stormstone (everything deep down).
    public static final DeferredBlock<Block> DEEP_STORMSTONE = BLOCKS.registerSimpleBlock("deep_stormstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> RIMESTONE = BLOCKS.registerSimpleBlock("rimestone",
            BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(1.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<Block> STORMSLATE = BLOCKS.registerSimpleBlock("stormslate",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> MAGNETITE_ROCK = BLOCKS.registerSimpleBlock("magnetite_rock",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.BASALT));
    public static final DeferredBlock<Block> FULGURITE_CRUST = BLOCKS.registerSimpleBlock("fulgurite_crust",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.TUFF));
    public static final DeferredBlock<Block> CINDER_ROCK = BLOCKS.registerSimpleBlock("cinder_rock",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.BASALT).lightLevel(s -> 2));
    public static final DeferredBlock<Block> STORMSANDSTONE = BLOCKS.registerSimpleBlock("stormsandstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.8f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));
    // Aurora Ice (Aurora Tundra): glowing, slippery ice that never melts.
    public static final DeferredBlock<Block> AURORA_ICE = BLOCKS.registerSimpleBlock("aurora_ice",
            BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(0.8f).friction(0.98f).sound(SoundType.GLASS).lightLevel(s -> 7));
    // Petrified Log (Petrified Forest): a tree turned to stone.
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> PETRIFIED_LOG = BLOCKS.registerBlock("petrified_log",
            net.minecraft.world.level.block.RotatedPillarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(2.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DRIPSTONE_BLOCK));
    // Falling surface blocks.
    public static final DeferredBlock<net.minecraft.world.level.block.ColoredFallingBlock> ASH_BLOCK = BLOCKS.registerBlock("ash_block",
            p -> new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(0x5A5458), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5f).sound(SoundType.SAND));
    public static final DeferredBlock<net.minecraft.world.level.block.ColoredFallingBlock> SCORCHED_SAND = BLOCKS.registerBlock("scorched_sand",
            p -> new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(0xB8643C), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.5f).sound(SoundType.SAND));
    public static final DeferredBlock<net.minecraft.world.level.block.ColoredFallingBlock> STORMGLASS_SAND = BLOCKS.registerBlock("stormglass_sand",
            p -> new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(0x9CC8E0), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5f).sound(SoundType.SAND));
    // Ores. Magnetite and Argentite also generate in the Overworld (stone and deepslate versions).
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> STORMSTONE_MAGNETITE_ORE = BLOCKS.registerBlock("stormstone_magnetite_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> STORMSTONE_ARGENTITE_ORE = BLOCKS.registerBlock("stormstone_argentite_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> CINDERITE_ORE = BLOCKS.registerBlock("cinderite_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(2, 5), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).lightLevel(s -> 4));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> PRISMITE_ORE = BLOCKS.registerBlock("prismite_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 6), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).lightLevel(s -> 6));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> STORMQUARTZ_ORE = BLOCKS.registerBlock("stormquartz_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 6), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).lightLevel(s -> 3));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> AURORITE_ORE = BLOCKS.registerBlock("aurorite_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(2, 5), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).lightLevel(s -> 5));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> AMBER_ORE = BLOCKS.registerBlock("amber_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(1, 4), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).lightLevel(s -> 2));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> MAGNETITE_ORE = BLOCKS.registerBlock("magnetite_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> DEEPSLATE_MAGNETITE_ORE = BLOCKS.registerBlock("deepslate_magnetite_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> ARGENTITE_ORE = BLOCKS.registerBlock("argentite_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> DEEPSLATE_ARGENTITE_ORE = BLOCKS.registerBlock("deepslate_argentite_ore",
            p -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), p),
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    // Storage blocks.
    public static final DeferredBlock<Block> MAGNETITE_BLOCK = BLOCKS.registerSimpleBlock("magnetite_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<Block> SILVER_BLOCK = BLOCKS.registerSimpleBlock("silver_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<Block> CINDERITE_BLOCK = BLOCKS.registerSimpleBlock("cinderite_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 6));
    public static final DeferredBlock<Block> PRISMITE_BLOCK = BLOCKS.registerSimpleBlock("prismite_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s -> 9));
    public static final DeferredBlock<Block> STORMQUARTZ_BLOCK = BLOCKS.registerSimpleBlock("stormquartz_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s -> 4));
    public static final DeferredBlock<Block> AURORITE_BLOCK = BLOCKS.registerSimpleBlock("aurorite_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 7));
    public static final DeferredBlock<Block> AMBER_BLOCK = BLOCKS.registerSimpleBlock("amber_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s -> 5));

    // Stormreach plant life.
    // Stormreach soil: Charged Loam, with Arcgrass on top. Plants and saplings grow on both.
    public static final DeferredBlock<Block> CHARGED_LOAM = BLOCKS.registerBlock("charged_loam",
            net.minecraft.world.level.block.Block::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DIRT).mapColor(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.stormreach.ArcgrassBlock> ARCGRASS_BLOCK = BLOCKS.registerBlock("arcgrass_block",
            com.sideflowgames.fulguritefusion.stormreach.ArcgrassBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GRASS_BLOCK).mapColor(MapColor.COLOR_LIGHT_BLUE));
    // Conductor: tall and thin, copper-tinted bark, glowing pale-blue leaves.
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> CONDUCTOR_LOG = BLOCKS.registerBlock("conductor_log",
            net.minecraft.world.level.block.RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BIRCH_LOG).mapColor(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<Block> CONDUCTOR_PLANKS = BLOCKS.registerBlock("conductor_planks",
            net.minecraft.world.level.block.Block::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BIRCH_PLANKS).mapColor(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<net.minecraft.world.level.block.LeavesBlock> CONDUCTOR_LEAVES = BLOCKS.registerBlock("conductor_leaves",
            net.minecraft.world.level.block.LeavesBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BIRCH_LEAVES).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 6));
    public static final DeferredBlock<net.minecraft.world.level.block.SaplingBlock> CONDUCTOR_SAPLING = BLOCKS.registerBlock("conductor_sapling",
            p -> new net.minecraft.world.level.block.SaplingBlock(com.sideflowgames.fulguritefusion.stormreach.StormFlora.CONDUCTOR, p),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BIRCH_SAPLING));
    // Thunderoak: broad and twisted, dark wood, violet leaves.
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> THUNDEROAK_LOG = BLOCKS.registerBlock("thunderoak_log",
            net.minecraft.world.level.block.RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DARK_OAK_LOG).mapColor(MapColor.COLOR_BROWN));
    public static final DeferredBlock<Block> THUNDEROAK_PLANKS = BLOCKS.registerBlock("thunderoak_planks",
            net.minecraft.world.level.block.Block::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS).mapColor(MapColor.COLOR_BROWN));
    public static final DeferredBlock<net.minecraft.world.level.block.LeavesBlock> THUNDEROAK_LEAVES = BLOCKS.registerBlock("thunderoak_leaves",
            net.minecraft.world.level.block.LeavesBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DARK_OAK_LEAVES).mapColor(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<net.minecraft.world.level.block.SaplingBlock> THUNDEROAK_SAPLING = BLOCKS.registerBlock("thunderoak_sapling",
            p -> new net.minecraft.world.level.block.SaplingBlock(com.sideflowgames.fulguritefusion.stormreach.StormFlora.THUNDEROAK, p),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DARK_OAK_SAPLING));
    // Sparkbush: a small, bushy tree with brightly glowing leaves.
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> SPARKBUSH_LOG = BLOCKS.registerBlock("sparkbush_log",
            net.minecraft.world.level.block.RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_LOG).mapColor(MapColor.TERRACOTTA_YELLOW));
    public static final DeferredBlock<Block> SPARKBUSH_PLANKS = BLOCKS.registerBlock("sparkbush_planks",
            net.minecraft.world.level.block.Block::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).mapColor(MapColor.TERRACOTTA_YELLOW));
    public static final DeferredBlock<net.minecraft.world.level.block.LeavesBlock> SPARKBUSH_LEAVES = BLOCKS.registerBlock("sparkbush_leaves",
            net.minecraft.world.level.block.LeavesBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_LEAVES).mapColor(MapColor.COLOR_YELLOW).lightLevel(s -> 10));
    public static final DeferredBlock<net.minecraft.world.level.block.SaplingBlock> SPARKBUSH_SAPLING = BLOCKS.registerBlock("sparkbush_sapling",
            p -> new net.minecraft.world.level.block.SaplingBlock(com.sideflowgames.fulguritefusion.stormreach.StormFlora.SPARKBUSH, p),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_SAPLING));
    // Ground cover, flowers and the marsh's reeds.
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.stormreach.StormPlantBlock> ARCGRASS = BLOCKS.registerBlock("arcgrass",
            com.sideflowgames.fulguritefusion.stormreach.StormPlantBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SHORT_GRASS).mapColor(MapColor.COLOR_LIGHT_BLUE));
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.stormreach.StormPlantBlock> ARC_FERN = BLOCKS.registerBlock("arc_fern",
            com.sideflowgames.fulguritefusion.stormreach.StormPlantBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.FERN).mapColor(MapColor.COLOR_LIGHT_BLUE));
    public static final DeferredBlock<net.minecraft.world.level.block.DoublePlantBlock> TALL_ARCGRASS = BLOCKS.registerBlock("tall_arcgrass",
            net.minecraft.world.level.block.DoublePlantBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.TALL_GRASS).mapColor(MapColor.COLOR_LIGHT_BLUE));
    public static final DeferredBlock<net.minecraft.world.level.block.FlowerBlock> VOLTBLOOM = BLOCKS.registerBlock("voltbloom",
            p -> new net.minecraft.world.level.block.FlowerBlock(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 5, p),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.POPPY).lightLevel(s -> 7));
    public static final DeferredBlock<net.minecraft.world.level.block.FlowerBlock> STORMCAP = BLOCKS.registerBlock("stormcap",
            p -> new net.minecraft.world.level.block.FlowerBlock(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 5, p),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.POPPY).lightLevel(s -> 6));
    public static final DeferredBlock<net.minecraft.world.level.block.FlowerBlock> AURORA_LILY = BLOCKS.registerBlock("aurora_lily",
            p -> new net.minecraft.world.level.block.FlowerBlock(net.minecraft.world.effect.MobEffects.REGENERATION, 4, p),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.POPPY).lightLevel(s -> 8));
    public static final DeferredBlock<net.minecraft.world.level.block.DoublePlantBlock> STATIC_REEDS = BLOCKS.registerBlock("static_reeds",
            net.minecraft.world.level.block.DoublePlantBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.TALL_GRASS).mapColor(MapColor.COLOR_CYAN));
    // Crystal Sprouts: decorative crystal growths on Stormstone.
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.stormreach.CrystalSproutBlock> CRYSTAL_SPROUTS = BLOCKS.registerBlock("crystal_sprouts",
            com.sideflowgames.fulguritefusion.stormreach.CrystalSproutBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollission().instabreak().sound(SoundType.SMALL_AMETHYST_BUD).lightLevel(s -> 5).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));

    // Cloudmoss Meadow: soft, pale moss ground with a carpet of it, and Puffblooms.
    public static final DeferredBlock<Block> CLOUDMOSS_BLOCK = BLOCKS.registerBlock("cloudmoss_block",
            net.minecraft.world.level.block.Block::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.MOSS_BLOCK).mapColor(MapColor.SNOW));
    public static final DeferredBlock<net.minecraft.world.level.block.CarpetBlock> CLOUDMOSS_CARPET = BLOCKS.registerBlock("cloudmoss_carpet",
            net.minecraft.world.level.block.CarpetBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.MOSS_CARPET).mapColor(MapColor.SNOW));
    public static final DeferredBlock<net.minecraft.world.level.block.FlowerBlock> PUFFBLOOM = BLOCKS.registerBlock("puffbloom",
            p -> new net.minecraft.world.level.block.FlowerBlock(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 6, p),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.POPPY).lightLevel(s -> 9));
    // Voltcap Mushroom Fields: spongy mycelium ground and glowing mushrooms, small and giant.
    public static final DeferredBlock<Block> VOLTCAP_MYCELIUM = BLOCKS.registerBlock("voltcap_mycelium",
            net.minecraft.world.level.block.Block::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DIRT).mapColor(MapColor.COLOR_CYAN).sound(SoundType.NYLIUM));
    public static final DeferredBlock<net.minecraft.world.level.block.MushroomBlock> VOLTCAP = BLOCKS.registerBlock("voltcap",
            p -> new net.minecraft.world.level.block.MushroomBlock(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "huge_voltcap")), p),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 8));
    public static final DeferredBlock<net.minecraft.world.level.block.HugeMushroomBlock> VOLTCAP_BLOCK = BLOCKS.registerBlock("voltcap_block",
            net.minecraft.world.level.block.HugeMushroomBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 12));
    public static final DeferredBlock<net.minecraft.world.level.block.HugeMushroomBlock> VOLTCAP_STEM = BLOCKS.registerBlock("voltcap_stem",
            net.minecraft.world.level.block.HugeMushroomBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.MUSHROOM_STEM).mapColor(MapColor.COLOR_LIGHT_GRAY));
    // Ion Lakes: glowing bulbs on the lake floor (they only live underwater).
    // Titan Altar (Stormreach arenas): summons the Storm Titan. Can't be broken.
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.titan.TitanAltarBlock> TITAN_ALTAR = BLOCKS.registerBlock("titan_altar",
            com.sideflowgames.fulguritefusion.titan.TitanAltarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1.0f, 3600000.0f).noLootTable()
                    .lightLevel(s -> 10).sound(SoundType.DEEPSLATE_BRICKS).noOcclusion());
    // Stormreach rift: a frame of Rift Frame blocks with a Rift Keystone in its top edge and a Lightning Rod on the
    // keystone. Lightning striking the rod opens the rift (see rift/RiftPortals).
    public static final DeferredBlock<Block> RIFT_FRAME = BLOCKS.registerSimpleBlock("rift_frame",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(8.0f, 1200.0f)
                    .sound(SoundType.DEEPSLATE_BRICKS).lightLevel(s -> 3));
    public static final DeferredBlock<Block> RIFT_KEYSTONE = BLOCKS.registerSimpleBlock("rift_keystone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(8.0f, 1200.0f)
                    .sound(SoundType.AMETHYST).lightLevel(s -> 9));
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.rift.RiftPortalBlock> RIFT_PORTAL = BLOCKS.registerBlock("rift_portal",
            com.sideflowgames.fulguritefusion.rift.RiftPortalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission().strength(-1.0f, 3600000.0f).noLootTable()
                    .lightLevel(s -> 12).sound(SoundType.GLASS).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.stormreach.IonBulbBlock> ION_BULB = BLOCKS.registerBlock("ion_bulb",
            com.sideflowgames.fulguritefusion.stormreach.IonBulbBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SEAGRASS).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 12).sound(SoundType.WET_GRASS));

    // ---------- Exotic material sources ----------
    // Sky Collector: Aurora Dust on clear high nights, Sunmote at clear desert/badlands middays.
    public static final DeferredBlock<SkyCollectorBlock> SKY_COLLECTOR = BLOCKS.registerBlock("sky_collector",
            SkyCollectorBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion().lightLevel(SkyCollectorBlock::light));
    // Abyssal Nacre Cluster: grows on prismarine near an active Conduit in a deep ocean.
    public static final DeferredBlock<NacreClusterBlock> NACRE_CLUSTER = BLOCKS.registerBlock("abyssal_nacre_cluster",
            NacreClusterBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5f)
                    .sound(SoundType.AMETHYST_CLUSTER).noOcclusion().lightLevel(state -> 5).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    // Frozen Soul Fire: soul fire smothered with powder snow; breaks into Soul Frost.
    public static final DeferredBlock<FrozenSoulFireBlock> FROZEN_SOUL_FIRE = BLOCKS.registerBlock("frozen_soul_fire",
            FrozenSoulFireBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(0.5f)
                    .sound(SoundType.GLASS).noOcclusion().lightLevel(state -> 7));

    // ---------- Backpacks (placed in the world; see backpack/) ----------
    private static DeferredBlock<com.sideflowgames.fulguritefusion.backpack.BackpackBlock> backpack(com.sideflowgames.fulguritefusion.backpack.BackpackTier tier) {
        return BLOCKS.registerBlock(tier.id + "_backpack", p -> new com.sideflowgames.fulguritefusion.backpack.BackpackBlock(tier, p),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5f).sound(SoundType.WOOL).noOcclusion());
    }
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.backpack.BackpackBlock> COPPER_BACKPACK = backpack(com.sideflowgames.fulguritefusion.backpack.BackpackTier.COPPER);
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.backpack.BackpackBlock> IRON_BACKPACK = backpack(com.sideflowgames.fulguritefusion.backpack.BackpackTier.IRON);
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.backpack.BackpackBlock> FULGURITE_BACKPACK = backpack(com.sideflowgames.fulguritefusion.backpack.BackpackTier.FULGURITE);
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.backpack.BackpackBlock> STORMFORGED_BACKPACK = backpack(com.sideflowgames.fulguritefusion.backpack.BackpackTier.STORMFORGED);
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.backpack.BackpackBlock> PRIME_BACKPACK = backpack(com.sideflowgames.fulguritefusion.backpack.BackpackTier.PRIME);

    // Storm Forge: the Forge Anvil core and its power port (see StormForgeBlockEntity).
    public static final DeferredBlock<StormForgeBlock> STORM_FORGE = BLOCKS.registerBlock("storm_forge",
            StormForgeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(5.0f, 1200f).requiresCorrectToolForDrops().sound(SoundType.ANVIL).noOcclusion());
    // Titan Forge: fuses Prime and Titan armor into Titan Prime (block/TitanForgeBlockEntity).
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.block.TitanForgeBlock> TITAN_FORGE = BLOCKS.registerBlock("titan_forge",
            com.sideflowgames.fulguritefusion.block.TitanForgeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(8.0f, 1200f).requiresCorrectToolForDrops().sound(SoundType.ANVIL).noOcclusion().lightLevel(s -> 6));
    public static final DeferredBlock<ForgePowerPortBlock> FORGE_POWER_PORT = BLOCKS.registerBlock("forge_power_port",
            ForgePowerPortBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK));

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
    public static final DeferredBlock<MachineControllerBlock> BANBURY_MIXER = controller("banbury_mixer", MachineType.BANBURY_MIXER);
    public static final DeferredBlock<MachineControllerBlock> LATEX_VULCANIZER = controller("latex_vulcanizer", MachineType.LATEX_VULCANIZER);

    // ---------- Rubber trees ----------
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> RUBBER_LOG = BLOCKS.registerBlock("rubber_log",
            net.minecraft.world.level.block.RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.JUNGLE_LOG).mapColor(MapColor.TERRACOTTA_BROWN));
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> RUBBER_WOOD = BLOCKS.registerBlock("rubber_wood",
            net.minecraft.world.level.block.RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.JUNGLE_WOOD).mapColor(MapColor.TERRACOTTA_BROWN));
    public static final DeferredBlock<Block> RUBBER_PLANKS = BLOCKS.registerSimpleBlock("rubber_planks",
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.JUNGLE_PLANKS).mapColor(MapColor.TERRACOTTA_WHITE));
    public static final DeferredBlock<net.minecraft.world.level.block.LeavesBlock> RUBBER_LEAVES = BLOCKS.registerBlock("rubber_leaves",
            net.minecraft.world.level.block.LeavesBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.JUNGLE_LEAVES));
    public static final DeferredBlock<net.minecraft.world.level.block.SaplingBlock> RUBBER_SAPLING = BLOCKS.registerBlock("rubber_sapling",
            props -> new net.minecraft.world.level.block.SaplingBlock(com.sideflowgames.fulguritefusion.block.rubber.RubberTree.GROWER, props),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.JUNGLE_SAPLING));
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.block.rubber.TreeTapBlock> TREE_TAP = BLOCKS.registerBlock("tree_tap",
            com.sideflowgames.fulguritefusion.block.rubber.TreeTapBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f).sound(SoundType.LANTERN).noOcclusion());
    // Latex as a placed fluid (from a Latex Bucket).
    public static final DeferredBlock<net.minecraft.world.level.block.LiquidBlock> LATEX = BLOCKS.register("latex",
            () -> new net.minecraft.world.level.block.LiquidBlock(ModFluids.LATEX.get(),
                    BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER).mapColor(MapColor.SNOW)));
    // A rubber log a Latex Harvester has drained dry: still wood, gives no latex.
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> DRIED_RUBBER_LOG = BLOCKS.registerBlock("dried_rubber_log",
            net.minecraft.world.level.block.RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.JUNGLE_LOG).mapColor(MapColor.TERRACOTTA_GRAY));
    // ---------- Latex Harvester: a ring of 8 blocks around the base of a rubber tree ----------
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.harvester.LatexHarvesterBlock> LATEX_HARVESTER = BLOCKS.registerBlock("latex_harvester",
            com.sideflowgames.fulguritefusion.harvester.LatexHarvesterBlock::new, machineProperties());
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.harvester.HarvesterTankBlock> HARVESTER_TANK = BLOCKS.registerBlock("harvester_tank",
            com.sideflowgames.fulguritefusion.harvester.HarvesterTankBlock::new, machineProperties());
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.harvester.HarvesterPowerPortBlock> HARVESTER_POWER_PORT = BLOCKS.registerBlock("harvester_power_port",
            com.sideflowgames.fulguritefusion.harvester.HarvesterPowerPortBlock::new, machineProperties());
    // A bucket set down on the ground to catch latex (no item of its own: it's placed with a vanilla bucket).
    public static final DeferredBlock<com.sideflowgames.fulguritefusion.block.rubber.TapBucketBlock> TAP_BUCKET = BLOCKS.registerBlock("tap_bucket",
            com.sideflowgames.fulguritefusion.block.rubber.TapBucketBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.LANTERN).noOcclusion());

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
    public static final DeferredItem<BlockItem> FLUID_PIPE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fluid_pipe", FLUID_PIPE);
    public static final DeferredItem<BlockItem> STORMSTONE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormstone", STORMSTONE);
    public static final DeferredItem<BlockItem> COBBLED_STORMSTONE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("cobbled_stormstone", COBBLED_STORMSTONE);
    public static final DeferredItem<BlockItem> POLISHED_STORMSTONE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("polished_stormstone", POLISHED_STORMSTONE);
    public static final DeferredItem<BlockItem> STORMSTONE_BRICKS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormstone_bricks", STORMSTONE_BRICKS);
    public static final DeferredItem<BlockItem> FULGURITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fulgurite_ore", FULGURITE_ORE);
    public static final DeferredItem<BlockItem> TEMPEST_CRYSTAL_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("tempest_crystal_ore", TEMPEST_CRYSTAL_ORE);
    public static final DeferredItem<BlockItem> CHARGED_STORMSAND_ITEM = ModItems.ITEMS.registerSimpleBlockItem("charged_stormsand", CHARGED_STORMSAND);
    public static final DeferredItem<BlockItem> FULGURITE_GLASS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fulgurite_glass", FULGURITE_GLASS);
    public static final DeferredItem<BlockItem> CHARGED_LOAM_ITEM = ModItems.ITEMS.registerSimpleBlockItem("charged_loam", CHARGED_LOAM);
    public static final DeferredItem<BlockItem> ARCGRASS_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("arcgrass_block", ARCGRASS_BLOCK);
    public static final DeferredItem<BlockItem> CONDUCTOR_LOG_ITEM = ModItems.ITEMS.registerSimpleBlockItem("conductor_log", CONDUCTOR_LOG);
    public static final DeferredItem<BlockItem> CONDUCTOR_PLANKS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("conductor_planks", CONDUCTOR_PLANKS);
    public static final DeferredItem<BlockItem> CONDUCTOR_LEAVES_ITEM = ModItems.ITEMS.registerSimpleBlockItem("conductor_leaves", CONDUCTOR_LEAVES);
    public static final DeferredItem<BlockItem> CONDUCTOR_SAPLING_ITEM = ModItems.ITEMS.registerSimpleBlockItem("conductor_sapling", CONDUCTOR_SAPLING);
    public static final DeferredItem<BlockItem> THUNDEROAK_LOG_ITEM = ModItems.ITEMS.registerSimpleBlockItem("thunderoak_log", THUNDEROAK_LOG);
    public static final DeferredItem<BlockItem> THUNDEROAK_PLANKS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("thunderoak_planks", THUNDEROAK_PLANKS);
    public static final DeferredItem<BlockItem> THUNDEROAK_LEAVES_ITEM = ModItems.ITEMS.registerSimpleBlockItem("thunderoak_leaves", THUNDEROAK_LEAVES);
    public static final DeferredItem<BlockItem> THUNDEROAK_SAPLING_ITEM = ModItems.ITEMS.registerSimpleBlockItem("thunderoak_sapling", THUNDEROAK_SAPLING);
    public static final DeferredItem<BlockItem> SPARKBUSH_LOG_ITEM = ModItems.ITEMS.registerSimpleBlockItem("sparkbush_log", SPARKBUSH_LOG);
    public static final DeferredItem<BlockItem> SPARKBUSH_PLANKS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("sparkbush_planks", SPARKBUSH_PLANKS);
    public static final DeferredItem<BlockItem> SPARKBUSH_LEAVES_ITEM = ModItems.ITEMS.registerSimpleBlockItem("sparkbush_leaves", SPARKBUSH_LEAVES);
    public static final DeferredItem<BlockItem> SPARKBUSH_SAPLING_ITEM = ModItems.ITEMS.registerSimpleBlockItem("sparkbush_sapling", SPARKBUSH_SAPLING);
    public static final DeferredItem<BlockItem> ARCGRASS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("arcgrass", ARCGRASS);
    public static final DeferredItem<BlockItem> ARC_FERN_ITEM = ModItems.ITEMS.registerSimpleBlockItem("arc_fern", ARC_FERN);
    public static final DeferredItem<BlockItem> TALL_ARCGRASS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("tall_arcgrass", TALL_ARCGRASS);
    public static final DeferredItem<BlockItem> VOLTBLOOM_ITEM = ModItems.ITEMS.registerSimpleBlockItem("voltbloom", VOLTBLOOM);
    public static final DeferredItem<BlockItem> STORMCAP_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormcap", STORMCAP);
    public static final DeferredItem<BlockItem> AURORA_LILY_ITEM = ModItems.ITEMS.registerSimpleBlockItem("aurora_lily", AURORA_LILY);
    public static final DeferredItem<BlockItem> STATIC_REEDS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("static_reeds", STATIC_REEDS);
    public static final DeferredItem<BlockItem> CRYSTAL_SPROUTS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("crystal_sprouts", CRYSTAL_SPROUTS);
    public static final DeferredItem<BlockItem> CLOUDMOSS_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("cloudmoss_block", CLOUDMOSS_BLOCK);
    public static final DeferredItem<BlockItem> CLOUDMOSS_CARPET_ITEM = ModItems.ITEMS.registerSimpleBlockItem("cloudmoss_carpet", CLOUDMOSS_CARPET);
    public static final DeferredItem<BlockItem> PUFFBLOOM_ITEM = ModItems.ITEMS.registerSimpleBlockItem("puffbloom", PUFFBLOOM);
    public static final DeferredItem<BlockItem> VOLTCAP_MYCELIUM_ITEM = ModItems.ITEMS.registerSimpleBlockItem("voltcap_mycelium", VOLTCAP_MYCELIUM);
    public static final DeferredItem<BlockItem> VOLTCAP_ITEM = ModItems.ITEMS.registerSimpleBlockItem("voltcap", VOLTCAP);
    public static final DeferredItem<BlockItem> VOLTCAP_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("voltcap_block", VOLTCAP_BLOCK);
    public static final DeferredItem<BlockItem> VOLTCAP_STEM_ITEM = ModItems.ITEMS.registerSimpleBlockItem("voltcap_stem", VOLTCAP_STEM);
    public static final DeferredItem<BlockItem> ION_BULB_ITEM = ModItems.ITEMS.registerSimpleBlockItem("ion_bulb", ION_BULB);
    public static final DeferredItem<BlockItem> DEEP_STORMSTONE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("deep_stormstone", DEEP_STORMSTONE);
    public static final DeferredItem<BlockItem> RIMESTONE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("rimestone", RIMESTONE);
    public static final DeferredItem<BlockItem> STORMSLATE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormslate", STORMSLATE);
    public static final DeferredItem<BlockItem> MAGNETITE_ROCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("magnetite_rock", MAGNETITE_ROCK);
    public static final DeferredItem<BlockItem> FULGURITE_CRUST_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fulgurite_crust", FULGURITE_CRUST);
    public static final DeferredItem<BlockItem> CINDER_ROCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("cinder_rock", CINDER_ROCK);
    public static final DeferredItem<BlockItem> STORMSANDSTONE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormsandstone", STORMSANDSTONE);
    public static final DeferredItem<BlockItem> AURORA_ICE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("aurora_ice", AURORA_ICE);
    public static final DeferredItem<BlockItem> PETRIFIED_LOG_ITEM = ModItems.ITEMS.registerSimpleBlockItem("petrified_log", PETRIFIED_LOG);
    public static final DeferredItem<BlockItem> ASH_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("ash_block", ASH_BLOCK);
    public static final DeferredItem<BlockItem> SCORCHED_SAND_ITEM = ModItems.ITEMS.registerSimpleBlockItem("scorched_sand", SCORCHED_SAND);
    public static final DeferredItem<BlockItem> STORMGLASS_SAND_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormglass_sand", STORMGLASS_SAND);
    public static final DeferredItem<BlockItem> STORMSTONE_MAGNETITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormstone_magnetite_ore", STORMSTONE_MAGNETITE_ORE);
    public static final DeferredItem<BlockItem> STORMSTONE_ARGENTITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormstone_argentite_ore", STORMSTONE_ARGENTITE_ORE);
    public static final DeferredItem<BlockItem> CINDERITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("cinderite_ore", CINDERITE_ORE);
    public static final DeferredItem<BlockItem> PRISMITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("prismite_ore", PRISMITE_ORE);
    public static final DeferredItem<BlockItem> STORMQUARTZ_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormquartz_ore", STORMQUARTZ_ORE);
    public static final DeferredItem<BlockItem> AURORITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("aurorite_ore", AURORITE_ORE);
    public static final DeferredItem<BlockItem> AMBER_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("amber_ore", AMBER_ORE);
    public static final DeferredItem<BlockItem> MAGNETITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("magnetite_ore", MAGNETITE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_MAGNETITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("deepslate_magnetite_ore", DEEPSLATE_MAGNETITE_ORE);
    public static final DeferredItem<BlockItem> ARGENTITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("argentite_ore", ARGENTITE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_ARGENTITE_ORE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("deepslate_argentite_ore", DEEPSLATE_ARGENTITE_ORE);
    public static final DeferredItem<BlockItem> MAGNETITE_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("magnetite_block", MAGNETITE_BLOCK);
    public static final DeferredItem<BlockItem> SILVER_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("silver_block", SILVER_BLOCK);
    public static final DeferredItem<BlockItem> CINDERITE_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("cinderite_block", CINDERITE_BLOCK);
    public static final DeferredItem<BlockItem> PRISMITE_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("prismite_block", PRISMITE_BLOCK);
    public static final DeferredItem<BlockItem> STORMQUARTZ_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormquartz_block", STORMQUARTZ_BLOCK);
    public static final DeferredItem<BlockItem> AURORITE_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("aurorite_block", AURORITE_BLOCK);
    public static final DeferredItem<BlockItem> AMBER_BLOCK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("amber_block", AMBER_BLOCK);
    public static final DeferredItem<BlockItem> TITAN_ALTAR_ITEM = ModItems.ITEMS.registerSimpleBlockItem("titan_altar", TITAN_ALTAR);
    public static final DeferredItem<BlockItem> RIFT_FRAME_ITEM = ModItems.ITEMS.registerSimpleBlockItem("rift_frame", RIFT_FRAME);
    public static final DeferredItem<BlockItem> RIFT_KEYSTONE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("rift_keystone", RIFT_KEYSTONE,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC));
    public static final DeferredItem<BlockItem> SKY_COLLECTOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem("sky_collector", SKY_COLLECTOR);
    public static final DeferredItem<BlockItem> NACRE_CLUSTER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("abyssal_nacre_cluster", NACRE_CLUSTER);
    public static final DeferredItem<BlockItem> FROZEN_SOUL_FIRE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("frozen_soul_fire", FROZEN_SOUL_FIRE);
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackItem> COPPER_BACKPACK_ITEM = ModItems.ITEMS.register("copper_backpack",
            () -> new com.sideflowgames.fulguritefusion.backpack.BackpackItem(COPPER_BACKPACK.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackItem> IRON_BACKPACK_ITEM = ModItems.ITEMS.register("iron_backpack",
            () -> new com.sideflowgames.fulguritefusion.backpack.BackpackItem(IRON_BACKPACK.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackItem> FULGURITE_BACKPACK_ITEM = ModItems.ITEMS.register("fulgurite_backpack",
            () -> new com.sideflowgames.fulguritefusion.backpack.BackpackItem(FULGURITE_BACKPACK.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackItem> STORMFORGED_BACKPACK_ITEM = ModItems.ITEMS.register("stormforged_backpack",
            () -> new com.sideflowgames.fulguritefusion.backpack.StormBackpackItem(STORMFORGED_BACKPACK.get(), new Item.Properties().stacksTo(1).fireResistant()));
    public static final DeferredItem<com.sideflowgames.fulguritefusion.backpack.BackpackItem> PRIME_BACKPACK_ITEM = ModItems.ITEMS.register("prime_backpack",
            () -> new com.sideflowgames.fulguritefusion.backpack.StormBackpackItem(PRIME_BACKPACK.get(), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> STORM_FORGE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("storm_forge", STORM_FORGE);
    public static final DeferredItem<BlockItem> TITAN_FORGE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("titan_forge", TITAN_FORGE,
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> FORGE_POWER_PORT_ITEM = ModItems.ITEMS.registerSimpleBlockItem("forge_power_port", FORGE_POWER_PORT);
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
    public static final DeferredItem<BlockItem> BANBURY_MIXER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("banbury_mixer", BANBURY_MIXER);
    public static final DeferredItem<BlockItem> LATEX_VULCANIZER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("latex_vulcanizer", LATEX_VULCANIZER);
    public static final DeferredItem<BlockItem> RUBBER_LOG_ITEM = ModItems.ITEMS.registerSimpleBlockItem("rubber_log", RUBBER_LOG);
    public static final DeferredItem<BlockItem> RUBBER_WOOD_ITEM = ModItems.ITEMS.registerSimpleBlockItem("rubber_wood", RUBBER_WOOD);
    public static final DeferredItem<BlockItem> RUBBER_PLANKS_ITEM = ModItems.ITEMS.registerSimpleBlockItem("rubber_planks", RUBBER_PLANKS);
    public static final DeferredItem<BlockItem> RUBBER_LEAVES_ITEM = ModItems.ITEMS.registerSimpleBlockItem("rubber_leaves", RUBBER_LEAVES);
    public static final DeferredItem<BlockItem> RUBBER_SAPLING_ITEM = ModItems.ITEMS.registerSimpleBlockItem("rubber_sapling", RUBBER_SAPLING);
    public static final DeferredItem<BlockItem> TREE_TAP_ITEM = ModItems.ITEMS.registerSimpleBlockItem("tree_tap", TREE_TAP);
    public static final DeferredItem<BlockItem> DRIED_RUBBER_LOG_ITEM = ModItems.ITEMS.registerSimpleBlockItem("dried_rubber_log", DRIED_RUBBER_LOG);
    public static final DeferredItem<BlockItem> LATEX_HARVESTER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("latex_harvester", LATEX_HARVESTER);
    public static final DeferredItem<BlockItem> HARVESTER_TANK_ITEM = ModItems.ITEMS.registerSimpleBlockItem("harvester_tank", HARVESTER_TANK);
    public static final DeferredItem<BlockItem> HARVESTER_POWER_PORT_ITEM = ModItems.ITEMS.registerSimpleBlockItem("harvester_power_port", HARVESTER_POWER_PORT);
    public static final DeferredItem<BlockItem> STORM_COLLIDER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("storm_collider", STORM_COLLIDER);

    public static final Supplier<BlockEntityType<FusionColliderBlockEntity>> FUSION_COLLIDER_BE = BLOCK_ENTITIES.register("fusion_collider",
            () -> BlockEntityType.Builder.of(FusionColliderBlockEntity::new, FUSION_COLLIDER.get()).build(null));
    public static final Supplier<BlockEntityType<ReactorConsoleBlockEntity>> REACTOR_CONSOLE_BE = BLOCK_ENTITIES.register("reactor_console",
            () -> BlockEntityType.Builder.of(ReactorConsoleBlockEntity::new, REACTOR_CONSOLE.get()).build(null));
    public static final Supplier<BlockEntityType<com.sideflowgames.fulguritefusion.pipe.FluidPipeBlockEntity>> FLUID_PIPE_BE = BLOCK_ENTITIES.register("fluid_pipe",
            () -> BlockEntityType.Builder.of(com.sideflowgames.fulguritefusion.pipe.FluidPipeBlockEntity::new, FLUID_PIPE.get()).build(null));
    public static final Supplier<BlockEntityType<DataCableBlockEntity>> DATA_CABLE_BE = BLOCK_ENTITIES.register("data_cable",
            () -> BlockEntityType.Builder.of(DataCableBlockEntity::new, DATA_CABLE.get()).build(null));
    public static final Supplier<BlockEntityType<SkyCollectorBlockEntity>> SKY_COLLECTOR_BE = BLOCK_ENTITIES.register("sky_collector",
            () -> BlockEntityType.Builder.of(SkyCollectorBlockEntity::new, SKY_COLLECTOR.get()).build(null));
    public static final Supplier<BlockEntityType<com.sideflowgames.fulguritefusion.backpack.BackpackBlockEntity>> BACKPACK_BE = BLOCK_ENTITIES.register("backpack",
            () -> BlockEntityType.Builder.of(com.sideflowgames.fulguritefusion.backpack.BackpackBlockEntity::new, COPPER_BACKPACK.get(), IRON_BACKPACK.get(),
                    FULGURITE_BACKPACK.get(), STORMFORGED_BACKPACK.get(), PRIME_BACKPACK.get()).build(null));

    // Recipe serializers.
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER, FulguriteFusion.MODID);
    public static final Supplier<com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeRecipe.Serializer> BACKPACK_UPGRADE =
            RECIPE_SERIALIZERS.register("backpack_upgrade", com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeRecipe.Serializer::new);

    public static final Supplier<BlockEntityType<StormForgeBlockEntity>> STORM_FORGE_BE = BLOCK_ENTITIES.register("storm_forge",
            () -> BlockEntityType.Builder.of(StormForgeBlockEntity::new, STORM_FORGE.get()).build(null));
    public static final Supplier<BlockEntityType<com.sideflowgames.fulguritefusion.block.TitanForgeBlockEntity>> TITAN_FORGE_BE = BLOCK_ENTITIES.register("titan_forge",
            () -> BlockEntityType.Builder.of(com.sideflowgames.fulguritefusion.block.TitanForgeBlockEntity::new, TITAN_FORGE.get()).build(null));
    public static final Supplier<BlockEntityType<ForgePowerPortBlockEntity>> FORGE_POWER_PORT_BE = BLOCK_ENTITIES.register("forge_power_port",
            () -> BlockEntityType.Builder.of(ForgePowerPortBlockEntity::new, FORGE_POWER_PORT.get()).build(null));
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
                    WIRE_EXTRUDER.get(), KILN.get(), CRUSHER.get(), BANBURY_MIXER.get(), LATEX_VULCANIZER.get()).build(null));

    public static final Supplier<BlockEntityType<com.sideflowgames.fulguritefusion.harvester.LatexHarvesterBlockEntity>> LATEX_HARVESTER_BE = BLOCK_ENTITIES.register("latex_harvester",
            () -> BlockEntityType.Builder.of(com.sideflowgames.fulguritefusion.harvester.LatexHarvesterBlockEntity::new, LATEX_HARVESTER.get()).build(null));
    public static final Supplier<BlockEntityType<com.sideflowgames.fulguritefusion.harvester.HarvesterPartBlockEntity>> HARVESTER_PART_BE = BLOCK_ENTITIES.register("harvester_part",
            () -> BlockEntityType.Builder.of(com.sideflowgames.fulguritefusion.harvester.HarvesterPartBlockEntity::new,
                    HARVESTER_TANK.get(), HARVESTER_POWER_PORT.get()).build(null));

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
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
