package com.sideflowgames.fulguritefusion.stormreach;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

// Arcgrass Block: the Stormreach's grass. It spreads onto nearby Charged Loam in the light, turns back into
// Charged Loam when covered, and bone meal grows Arcgrass, ferns and storm flowers around it.
public class ArcgrassBlock extends Block implements BonemealableBlock {
    public static final MapCodec<ArcgrassBlock> CODEC = simpleCodec(ArcgrassBlock::new);

    public ArcgrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    private static boolean canStay(LevelReader level, BlockPos pos) {
        BlockPos up = pos.above();
        BlockState above = level.getBlockState(up);
        return above.getFluidState().isEmpty() && !above.isFaceSturdy(level, up, Direction.DOWN);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canStay(level, pos)) {
            level.setBlockAndUpdate(pos, ModBlocks.CHARGED_LOAM.get().defaultBlockState());
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) < 9) return;
        for (int i = 0; i < 4; i++) {
            BlockPos p = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (level.getBlockState(p).is(ModBlocks.CHARGED_LOAM.get()) && canStay(level, p)) {
                level.setBlockAndUpdate(p, defaultBlockState());
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        for (int i = 0; i < 48; i++) {
            BlockPos p = pos.above();
            int steps = random.nextInt(4) + 1;
            for (int s = 0; s < steps; s++) p = p.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (!level.getBlockState(p).isAir() || !level.getBlockState(p.below()).is(this)) continue;
            int roll = random.nextInt(16);
            Block plant = roll < 10 ? ModBlocks.ARCGRASS.get()
                    : roll < 13 ? ModBlocks.ARC_FERN.get()
                    : roll == 13 ? ModBlocks.VOLTBLOOM.get()
                    : roll == 14 ? ModBlocks.STORMCAP.get()
                    : ModBlocks.AURORA_LILY.get();
            level.setBlock(p, plant.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
