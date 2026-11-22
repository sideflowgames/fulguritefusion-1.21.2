package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.prime.PrimeEvents;
import com.sideflowgames.fulguritefusion.prime.PrimeNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

// Prime armor, client side: while gliding with a full Prime set, holding jump pushes you along like a firework
// rocket that never runs out (it spends the chestplate's Fm instead; the server is told every half second).
// Gliding leaves a violet trail.
public final class PrimeClient {
    private PrimeClient() {}

    private static int unreported;

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) return;
        boolean gliding = player.isFallFlying() && PrimeEvents.fullPrimeSet(player);
        if (gliding) {
            if (player.tickCount % 2 == 0) {
                player.level().addParticle(new DustParticleOptions(PrimeEvents.VIOLET, 0.8f),
                        player.getX(), player.getY() + 0.6, player.getZ(), 0, 0, 0);
            }
            // Hold jump to boost (not on the jump that opened the glide).
            if (mc.screen == null && mc.options.keyJump.isDown() && player.getFallFlyingTicks() > 6) {
                Vec3 look = player.getLookAngle(), v = player.getDeltaMovement();
                player.setDeltaMovement(v.add(look.x * 0.1 + (look.x * 1.5 - v.x) * 0.5,
                        look.y * 0.1 + (look.y * 1.5 - v.y) * 0.5,
                        look.z * 0.1 + (look.z * 1.5 - v.z) * 0.5));
                player.level().addParticle(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.4, player.getZ(),
                        -look.x * 0.3, -look.y * 0.3, -look.z * 0.3);
                if (unreported == 0) player.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.3f, 1.6f);
                unreported++;
            }
        }
        if (unreported >= 10 || (unreported > 0 && !gliding)) {
            PacketDistributor.sendToServer(new PrimeNetwork.BoostPayload(unreported));
            unreported = 0;
        }
    }
}
