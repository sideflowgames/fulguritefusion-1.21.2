package com.sideflowgames.fulguritefusion.util;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

// In 26.1 blocks can no longer add item tooltips themselves. This block item asks its block for them.
public class TooltipBlockItem extends BlockItem {
    public interface Tooltip {
        void appendItemTooltip(ItemStack stack, TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag);
    }

    public TooltipBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (getBlock() instanceof Tooltip t) t.appendItemTooltip(stack, context, tooltip, flag);
    }
}
