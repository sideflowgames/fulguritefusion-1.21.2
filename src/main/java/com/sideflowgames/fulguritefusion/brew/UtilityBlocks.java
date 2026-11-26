package com.sideflowgames.fulguritefusion.brew;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

// Utility blocks made from Stormreach mob parts. Registered through ModBlocks / ModItems' registers; ModBrews.register
// calls init() before the registers fire.
public final class UtilityBlocks {
    private UtilityBlocks() {}

    public static final DeferredBlock<IonLampBlock> ION_LAMP = ModBlocks.BLOCKS.registerBlock("ion_lamp", IonLampBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.6f).sound(SoundType.GLASS).lightLevel(s -> 15));
    public static final DeferredBlock<StormWardBlock> STORM_WARD = ModBlocks.BLOCKS.registerBlock("storm_ward", StormWardBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(4.0f, 9.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().lightLevel(s -> s.getValue(StormWardBlock.ACTIVE) ? 9 : 0));
    public static final DeferredBlock<UpdraftVentBlock> UPDRAFT_VENT = ModBlocks.BLOCKS.registerBlock("updraft_vent", UpdraftVentBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<SilkRopeBlock> SILK_ROPE = ModBlocks.BLOCKS.registerBlock("silk_rope", SilkRopeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.2f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<ReaverSpikesBlock> REAVER_SPIKES = ModBlocks.BLOCKS.registerBlock("reaver_spikes", ReaverSpikesBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> ION_LAMP_ITEM = ModItems.ITEMS.registerSimpleBlockItem("ion_lamp", ION_LAMP);
    public static final DeferredItem<BlockItem> STORM_WARD_ITEM = ModItems.ITEMS.registerSimpleBlockItem("storm_ward", STORM_WARD);
    public static final DeferredItem<BlockItem> UPDRAFT_VENT_ITEM = ModItems.ITEMS.registerSimpleBlockItem("updraft_vent", UPDRAFT_VENT);
    public static final DeferredItem<BlockItem> SILK_ROPE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("silk_rope", SILK_ROPE);
    public static final DeferredItem<BlockItem> REAVER_SPIKES_ITEM = ModItems.ITEMS.registerSimpleBlockItem("reaver_spikes", REAVER_SPIKES);

    public static final Supplier<BlockEntityType<StormWardBlockEntity>> STORM_WARD_BE = ModBlocks.BLOCK_ENTITIES.register("storm_ward",
            () -> new BlockEntityType<>(StormWardBlockEntity::new, STORM_WARD.get()));

    public static void init() {}

    public static void addToTab(CreativeModeTab.Output output) {
        output.accept(ION_LAMP_ITEM.get());
        output.accept(STORM_WARD_ITEM.get());
        output.accept(UPDRAFT_VENT_ITEM.get());
        output.accept(SILK_ROPE_ITEM.get());
        output.accept(REAVER_SPIKES_ITEM.get());
        for (var p : ModBrews.POTIONS.getEntries()) {
            for (Item i : new Item[] {net.minecraft.world.item.Items.POTION, net.minecraft.world.item.Items.SPLASH_POTION,
                    net.minecraft.world.item.Items.LINGERING_POTION}) {
                output.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(i, p));
            }
        }
    }
}
