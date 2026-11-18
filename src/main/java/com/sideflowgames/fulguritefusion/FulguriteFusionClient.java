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
import com.sideflowgames.fulguritefusion.client.EuConverterScreen;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.sideflowgames.fulguritefusion.client.FulguriteChargerScreen;
import com.sideflowgames.fulguritefusion.client.StormBatteryScreen;
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
    }

    // Draws the hanging wires between Machine Connectors.
    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MACHINE_CONNECTOR_BE.get(), ConnectorRenderer::new);
    }
}
