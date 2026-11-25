package com.sideflowgames.fulguritefusion.client;

import java.lang.reflect.Field;
import java.util.Set;

import com.mojang.serialization.Lifecycle;
import com.sideflowgames.fulguritefusion.FulguriteFusion;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

// Minecraft warns "These settings are using experimental features" whenever a new world loads world generation
// data that didn't ship with the game. The Stormreach dimension, its biomes and terrain are all such data, so
// every world with this mod got the warning. This skips it, but only when nothing else is the cause: no
// experimental feature flags turned on, and every non-vanilla world generation entry belongs to this mod or to
// NeoForge. Anything from another mod or datapack still shows the warning as normal.
@EventBusSubscriber(modid = FulguriteFusion.MODID, value = Dist.CLIENT)
public final class ExperimentalWarningSkip {
    private ExperimentalWarningSkip() {}

    private static final String TITLE_KEY = "selectWorld.warning.experimental.title";
    private static final Set<String> OURS = Set.of(FulguriteFusion.MODID, "neoforge");

    @SubscribeEvent
    public static void onOpen(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof ConfirmScreen confirm)) return;
        if (!(confirm.getTitle().getContents() instanceof TranslatableContents tc) || !TITLE_KEY.equals(tc.getKey())) return;
        if (!(event.getCurrentScreen() instanceof CreateWorldScreen create)) return;
        try {
            WorldCreationContext ctx = create.getUiState().getSettings();
            if (FeatureFlags.isExperimental(ctx.dataConfiguration().enabledFeatures())) return;
            if (!onlyOurs(ctx.datapackDimensions())) return;
            for (var it = ctx.worldgenLoadContext().registries().iterator(); it.hasNext(); ) {
                if (!onlyOurs(it.next().value())) return;
            }
            Field f = ConfirmScreen.class.getDeclaredField("callback");
            f.setAccessible(true);
            BooleanConsumer callback = (BooleanConsumer) f.get(confirm);
            event.setCanceled(true);
            // Answer "Yes" for the player once the current screen change has finished.
            Minecraft.getInstance().execute(() -> callback.accept(true));
        } catch (Throwable t) {
            // If anything here doesn't match this version of the game, leave the warning in place.
            FulguriteFusion.LOGGER.debug("Could not skip the experimental settings warning", t);
        }
    }

    // True if every entry in the registry that isn't marked stable comes from this mod or NeoForge.
    private static <T> boolean onlyOurs(Registry<T> registry) {
        for (ResourceKey<T> key : registry.registryKeySet()) {
            Lifecycle life = registry.registrationInfo(key).map(i -> i.lifecycle()).orElse(Lifecycle.stable());
            if (!life.equals(Lifecycle.stable()) && !OURS.contains(key.location().getNamespace())) return false;
        }
        return true;
    }
}
