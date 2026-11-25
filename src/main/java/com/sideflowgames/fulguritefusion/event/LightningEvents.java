package com.sideflowgames.fulguritefusion.event;

import java.util.List;
import java.util.function.Predicate;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.block.StormFurnaceBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

// Reacts to lightning strikes:
//  - a vanilla furnace that is struck becomes a Weak Storm Furnace
//  - a Weak or Enhanced Storm Furnace struck with the right offerings upgrades one tier (the ritual)
//  - sand that is struck grows a Raw Fulgurite vein downward
// A lightning rod standing on the block passes the strike to the block below it.
//
// Ritual: blocks go against all four side faces of the furnace; items are dropped on top of it.
//  Weak -> Enhanced: 4 copper blocks, 2 charged Spark Cells, 1 diamond (item counts set in the config)
//  Enhanced -> Charged: 4 gold blocks, 6 charged Spark Cells, 6 netherite ingots
// If anything is missing, nothing upgrades (and the lightning will likely burn the dropped items).
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class LightningEvents {
    private LightningEvents() {}

    private record Offering(Item item, int count) {}

    private static final Predicate<BlockState> COPPER_BLOCK = s -> s.is(Blocks.COPPER_BLOCK) || s.is(Blocks.EXPOSED_COPPER)
            || s.is(Blocks.WEATHERED_COPPER) || s.is(Blocks.OXIDIZED_COPPER) || s.is(Blocks.WAXED_COPPER_BLOCK)
            || s.is(Blocks.WAXED_EXPOSED_COPPER) || s.is(Blocks.WAXED_WEATHERED_COPPER) || s.is(Blocks.WAXED_OXIDIZED_COPPER);
    private static final Predicate<BlockState> GOLD_BLOCK = s -> s.is(Blocks.GOLD_BLOCK);

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
        } else if (state.is(ModBlocks.STORM_FURNACE.get())) {
            if (Config.RITUAL_ENABLED.get()) {
                tryUpgrade(level, hit, state, COPPER_BLOCK, ModBlocks.ENHANCED_STORM_FURNACE.get(),
                        List.of(new Offering(ModItems.SPARK_CELL.get(), Config.ENHANCED_SPARK_CELLS.get()),
                                new Offering(ModItems.MAGNETITE_INGOT.get(), Config.ENHANCED_MAGNETITE.get())));
            }
        } else if (state.is(ModBlocks.ENHANCED_STORM_FURNACE.get())) {
            if (Config.RITUAL_ENABLED.get()) {
                tryUpgrade(level, hit, state, GOLD_BLOCK, ModBlocks.CHARGED_STORM_FURNACE.get(),
                        List.of(new Offering(ModItems.SPARK_CELL.get(), Config.CHARGED_SPARK_CELLS.get()),
                                new Offering(ModItems.SILVER_INGOT.get(), Config.CHARGED_SILVER.get())));
            }
        } else if (isSand(state)) {
            growVein(level, hit, level.getRandom());
        }
    }

    private static void tryUpgrade(ServerLevel level, BlockPos pos, BlockState state, Predicate<BlockState> sideBlock,
                                   Block nextTier, List<Offering> offerings) {
        // 1. A matching block against each of the four side faces.
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!sideBlock.test(level.getBlockState(pos.relative(dir)))) return;
        }

        // 2. The items, lying on top of the furnace.
        AABB top = new AABB(pos.above()).inflate(0.25, 0.5, 0.25);
        List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, top, ItemEntity::isAlive);
        for (Offering offering : offerings) {
            int have = 0;
            for (ItemEntity e : dropped) {
                if (e.getItem().is(offering.item())) have += e.getItem().getCount();
            }
            if (have < offering.count()) return;
        }

        // 3. Everything is there: consume the offerings and the side blocks.
        for (Offering offering : offerings) {
            int needed = offering.count();
            for (ItemEntity e : dropped) {
                if (needed <= 0) break;
                ItemStack stack = e.getItem();
                if (!stack.is(offering.item())) continue;
                int take = Math.min(needed, stack.getCount());
                needed -= take;
                if (take >= stack.getCount()) {
                    e.discard();
                } else {
                    e.setItem(stack.copyWithCount(stack.getCount() - take));
                }
            }
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            level.setBlock(pos.relative(dir), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        // 4. Swap in the next tier, keeping the furnace's stored power, fuel and side settings.
        BlockEntity oldBe = level.getBlockEntity(pos);
        CompoundTag saved = oldBe != null ? oldBe.saveWithoutMetadata(level.registryAccess()) : null;
        level.setBlock(pos, nextTier.defaultBlockState()
                .setValue(StormFurnaceBlock.FACING, state.getValue(StormFurnaceBlock.FACING)), Block.UPDATE_ALL);
        BlockEntity newBe = level.getBlockEntity(pos);
        if (saved != null && newBe != null) {
            newBe.loadWithComponents(saved, level.registryAccess());
            newBe.setChanged();
        }

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                60, 0.6, 0.6, 0.6, 0.3);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.5f, 0.8f);
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
