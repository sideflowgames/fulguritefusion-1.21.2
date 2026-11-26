package com.sideflowgames.fulguritefusion.event;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.item.FmChargeable;
import com.sideflowgames.fulguritefusion.item.StormforgedTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

// Stormforged tool modes (sneak + right-click the air to cycle):
//   Arc Smelting: whatever a broken block drops comes out smelted (ores to ingots, logs to charcoal, sand to
//   glass...). 100 Fm per item smelted.
//   Vein Mining: breaking a block also breaks every connected block of the same kind (up to 64), 150 Fm each.
//   The extra blocks drop normally (smelted too, when Arc Smelting is on).
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class StormToolEvents {
    private StormToolEvents() {}

    private static final int SMELT_COST = 100;
    private static final int VEIN_COST = 150;
    private static final int VEIN_MAX = 64;
    private static boolean veining; // stops the extra blocks starting veins of their own

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!StormforgedTools.smelting(tool)) return;
        ServerLevel level = event.getLevel();
        boolean any = false;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            SingleRecipeInput input = new SingleRecipeInput(stack);
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) continue;
            ItemStack result = recipe.get().value().assemble(input);
            if (result.isEmpty()) continue;
            int cost = SMELT_COST * stack.getCount();
            if (FmChargeable.get(tool) < cost) break;
            FmChargeable.use(tool, cost);
            drop.setItem(result.copyWithCount(result.getCount() * stack.getCount()));
            any = true;
        }
        if (any) {
            BlockPos p = event.getPos();
            level.sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.01);
        }
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (veining || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        ItemStack tool = player.getMainHandItem();
        if (!StormforgedTools.veinMining(tool)) return;
        BlockState state = event.getState();
        if (!tool.isCorrectToolForDrops(state) && !state.is(net.minecraft.tags.BlockTags.LOGS)) return;
        Block block = state.getBlock();
        BlockPos start = event.getPos();

        // Gather the connected blocks of the same kind (touching on faces, edges or corners).
        Set<BlockPos> vein = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        Set<BlockPos> seen = new HashSet<>();
        seen.add(start);
        while (!queue.isEmpty() && vein.size() < VEIN_MAX) {
            BlockPos cur = queue.poll();
            for (BlockPos n : BlockPos.betweenClosed(cur.offset(-1, -1, -1), cur.offset(1, 1, 1))) {
                if (seen.contains(n) || vein.size() >= VEIN_MAX) continue;
                BlockPos imm = n.immutable();
                seen.add(imm);
                if (level.getBlockState(imm).is(block)) {
                    vein.add(imm);
                    queue.add(imm);
                }
            }
        }
        veining = true;
        try {
            for (BlockPos p : vein) {
                if (FmChargeable.get(tool) < VEIN_COST || player.getMainHandItem() != tool) break;
                FmChargeable.use(tool, VEIN_COST);
                player.gameMode.destroyBlock(p);
            }
        } finally {
            veining = false;
        }
    }
}
