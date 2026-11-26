package com.sideflowgames.fulguritefusion.util;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.FuelValues;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

// 26.1: burn times come from the level's FuelValues instead of a global table.
// On the server they come from the running server; the client sets CLIENT (see FulguriteFusionClient).
public final class Fuel {
    private Fuel() {}

    public static Supplier<FuelValues> CLIENT = () -> null;

    @Nullable
    public static FuelValues values() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.fuelValues();
        return CLIENT.get();
    }

    public static int burnTime(ItemStack stack) {
        FuelValues values = values();
        return values == null ? 0 : stack.getBurnTime(RecipeType.SMELTING, values);
    }
}
