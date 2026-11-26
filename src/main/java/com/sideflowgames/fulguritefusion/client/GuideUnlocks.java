package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.guide.GuideProgress;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

// Unlocks guide topics: once a second, any topic whose items the player is holding lights up on the map.
@EventBusSubscriber(modid = FulguriteFusion.MODID, value = Dist.CLIENT)
public final class GuideUnlocks {
    private GuideUnlocks() {}

    // Unlocked this session and not yet opened (they pulse on the map).
    public static final Set<String> NEW = new HashSet<>();

    public static boolean unlocked(String id) {
        return GuideData.START.equals(id) || GuideProgress.CLIENT_UNLOCKED.contains(id);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !GuideProgress.clientSynced || mc.player.tickCount % 20 != 0) return;
        GuideData data = GuideData.get();
        var inv = mc.player.getInventory();
        for (GuideData.Chapter c : data.chapters.values()) {
            if (unlocked(c.id)) continue;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && c.unlock.contains(s.getItem())) {
                    GuideProgress.CLIENT_UNLOCKED.add(c.id);
                    NEW.add(c.id);
                    net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new GuideProgress.UnlockPayload(c.id));
                    mc.player.sendOverlayMessage(Component.literal("⚡ New in your guide: ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(c.title).withStyle(ChatFormatting.WHITE)));
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        GuideProgress.CLIENT_UNLOCKED.clear();
        GuideProgress.clientSynced = false;
        NEW.clear();
    }
}
