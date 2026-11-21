package com.sideflowgames.fulguritefusion;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
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

    // Upgrades installed in a backpack (its upgrade slots).
    public static final Supplier<DataComponentType<ItemContainerContents>> BACKPACK_UPGRADES = COMPONENTS.register("backpack_upgrades",
            () -> DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding().build());

    // A backpack upgrade's filter: the items in its 9 filter slots (copies, not real items).
    public static final Supplier<DataComponentType<ItemContainerContents>> FILTER = COMPONENTS.register("filter",
            () -> DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding().build());

    // A backpack upgrade's settings bits: 1 = allow list (else block list), 2 = also void XP (Storm Void).
    public static final Supplier<DataComponentType<Integer>> FILTER_MODE = COMPONENTS.register("filter_mode",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
