package com.sideflowgames.fulguritefusion.backpack;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

// A backpack. Right-click to open it; press the backpack key (B) to open the one you're wearing or carrying.
// Wear it in the chestplate slot (or Curios' back slot) to see it on your back.
// Sneak + right-click a block to set it down; it then opens like a chest.
public class BackpackItem extends BlockItem {
    public final BackpackTier tier;

    public BackpackItem(BackpackBlock block, Properties properties) {
        super(block, properties);
        this.tier = block.tier;
    }

    // Placing only while sneaking; a normal right-click opens it.
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && !player.isSecondaryUseActive()) return InteractionResult.PASS;
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            int slot = hand == InteractionHand.MAIN_HAND ? sp.getInventory().selected : 40;
            open(sp, stack, slot);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // Open a backpack item. lockedSlot: its inventory slot (it can't be moved while open), or -1.
    public static void open(ServerPlayer player, ItemStack stack, int lockedSlot) {
        if (!(stack.getItem() instanceof BackpackItem item)) return;
        BackpackTier tier = item.tier;
        java.util.function.IntSupplier charge = com.sideflowgames.fulguritefusion.item.FmChargeable.is(stack)
                ? () -> (int) (com.sideflowgames.fulguritefusion.item.FmChargeable.fraction(stack) * 1000) : null;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new BackpackMenu(id, inv, tier,
                new BackpackItemContainer(stack, tier.slots()),
                new BackpackItemContainer(stack, tier.upgradeSlots, com.sideflowgames.fulguritefusion.ModDataComponents.BACKPACK_UPGRADES.get()),
                lockedSlot, charge), stack.getHoverName()), buf -> {
            buf.writeVarInt(tier.ordinal());
            buf.writeVarInt(lockedSlot);
        });
    }

    // Wearable in the chestplate slot.
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int used = 0;
        for (ItemStack s : stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems()) used++;
        tooltip.add(Component.literal(used + " / " + tier.slots() + " slots used").withStyle(ChatFormatting.GRAY));
        var ups = stack.getOrDefault(com.sideflowgames.fulguritefusion.ModDataComponents.BACKPACK_UPGRADES.get(), ItemContainerContents.EMPTY);
        int upCount = 0;
        for (ItemStack u : ups.nonEmptyItems()) {
            tooltip.add(Component.literal(" + ").append(u.getHoverName()).withStyle(ChatFormatting.AQUA));
            upCount++;
        }
        tooltip.add(Component.literal(upCount + " / " + tier.upgradeSlots + " upgrade slots").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("Right-click or press B to open; sneak + right-click a block to set it down")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
