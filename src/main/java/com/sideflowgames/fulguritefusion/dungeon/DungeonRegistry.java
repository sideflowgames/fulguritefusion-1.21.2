package com.sideflowgames.fulguritefusion.dungeon;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DungeonRegistry {
    private DungeonRegistry() {}

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, FulguriteFusion.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE, FulguriteFusion.MODID);

    public static final Supplier<StructureType<DungeonStructure>> DUNGEON = STRUCTURE_TYPES.register("stormreach_dungeon",
            () -> () -> DungeonStructure.CODEC);
    public static final Supplier<StructurePieceType> ROOM = PIECE_TYPES.register("dungeon_room",
            () -> (StructurePieceType.ContextlessType) DungeonPiece::new);

    public static void register(IEventBus modEventBus) {
        DungeonBlocks.init();
        STRUCTURE_TYPES.register(modEventBus);
        PIECE_TYPES.register(modEventBus);
    }
}
