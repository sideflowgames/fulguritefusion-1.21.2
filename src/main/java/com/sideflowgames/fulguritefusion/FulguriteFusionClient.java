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
        com.sideflowgames.fulguritefusion.client.ChainBolts.init();
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RecipesReceivedEvent event) ->
                com.sideflowgames.fulguritefusion.client.GuideRecipes.received(event.getRecipeMap()));
        com.sideflowgames.fulguritefusion.util.Fuel.CLIENT = () -> net.minecraft.client.Minecraft.getInstance().level == null ? null
                : net.minecraft.client.Minecraft.getInstance().level.fuelValues();
        com.sideflowgames.fulguritefusion.client.ItemPipeVisuals.init();
        com.sideflowgames.fulguritefusion.client.StormreachClient.init();
        com.sideflowgames.fulguritefusion.client.TrinketClient.init();
    }

    // 26.1: key categories are registered objects. Shown as "key.category.fulguritefusion.fulguritefusion" in the lang file.
    public static final KeyMapping.Category KEY_CATEGORY =
            new KeyMapping.Category(net.minecraft.resources.Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "fulguritefusion"));

    // R while riding a bonded Stormwing: lightning dive. Rebindable under Controls > Fulgurite Fusion.
    public static final KeyMapping STORMWING_DIVE = new KeyMapping("key.fulguritefusion.stormwing_dive",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY);

    // Spears: a first-person wind-up pose that suits our flat spear sprites.
    @SubscribeEvent
    static void onClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new com.sideflowgames.fulguritefusion.client.SpearHandTransform(),
                ModItems.FULGURITE_SPEAR.get(), ModItems.STORM_SPEAR.get(), ModItems.PRIME_SPEAR.get());
    }

    // Latex: how the fluid looks in the world and in pipes/tanks of other mods.
    @SubscribeEvent
    static void onFluidExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
    }

    // 26.1: the still/flowing textures moved from IClientFluidTypeExtensions to a fluid model.
    @SubscribeEvent
    static void onFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        event.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                        new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "block/latex_still")),
                        new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "block/latex_flow")),
                        null, (net.neoforged.neoforge.client.fluid.FluidTintSource) null),
                com.sideflowgames.fulguritefusion.ModFluids.LATEX, com.sideflowgames.fulguritefusion.ModFluids.FLOWING_LATEX);
    }

    // Armor charge HUD, just left of the XP bar.
    @SubscribeEvent
    static void onRegisterParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.LATEX_DRIP.get(), com.sideflowgames.fulguritefusion.client.LatexDripParticle.Provider::new);
    }

    // The guide book's 3D build pictures.
    @SubscribeEvent
    static void onRegisterPictureInPicture(net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent event) {
        event.register(com.sideflowgames.fulguritefusion.client.GuideSceneRenderer.State.class,
                com.sideflowgames.fulguritefusion.client.GuideSceneRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "armor_charge"),
                com.sideflowgames.fulguritefusion.client.ArmorChargeHud::render);
    }

    // B: open the backpack you're wearing or carrying. Rebindable under Controls > Fulgurite Fusion.
    public static final KeyMapping OPEN_BACKPACK = new KeyMapping("key.fulguritefusion.open_backpack",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KEY_CATEGORY);

    // V: toggle the Prime pickaxe's 3x3 hammer mode. Rebindable under Controls > Fulgurite Fusion.
    public static final KeyMapping PRIME_HAMMER = new KeyMapping("key.fulguritefusion.prime_hammer",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_CATEGORY);

    // G: Void Arc Band blink. Rebindable under Controls > Fulgurite Fusion.
    public static final KeyMapping VOID_BLINK = new KeyMapping("key.fulguritefusion.void_blink",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY);

    // Worn backpacks show on the player's back.
    @SubscribeEvent
    static void onAddLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) renderer.addLayer(new com.sideflowgames.fulguritefusion.client.BackpackLayer(renderer));
        }
    }

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(KEY_CATEGORY);
        event.register(OPEN_BACKPACK);
        event.register(STORMWING_DIVE);
        event.register(PRIME_HAMMER);
        event.register(VOID_BLINK);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_BACKPACK.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.sideflowgames.fulguritefusion.backpack.BackpackNetwork.OpenPayload());
            }
        }
        while (PRIME_HAMMER.consumeClick()) {
            if (mc.player != null && mc.screen == null
                    && mc.player.getMainHandItem().getItem() instanceof com.sideflowgames.fulguritefusion.item.PrimeTools.Pickaxe) {
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.sideflowgames.fulguritefusion.prime.PrimeNetwork.HammerPayload());
            }
        }
        com.sideflowgames.fulguritefusion.client.PrimeClient.tick(mc);
        com.sideflowgames.fulguritefusion.client.TitanClient.tick(mc);
        com.sideflowgames.fulguritefusion.client.TrinketClient.tick(mc);
        com.sideflowgames.fulguritefusion.client.ChainBolts.tick();
        com.sideflowgames.fulguritefusion.client.StormreachClient.tick(mc);
        while (STORMWING_DIVE.consumeClick()) {
            if (mc.player != null && mc.player.getVehicle() instanceof StormwingEntity bird
                    && bird.getControllingPassenger() == mc.player && bird.clientStartDive()) {
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new StormwingRiding.DivePayload());
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
        event.register(com.sideflowgames.fulguritefusion.farm.FarmMenus.SHOCK_FARM.get(), com.sideflowgames.fulguritefusion.client.ShockFarmScreen::new);
        event.register(com.sideflowgames.fulguritefusion.transport.TransportBlocks.PIPE_FILTER_MENU.get(), com.sideflowgames.fulguritefusion.client.PipeFilterScreen::new);
        event.register(ModMenus.BACKPACK.get(), com.sideflowgames.fulguritefusion.client.BackpackScreen::new);
        event.register(ModMenus.EQUIPMENT.get(), com.sideflowgames.fulguritefusion.client.EquipmentScreen::new);
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
        event.registerBlockEntityRenderer(ModBlocks.LATEX_HARVESTER_BE.get(), com.sideflowgames.fulguritefusion.client.LatexHarvesterRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.STORM_FORGE_BE.get(), com.sideflowgames.fulguritefusion.client.StormForgeRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.TITAN_FORGE_BE.get(), com.sideflowgames.fulguritefusion.client.StormForgeRenderer::new);
        // Ghost build outlines (shown while the block is powered by redstone).
        event.registerBlockEntityRenderer(ModBlocks.REACTOR_CONSOLE_BE.get(),
                ctx -> new com.sideflowgames.fulguritefusion.client.GhostRenderer<>(com.sideflowgames.fulguritefusion.client.GhostBuild::console, 6));
        // Shock Farm: ghost outline while unbuilt, the whole farm as one model once formed.
        event.registerBlockEntityRenderer(com.sideflowgames.fulguritefusion.farm.FarmBlocks.CONTROLLER_BE.get(),
                com.sideflowgames.fulguritefusion.client.ShockFarmRenderer::new);
        // Collider rings: ghost outline while incomplete, one big model once formed.
        event.registerBlockEntityRenderer(ModBlocks.STORM_COLLIDER_BE.get(), com.sideflowgames.fulguritefusion.client.ColliderRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.MINI_COLLIDER_BE.get(), com.sideflowgames.fulguritefusion.client.ColliderRenderer::new);
        event.registerEntityRenderer(ModEntities.LUMEN_SLUG.get(), LumenSlugRenderer::new);
        event.registerEntityRenderer(ModEntities.VOIDMOTH.get(), VoidmothRenderer::new);
        event.registerEntityRenderer(ModEntities.STORMWING.get(), StormwingRenderer::new);
        event.registerEntityRenderer(ModEntities.STORMWING_HATCHLING.get(), StormwingHatchlingRenderer::new);
        event.registerEntityRenderer(ModEntities.ARC_SERPENT.get(), ArcSerpentRenderer::new);
        event.registerEntityRenderer(ModEntities.STORMHOUND.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "stormhound", 0.5f, 1.0f, 10));
        event.registerEntityRenderer(ModEntities.VOLTCAP_CRAWLER.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "voltcap_crawler", 0.5f, 1.0f, 6));
        event.registerEntityRenderer(ModEntities.THUNDERHEAD.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "thunderhead", 3.0f, 1.0f, 12));
        event.registerEntityRenderer(ModEntities.MAGMA_CONDUIT.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "magma_conduit", 2.2f, 1.0f, 12));
        event.registerEntityRenderer(ModEntities.VOID_ARC.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "void_arc", 0.0f, 1.0f, 8));
        event.registerEntityRenderer(ModEntities.STORM_TITAN.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "storm_titan", 4.0f, 1.0f, 12));
        event.registerEntityRenderer(ModEntities.TITAN_CONDUIT.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "titan_conduit", 0.6f, 1.0f, 12));
        event.registerEntityRenderer(ModEntities.STORM_CORE.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "storm_core", 0.0f, 1.0f, 14));
        event.registerEntityRenderer(ModEntities.MAGMA_PYLON.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "magma_pylon", 0.8f, 1.0f, 10));
        event.registerEntityRenderer(ModEntities.VOID_ECHO.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "void_arc", 0.0f, 1.0f, 8));
        event.registerEntityRenderer(ModEntities.SPARKHORN.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "sparkhorn", 0.6f, 1.0f, 0));
        event.registerEntityRenderer(ModEntities.PUFFLING.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "puffling", 0.3f, 1.0f, 0));
        event.registerEntityRenderer(ModEntities.STATIC_JELLY.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "static_jelly", 0.0f, 1.0f, 12));
        event.registerEntityRenderer(ModEntities.ION_EEL.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "ion_eel", 0.0f, 1.0f, 10));
        event.registerEntityRenderer(ModEntities.RUBBERBACK_SLOTH.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "rubberback_sloth", 0.6f, 1.0f, 0));
        event.registerEntityRenderer(ModEntities.GLIMMERWING.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "glimmerwing", 0.0f, 1.0f, 13));
        event.registerEntityRenderer(ModEntities.VOLTCAP_SNAIL.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "voltcap_snail", 0.35f, 1.0f, 8));
        event.registerEntityRenderer(ModEntities.GLASSCRAB.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "glasscrab", 0.45f, 1.0f, 0));
        event.registerEntityRenderer(ModEntities.ARCWURM.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "arcwurm", 0.8f, 1.0f, 0));
        event.registerEntityRenderer(ModEntities.CRYSTAL_STALKER.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "crystal_stalker", 0.8f, 1.0f, 0));
        event.registerEntityRenderer(ModEntities.STATIC_WRAITH.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "static_wraith", 0.0f, 1.0f, 6));
        event.registerEntityRenderer(ModEntities.SHOCKROACH.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "shockroach", 0.25f, 1.0f, 0));
        event.registerEntityRenderer(ModEntities.ARC_REAVER.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "arc_reaver", 0.5f, 1.0f, 4));
        event.registerEntityRenderer(ModEntities.THUNDERHULK.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "thunderhulk", 1.1f, 1.0f, 0));
        event.registerEntityRenderer(ModEntities.STORMCALLER_SHADE.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "stormcaller_shade", 0.0f, 1.0f, 6));
        event.registerEntityRenderer(ModEntities.STATIC_LEECH.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "static_leech", 0.3f, 1.0f, 6));
        event.registerEntityRenderer(ModEntities.ION_SWARM.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "ion_swarm", 0.0f, 1.0f, 15));
        event.registerEntityRenderer(ModEntities.VOLTSPITTER.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "voltspitter", 0.6f, 1.0f, 3));
        event.registerEntityRenderer(ModEntities.TEMPEST_RAPTOR.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "tempest_raptor", 0.4f, 1.0f, 2));
        event.registerEntityRenderer(ModEntities.GLOOMARC_SPIDER.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "gloomarc_spider", 0.8f, 1.0f, 3));
        event.registerEntityRenderer(ModEntities.PEAK_COLOSSUS.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "peak_colossus", 2.0f, 1.0f, 0));
        event.registerEntityRenderer(ModEntities.CINDER_BEHEMOTH.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "cinder_behemoth", 2.0f, 1.0f, 6));
        event.registerEntityRenderer(ModEntities.AURORA_WRAITH.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "aurora_wraith", 0.0f, 1.0f, 12));
        event.registerEntityRenderer(ModEntities.PRISM_SENTINEL.get(), ctx -> new com.sideflowgames.fulguritefusion.client.StormreachMobRenderer<>(ctx, "prism_sentinel", 1.6f, 1.0f, 8));
        event.registerEntityRenderer(ModEntities.CHARGED_ORB.get(), ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.25f, true));
        event.registerEntityRenderer(ModEntities.SPEAR.get(), com.sideflowgames.fulguritefusion.client.SpearRenderer::new);
    }
}
