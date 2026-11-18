package com.sideflowgames.fulguritefusion;

import java.util.function.Supplier;

import com.sideflowgames.fulguritefusion.block.FulguriteChargerBlock;
import com.sideflowgames.fulguritefusion.block.FulguriteChargerBlockEntity;
import com.sideflowgames.fulguritefusion.block.StormFurnaceBlock;
import com.sideflowgames.fulguritefusion.block.StormFurnaceBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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

    // A furnace struck by lightning. Burns fuel for Fm; can no longer smelt.
    public static final DeferredBlock<StormFurnaceBlock> STORM_FURNACE = BLOCKS.registerBlock("storm_furnace",
            StormFurnaceBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.5f)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(StormFurnaceBlock.LIT) ? 13 : 0));

    // Fills Empty Spark Cells with Fm.
    public static final DeferredBlock<FulguriteChargerBlock> FULGURITE_CHARGER = BLOCKS.registerBlock("fulgurite_charger",
            FulguriteChargerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL));

    public static final DeferredItem<BlockItem> RAW_FULGURITE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("raw_fulgurite", RAW_FULGURITE);
    public static final DeferredItem<BlockItem> STORM_FURNACE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("storm_furnace", STORM_FURNACE);
    public static final DeferredItem<BlockItem> FULGURITE_CHARGER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fulgurite_charger", FULGURITE_CHARGER);

    public static final Supplier<BlockEntityType<StormFurnaceBlockEntity>> STORM_FURNACE_BE = BLOCK_ENTITIES.register("storm_furnace",
            () -> new BlockEntityType<>(StormFurnaceBlockEntity::new, STORM_FURNACE.get()));

    public static final Supplier<BlockEntityType<FulguriteChargerBlockEntity>> FULGURITE_CHARGER_BE = BLOCK_ENTITIES.register("fulgurite_charger",
            () -> new BlockEntityType<>(FulguriteChargerBlockEntity::new, FULGURITE_CHARGER.get()));

    private ModBlocks() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }
}
