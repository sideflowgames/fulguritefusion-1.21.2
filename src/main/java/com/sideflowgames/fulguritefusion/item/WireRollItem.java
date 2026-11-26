package com.sideflowgames.fulguritefusion.item;

import java.util.List;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

// Raw / Insulated / Reinforced / Heavy Wire. Right-click one Machine Connector, then another, to string a hanging
// wire between them (one roll per wire). Reach and capacity depend on the kind (see WireTier).
// Sneak + right-click in the air to cancel.
public class WireRollItem extends Item {
    private static final String START = "WireStart";
    public static final int MAX_LINKS_PER_CONNECTOR = 8;

    private final com.sideflowgames.fulguritefusion.power.WireTier tier;

    public WireRollItem(com.sideflowgames.fulguritefusion.power.WireTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public com.sideflowgames.fulguritefusion.power.WireTier tier() {
        return tier;
    }

    public int maxLength() {
        return tier.maxLength();
    }

    public boolean isInsulated() {
        return tier.insulated;
    }

    private static BlockPos getStart(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        return tag.contains(START) ? BlockPos.of(tag.getLongOr(START, 0L)) : null;
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
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos start = getStart(stack);

        // First click: remember this connector.
        if (start == null || start.equals(pos)) {
            setStart(stack, pos);
            message(player, "Wire attached. Right-click another Machine Connector (max " + maxLength() + " blocks).");
            return InteractionResult.CONSUME;
        }

        // Second click: string the wire.
        setStart(stack, null);
        if (!(level.getBlockEntity(start) instanceof MachineConnectorBlockEntity there)) {
            message(player, "The first connector is gone. Start again.");
            return InteractionResult.CONSUME;
        }
        double length = WireMath.attachPoint(level, start).distanceTo(WireMath.attachPoint(level, pos));
        if (length > maxLength()) {
            message(player, "Too far: " + (int) Math.ceil(length) + " blocks (max " + maxLength() + ").");
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
        here.addLink(start, tier);
        there.addLink(pos, tier);
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
        message(player, "Wired (" + (int) Math.ceil(length) + " blocks).");
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && getStart(stack) != null) {
            if (!level.isClientSide()) {
                setStart(stack, null);
                message(player, "Wire cancelled.");
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // Glows while a wire is half-placed.
    @Override
    public boolean isFoil(ItemStack stack) {
        return getStart(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag flag) {
        java.util.List<Component> tooltip = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut);
        tooltip.add(Component.literal("Right-click two Machine Connectors to wire them").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Spans up to " + maxLength() + " blocks").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Carries up to " + tier.rate() + " Fm/t (a network runs at its weakest wire)").withStyle(ChatFormatting.GRAY));
        if (!tier.insulated) {
            String hearts = String.format(java.util.Locale.ROOT, "%.1f", tier.shockDamage() / 2).replace(".0", "");
            tooltip.add(Component.literal("Bare copper: shocks for " + hearts + " hearts within "
                    + String.format(java.util.Locale.ROOT, "%.1f", tier.shockRange()) + " blocks, electrifies water"
                    + (tier.shockBurns() ? ", sets you alight" : "")).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("Insulate it in the Wire Extruder with Malleable Rubber").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void message(Player player, String text) {
        if (player != null) player.sendOverlayMessage(Component.literal(text));
    }
}
