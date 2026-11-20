package com.sideflowgames.fulguritefusion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Fulgurite Fusion's own sounds (files in assets/fulguritefusion/sounds, listed in sounds.json).
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, FulguriteFusion.MODID);

    // Electrical arc crackle: the Stormwing's lightning dive and flares (no thunder).
    public static final Supplier<SoundEvent> STORMWING_ARC = SOUNDS.register("stormwing_arc",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "stormwing_arc")));

    // Stormwing calls: a clear, rising whistled cry (ambient), a sharp squawk with sparks (hurt),
    // and a long sagging cry (death). Hatchlings use the same calls, higher and softer.
    public static final Supplier<SoundEvent> STORMWING_CALL = register("stormwing_call");
    public static final Supplier<SoundEvent> STORMWING_HURT = register("stormwing_hurt");
    public static final Supplier<SoundEvent> STORMWING_DEATH = register("stormwing_death");

    // Fusion Reactor: the rumbling hum that builds over the 30 seconds of outer laps, the portal beam, and the impact.
    public static final Supplier<SoundEvent> REACTOR_HUM = register("reactor_hum");
    public static final Supplier<SoundEvent> REACTOR_BEAM = register("reactor_beam");
    // The 3 fast inner-ring laps: a quick, climbing whine.
    public static final Supplier<SoundEvent> REACTOR_INNER = register("reactor_inner");
    public static final Supplier<SoundEvent> REACTOR_IMPACT = register("reactor_impact");

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, name)));
    }

    private ModSounds() {}

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
