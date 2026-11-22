package com.sideflowgames.fulguritefusion.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Arc Casing: the building block of every processing machine (Crude, Tempered, Charged, Prime = tiers 1-4).
// Once its machine forms, the casing turns invisible and the machine's full model is drawn instead.
public class ArcCasingBlock extends BaseEntityBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final MapCodec<ArcCasingBlock> CODEC = simpleCodec(p -> new ArcCasingBlock(1, p));

    private final int tier;

    public ArcCasingBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    public int tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcCasingBlockEntity(pos, state);
    }

    // A formed casing works like its controller: empty hand opens the machine's menu (sneak: status).
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(FORMED)) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ArcCasingBlockEntity casing && casing.controllerPos() != null) {
            BlockPos c = casing.controllerPos();
            if (level.getBlockEntity(c) instanceof com.sideflowgames.fulguritefusion.harvester.LatexHarvesterBlockEntity h) h.interact(player);
            else MachineControllerBlock.interact(level, c, level.getBlockState(c), player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Holding something: a formed casing still opens the machine's menu, like the controller does, except that a
    // Machine Connector goes onto the casing (so you can put one on the power port). Sneak to place other blocks.
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(FORMED) || stack.is(com.sideflowgames.fulguritefusion.ModBlocks.MACHINE_CONNECTOR_ITEM.get())) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        // A Latex Vulcanizer's casings take a Latex Bucket (or any latex container) poured in.
        if (level.getBlockEntity(pos) instanceof ArcCasingBlockEntity casing && casing.getFluidHandler(null) != null
                && net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, level, pos, null)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // Breaking a casing un-forms its machine.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ArcCasingBlockEntity casing) {
            casing.notifyController();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
