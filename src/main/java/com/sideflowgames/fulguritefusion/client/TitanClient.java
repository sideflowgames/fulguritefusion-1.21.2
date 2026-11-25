package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.titan.TitanArmorEvents;
import com.sideflowgames.fulguritefusion.titan.TitanNetwork;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

// Titan armor, client side: the static dash. With a full Titan set, double-tap forward, back, left or right to
// dash that way (3 second cooldown). The client moves the player; the server is told so it can hurt monsters in
// the way and take the Fm.
public final class TitanClient {
    private TitanClient() {}

    private static final int TAP_WINDOW = 7;
    private static final boolean[] wasDown = new boolean[4];
    private static final int[] lastTap = new int[4];
    private static int cooldown;

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) return;
        if (cooldown > 0) cooldown--;
        KeyMapping[] keys = {mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight};
        boolean ready = mc.screen == null && cooldown == 0 && TitanArmorEvents.fullTitanSet(player) && !player.isFallFlying();
        for (int i = 0; i < 4; i++) {
            boolean down = keys[i].isDown();
            if (down && !wasDown[i]) {
                if (ready && player.tickCount - lastTap[i] <= TAP_WINDOW) {
                    dash(player, i);
                    lastTap[i] = -100;
                } else {
                    lastTap[i] = player.tickCount;
                }
            }
            wasDown[i] = down;
        }
    }

    private static void dash(LocalPlayer player, int key) {
        // Forward, back, left, right relative to where the player faces.
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 dir = switch (key) {
            case 0 -> forward;
            case 1 -> forward.scale(-1);
            case 2 -> right.scale(-1);
            default -> right;
        };
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(dir.x * 1.8, Math.max(v.y, 0.15), dir.z * 1.8);
        cooldown = TitanArmorEvents.DASH_COOLDOWN;
        for (int i = 0; i < 12; i++) {
            player.level().addParticle(ParticleTypes.ELECTRIC_SPARK, player.getRandomX(0.6), player.getRandomY(), player.getRandomZ(0.6), 0, 0, 0);
        }
        PacketDistributor.sendToServer(new TitanNetwork.DashPayload((float) dir.x, (float) dir.z));
    }
}
