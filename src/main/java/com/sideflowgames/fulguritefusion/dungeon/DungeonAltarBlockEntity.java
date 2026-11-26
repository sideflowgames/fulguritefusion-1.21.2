package com.sideflowgames.fulguritefusion.dungeon;

import com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class DungeonAltarBlockEntity extends BlockEntity {
    public static final String BOSS_TAG = "fulguritefusion_dungeon_altar";
    private static final double WAKE = 12.0;

    private DungeonKind kind = DungeonKind.STORMSPIRE;
    private boolean spawned, cleared;
    @Nullable private UUID boss;
    @Nullable private BoundingBox vault;

    public DungeonAltarBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonBlocks.ALTAR_BE.get(), pos, state);
    }

    // Set by the dungeon generator: which boss, and the treasure vault to unseal.
    public void configure(DungeonKind kind, BoundingBox vault) {
        this.kind = kind;
        this.vault = vault;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DungeonAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel server) || altar.cleared || level.getGameTime() % 20 != 0) return;
        if (server.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        var player = server.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, WAKE,
                p -> p.isAlive() && !p.isSpectator() && !((net.minecraft.world.entity.player.Player) p).isCreative());
        if (!altar.spawned) {
            if (player != null) altar.wake(server, player);
        } else if (altar.boss != null && server.getEntity(altar.boss) == null && player != null) {
            // The boss is gone without dying (it can't have been unloaded with a player standing here): wake it again.
            altar.spawned = false;
            altar.setChanged();
        }
    }

    private void wake(ServerLevel server, net.minecraft.world.entity.player.Player player) {
        if (!(kind.boss().create(server, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED) instanceof Mob mob)) return;
        BlockPos at = worldPosition.offset(0, 1, 5);
        mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
        mob.finalizeSpawn(server, server.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
        mob.setPersistenceRequired();
        if (mob instanceof StormBeast beast) beast.charge();
        mob.getPersistentData().putLong(BOSS_TAG, worldPosition.asLong());
        mob.setTarget(player);
        server.addFreshEntity(mob);
        boss = mob.getUUID();
        spawned = true;
        setChanged();
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 1, 0, 0, 0, 0);
        server.playSound(null, worldPosition, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5f, 0.7f);
        Component msg = Component.literal("The guardian of the " + kind.title() + " awakens!").withStyle(ChatFormatting.DARK_AQUA);
        for (ServerPlayer p : server.players()) if (p.distanceToSqr(at.getX(), at.getY(), at.getZ()) < 48 * 48) p.sendSystemMessage(msg);
    }

    // The boss died: unseal the treasure vault.
    public void bossDefeated(ServerLevel server) {
        if (cleared) return;
        cleared = true;
        setChanged();
        if (vault != null) {
            BlockState wall = kind.wall();
            for (BlockPos p : BlockPos.betweenClosed(vault.minX(), vault.minY(), vault.minZ(), vault.maxX(), vault.maxY(), vault.maxZ())) {
                BlockState s = server.getBlockState(p);
                if (s.is(DungeonBlocks.STORM_SEAL.get())) {
                    server.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.1);
                } else if (s.is(DungeonBlocks.WARDED_BRICK.get())) {
                    server.setBlock(p, wall, 3);
                }
            }
        }
        server.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 2.0f, 0.6f);
        Component msg = Component.literal("The " + kind.title() + "'s vault seal breaks.").withStyle(ChatFormatting.GOLD);
        for (ServerPlayer p : server.players()) if (p.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 64 * 64) p.sendSystemMessage(msg);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveDungeonAltarBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveDungeonAltarBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        tag.putString("Kind", kind.id);
        tag.putBoolean("Spawned", spawned);
        tag.putBoolean("Cleared", cleared);
        if (boss != null) com.sideflowgames.fulguritefusion.util.NbtBridge.putUUID(tag, "Boss", boss);
        if (vault != null) tag.putIntArray("Vault", new int[] {vault.minX(), vault.minY(), vault.minZ(), vault.maxX(), vault.maxY(), vault.maxZ()});
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadDungeonAltarBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadDungeonAltarBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        kind = DungeonKind.byId(tag.getStringOr("Kind", ""));
        spawned = tag.getBooleanOr("Spawned", false);
        cleared = tag.getBooleanOr("Cleared", false);
        boss = com.sideflowgames.fulguritefusion.util.NbtBridge.hasUUID(tag, "Boss") ? com.sideflowgames.fulguritefusion.util.NbtBridge.getUUID(tag, "Boss") : null;
        int[] v = tag.getIntArray("Vault").orElse(new int[0]);
        vault = v.length == 6 ? new BoundingBox(v[0], v[1], v[2], v[3], v[4], v[5]) : null;
    }
}
