package com.sideflowgames.fulguritefusion.transport;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// Arc Tuner: use it on an Arc Sender to pick it up, then on an Arc Receiver within 5 blocks to link them.
// Sneak + use on a Sender to unlink it.
public class ArcTunerItem extends Item {
    public ArcTunerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (!(state.getBlock() instanceof ArcNodeBlock node)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (node.isSender()) {
            if (player != null && player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof ArcSenderBlockEntity be) {
                be.setTarget(null);
                say(player, "Arc Sender unlinked", ChatFormatting.GRAY);
                return InteractionResult.CONSUME;
            }
            CompoundTag tag = new CompoundTag();
            tag.putLong("Sender", pos.asLong());
            tag.putString("Dim", level.dimension().identifier().toString());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            say(player, "Sender tuned. Now use the Tuner on an Arc Receiver within 5 blocks", ChatFormatting.AQUA);
            return InteractionResult.CONSUME;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("Sender")) {
            say(player, "Use the Tuner on an Arc Sender first", ChatFormatting.GOLD);
            return InteractionResult.CONSUME;
        }
        BlockPos sender = BlockPos.of(tag.getLongOr("Sender", 0L));
        if (!level.dimension().identifier().toString().equals(tag.getStringOr("Dim", ""))
                || !(level.getBlockEntity(sender) instanceof ArcSenderBlockEntity be)) {
            say(player, "That Arc Sender is gone", ChatFormatting.RED);
            stack.remove(DataComponents.CUSTOM_DATA);
            return InteractionResult.CONSUME;
        }
        if (!ArcSenderBlockEntity.inRange(sender, pos)) {
            say(player, "Too far: an arc only reaches " + ArcSenderBlockEntity.RANGE + " blocks", ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }
        be.setTarget(pos);
        stack.remove(DataComponents.CUSTOM_DATA);
        say(player, "Linked: the Sender will arc items to this Receiver", ChatFormatting.GREEN);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    private static void say(Player player, String text, ChatFormatting color) {
        if (player != null) player.sendOverlayMessage(Component.literal(text).withStyle(color));
    }
}
