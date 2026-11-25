package com.sideflowgames.fulguritefusion.stormreach;

import com.sideflowgames.fulguritefusion.FulguriteFusion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

// The Stormreach: an end-game dimension of shattered, floating landmasses over an endless storm.
// Worldgen lives in data/fulguritefusion/{dimension,dimension_type,worldgen}/stormreach*.
// Released in Fulgurite Fusion 2.0: open to everyone. The way in is the rift portal, which only lets through
// players who have defeated the Storm Titan (RiftPortals / TitanTrials.defeated); the Titan is reached with the
// Storm Sigil. Set RELEASED to false to lock it to the developer again (the player named "Dev" in the
// development build).
public final class Stormreach {
    private Stormreach() {}

    public static final ResourceKey<Level> LEVEL =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "stormreach"));

    // True only when running from the development environment, not from a built jar.
    public static boolean devBuild() {
        return !net.neoforged.fml.loading.FMLEnvironment.production;
    }

    public static final String DEV_NAME = "Dev";

    // 2.0: the Stormreach and the Titan Trials are open to players (each entrance still has its own rules).
    public static final boolean RELEASED = true;

    // The developer: the player named Dev, in the development build only.
    public static boolean developer(net.minecraft.world.entity.Entity entity) {
        return devBuild() && entity instanceof net.minecraft.world.entity.player.Player p && DEV_NAME.equals(p.getGameProfile().getName());
    }

    // May this entity use the Stormreach's entrances (Storm Sigil, rift portal)?
    public static boolean allowed(net.minecraft.world.entity.Entity entity) {
        return RELEASED || developer(entity);
    }

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(Stormreach::onTravel);
        StormreachTravel.register();
    }

    // Before release: only the developer gets in, by command, portal or any other way.
    private static void onTravel(net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent event) {
        if (event.getDimension() != LEVEL || allowed(event.getEntity())) return;
        event.setCanceled(true);
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "The Stormreach is still in development. It's coming in Fulgurite Fusion 2.0.")
                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
        }
    }

    public static boolean is(Level level) {
        return level != null && level.dimension() == LEVEL;
    }
}
