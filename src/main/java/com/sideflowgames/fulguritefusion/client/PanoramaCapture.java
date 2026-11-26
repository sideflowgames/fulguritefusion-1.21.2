package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

// Dev only (never registered in a release build): /ffpanorama captures a title-screen panorama from where you
// stand, the 6 cube faces at 1024x1024, into run/screenshots (panorama_0.png ... panorama_5.png). The HUD is
// hidden while it captures. Set the time, weather and view first.
@EventBusSubscriber(modid = FulguriteFusion.MODID, value = Dist.CLIENT)
public final class PanoramaCapture {
    private PanoramaCapture() {}

    private static int countdown = -1;

    @SubscribeEvent
    public static void onCommands(RegisterClientCommandsEvent event) {
        if (FMLEnvironment.isProduction()) return;
        event.getDispatcher().register(Commands.literal("ffpanorama").executes(ctx -> {
            countdown = 20; // let the chat screen close first
            Minecraft.getInstance().gui.getChat().addClientSystemMessage(Component.literal("Capturing panorama in 1 second... hold still")
                    .withStyle(ChatFormatting.AQUA));
            return 1;
        }));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (countdown < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || --countdown > 0) return;
        countdown = -1;
        Component result = mc.grabPanoramixScreenshot(mc.gameDirectory);
        mc.gui.getChat().addClientSystemMessage(result);
    }
}
