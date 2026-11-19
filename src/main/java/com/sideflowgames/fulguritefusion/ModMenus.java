package com.sideflowgames.fulguritefusion;

import java.util.function.Supplier;

import com.sideflowgames.fulguritefusion.menu.EuConverterMenu;
import com.sideflowgames.fulguritefusion.menu.FulguriteChargerMenu;
import com.sideflowgames.fulguritefusion.menu.ProcessingMachineMenu;
import com.sideflowgames.fulguritefusion.menu.StormBatteryMenu;
import com.sideflowgames.fulguritefusion.menu.StormFurnaceMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, FulguriteFusion.MODID);

    public static final Supplier<MenuType<StormFurnaceMenu>> STORM_FURNACE =
            MENUS.register("storm_furnace", () -> IMenuTypeExtension.create(StormFurnaceMenu::new));

    public static final Supplier<MenuType<FulguriteChargerMenu>> FULGURITE_CHARGER =
            MENUS.register("fulgurite_charger", () -> IMenuTypeExtension.create(FulguriteChargerMenu::new));

    public static final Supplier<MenuType<EuConverterMenu>> EU_CONVERTER =
            MENUS.register("eu_converter", () -> IMenuTypeExtension.create(EuConverterMenu::new));

    // Shared by all five multiblock processing machines.
    public static final Supplier<MenuType<ProcessingMachineMenu>> PROCESSING_MACHINE =
            MENUS.register("processing_machine", () -> IMenuTypeExtension.create(ProcessingMachineMenu::new));

    public static final Supplier<MenuType<StormBatteryMenu>> STORM_BATTERY =
            MENUS.register("storm_battery", () -> IMenuTypeExtension.create(StormBatteryMenu::new));

    private ModMenus() {}

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
