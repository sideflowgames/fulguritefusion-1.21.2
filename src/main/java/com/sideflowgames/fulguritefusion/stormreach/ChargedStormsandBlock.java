package com.sideflowgames.fulguritefusion.stormreach;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.state.BlockState;

// Charged Stormsand: glowing sand on the Stormreach's high ground. It falls like sand, gives off the odd spark,
// and a lightning strike fuses it into Fulgurite Glass (see StormreachStorm).
public class ChargedStormsandBlock extends ColoredFallingBlock {
    public static final MapCodec<ColoredFallingBlock> CODEC = simpleCodec(p -> new ChargedStormsandBlock(p));

    public ChargedStormsandBlock(Properties properties) {
        super(new ColorRGBA(0x9C8CC8), properties);
    }

    @Override
    public MapCodec<ColoredFallingBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(24) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + random.nextDouble(), pos.getY() + 1.02,
                    pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }
}
