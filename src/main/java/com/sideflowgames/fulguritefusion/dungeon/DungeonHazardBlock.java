package com.sideflowgames.fulguritefusion.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// The four dungeons' signature hazards (behaviour in DungeonHazardBlockEntity):
//   0 Storm Conductor (Stormspire balconies): marks the ground under a nearby player, then lightning strikes there
//   1 Crusher (Ashen Foundry ceilings): slams down on the walkway below every few seconds
//   2 Rime Totem (Aurora Sanctum): freezes everyone near it
//   3 Hanging Prism (Prism Vault ceilings): breaks loose and falls when someone walks underneath
public class DungeonHazardBlock extends BaseEntityBlock {
    public static final int CONDUCTOR = 0, CRUSHER = 1, TOTEM = 2, PRISM = 3;
    private static final VoxelShape[] SHAPES = {
            Block.box(4, 0, 4, 12, 16, 12), Block.box(0, 0, 0, 16, 16, 16),
            Block.box(3, 0, 3, 13, 16, 13), Block.box(4, 4, 4, 12, 16, 12)};

    public final int hazard;
    private final MapCodec<DungeonHazardBlock> codec;

    public DungeonHazardBlock(int hazard, Properties properties) {
        super(properties);
        this.hazard = hazard;
        this.codec = simpleCodec(p -> new DungeonHazardBlock(hazard, p));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[hazard];
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonHazardBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, DungeonBlocks.HAZARD_BE.get(), DungeonHazardBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        switch (hazard) {
            case CONDUCTOR -> {
                if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 1.0, z, 0, 0.05, 0);
            }
            case CRUSHER -> {
                if (random.nextInt(6) == 0) level.addParticle(ParticleTypes.SMOKE, x + random.nextGaussian() * 0.3, pos.getY() - 0.05, z + random.nextGaussian() * 0.3, 0, -0.02, 0);
            }
            case TOTEM -> {
                for (int i = 0; i < 2; i++) level.addParticle(ParticleTypes.SNOWFLAKE, x + random.nextGaussian() * 2.5, y + random.nextGaussian(), z + random.nextGaussian() * 2.5, 0, -0.02, 0);
            }
            default -> {
                if (random.nextInt(5) == 0) level.addParticle(ParticleTypes.END_ROD, x, pos.getY() + 0.2, z, 0, -0.03, 0);
            }
        }
    }
}
