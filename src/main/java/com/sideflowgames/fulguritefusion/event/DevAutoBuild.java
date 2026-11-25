package com.sideflowgames.fulguritefusion.event;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.client.GhostBuild;
import java.util.List;
import com.sideflowgames.fulguritefusion.block.FusionColliderBlockEntity;
import com.sideflowgames.fulguritefusion.block.ReactorConsoleBlock;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.level.BlockEvent;

// Dev only (does nothing in a release build or on a dedicated server): a creative player who places a Fusion
// Collider gets the whole Fusion Reactor built around it at once: blast chamber, both rings, injectors, portals
// and ports (exactly the ghost build), plus a Reactor Control Console outside the Ring Injection Port corner and
// the two Data Cables joining them, so the rings link and form within a few seconds. Sneak while placing to put
// down just the block.
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class DevAutoBuild {
    private DevAutoBuild() {}

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (FMLEnvironment.production || !FMLEnvironment.dist.isClient()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Player player) || !player.isCreative() || player.isShiftKeyDown()) return;
        if (!event.getPlacedBlock().is(ModBlocks.FUSION_COLLIDER.get())) return;

        List<GhostBuild.Ghost> ghosts = GhostBuild.fusionReactor(level, event.getPos());
        int placed = 0;
        for (GhostBuild.Ghost g : ghosts) {
            level.setBlock(g.pos(), g.want() == null ? Blocks.AIR.defaultBlockState() : g.want(), Block.UPDATE_ALL);
            placed++;
        }
        placed += console(level, event.getPos());
        player.displayClientMessage(Component.literal("[Dev] Fusion Reactor auto-built: " + placed + " blocks placed"
                        + " (console and cables at the Ring Injection Port corner)")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    // The Reactor Control Console, 4 blocks out past the corner with the Ring Injection Ports and facing away
    // from the reactor, and a Data Cable from each of its data ports to one port:
    //   port (46, 44) -> out along x to 51, then along z to the console's right data port (51, 50)
    //   port (44, 46) -> out along z to 49, then along x to the console's left data port (49, 50)
    // (shown for the +x/+z corner; the other corners are mirrored).
    private static int console(ServerLevel level, BlockPos core) {
        int corner = 0;
        search:
        for (int c = 0; c < FusionColliderBlockEntity.CORNERS.length; c++) {
            for (int[] spot : FusionColliderBlockEntity.injectionSpots(c)) {
                if (level.getBlockState(core.offset(spot[0], 0, spot[1])).is(ModBlocks.RING_INJECTION_PORT.get())) {
                    corner = c;
                    break search;
                }
            }
        }
        int sx = FusionColliderBlockEntity.CORNERS[corner][0], sz = FusionColliderBlockEntity.CORNERS[corner][1];
        BlockPos panel = core.offset(sx * 50, 1, sz * 50);
        Direction front = sz > 0 ? Direction.SOUTH : Direction.NORTH;
        int placed = 0;
        level.setBlock(panel, ModBlocks.REACTOR_CONSOLE.get().defaultBlockState().setValue(ReactorConsoleBlock.FACING, front), Block.UPDATE_ALL);
        placed++;
        for (GhostBuild.Ghost g : GhostBuild.console(level, panel)) {
            if (g.want() == null) continue;
            level.setBlock(g.pos(), g.want(), Block.UPDATE_ALL);
            placed++;
        }
        List<BlockPos> cable = new ArrayList<>();
        for (int x = 47; x <= 51; x++) cable.add(core.offset(sx * x, 0, sz * 44));
        for (int z = 45; z <= 49; z++) cable.add(core.offset(sx * 51, 0, sz * z));
        for (int z = 47; z <= 49; z++) cable.add(core.offset(sx * 44, 0, sz * z));
        for (int x = 45; x <= 49; x++) cable.add(core.offset(sx * x, 0, sz * 49));
        for (BlockPos p : cable) {
            level.setBlock(p, ModBlocks.DATA_CABLE.get().defaultBlockState(), Block.UPDATE_ALL);
            placed++;
        }
        for (BlockPos p : cable) {
            level.setBlock(p, Block.updateFromNeighbourShapes(level.getBlockState(p), level, p), Block.UPDATE_ALL);
        }
        return placed;
    }
}
