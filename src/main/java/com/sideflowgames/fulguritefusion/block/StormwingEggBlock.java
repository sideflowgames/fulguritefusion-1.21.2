package com.sideflowgames.fulguritefusion.block;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.entity.StormwingEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

// Stormwing Egg: found (rarely) in wild nests. Place it beside a running Storm Collider to incubate it;
// whoever places it is the hatchling's owner. Breaking it keeps its incubation progress in the item.
public class StormwingEggBlock extends BaseEntityBlock implements com.sideflowgames.fulguritefusion.util.TooltipBlockItem.Tooltip {
    public static final MapCodec<StormwingEggBlock> CODEC = simpleCodec(StormwingEggBlock::new);
    private static final VoxelShape SHAPE = BaseEntityBlock.box(4, 0, 4, 12, 11, 12);

    public StormwingEggBlock(Properties properties) {
        super(properties);
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
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StormwingEggBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.STORMWING_EGG_BE.get(), StormwingEggBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof StormwingEggBlockEntity egg) {
            egg.setOwner(player.getUUID());
        }
    }

    // Taking an egg from a nest angers its Stormwing.
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) StormwingEntity.alarm(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof StormwingEggBlockEntity egg && egg.hasProgress()) {
            for (ItemStack stack : drops) {
                if (stack.is(asItem())) {
                    var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                            net.minecraft.util.ProblemReporter.DISCARDING, egg.getLevel().registryAccess());
                    com.sideflowgames.fulguritefusion.util.NbtBridge.write(out, egg.saveProgress());
                    BlockItem.setBlockEntityData(stack, ModBlocks.STORMWING_EGG_BE.get(), out);
                }
            }
        }
        return drops;
    }

    @Override
    public void appendItemTooltip(ItemStack stack, Item.TooltipContext context, java.util.function.Consumer<Component> tooltipOut, TooltipFlag flag) {
        java.util.List<Component> tooltip = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut);
        tooltip.add(Component.literal("Incubate beside a running Storm Collider").withStyle(ChatFormatting.GRAY));
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTagWithoutId();
            int target = tag.getIntOr("Target", 0);
            if (target > 0) {
                int pct = Math.min(100, tag.getIntOr("Incubation", 0) * 100 / target);
                tooltip.add(Component.literal("Incubated: " + pct + "%").withStyle(ChatFormatting.AQUA));
            }
        }
    }

    // A faint crackle while it's warm.
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.3 + random.nextDouble() * 0.5, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
    }
}
