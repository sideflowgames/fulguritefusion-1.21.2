package com.sideflowgames.fulguritefusion;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Latex: a thick white fluid from rubber trees. Collected by the Latex Harvester (in its tank, pipes can drain it)
// or a Tree Tap's bucket; pours from a Latex Bucket like water, but slowly and not far.
public final class ModFluids {
    private ModFluids() {}

    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, FulguriteFusion.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, FulguriteFusion.MODID);

    public static final DeferredHolder<FluidType, FluidType> LATEX_TYPE = TYPES.register("latex", () -> new FluidType(
            FluidType.Properties.create()
                    .descriptionId("block.fulguritefusion.latex")
                    .density(1400).viscosity(6000)
                    .canSwim(false).canDrown(true).canExtinguish(true).supportsBoating(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LATEX =
            FLUIDS.register("latex", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_LATEX =
            FLUIDS.register("flowing_latex", () -> new BaseFlowingFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(LATEX_TYPE, LATEX, FLOWING_LATEX)
                .bucket(ModItems.LATEX_BUCKET).block(ModBlocks.LATEX)
                .slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30).explosionResistance(100f);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }
}
