package com.sideflowgames.fulguritefusion.backpack;

import com.sideflowgames.fulguritefusion.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

// A backpack upgrade. Put it in one of the backpack's upgrade slots (left of the storage).
// Right-click it in your hand to set its filter (allow or block list).
public class BackpackUpgradeItem extends Item {
    public static final int FILTER_SLOTS = 9;
    public static final int MODE_ALLOW = 1, MODE_VOID_XP = 2;

    public final UpgradeKind kind;
    public final boolean storm;

    public BackpackUpgradeItem(UpgradeKind kind, boolean storm, Properties properties) {
        super(properties);
        this.kind = kind;
        this.storm = storm;
    }

    // Whether this upgrade can go in a backpack of this tier.
    public boolean fits(BackpackTier tier) {
        if (storm) return tier == BackpackTier.STORMFORGED;
        return tier.ordinal() >= kind.minTier.ordinal();
    }

    public static int mode(ItemStack upgrade) {
        if (!(upgrade.getItem() instanceof BackpackUpgradeItem u)) return 0;
        return upgrade.getOrDefault(ModDataComponents.FILTER_MODE.get(), u.kind.defaultAllow ? MODE_ALLOW : 0);
    }

    public static boolean allowList(ItemStack upgrade) {
        return (mode(upgrade) & MODE_ALLOW) != 0;
    }

    public static boolean voidXp(ItemStack upgrade) {
        return (mode(upgrade) & MODE_VOID_XP) != 0;
    }

    public static List<ItemStack> filter(ItemStack upgrade) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack s : upgrade.getOrDefault(ModDataComponents.FILTER.get(), ItemContainerContents.EMPTY).nonEmptyItems()) list.add(s);
        return list;
    }

    // Whether this upgrade acts on the item. Matches by item type only.
    // A Void upgrade with an empty filter destroys nothing, whichever list it is set to.
    public static boolean matches(ItemStack upgrade, ItemStack item) {
        List<ItemStack> list = filter(upgrade);
        if (list.isEmpty() && upgrade.getItem() instanceof BackpackUpgradeItem u && u.kind == UpgradeKind.VOID) return false;
        boolean listed = false;
        for (ItemStack f : list) {
            if (ItemStack.isSameItem(f, item)) {
                listed = true;
                break;
            }
        }
        return allowList(upgrade) == listed;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            int slot = hand == InteractionHand.MAIN_HAND ? sp.getInventory().selected : 40;
            boolean voidXp = storm && kind == UpgradeKind.VOID;
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new UpgradeFilterMenu(id, inv, stack, slot, voidXp),
                    stack.getHoverName()), buf -> {
                buf.writeVarInt(slot);
                buf.writeBoolean(voidXp);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return storm || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String what = switch (kind) {
            case PICKUP -> storm ? "Picked-up items go into the backpack. Powered: also grabs items within 3 blocks"
                    : "Picked-up items go into the backpack first";
            case MAGNET -> storm ? "Pulls items to you from 6 blocks (12 while powered)" : "Pulls items to you from 6 blocks";
            case VOID -> storm ? "Destroys filtered items you pick up. Powered: can also void XP orbs"
                    : "Destroys filtered items you pick up";
            case COMPACTING -> storm ? "Packs 2x2 sets into blocks (and 3x3 while powered)" : "Packs 2x2 sets into blocks";
            case FEEDING -> storm ? "Eats food from the pack when hungry. Powered: heals you when you're full"
                    : "Eats food from the pack when you're hungry";
            case REFILL -> storm ? "Refills hotbar stacks. Powered: instant, also offhand, and re-equips armor that breaks"
                    : "Refills hotbar stacks from the pack";
            case RESTOCK -> storm ? "Restock button in containers. Powered: also from the backpack screen, at a container up to 8 blocks away"
                    : "Restock button in containers: fills the pack's stacks from the container";
            case DEPOSIT -> storm ? "Deposit button in containers. Powered: also from the backpack screen, at a container up to 8 blocks away"
                    : "Deposit button in containers: moves pack items the container already holds";
            case TOOL_SWAPPER -> storm ? "Swaps in the best tool when you mine. Powered: also the best weapon when you attack"
                    : "Swaps the best tool from the pack into your hand when you mine";
        };
        tooltip.add(Component.literal(what).withStyle(ChatFormatting.GRAY));
        String fitsIn = storm ? "Stormforged Backpack only" : switch (kind.minTier) {
            case COPPER -> "any backpack";
            case IRON -> "Iron Backpack or better";
            case FULGURITE -> "Fulgurite Backpack or better";
            case STORMFORGED -> "Stormforged Backpack only";
        };
        tooltip.add(Component.literal("Fits: " + fitsIn).withStyle(ChatFormatting.DARK_GRAY));
        if (storm) tooltip.add(Component.literal("Uses the backpack's Fm for its boost").withStyle(ChatFormatting.DARK_AQUA));
        int n = filter(stack).size();
        String list = allowList(stack) ? "Allow list" : "Block list";
        tooltip.add(Component.literal(list + ", " + n + " item" + (n == 1 ? "" : "s")
                + (storm && kind == UpgradeKind.VOID && voidXp(stack) ? ", voiding XP" : "")).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Right-click to set the filter").withStyle(ChatFormatting.DARK_GRAY));
    }
}
