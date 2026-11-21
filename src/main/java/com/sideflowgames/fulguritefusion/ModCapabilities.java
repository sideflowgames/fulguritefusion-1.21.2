package com.sideflowgames.fulguritefusion;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// Lets hoppers, other mods' pipes and FE cables connect to our machines.
// Hoppers and other mods' pipes insert/extract items, following each face's None/Pull/Push setting.
public final class ModCapabilities {
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlocks.STORM_FURNACE_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlocks.FULGURITE_CHARGER_BE.get(),
                (be, side) -> be.getItemHandler(side));
        // Hoppers and pipes can load Spark Cells into the Collider Input (insert only).
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlocks.COLLIDER_INPUT_BE.get(),
                (be, side) -> be.getItemHandler(side));
        // Injectors take materials in; the Storm Collider gives products out of its bottom face.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlocks.COLLIDER_INJECTOR_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlocks.STORM_COLLIDER_BE.get(),
                (be, side) -> be.getItemHandler(side));
        // Multiblock machines: items in and out only through their input and output port casings.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlocks.ARC_CASING_BE.get(),
                (be, side) -> be.getItemHandler(side));
        // Sky Collector: hoppers and pipes can take what it gathered.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlocks.SKY_COLLECTOR_BE.get(), (be, side) -> be.getOutput());
        // Fusion Reactor: hoppers and pipes load fuel into a Console Fuel Input (front or back).
        event.registerBlock(Capabilities.ItemHandler.BLOCK,
                (level, pos, state, be, side) -> com.sideflowgames.fulguritefusion.block.ReactorConsoleBlockEntity.fuelInputHandler(level, pos),
                ModBlocks.CONSOLE_FUEL_INPUT.get());
        // FE cables can pull Forge Energy from the FE Converter's front face.
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlocks.FE_CONVERTER_BE.get(),
                (be, side) -> be.getEnergyHandler(side));
    }
}
