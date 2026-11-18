package com.sideflowgames.fulguritefusion.event;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.block.StormFurnaceBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

// Reacts to lightning strikes:
//  - a furnace that is struck becomes a Storm Furnace
//  - sand that is struck grows a Raw Fulgurite vein downward
// A lightning rod standing on a furnace or on sand passes the strike to the block below it.
@EventBusSubscriber(modid = FulguriteFusion.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class LightningEvents {
    private LightningEvents() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) return;
        if (!(event.getEntity() instanceof LightningBolt bolt)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos hit = BlockPos.containing(bolt.getX(), bolt.getY() - 1.0E-6, bolt.getZ());
        BlockState state = level.getBlockState(hit);
        if (state.is(Blocks.LIGHTNING_ROD)) {
            hit = hit.below();
            state = level.getBlockState(hit);
        }

        if (state.is(Blocks.FURNACE)) {
            Direction facing = state.getValue(AbstractFurnaceBlock.FACING);
            // Replacing the furnace makes it drop whatever was inside, like breaking it.
            level.setBlock(hit, ModBlocks.STORM_FURNACE.get().defaultBlockState()
                    .setValue(StormFurnaceBlock.FACING, facing), Block.UPDATE_ALL);
        } else if (isSand(state)) {
            growVein(level, hit, level.getRandom());
        }
    }

    private static boolean isSand(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND);
    }

    // A vein of 3 to 6 blocks that branches mostly downward through the sand.
    private static void growVein(ServerLevel level, BlockPos start, RandomSource random) {
        BlockState fulgurite = ModBlocks.RAW_FULGURITE.get().defaultBlockState();
        int size = 3 + random.nextInt(4);
        BlockPos current = start;
        level.setBlock(current, fulgurite, Block.UPDATE_ALL);
        int placed = 1;
        for (int attempt = 0; attempt < size * 4 && placed < size; attempt++) {
            BlockPos next = random.nextFloat() < 0.7f
                    ? current.below()
                    : current.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
            if (isSand(level.getBlockState(next))) {
                level.setBlock(next, fulgurite, Block.UPDATE_ALL);
                current = next;
                placed++;
            }
        }
    }
}
