package com.sideflowgames.fulguritefusion.rift;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// The surface of an open Stormreach rift. Walk into it to cross over, if the storm knows you (you've defeated the
// Storm Titan). Like a Nether portal, it collapses as soon as its frame is broken.
public class RiftPortalBlock extends Block {
    public static final MapCodec<RiftPortalBlock> CODEC = simpleCodec(RiftPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_SHAPE = Block.box(6, 0, 0, 10, 16, 16);

    public RiftPortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    // The rift holds only while every neighbour in its own plane is more rift, frame or keystone.
    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess scheduledTicks, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource shapeRandom) {
        Direction.Axis axis = state.getValue(AXIS);
        boolean inPlane = dir.getAxis() == Direction.Axis.Y || dir.getAxis() == axis;
        if (inPlane && !holds(neighbor)) return Blocks.AIR.defaultBlockState();
        return state;
    }

    static boolean holds(BlockState s) {
        return s.is(ModBlocks.RIFT_PORTAL.get()) || s.is(ModBlocks.RIFT_FRAME.get()) || s.is(ModBlocks.RIFT_KEYSTONE.get());
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, net.minecraft.world.entity.InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) RiftPortals.enter(player, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + random.nextDouble(), y = pos.getY() + random.nextDouble(), z = pos.getZ() + random.nextDouble();
            level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.REVERSE_PORTAL, x, y, z,
                    (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2);
        }
    }
}
