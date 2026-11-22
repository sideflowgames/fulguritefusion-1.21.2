package com.sideflowgames.fulguritefusion.block.rubber;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

// Setting a bucket down to catch latex, and wandering traders selling rubber saplings.
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class RubberEvents {
    private RubberEvents() {}

    // Right-click the top of a block with an empty bucket to set it down there (as a Tap Bucket).
    @SubscribeEvent
    public static void placeBucket(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!held.is(Items.BUCKET) || event.getFace() != Direction.UP) return;
        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos pos = event.getPos().above();
        BlockState clicked = level.getBlockState(event.getPos());
        // Don't take over clicks on machines, chests and the like (they open their menus).
        if (clicked.getMenuProvider(level, event.getPos()) != null || level.getBlockEntity(event.getPos()) != null) return;
        if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()) return;
        BlockState bucket = ModBlocks.TAP_BUCKET.get().defaultBlockState();
        if (!bucket.canSurvive(level, pos) || !player.mayUseItemAt(pos, Direction.UP, held)) return;
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, bucket);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.6f, 1.4f);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @SubscribeEvent
    public static void trades(WandererTradesEvent event) {
        event.getGenericTrades().add(new BasicItemListing(5, new ItemStack(ModBlocks.RUBBER_SAPLING_ITEM.get()), 8, 1));
    }
}
