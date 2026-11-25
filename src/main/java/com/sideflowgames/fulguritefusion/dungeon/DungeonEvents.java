package com.sideflowgames.fulguritefusion.dungeon;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

// A dungeon boss carries its altar's position; when it dies, the altar unseals the treasure vault.
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class DungeonEvents {
    private DungeonEvents() {}

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        var e = event.getEntity();
        if (!(e.level() instanceof ServerLevel server) || !e.getPersistentData().contains(DungeonAltarBlockEntity.BOSS_TAG)) return;
        BlockPos altar = BlockPos.of(e.getPersistentData().getLong(DungeonAltarBlockEntity.BOSS_TAG));
        if (server.getBlockEntity(altar) instanceof DungeonAltarBlockEntity be) be.bossDefeated(server);
    }
}
