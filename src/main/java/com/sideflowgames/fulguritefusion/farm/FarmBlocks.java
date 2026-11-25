package com.sideflowgames.fulguritefusion.farm;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

// The Shock Farm: a Fm-powered multiblock that spawns the mobs of the biome it stands in on a pad of Galvanic Soil,
// electrocutes them with Tesla Prongs and gathers the drops and XP into a Farm Collector. See ShockFarmControllerBlockEntity.
public final class FarmBlocks {
    private FarmBlocks() {}

    public static final DeferredBlock<GalvanicSoilBlock> GALVANIC_SOIL = ModBlocks.BLOCKS.registerBlock("galvanic_soil", GalvanicSoilBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.8f).sound(SoundType.SOUL_SOIL));
    public static final DeferredBlock<TeslaProngBlock> TESLA_PRONG = ModBlocks.BLOCKS.registerBlock("tesla_prong", TeslaProngBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 6.0f).sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredBlock<ShockFarmControllerBlock> CONTROLLER = ModBlocks.BLOCKS.registerBlock("shock_farm_controller",
            ShockFarmControllerBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 8.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(ShockFarmControllerBlock.ACTIVE) ? 7 : 0));
    public static final DeferredBlock<FarmCollectorBlock> COLLECTOR = ModBlocks.BLOCKS.registerBlock("farm_collector", FarmCollectorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<FarmPowerPortBlock> POWER_PORT = ModBlocks.BLOCKS.registerBlock("farm_power_port", FarmPowerPortBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> GALVANIC_SOIL_ITEM = ModItems.ITEMS.registerSimpleBlockItem("galvanic_soil", GALVANIC_SOIL);
    public static final DeferredItem<BlockItem> TESLA_PRONG_ITEM = ModItems.ITEMS.registerSimpleBlockItem("tesla_prong", TESLA_PRONG);
    public static final DeferredItem<BlockItem> CONTROLLER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("shock_farm_controller", CONTROLLER);
    public static final DeferredItem<BlockItem> COLLECTOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem("farm_collector", COLLECTOR);
    public static final DeferredItem<BlockItem> POWER_PORT_ITEM = ModItems.ITEMS.registerSimpleBlockItem("farm_power_port", POWER_PORT);

    // Upgrade cards for the controller's upgrade slots (see FarmUpgradeItem).
    public static final DeferredItem<FarmUpgradeItem> UPGRADE_CARD = card("farm_upgrade_card", FarmUpgradeItem.Kind.BLANK, 0);
    public static final DeferredItem<FarmUpgradeItem> MOB_FILTER_CARD = card("mob_filter_card", FarmUpgradeItem.Kind.MOB_FILTER, 0);
    public static final DeferredItem<FarmUpgradeItem> SPEED_CARD_1 = card("speed_card_1", FarmUpgradeItem.Kind.SPEED, 1);
    public static final DeferredItem<FarmUpgradeItem> SPEED_CARD_2 = card("speed_card_2", FarmUpgradeItem.Kind.SPEED, 2);
    public static final DeferredItem<FarmUpgradeItem> SPEED_CARD_3 = card("speed_card_3", FarmUpgradeItem.Kind.SPEED, 3);
    public static final DeferredItem<FarmUpgradeItem> EFFICIENCY_CARD_1 = card("efficiency_card_1", FarmUpgradeItem.Kind.EFFICIENCY, 1);
    public static final DeferredItem<FarmUpgradeItem> EFFICIENCY_CARD_2 = card("efficiency_card_2", FarmUpgradeItem.Kind.EFFICIENCY, 2);
    public static final DeferredItem<FarmUpgradeItem> EFFICIENCY_CARD_3 = card("efficiency_card_3", FarmUpgradeItem.Kind.EFFICIENCY, 3);
    public static final DeferredItem<FarmUpgradeItem> LOOTING_CARD_1 = card("looting_card_1", FarmUpgradeItem.Kind.LOOTING, 1);
    public static final DeferredItem<FarmUpgradeItem> LOOTING_CARD_2 = card("looting_card_2", FarmUpgradeItem.Kind.LOOTING, 2);
    public static final DeferredItem<FarmUpgradeItem> LOOTING_CARD_3 = card("looting_card_3", FarmUpgradeItem.Kind.LOOTING, 3);

    private static DeferredItem<FarmUpgradeItem> card(String name, FarmUpgradeItem.Kind kind, int tier) {
        return ModItems.ITEMS.registerItem(name, p -> new FarmUpgradeItem(kind, tier, p), new net.minecraft.world.item.Item.Properties().stacksTo(16));
    }

    public static final Supplier<BlockEntityType<ShockFarmControllerBlockEntity>> CONTROLLER_BE = ModBlocks.BLOCK_ENTITIES.register(
            "shock_farm_controller", () -> BlockEntityType.Builder.of(ShockFarmControllerBlockEntity::new, CONTROLLER.get()).build(null));
    public static final Supplier<BlockEntityType<FarmCollectorBlockEntity>> COLLECTOR_BE = ModBlocks.BLOCK_ENTITIES.register(
            "farm_collector", () -> BlockEntityType.Builder.of(FarmCollectorBlockEntity::new, COLLECTOR.get()).build(null));

    public static final Supplier<BlockEntityType<FarmPowerPortBlockEntity>> POWER_PORT_BE = ModBlocks.BLOCK_ENTITIES.register(
            "farm_power_port", () -> BlockEntityType.Builder.of(FarmPowerPortBlockEntity::new, POWER_PORT.get()).build(null));

    public static void init() {
        FarmMenus.init();
    }

    public static void addToTab(CreativeModeTab.Output output) {
        output.accept(GALVANIC_SOIL_ITEM.get());
        output.accept(TESLA_PRONG_ITEM.get());
        output.accept(CONTROLLER_ITEM.get());
        output.accept(COLLECTOR_ITEM.get());
        output.accept(POWER_PORT_ITEM.get());
        for (DeferredItem<FarmUpgradeItem> c : java.util.List.of(UPGRADE_CARD, MOB_FILTER_CARD, SPEED_CARD_1, SPEED_CARD_2, SPEED_CARD_3,
                EFFICIENCY_CARD_1, EFFICIENCY_CARD_2, EFFICIENCY_CARD_3, LOOTING_CARD_1, LOOTING_CARD_2, LOOTING_CARD_3)) {
            output.accept(c.get());
        }
    }
}
