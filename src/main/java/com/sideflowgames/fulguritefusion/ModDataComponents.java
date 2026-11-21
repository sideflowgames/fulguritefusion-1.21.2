package com.sideflowgames.fulguritefusion;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Data stored on item stacks.
public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, FulguriteFusion.MODID);

    // Stored Fm in forged gear (see FmCharge).
    public static final Supplier<DataComponentType<Integer>> FM_CHARGE = COMPONENTS.register("fm_charge",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    // Hydrogen held in a Stormforged helmet, in ticks underwater (vented on surfacing).
    public static final Supplier<DataComponentType<Integer>> HYDROGEN = COMPONENTS.register("hydrogen",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    // Stormforged tool mode bits: 1 = arc smelting, 2 = vein mining (sneak + right-click the air to cycle).
    public static final Supplier<DataComponentType<Integer>> TOOL_MODE = COMPONENTS.register("tool_mode",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
