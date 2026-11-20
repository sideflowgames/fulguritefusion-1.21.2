package com.sideflowgames.fulguritefusion;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import com.sideflowgames.fulguritefusion.client.ConnectorRenderer;
import com.sideflowgames.fulguritefusion.client.StormBatteryRenderer;
import com.sideflowgames.fulguritefusion.client.FusionColliderRenderer;
import com.sideflowgames.fulguritefusion.client.DataCableRenderer;
import com.sideflowgames.fulguritefusion.client.EuConverterScreen;
import com.sideflowgames.fulguritefusion.client.ArcSerpentRenderer;
import com.sideflowgames.fulguritefusion.client.LumenSlugRenderer;
import com.sideflowgames.fulguritefusion.client.MachineControllerRenderer;
import com.sideflowgames.fulguritefusion.client.ProcessingMachineScreen;
import com.sideflowgames.fulguritefusion.client.StormwingRenderer;
import com.sideflowgames.fulguritefusion.client.StormwingHatchlingRenderer;
import com.sideflowgames.fulguritefusion.client.VoidmothRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.sideflowgames.fulguritefusion.client.FulguriteChargerScreen;
import com.sideflowgames.fulguritefusion.client.StormBatteryScreen;
import com.sideflowgames.fulguritefusion.client.MiniColliderScreen;
import com.sideflowgames.fulguritefusion.client.StormColliderScreen;
import com.sideflowgames.fulguritefusion.client.ColliderPartScreen;
import com.sideflowgames.fulguritefusion.client.StormFurnaceScreen;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = FulguriteFusion.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = FulguriteFusion.MODID, value = Dist.CLIENT)
public class FulguriteFusionClient {
    public FulguriteFusionClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        FulguriteFusion.LOGGER.info("HELLO FROM CLIENT SETUP");
        FulguriteFusion.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    // Connect each machine menu to the screen that draws it.
    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.STORM_FURNACE.get(), StormFurnaceScreen::new);
        event.register(ModMenus.FULGURITE_CHARGER.get(), FulguriteChargerScreen::new);
        event.register(ModMenus.EU_CONVERTER.get(), EuConverterScreen::new);
        event.register(ModMenus.STORM_BATTERY.get(), StormBatteryScreen::new);
        event.register(ModMenus.PROCESSING_MACHINE.get(), ProcessingMachineScreen::new);
        event.register(ModMenus.MINI_COLLIDER.get(), MiniColliderScreen::new);
        event.register(ModMenus.STORM_COLLIDER.get(), StormColliderScreen::new);
        event.register(ModMenus.COLLIDER_PART.get(), ColliderPartScreen::new);
        event.register(ModMenus.REACTOR_CONSOLE.get(), com.sideflowgames.fulguritefusion.client.ReactorConsoleScreen::new);
    }

    // Draws the hanging wires between Machine Connectors, and our mobs.
    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MACHINE_CONNECTOR_BE.get(), ConnectorRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.STORM_BATTERY_BE.get(), StormBatteryRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.FUSION_COLLIDER_BE.get(), FusionColliderRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.DATA_CABLE_BE.get(), DataCableRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.MACHINE_CONTROLLER_BE.get(), MachineControllerRenderer::new);
        event.registerEntityRenderer(ModEntities.LUMEN_SLUG.get(), LumenSlugRenderer::new);
        event.registerEntityRenderer(ModEntities.VOIDMOTH.get(), VoidmothRenderer::new);
        event.registerEntityRenderer(ModEntities.STORMWING.get(), StormwingRenderer::new);
        event.registerEntityRenderer(ModEntities.STORMWING_HATCHLING.get(), StormwingHatchlingRenderer::new);
        event.registerEntityRenderer(ModEntities.ARC_SERPENT.get(), ArcSerpentRenderer::new);
    }
}
