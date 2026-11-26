package com.sideflowgames.fulguritefusion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Fulgurite Fusion's own sounds (files in assets/fulguritefusion/sounds, listed in sounds.json).
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, FulguriteFusion.MODID);

    // Electrical arc crackle: the Stormwing's lightning dive and flares (no thunder).
    public static final Supplier<SoundEvent> STORMWING_ARC = SOUNDS.register("stormwing_arc",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "stormwing_arc")));

    // Stormwing calls: a clear, rising whistled cry (ambient), a sharp squawk with sparks (hurt),
    // and a long sagging cry (death). Hatchlings use the same calls, higher and softer.
    public static final Supplier<SoundEvent> STORMWING_CALL = register("stormwing_call");
    public static final Supplier<SoundEvent> STORMWING_HURT = register("stormwing_hurt");
    public static final Supplier<SoundEvent> STORMWING_DEATH = register("stormwing_death");
    // Arc Sender firing: a short electric buzz.
    public static final Supplier<SoundEvent> ARC_BUZZ = register("arc_buzz");

    // Fusion Reactor: the rumbling hum that builds over the 30 seconds of outer laps, the portal beam, and the impact.
    public static final Supplier<SoundEvent> REACTOR_HUM = registerFar("reactor_hum", 160f);
    public static final Supplier<SoundEvent> REACTOR_BEAM = registerFar("reactor_beam", 160f);
    // The 3 fast inner-ring laps: a quick, climbing whine.
    public static final Supplier<SoundEvent> REACTOR_INNER = registerFar("reactor_inner", 160f);
    public static final Supplier<SoundEvent> REACTOR_IMPACT = registerFar("reactor_impact", 160f);

    // The server only sends a sound to players within its range; variable-range sounds reach just 16 blocks at
    // volume 1. The reactor is huge, so its sounds get a fixed long range (players can stand at the console,
    // or anywhere round the 93-block outer ring, and still hear a run).
    private static Supplier<SoundEvent> registerFar(String name, float range) {
        return SOUNDS.register(name, () -> SoundEvent.createFixedRangeEvent(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, name), range));
    }

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, name)));
    }

    private ModSounds() {}

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
