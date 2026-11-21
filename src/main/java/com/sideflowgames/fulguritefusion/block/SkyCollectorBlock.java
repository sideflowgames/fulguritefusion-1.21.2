package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Sky Collector: an open-sky dish that gathers what falls from the sky. No power needed.
//   - Clear nights, high up (Y 200 or more): Aurora Dust.
//   - Clear middays in a desert or badlands: Sunmote.
// It needs open sky above it. Hoppers can pull from it; right-click with an empty hand to take what it holds.
public class SkyCollectorBlock extends BaseEntityBlock {
    public static final MapCodec<SkyCollectorBlock> CODEC = simpleCodec(SkyCollectorBlock::new);
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 6, 12), Block.box(1, 6, 1, 15, 10, 15));

    public enum Mode implements StringRepresentable {
        IDLE("idle"), AURORA("aurora"), SUN("sun");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public SkyCollectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.IDLE));
    }

    public static int light(BlockState state) {
        return state.getValue(MODE) == Mode.IDLE ? 0 : 9;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SkyCollectorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.SKY_COLLECTOR_BE.get(), SkyCollectorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SkyCollectorBlockEntity be) be.interact(player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SkyCollectorBlockEntity be) be.dropContents();
        super.onRemove(state, level, pos, newState, moved);
    }

    // Shimmer while it gathers.
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Mode mode = state.getValue(MODE);
        if (mode == Mode.IDLE || random.nextInt(3) != 0) return;
        double x = pos.getX() + 0.2 + random.nextDouble() * 0.6, z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
        double y = pos.getY() + 1.2 + random.nextDouble() * 1.5;
        level.addParticle(mode == Mode.AURORA ? ParticleTypes.GLOW : ParticleTypes.WAX_ON, x, y, z, 0, -0.05, 0);
    }
}
