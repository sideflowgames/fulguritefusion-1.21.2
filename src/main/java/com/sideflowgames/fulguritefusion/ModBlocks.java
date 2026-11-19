package com.sideflowgames.fulguritefusion;

import java.util.function.Supplier;

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
    public static final DeferredBlock<TransparentBlock> FULGURITE_RING = BLOCKS.registerBlock("fulgurite_ring",
            TransparentBlock::new,
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

    // Stormwing Nest: a 3x3 nest of twigs a Stormwing builds where it lives. Placeholder until nests are designed.
    public static final DeferredBlock<Block> STORMWING_NEST = BLOCKS.registerSimpleBlock("stormwing_nest",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.GRASS).ignitedByLava());

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
    public static final DeferredItem<BlockItem> MINI_COLLIDER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("mini_collider", MINI_COLLIDER);
    public static final DeferredItem<BlockItem> STORMWING_NEST_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormwing_nest", STORMWING_NEST);
    public static final DeferredItem<BlockItem> STORM_COLLIDER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("storm_collider", STORM_COLLIDER);

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
