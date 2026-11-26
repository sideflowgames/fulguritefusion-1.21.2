package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.backpack.BackpackNetwork;
import com.sideflowgames.fulguritefusion.backpack.BackpackUpgradeItem;
import com.sideflowgames.fulguritefusion.backpack.BackpackUpgrades;
import com.sideflowgames.fulguritefusion.backpack.UpgradeKind;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Adds Deposit / Restock buttons beside any open container screen while you carry a backpack with those
// upgrades.
@EventBusSubscriber(modid = FulguriteFusion.MODID, value = Dist.CLIENT)
public final class BackpackClientEvents {
    private BackpackClientEvents() {}

    private static boolean has(UpgradeKind kind) {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        for (ItemStack pack : BackpackUpgrades.packs(player)) {
            for (ItemStack u : BackpackUpgrades.upgrades(pack)) {
                if (((BackpackUpgradeItem) u.getItem()).kind == kind) return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen
                || screen instanceof BackpackScreen || screen instanceof UpgradeFilterScreen) return;
        var player = Minecraft.getInstance().player;
        if (player == null || screen.getMenu().slots.stream().noneMatch(s -> s.container != player.getInventory())) return;
        int x = screen.getGuiLeft() + screen.getXSize() + 2, y = screen.getGuiTop() + 4;
        if (has(UpgradeKind.DEPOSIT)) {
            Button b = Button.builder(Component.literal("Deposit"),
                    btn -> net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new BackpackNetwork.UtilityPayload(0))).bounds(x, y, 54, 16).build();
            b.setTooltip(Tooltip.create(Component.literal("Move backpack items into this container")));
            event.addListener(b);
            y += 18;
        }
        if (has(UpgradeKind.RESTOCK)) {
            Button b = Button.builder(Component.literal("Restock"),
                    btn -> net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new BackpackNetwork.UtilityPayload(1))).bounds(x, y, 54, 16).build();
            b.setTooltip(Tooltip.create(Component.literal("Fill your backpack from this container")));
            event.addListener(b);
        }
    }
}
