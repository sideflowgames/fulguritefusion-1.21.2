package com.sideflowgames.fulguritefusion.entity.stormreach;

import com.sideflowgames.fulguritefusion.stormreach.Stormreach;
import com.sideflowgames.fulguritefusion.stormreach.StormreachStorm;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

// A signature beast: a rare elite of one Stormreach biome, with a boss bar for everyone within 40 blocks and a
// guaranteed haul (its loot table: the biome's ore in bulk and a unique part). Only one of each kind lives within
// 160 blocks of another, and only 1 spawn try in 12 succeeds (1 in 5 in a storm).
public abstract class StormElite extends StormBeast {
    private static final double BAR_RANGE = 40;
    private final ServerBossEvent bossBar;
    private final Set<ServerPlayer> watching = new HashSet<>();

    protected StormElite(EntityType<? extends Monster> type, Level level, String name, BossEvent.BossBarColor color) {
        super(type, level, name);
        bossBar = new ServerBossEvent(getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        xpReward = 120;
        fieldRadius = 8.0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 10 == 0 && level() instanceof ServerLevel server) {
            for (ServerPlayer p : server.players()) {
                boolean near = p.isAlive() && p.distanceToSqr(this) < BAR_RANGE * BAR_RANGE;
                if (near && watching.add(p)) bossBar.addPlayer(p);
                else if (!near && watching.remove(p)) bossBar.removePlayer(p);
            }
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (watching.remove(player)) bossBar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
        watching.clear();
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossBar.setName(getDisplayName());
    }

    // Elites don't wander off while they're hurt.
    @Override
    public boolean removeWhenFarAway(double distance) {
        return getHealth() >= getMaxHealth();
    }

    public static <T extends Mob> boolean checkEliteSpawn(EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason,
                                                          BlockPos pos, RandomSource random) {
        if (MobSpawnType.isSpawner(reason) || reason == MobSpawnType.SPAWN_EGG) return true;
        ServerLevel server = level.getLevel();
        if (!Stormreach.is(server) || level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (com.sideflowgames.fulguritefusion.brew.StormWardBlockEntity.warded(server, pos)) return false;
        if (random.nextInt(StormreachStorm.isStorming(server) ? 5 : 12) != 0) return false;
        if (level.getBrightness(LightLayer.BLOCK, pos) >= 8) return false;
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
        return server.getEntities(type, new AABB(pos).inflate(160), e -> true).isEmpty();
    }
}
