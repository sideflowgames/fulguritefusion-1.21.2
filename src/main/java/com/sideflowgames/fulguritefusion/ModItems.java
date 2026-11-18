package com.sideflowgames.fulguritefusion;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// All of Fulgurite Fusion's items and its creative tab live here.
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FulguriteFusion.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FulguriteFusion.MODID);

    // Clear grey glass vial, crafted from glass, copper and redstone. Stacks to 16.
    public static final DeferredItem<Item> EMPTY_SPARK_CELL = ITEMS.registerSimpleItem("empty_spark_cell",
            new Item.Properties().stacksTo(16));

    // Charged, crystal-blue vial. The enchantment glint gives it the sparkle. Stacks to 16.
    // No crafting recipe: it will be filled by the Fulgurite Charger.
    public static final DeferredItem<Item> SPARK_CELL = ITEMS.registerSimpleItem("spark_cell",
            new Item.Properties().stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    // Dropped when mining Raw Fulgurite (4-8 per block, boosted by Fortune).
    public static final DeferredItem<Item> FULGURITE_SHARD = ITEMS.registerSimpleItem("fulgurite_shard",
            new Item.Properties());

    // Refined fulgurite: 9 shards in a crafting grid make 2.
    public static final DeferredItem<Item> FULGURITE = ITEMS.registerSimpleItem("fulgurite",
            new Item.Properties());

    // The mod's own creative tab, shown after the Combat tab.
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fulguritefusion"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> SPARK_CELL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.RAW_FULGURITE_ITEM.get());
                        output.accept(FULGURITE_SHARD.get());
                        output.accept(FULGURITE.get());
                        output.accept(EMPTY_SPARK_CELL.get());
                        output.accept(SPARK_CELL.get());
                        output.accept(ModBlocks.STORM_FURNACE_ITEM.get());
                        output.accept(ModBlocks.ENHANCED_STORM_FURNACE_ITEM.get());
                        output.accept(ModBlocks.CHARGED_STORM_FURNACE_ITEM.get());
                        output.accept(ModBlocks.FULGURITE_CHARGER_ITEM.get());
                        output.accept(ModBlocks.STORM_BATTERY_ITEM.get());
                        output.accept(ModBlocks.FE_CONVERTER_ITEM.get());
                        output.accept(ModBlocks.EU_CONVERTER_ITEM.get());
                    })
                    .build());

    private ModItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
