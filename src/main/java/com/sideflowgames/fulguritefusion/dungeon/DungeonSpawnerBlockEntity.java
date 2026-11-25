package com.sideflowgames.fulguritefusion.dungeon;

import com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DungeonSpawnerBlockEntity extends BlockEntity {
    private static final double TRIGGER = 9.0;
    private final List<String> mobs = new ArrayList<>();
    private int count = 3;
    private boolean charged;

    public DungeonSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonBlocks.SPAWNER_BE.get(), pos, state);
    }

    // Set by the dungeon generator.
    public void configure(List<String> mobIds, int count, boolean charged) {
        mobs.clear();
        mobs.addAll(mobIds);
        this.count = count;
        this.charged = charged;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DungeonSpawnerBlockEntity cage) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 10 != 0 || state.getValue(DungeonSpawnerBlock.SPENT)) return;
        Player player = server.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, TRIGGER,
                p -> p.isAlive() && !p.isSpectator() && !((Player) p).isCreative());
        if (player == null || server.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        cage.release(server, player);
    }

    private void release(ServerLevel server, Player player) {
        List<String> pool = mobs.isEmpty() ? List.of("fulguritefusion:arc_reaver") : mobs;
        for (int i = 0; i < count; i++) {
            var type = EntityType.byString(pool.get(server.random.nextInt(pool.size()))).orElse(null);
            if (type == null || !(type.create(server) instanceof Mob mob)) continue;
            BlockPos at = spot(server, mob);
            mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, server.random.nextFloat() * 360, 0);
            mob.finalizeSpawn(server, server.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null);
            mob.setPersistenceRequired();
            if (charged && mob instanceof StormBeast beast) beast.charge();
            mob.setTarget(player);
            server.addFreshEntity(mob);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY(0.5), mob.getZ(), 20, 0.4, 0.6, 0.4, 0.2);
        }
        server.sendParticles(ParticleTypes.EXPLOSION, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 1, 0, 0, 0, 0);
        server.playSound(null, worldPosition, SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.HOSTILE, 1.5f, 0.6f);
        server.setBlock(worldPosition, getBlockState().setValue(DungeonSpawnerBlock.SPENT, true), 3);
    }

    // A free spot around the cage that fits the mob, or the block above it.
    private BlockPos spot(ServerLevel server, Mob mob) {
        for (int tries = 0; tries < 16; tries++) {
            BlockPos p = worldPosition.offset(server.random.nextInt(7) - 3, server.random.nextInt(3) - 1, server.random.nextInt(7) - 3);
            if (!server.getBlockState(p.below()).isSolid()) continue;
            if (server.noCollision(mob, mob.getType().getDimensions().makeBoundingBox(p.getX() + 0.5, p.getY(), p.getZ() + 0.5))) return p;
        }
        return worldPosition.above();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (String m : mobs) list.add(StringTag.valueOf(m));
        tag.put("Mobs", list);
        tag.putInt("Count", count);
        tag.putBoolean("Charged", charged);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mobs.clear();
        for (Tag t : tag.getList("Mobs", Tag.TAG_STRING)) mobs.add(t.getAsString());
        if (tag.contains("Count")) count = tag.getInt("Count");
        charged = tag.getBoolean("Charged");
    }
}
