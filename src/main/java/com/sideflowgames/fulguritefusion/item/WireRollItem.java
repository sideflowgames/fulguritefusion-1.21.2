package com.sideflowgames.fulguritefusion.item;

import java.util.List;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.block.MachineConnectorBlockEntity;
import com.sideflowgames.fulguritefusion.power.WireMath;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

// Raw / Insulated Wire Roll. Right-click one Machine Connector, then another, to string a hanging wire
// between them (one roll per wire, up to 16 blocks long). Sneak + right-click in the air to cancel.
public class WireRollItem extends Item {
    private static final String START = "WireStart";
    public static final int MAX_LINKS_PER_CONNECTOR = 8;

    private final boolean insulated;

    public WireRollItem(boolean insulated, Properties properties) {
        super(properties);
        this.insulated = insulated;
    }

    public boolean isInsulated() {
        return insulated;
    }

    private static BlockPos getStart(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        return tag.contains(START) ? BlockPos.of(tag.getLong(START)) : null;
    }

    private static void setStart(ItemStack stack, BlockPos pos) {
        if (pos == null) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            CompoundTag tag = new CompoundTag();
            tag.putLong(START, pos.asLong());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof MachineConnectorBlockEntity here)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos start = getStart(stack);

        // First click: remember this connector.
        if (start == null || start.equals(pos)) {
            setStart(stack, pos);
            message(player, "Wire attached. Right-click another Machine Connector (max " + Config.WIRE_MAX_LENGTH.get() + " blocks).");
            return InteractionResult.CONSUME;
        }

        // Second click: string the wire.
        setStart(stack, null);
        if (!(level.getBlockEntity(start) instanceof MachineConnectorBlockEntity there)) {
            message(player, "The first connector is gone. Start again.");
            return InteractionResult.CONSUME;
        }
        double length = WireMath.attachPoint(start).distanceTo(WireMath.attachPoint(pos));
        if (length > Config.WIRE_MAX_LENGTH.get()) {
            message(player, "Too far: " + (int) Math.ceil(length) + " blocks (max " + Config.WIRE_MAX_LENGTH.get() + ").");
            return InteractionResult.CONSUME;
        }
        if (here.isLinkedTo(start)) {
            message(player, "Those connectors are already wired together.");
            return InteractionResult.CONSUME;
        }
        if (here.getLinks().size() >= MAX_LINKS_PER_CONNECTOR || there.getLinks().size() >= MAX_LINKS_PER_CONNECTOR) {
            message(player, "A connector can hold at most " + MAX_LINKS_PER_CONNECTOR + " wires.");
            return InteractionResult.CONSUME;
        }
        here.addLink(start, insulated);
        there.addLink(pos, insulated);
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
        message(player, "Wired (" + (int) Math.ceil(length) + " blocks).");
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && getStart(stack) != null) {
            if (!level.isClientSide) {
                setStart(stack, null);
                message(player, "Wire cancelled.");
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    // Glows while a wire is half-placed.
    @Override
    public boolean isFoil(ItemStack stack) {
        return getStart(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click two Machine Connectors to wire them").withStyle(ChatFormatting.GRAY));
        if (!insulated) tooltip.add(Component.literal("Bare copper: shocks and electrifies water").withStyle(ChatFormatting.GOLD));
    }

    private static void message(Player player, String text) {
        if (player != null) player.displayClientMessage(Component.literal(text), true);
    }
}
