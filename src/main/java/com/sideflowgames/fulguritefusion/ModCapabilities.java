package com.sideflowgames.fulguritefusion;

import com.sideflowgames.fulguritefusion.util.LegacyCaps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// Lets hoppers, other mods' pipes and FE cables connect to our machines.
// Hoppers and other mods' pipes insert/extract items, following each face's None/Pull/Push setting.
// 26.1: the machines keep their old item/fluid handlers; LegacyCaps offers them through the new capabilities.
public final class ModCapabilities {
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.STORM_FURNACE_BE.get(),
                (be, side) -> LegacyCaps.items(be.getItemHandler(side), be.capStorage()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.FULGURITE_CHARGER_BE.get(),
                (be, side) -> LegacyCaps.items(be.getItemHandler(side), be.capStorage()));
        // Hoppers and pipes can load Spark Cells into the Collider Input (insert only).
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.COLLIDER_INPUT_BE.get(),
                (be, side) -> LegacyCaps.items(be.getItemHandler(side), be.capStorage()));
        // Injectors take materials in; the Storm Collider gives products out of its bottom face.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.COLLIDER_INJECTOR_BE.get(),
                (be, side) -> LegacyCaps.items(be.getItemHandler(side), be.capStorage()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.STORM_COLLIDER_BE.get(),
                (be, side) -> LegacyCaps.items(be.getItemHandler(side), be.capStorage()));
        // Multiblock machines: items in and out only through their input and output port casings.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.ARC_CASING_BE.get(),
                (be, side) -> LegacyCaps.items(be.getItemHandler(side), be.capStorage()));
        // Sky Collector: hoppers and pipes can take what it gathered.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.SKY_COLLECTOR_BE.get(),
                (be, side) -> LegacyCaps.items(be.getOutput(), be.capStorage()));
        // Fusion Reactor: hoppers and pipes load fuel into a Console Fuel Input (front or back).
        event.registerBlock(Capabilities.Item.BLOCK,
                (level, pos, state, be, side) -> LegacyCaps.items(
                        com.sideflowgames.fulguritefusion.block.ReactorConsoleBlockEntity.fuelInputHandler(level, pos),
                        com.sideflowgames.fulguritefusion.block.ReactorConsoleBlockEntity.fuelInputStorage(level, pos)),
                ModBlocks.CONSOLE_FUEL_INPUT.get());
        // Latex Harvester: pipes (and buckets) drain latex from its tank corner.
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlocks.HARVESTER_PART_BE.get(),
                (be, side) -> LegacyCaps.fluids(be.fluidHandler(), be.fluidStorage()));
        // Latex Vulcanizer: pipes and buckets fill it through any of its casings (not from the front).
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlocks.ARC_CASING_BE.get(),
                (be, side) -> LegacyCaps.fluids(be.getFluidHandler(side), be.fluidStorage()));
        // Shock Farm: hoppers and pipes pull drops out of a Farm Collector.
        event.registerBlockEntity(Capabilities.Item.BLOCK, com.sideflowgames.fulguritefusion.farm.FarmBlocks.COLLECTOR_BE.get(),
                (be, side) -> LegacyCaps.items(new net.neoforged.neoforge.items.wrapper.InvWrapper(be)));
        // FE cables can pull Forge Energy from the FE Converter's front face.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlocks.FE_CONVERTER_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
    }
}
