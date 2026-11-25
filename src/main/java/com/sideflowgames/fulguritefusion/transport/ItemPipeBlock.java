package com.sideflowgames.fulguritefusion.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

// Item Pipe, in three tiers: Copper (2 items a tick), Fulgurite (16) and Stormforged (32). Pipes of any tier join
// each other and any inventory beside them. Right-click a pipe where it meets an inventory to switch that side
// between Insert (items are delivered into it), Extract (items are pulled out of it) and Off. Sneak + right-click an
// Insert or Extract side of a Fulgurite or Stormforged pipe to give it a filter. Items move instantly; the glass
// windows show them sliding along. The speed is set by the pipe doing the extracting.
public class ItemPipeBlock extends BaseEntityBlock {
    public static final MapCodec<ItemPipeBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(b -> b.tier), propertiesCodec()).apply(i, ItemPipeBlock::new));
    public static final Map<Direction, EnumProperty<PipeSide>> SIDES = new EnumMap<>(Direction.class);
    static {
        for (Direction d : Direction.values()) SIDES.put(d, EnumProperty.create(d.getSerializedName(), PipeSide.class));
    }
    private static final VoxelShape[] SHAPES = buildShapes();

    private final int tier;

    public ItemPipeBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
        BlockState state = stateDefinition.any();
        for (EnumProperty<PipeSide> side : SIDES.values()) state = state.setValue(side, PipeSide.NONE);
        registerDefaultState(state);
    }

    public int tier() {
        return tier;
    }

    // Items a tick this tier moves.
    public static int rate(int tier) {
        return tier == 3 ? 32 : tier == 2 ? 16 : 2;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (EnumProperty<PipeSide> side : SIDES.values()) builder.add(side);
    }

    static boolean hasInventory(LevelAccessor level, BlockPos pos, Direction d) {
        BlockPos n = pos.relative(d);
        if (level.getBlockState(n).getBlock() instanceof ItemPipeBlock) return false;
        return level instanceof Level l && l.getCapability(Capabilities.ItemHandler.BLOCK, n, d.getOpposite()) != null;
    }

    static PipeSide sideFor(LevelAccessor level, BlockPos pos, Direction d) {
        if (level.getBlockState(pos.relative(d)).getBlock() instanceof ItemPipeBlock) return PipeSide.PIPE;
        if (!hasInventory(level, pos, d)) return PipeSide.NONE;
        int mode = level.getBlockEntity(pos) instanceof ItemPipeBlockEntity be ? be.mode(d) : ItemPipeBlockEntity.IN;
        return mode == ItemPipeBlockEntity.OFF ? PipeSide.NONE : mode == ItemPipeBlockEntity.OUT ? PipeSide.OUT : PipeSide.IN;
    }

    static BlockState computed(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) state = state.setValue(SIDES.get(d), sideFor(level, pos, d));
        return state;
    }

    static void refresh(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof ItemPipeBlock)) return;
        BlockState now = computed(s, level, pos);
        if (now != s) level.setBlock(pos, now, Block.UPDATE_ALL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computed(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return state.setValue(SIDES.get(direction), sideFor(level, pos, direction));
    }

    // An inventory's capability can come and go without a shape change (a machine forming, say).
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (!level.isClientSide) refresh(level, pos);
    }

    // Which inventory side the player clicked: the arm they hit, or the face of the core.
    private static Direction clickedSide(BlockPos pos, BlockHitResult hit) {
        Vec3 rel = hit.getLocation().subtract(Vec3.atCenterOf(pos));
        Direction best = hit.getDirection();
        double bestV = 0.3;
        for (Direction d : Direction.values()) {
            double v = rel.x * d.getStepX() + rel.y * d.getStepY() + rel.z * d.getStepZ();
            if (v > bestV) {
                bestV = v;
                best = d;
            }
        }
        return best;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Direction d = clickedSide(pos, hit);
        if (!hasInventory(level, pos, d)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity be)) return InteractionResult.PASS;
        String side = d.getSerializedName().substring(0, 1).toUpperCase() + d.getSerializedName().substring(1);
        if (player.isShiftKeyDown()) {
            if (tier == 1) {
                player.displayClientMessage(Component.literal("Filters need a Fulgurite or Stormforged Item Pipe")
                        .withStyle(ChatFormatting.GOLD), true);
            } else if (be.mode(d) == ItemPipeBlockEntity.OFF) {
                player.displayClientMessage(Component.literal(side + " is off").withStyle(ChatFormatting.GRAY), true);
            } else if (player instanceof ServerPlayer sp) {
                sp.openMenu(be.filterMenu(d), buf -> be.writeFilterMenu(buf, d));
            }
            return InteractionResult.CONSUME;
        }
        int mode = (be.mode(d) + 1) % 3;
        be.setMode(d, mode);
        refresh(level, pos);
        String what = mode == ItemPipeBlockEntity.OUT ? "Extract" : mode == ItemPipeBlockEntity.IN ? "Insert" : "Off";
        ChatFormatting color = mode == ItemPipeBlockEntity.OUT ? ChatFormatting.GOLD : mode == ItemPipeBlockEntity.IN
                ? ChatFormatting.AQUA : ChatFormatting.GRAY;
        player.displayClientMessage(Component.literal(side + ": " + what).withStyle(color), true);
        return InteractionResult.CONSUME;
    }

    private static VoxelShape[] buildShapes() {
        double a = 5, b = 11;
        VoxelShape core = Block.box(a, a, a, b, b, b);
        VoxelShape[] arms = new VoxelShape[6];
        for (Direction d : Direction.values()) {
            double x0 = a, y0 = a, z0 = a, x1 = b, y1 = b, z1 = b;
            switch (d) {
                case DOWN -> y0 = 0;
                case UP -> y1 = 16;
                case NORTH -> z0 = 0;
                case SOUTH -> z1 = 16;
                case WEST -> x0 = 0;
                case EAST -> x1 = 16;
            }
            arms[d.ordinal()] = Block.box(x0, y0, z0, x1, y1, z1);
        }
        VoxelShape[] shapes = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = core;
            for (Direction d : Direction.values()) if ((mask & (1 << d.ordinal())) != 0) s = Shapes.or(s, arms[d.ordinal()]);
            shapes[mask] = s;
        }
        return shapes;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction d : Direction.values()) if (state.getValue(SIDES.get(d)) != PipeSide.NONE) mask |= 1 << d.ordinal();
        return SHAPES[mask];
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TransportBlocks.ITEM_PIPE_BE.get(), ItemPipeBlockEntity::serverTick);
    }
}
