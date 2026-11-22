package com.sideflowgames.fulguritefusion;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, FulguriteFusion.MODID);

    // A drop of latex: hangs from a Tree Tap's spout, falls straight down and splats.
    public static final Supplier<SimpleParticleType> LATEX_DRIP = PARTICLES.register("latex_drip", () -> new SimpleParticleType(false));

    public static void register(IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
    }
}
