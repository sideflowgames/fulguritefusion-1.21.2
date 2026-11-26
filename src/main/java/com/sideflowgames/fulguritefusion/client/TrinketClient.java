package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusionClient;
import com.sideflowgames.fulguritefusion.backpack.CuriosCompat;
import com.sideflowgames.fulguritefusion.trinket.TrinketItem;
import com.sideflowgames.fulguritefusion.trinket.TrinketKind;
import com.sideflowgames.fulguritefusion.trinket.Trinkets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

// Trinkets, client side:
//   - a "Gear" button on the inventory screen opens the equipment panel (hidden when Curios is installed: its own
//     screen holds the trinkets then);
//   - Cloudstep Charm: press jump in mid-air for an extra jump (two with the Empowered charm). A full Prime set
//     opens its glide on that press instead;
//   - Void Arc Band: the Void Blink key (default G) blinks you the way you're moving (or facing, standing still).
public final class TrinketClient {
    private TrinketClient() {}

    private static Button gear;
    private static boolean jumpWasDown;
    private static int airJumps;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(TrinketClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(TrinketClient::onScreenRender);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        gear = null;
        if (!(event.getScreen() instanceof InventoryScreen inv) || CuriosCompat.loaded()) return;
        gear = Button.builder(Component.literal("Gear"), b -> net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new Trinkets.OpenPayload()))
                .bounds(inv.getGuiLeft() + inv.getXSize() - 36, inv.getGuiTop() - 15, 36, 14).build();
        event.addListener(gear);
    }

    // The recipe book moves the inventory sideways; keep the button on its corner.
    private static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (gear != null && event.getScreen() instanceof InventoryScreen inv) {
            gear.setPosition(inv.getGuiLeft() + inv.getXSize() - 36, inv.getGuiTop() - 15);
        }
    }

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) return;
        // Cloudstep Charm: extra jumps in mid-air.
        boolean jump = mc.options.keyJump.isDown();
        int cloud = Trinkets.level(player, TrinketKind.CLOUDSTEP_CHARM);
        if (player.onGround() || player.isInWater() || player.onClimbable()) airJumps = 0;
        if (cloud > 0 && jump && !jumpWasDown && mc.screen == null && !player.onGround() && !player.isFallFlying()
                && !player.getAbilities().flying && !player.isPassenger() && !player.isInWater() && airJumps < (cloud == 3 ? 3 : cloud == 2 ? 2 : 1)) {
            airJumps++;
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x, 0.55, v.z);
            player.resetFallDistance();
            for (int i = 0; i < 8; i++) player.level().addParticle(ParticleTypes.CLOUD, player.getRandomX(0.5), player.getY(), player.getRandomZ(0.5), 0, -0.05, 0);
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new Trinkets.DoubleJumpPayload());
        }
        jumpWasDown = jump;
        // Void Arc Band: blink.
        while (FulguriteFusionClient.VOID_BLINK.consumeClick()) {
            ItemStack band = Trinkets.worn(player, TrinketKind.VOID_ARC_BAND);
            if (mc.screen != null || !(band.getItem() instanceof TrinketItem item) || player.getCooldowns().isOnCooldown(band)) continue;
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)), left = new Vec3(forward.z, 0, -forward.x);
            Vec3 dir = forward.scale(player.input.getMoveVector().y).add(left.scale(player.input.getMoveVector().x));
            if (dir.lengthSqr() < 1e-3) dir = forward;
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new Trinkets.BlinkPayload((float) dir.x, (float) dir.z));
        }
    }
}
