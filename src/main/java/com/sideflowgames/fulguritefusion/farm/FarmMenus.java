package com.sideflowgames.fulguritefusion.farm;

import com.sideflowgames.fulguritefusion.ModMenus;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

// Registered on ModMenus' register; FarmBlocks.init() loads this class before the registers fire.
public final class FarmMenus {
    private FarmMenus() {}

    public static final Supplier<MenuType<ShockFarmMenu>> SHOCK_FARM =
            ModMenus.MENUS.register("shock_farm", () -> IMenuTypeExtension.create(ShockFarmMenu::new));

    static void init() {}
}
