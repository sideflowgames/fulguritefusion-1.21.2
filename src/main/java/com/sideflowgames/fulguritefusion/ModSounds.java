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

    private ModSounds() {}

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
