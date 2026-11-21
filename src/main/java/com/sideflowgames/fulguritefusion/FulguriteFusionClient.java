package com.sideflowgames.fulguritefusion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import com.sideflowgames.fulguritefusion.entity.StormwingEntity;
import com.sideflowgames.fulguritefusion.entity.StormwingRiding;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
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

        // Stormwing riding: Space climbs and Shift descends; the dive key is checked every tick.
        StormwingEntity.riderVertical = () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) return 0;
            if (mc.options.keyJump.isDown()) return 1;
            if (mc.options.keyShift.isDown()) return -1;
            return 0;
        };
        StormwingEntity.riderSprint = () -> {
            Minecraft mc = Minecraft.getInstance();
            return mc.screen == null && mc.options.keySprint.isDown();
        };
        NeoForge.EVENT_BUS.addListener(FulguriteFusionClient::onClientTick);
    }

    // R while riding a bonded Stormwing: lightning dive. Rebindable under Controls > Fulgurite Fusion.
    public static final KeyMapping STORMWING_DIVE = new KeyMapping("key.fulguritefusion.stormwing_dive",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.fulguritefusion");

    // Spears: a first-person wind-up pose that suits our flat spear sprites.
    @SubscribeEvent
    static void onClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new com.sideflowgames.fulguritefusion.client.SpearHandTransform(),
                ModItems.FULGURITE_SPEAR.get(), ModItems.STORM_SPEAR.get());
    }

    // Armor charge HUD, just left of the XP bar.
    @SubscribeEvent
    static void onRegisterGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.EXPERIENCE_BAR,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "armor_charge"),
                com.sideflowgames.fulguritefusion.client.ArmorChargeHud::render);
    }

    // B: open the backpack you're wearing or carrying. Rebindable under Controls > Fulgurite Fusion.
    public static final KeyMapping OPEN_BACKPACK = new KeyMapping("key.fulguritefusion.open_backpack",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.fulguritefusion");

    // Worn backpacks show on the player's back.
    @SubscribeEvent
    static void onAddLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer) {
                renderer.addLayer(new com.sideflowgames.fulguritefusion.client.BackpackLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BACKPACK);
        event.register(STORMWING_DIVE);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_BACKPACK.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                PacketDistributor.sendToServer(new com.sideflowgames.fulguritefusion.backpack.BackpackNetwork.OpenPayload());
            }
        }
        while (STORMWING_DIVE.consumeClick()) {
            if (mc.player != null && mc.player.getVehicle() instanceof StormwingEntity bird
                    && bird.getControllingPassenger() == mc.player && bird.clientStartDive()) {
                PacketDistributor.sendToServer(new StormwingRiding.DivePayload());
            }
        }
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
        event.register(ModMenus.BACKPACK.get(), com.sideflowgames.fulguritefusion.client.BackpackScreen::new);
        event.register(ModMenus.UPGRADE_FILTER.get(), com.sideflowgames.fulguritefusion.client.UpgradeFilterScreen::new);
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
        event.registerBlockEntityRenderer(ModBlocks.STORM_FORGE_BE.get(), com.sideflowgames.fulguritefusion.client.StormForgeRenderer::new);
        // Ghost build outlines (shown while the block is powered by redstone).
        event.registerBlockEntityRenderer(ModBlocks.REACTOR_CONSOLE_BE.get(),
                ctx -> new com.sideflowgames.fulguritefusion.client.GhostRenderer<>(com.sideflowgames.fulguritefusion.client.GhostBuild::console, 6));
        event.registerBlockEntityRenderer(ModBlocks.STORM_COLLIDER_BE.get(),
                ctx -> new com.sideflowgames.fulguritefusion.client.GhostRenderer<>(com.sideflowgames.fulguritefusion.client.GhostBuild::colliderRing, 5));
        event.registerBlockEntityRenderer(ModBlocks.MINI_COLLIDER_BE.get(),
                ctx -> new com.sideflowgames.fulguritefusion.client.GhostRenderer<>(com.sideflowgames.fulguritefusion.client.GhostBuild::colliderRing, 5));
        event.registerEntityRenderer(ModEntities.LUMEN_SLUG.get(), LumenSlugRenderer::new);
        event.registerEntityRenderer(ModEntities.VOIDMOTH.get(), VoidmothRenderer::new);
        event.registerEntityRenderer(ModEntities.STORMWING.get(), StormwingRenderer::new);
        event.registerEntityRenderer(ModEntities.STORMWING_HATCHLING.get(), StormwingHatchlingRenderer::new);
        event.registerEntityRenderer(ModEntities.ARC_SERPENT.get(), ArcSerpentRenderer::new);
        event.registerEntityRenderer(ModEntities.SPEAR.get(), com.sideflowgames.fulguritefusion.client.SpearRenderer::new);
    }
}
