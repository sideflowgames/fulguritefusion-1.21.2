package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.stormreach.Stormreach;
import com.sideflowgames.fulguritefusion.stormreach.StormreachStorm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;

// The Stormreach's sky on the client: rain and storm darkness fade in and out with the server's storm/calm cycle
// (StormreachStorm), whatever the Overworld weather is doing. Thunder there is turned down and comes from where
// the bolt landed, so a constant storm doesn't deafen you.
public final class StormreachClient {
    private StormreachClient() {}

    private static boolean storming = true;
    private static float strength;

    public static void init() {
        StormreachStorm.clientHandler = s -> storming = s;
        NeoForge.EVENT_BUS.addListener(StormreachClient::onSound);
    }

    // Called every client tick.
    public static void tick(Minecraft mc) {
        if (mc.level == null || !Stormreach.is(mc.level)) {
            strength = 1f;
            return;
        }
        float target = storming ? 1f : 0f;
        strength += Math.signum(target - strength) * Math.min(Math.abs(target - strength), 0.004f); // ~12 s fade
        mc.level.setRainLevel(strength);
        mc.level.setThunderLevel(strength);
    }

    private static void onSound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getInstance();
        SoundInstance sound = event.getSound();
        if (sound == null || mc.level == null || !Stormreach.is(mc.level)) return;
        if (!sound.getLocation().equals(SoundEvents.LIGHTNING_BOLT_THUNDER.getLocation())) return;
        event.setSound(new SimpleSoundInstance(SoundEvents.LIGHTNING_BOLT_THUNDER, sound.getSource(), 2.5f,
                0.8f + mc.level.random.nextFloat() * 0.2f, RandomSource.create(), sound.getX(), sound.getY(), sound.getZ()));
    }
}
