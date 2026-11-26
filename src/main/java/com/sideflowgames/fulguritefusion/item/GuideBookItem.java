package com.sideflowgames.fulguritefusion.item;

import net.minecraft.world.InteractionResult;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

// The Fulgurite Fusion Guide: right-click to open the guide book (its pages are in assets/.../guide/guide.json).
public class GuideBookItem extends Item {
    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() && FMLEnvironment.getDist().isClient()) {
            com.sideflowgames.fulguritefusion.client.GuideMapScreen.openFresh();
        }
        return InteractionResult.SUCCESS;
    }
}
