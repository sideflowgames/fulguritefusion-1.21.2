package com.sideflowgames.fulguritefusion.transport;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.ModMenus;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

// Item transport: Item Pipes (Copper, Fulgurite, Stormforged) and the Arc Sender / Arc Receiver with the Arc Tuner.
public final class TransportBlocks {
    private TransportBlocks() {}

    private static BlockBehaviour.Properties pipe(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(1.0f, 4.0f).sound(SoundType.COPPER).noOcclusion();
    }

    public static final DeferredBlock<ItemPipeBlock> COPPER_PIPE = ModBlocks.BLOCKS.registerBlock("copper_item_pipe",
            p -> new ItemPipeBlock(1, p), () -> pipe(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<ItemPipeBlock> FULGURITE_PIPE = ModBlocks.BLOCKS.registerBlock("fulgurite_item_pipe",
            p -> new ItemPipeBlock(2, p), () -> pipe(MapColor.COLOR_CYAN));
    public static final DeferredBlock<ItemPipeBlock> STORMFORGED_PIPE = ModBlocks.BLOCKS.registerBlock("stormforged_item_pipe",
            p -> new ItemPipeBlock(3, p), () -> pipe(MapColor.COLOR_PURPLE));

    public static final DeferredBlock<ArcNodeBlock> ARC_SENDER = ModBlocks.BLOCKS.registerBlock("arc_sender",
            p -> new ArcNodeBlock(true, p), () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 6.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 5));
    public static final DeferredBlock<ArcNodeBlock> ARC_RECEIVER = ModBlocks.BLOCKS.registerBlock("arc_receiver",
            p -> new ArcNodeBlock(false, p), () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.0f, 6.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 5));

    public static final DeferredItem<BlockItem> COPPER_PIPE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("copper_item_pipe", COPPER_PIPE);
    public static final DeferredItem<BlockItem> FULGURITE_PIPE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("fulgurite_item_pipe", FULGURITE_PIPE);
    public static final DeferredItem<BlockItem> STORMFORGED_PIPE_ITEM = ModItems.ITEMS.registerSimpleBlockItem("stormforged_item_pipe", STORMFORGED_PIPE);
    public static final DeferredItem<BlockItem> ARC_SENDER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("arc_sender", ARC_SENDER);
    public static final DeferredItem<BlockItem> ARC_RECEIVER_ITEM = ModItems.ITEMS.registerSimpleBlockItem("arc_receiver", ARC_RECEIVER);
    public static final DeferredItem<ArcTunerItem> ARC_TUNER = ModItems.ITEMS.registerItem("arc_tuner", ArcTunerItem::new,
            () -> new Item.Properties().stacksTo(1));

    public static final Supplier<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE_BE = ModBlocks.BLOCK_ENTITIES.register("item_pipe",
            () -> new BlockEntityType<>(ItemPipeBlockEntity::new, COPPER_PIPE.get(), FULGURITE_PIPE.get(), STORMFORGED_PIPE.get()));
    public static final Supplier<BlockEntityType<ArcSenderBlockEntity>> ARC_SENDER_BE = ModBlocks.BLOCK_ENTITIES.register("arc_sender",
            () -> new BlockEntityType<>(ArcSenderBlockEntity::new, ARC_SENDER.get()));

    public static final Supplier<MenuType<PipeFilterMenu>> PIPE_FILTER_MENU =
            ModMenus.MENUS.register("pipe_filter", () -> IMenuTypeExtension.create(PipeFilterMenu::new));

    public static void register(IEventBus modEventBus) {
        PipeTravel.register(modEventBus);
    }

    public static void addToTab(CreativeModeTab.Output output) {
        output.accept(COPPER_PIPE_ITEM.get());
        output.accept(FULGURITE_PIPE_ITEM.get());
        output.accept(STORMFORGED_PIPE_ITEM.get());
        output.accept(ARC_SENDER_ITEM.get());
        output.accept(ARC_RECEIVER_ITEM.get());
        output.accept(ARC_TUNER.get());
    }
}
