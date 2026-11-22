package com.sideflowgames.fulguritefusion.harvester;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Shared helpers for the blocks of a Latex Harvester.
final class HarvesterPart {
    private HarvesterPart() {}

    // Set while the harvester is built: the block hides and the harvester's model draws it.
    static final BooleanProperty FORMED = BooleanProperty.create("formed");

    // Right-click any part: the harvester's status (or what's missing).
    static void interact(Level level, BlockPos controller, Player player) {
        if (level.getBlockEntity(controller) instanceof LatexHarvesterBlockEntity h) h.interact(player);
    }

    static void notify(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HarvesterPartBlockEntity part && part.controllerPos() != null
                && level.getBlockEntity(part.controllerPos()) instanceof LatexHarvesterBlockEntity h) {
            h.requestCheck();
        }
    }
}
