package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.item.WireRollItem;
import com.sideflowgames.fulguritefusion.power.WireMath;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import com.sideflowgames.fulguritefusion.power.FmHandler;
import com.sideflowgames.fulguritefusion.power.FmSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Machine Connector: sticks to any face of any block, like a torch.
//   - On a machine (Storm Furnace, Storm Battery, Charger, converters...): links that machine to the network.
//   - On a plain block (fence, log, wall...): a relay, so you can build power poles. Relays don't count as machines.
//   - On a multiblock machine: only its power port accepts a connector.
// Wire Rolls string hanging wires between connectors. It pops off if the block it's on is removed,
// dropping a roll for each wire attached to it. Right-click (empty hand) to see the network's status.
public class MachineConnectorBlock extends BaseEntityBlock {
    public static final MapCodec<MachineConnectorBlock> CODEC = simpleCodec(MachineConnectorBlock::new);
    // Which way the connector points (away from the block it's stuck to).
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    // The connector's shape when pointing up: {side min, out min, side min, side max, out max, side max}.
    private static final double[][] BOXES = {{2, 0, 2, 14, 2, 14}, {6, 2, 6, 10, 9, 10}, {5, 9, 5, 11, 11, 11}};
    // On thin blocks (fences, walls, posts...) the wide base is swapped for a small collar that hugs the post.
    public static final BooleanProperty SLIM = BooleanProperty.create("slim");
    // How far (in pixels) the post it's on sits back from the edge of its block, e.g. 6 for a fence post, 4 for a
    // wall post. The collar and stem reach back that far, so the connector actually touches the post.
    public static final IntegerProperty GAP = IntegerProperty.create("gap", 0, 8);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape[]> SLIM_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.values()) {
            SHAPES.put(d, build(BOXES, d));
            VoxelShape[] byGap = new VoxelShape[9];
            for (int g = 0; g <= 8; g++) {
                byGap[g] = build(new double[][]{{5, -g, 5, 11, 2 - g, 11}, {6, 2 - g, 6, 10, 9, 10}, BOXES[2]}, d);
            }
            SLIM_SHAPES.put(d, byGap);
        }
    }

    // The gap between this block's edge and the nearest part of the block it's stuck to, measured across the
    // middle of the face (where the collar sits).
    private static int gapFor(BlockGetter level, BlockPos pos, Direction facing) {
        BlockPos attached = pos.relative(facing.getOpposite());
        VoxelShape shape = level.getBlockState(attached).getShape(level, attached);
        if (shape.isEmpty()) return 0;
        Direction.Axis axis = facing.getAxis();
        double best = Double.MAX_VALUE;
        for (net.minecraft.world.phys.AABB box : shape.toAabbs()) {
            // Only boxes under the collar (5..11 px across both other axes).
            boolean under = true;
            for (Direction.Axis other : Direction.Axis.values()) {
                if (other == axis) continue;
                if (box.max(other) <= 5 / 16.0 || box.min(other) >= 11 / 16.0) under = false;
            }
            if (!under) continue;
            // Distance from the face the connector is on to this box.
            double gap = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 - box.max(axis) : box.min(axis);
            best = Math.min(best, gap);
        }
        if (best == Double.MAX_VALUE) return 0;
        return (int) Math.max(0, Math.min(8, Math.ceil(best * 16 - 1e-6)));
    }

    private static VoxelShape build(double[][] boxes, Direction d) {
        VoxelShape shape = Shapes.empty();
        for (double[] b : boxes) shape = Shapes.or(shape, turned(b, d));
        return shape;
    }

    // Slim when the face it's stuck to isn't a full block face (fence posts, walls, bars, slabs...).
    private static boolean slimFor(BlockGetter level, BlockPos pos, Direction facing) {
        BlockPos attached = pos.relative(facing.getOpposite());
        BlockState on = level.getBlockState(attached);
        return !Block.isFaceFull(on.getShape(level, attached), facing);
    }

    // Turn an upward-pointing box to point in direction d (the base always sits against the attached face).
    private static VoxelShape turned(double[] b, Direction d) {
        double a0 = b[0], h0 = b[1], c0 = b[2], a1 = b[3], h1 = b[4], c1 = b[5];
        return switch (d) {
            case UP -> Block.box(a0, h0, c0, a1, h1, c1);
            case DOWN -> Block.box(a0, 16 - h1, c0, a1, 16 - h0, c1);
            case SOUTH -> Block.box(a0, c0, h0, a1, c1, h1);
            case NORTH -> Block.box(a0, c0, 16 - h1, a1, c1, 16 - h0);
            case EAST -> Block.box(h0, c0, a0, h1, c1, a1);
            case WEST -> Block.box(16 - h1, c0, a0, 16 - h0, c1, a1);
        };
    }

    public MachineConnectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(SLIM, false).setValue(GAP, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SLIM, GAP);
    }

    // The block this connector is stuck to.
    public static BlockPos attachedPos(BlockState state, BlockPos pos) {
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.UP;
        return pos.relative(facing.getOpposite());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SLIM) ? SLIM_SHAPES.get(state.getValue(FACING))[state.getValue(GAP)] : SHAPES.get(state.getValue(FACING));
    }

    // A machine = anything that stores, makes or uses Fm (and, for multiblocks, accepts a connector there).
    public static boolean isMachine(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return (be instanceof FmHandler handler && handler.acceptsConnector()) || be instanceof FmSource;
    }

    // Any solid block will hold it, except multiblock machines, which only take it on their power port.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos attached = attachedPos(state, pos);
        BlockState on = level.getBlockState(attached);
        if (on.isAir() || on.canBeReplaced()) return false;
        BlockEntity be = level.getBlockEntity(attached);
        if (be instanceof ArcCasingBlockEntity || be instanceof MachineControllerBlockEntity) {
            return ((FmHandler) be).acceptsConnector();
        }
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        boolean slim = slimFor(context.getLevel(), context.getClickedPos(), facing);
        BlockState state = defaultBlockState().setValue(FACING, facing).setValue(SLIM, slim)
                .setValue(GAP, slim ? gapFor(context.getLevel(), context.getClickedPos(), facing) : 0);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess scheduledTicks, BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState, net.minecraft.util.RandomSource shapeRandom) {
        if (direction == state.getValue(FACING).getOpposite()) {
            if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
            boolean slim = slimFor(level, pos, state.getValue(FACING));
            return state.setValue(SLIM, slim).setValue(GAP, slim ? gapFor(level, pos, state.getValue(FACING)) : 0);
        }
        return super.updateShape(state, level, scheduledTicks, pos, direction, neighbourPos, neighbourState, shapeRandom);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineConnectorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.MACHINE_CONNECTOR_BE.get(), MachineConnectorBlockEntity::serverTick);
    }

    // Let Wire Rolls do their thing instead of the block's own right-click.
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof WireRollItem) return InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    // Removing a connector takes its wires down: one roll drops per wire, and the other end forgets it.
    // 26.1: blocks no longer get onRemove. Called from the block entity just before it is removed
    // (or after removal for blocks without one); newState is the block now standing here.
    public void onRemoveLegacy(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof MachineConnectorBlockEntity connector) {
            for (MachineConnectorBlockEntity.Link link : connector.getLinks()) {
                if (level.getBlockEntity(link.other()) instanceof MachineConnectorBlockEntity other) other.removeLink(pos);
                ItemStack roll = new ItemStack(ModItems.wireItem(link.tier()));
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), roll);
            }
        }
    }

    // Raw wires spark along their length while power is flowing.
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof MachineConnectorBlockEntity connector) || !connector.isPowered()) return;
        Vec3 a = WireMath.attachPoint(pos, state.getValue(FACING));
        for (MachineConnectorBlockEntity.Link link : connector.getLinks()) {
            if (link.insulated() || pos.asLong() > link.other().asLong()) continue; // draw each wire once
            // Thicker raw wire carries more and spits more sparks; raw heavy wire throws the odd ember too.
            int gauge = link.tier().gauge;
            for (int n = 0; n <= gauge; n++) {
                if (random.nextFloat() < 0.35f) {
                    Vec3 p = WireMath.point(a, WireMath.attachPoint(level, link.other()), random.nextDouble(), link.tier());
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 0, -0.02, 0);
                    if (gauge == 2 && random.nextFloat() < 0.15f) level.addParticle(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 0, -0.01, 0);
                }
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineConnectorBlockEntity connector) {
            player.sendOverlayMessage(Component.literal(connector.statusText()));
        }
        return InteractionResult.SUCCESS;
    }
}
