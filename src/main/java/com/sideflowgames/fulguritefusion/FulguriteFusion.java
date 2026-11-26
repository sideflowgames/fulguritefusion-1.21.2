package com.sideflowgames.fulguritefusion;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.sideflowgames.fulguritefusion.pairing.PairingDiscoveries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(FulguriteFusion.MODID)
public class FulguriteFusion {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "fulguritefusion";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    public FulguriteFusion(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // Register our blocks (ModBlocks.java), then items and the creative tab (ModItems.java)
        ModFluids.register(modEventBus);
        ModParticles.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModArmorMaterials.register(modEventBus);
        ModMenus.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModFeatures.register(modEventBus);
        com.sideflowgames.fulguritefusion.dungeon.DungeonRegistry.register(modEventBus);
        com.sideflowgames.fulguritefusion.brew.ModBrews.register(modEventBus);
        com.sideflowgames.fulguritefusion.transport.TransportBlocks.register(modEventBus);

        // Hopper and pipe support for our machines
        modEventBus.addListener(ModCapabilities::register);

        // Which pairings each player has discovered (hides fuel yields and side effects until then)
        PairingDiscoveries.register(modEventBus);

        // Stormwing riding: the dive key packet, and no falling off mid-air when Shift is held to descend
        com.sideflowgames.fulguritefusion.entity.StormwingRiding.register(modEventBus);
        // Backpacks: the open-backpack key packet
        com.sideflowgames.fulguritefusion.backpack.BackpackNetwork.register(modEventBus);
        com.sideflowgames.fulguritefusion.prime.PrimeNetwork.register(modEventBus);
        com.sideflowgames.fulguritefusion.titan.TitanNetwork.register(modEventBus);
        com.sideflowgames.fulguritefusion.trinket.Trinkets.register(modEventBus);
        com.sideflowgames.fulguritefusion.item.ChainBolt.register(modEventBus);
        com.sideflowgames.fulguritefusion.stormreach.StormreachStorm.register(modEventBus);
        // 26.1: send the crafting recipes to clients too, for the guide book's recipe grids.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent event) ->
                event.sendRecipes(net.minecraft.world.item.crafting.RecipeType.CRAFTING));
        com.sideflowgames.fulguritefusion.stormreach.Stormreach.register();
        com.sideflowgames.fulguritefusion.titan.TitanTrials.register(modEventBus);
        com.sideflowgames.fulguritefusion.rift.RiftPortals.register();
        // Guide map: which topics each player has unlocked
        com.sideflowgames.fulguritefusion.guide.GuideProgress.register(modEventBus);

        // Lets this class listen for game events, like onServerStarting below
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's config so FML creates and loads the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Fulgurite Fusion: common setup");
        event.enqueueWork(() -> {
            // Rubber wood burns like any other wood.
            // FireBlock.setFlammable is private in 26.1, so it is called through reflection (NeoForge runs on Mojang names).
            try {
                var fire = net.minecraft.world.level.block.Blocks.FIRE;
                var set = net.minecraft.world.level.block.FireBlock.class.getDeclaredMethod("setFlammable",
                        net.minecraft.world.level.block.Block.class, int.class, int.class);
                set.setAccessible(true);
                set.invoke(fire, ModBlocks.RUBBER_LOG.get(), 5, 5);
                set.invoke(fire, ModBlocks.RUBBER_WOOD.get(), 5, 5);
                set.invoke(fire, ModBlocks.RUBBER_PLANKS.get(), 5, 20);
                set.invoke(fire, ModBlocks.RUBBER_LEAVES.get(), 30, 60);
                set.invoke(fire, ModBlocks.DRIED_RUBBER_LOG.get(), 5, 5);
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("Could not make rubber wood flammable", e);
            }
            // Stormreach woods and plants are storm-hardened: never added here, so they don't catch or spread fire.
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Fulgurite Fusion: server starting");
    }
}
