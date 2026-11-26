package com.sideflowgames.fulguritefusion.block;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModEntities;
import com.sideflowgames.fulguritefusion.entity.StormwingHatchlingEntity;
import com.sideflowgames.fulguritefusion.entity.StormwingVariant;
import com.sideflowgames.fulguritefusion.pairing.Pairings.Tier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

// A Stormwing Egg's incubation. It warms up only while a Storm Collider within range is running,
// and hatches after 20-40 minutes of that (see the config).
// Every pairing the collider fires meanwhile is remembered:
//   - the material fired most often decides the hatchling's variant (none at all = a plain bird);
//   - each Bad pairing adds a 15% chance, each Catastrophic one 40%, that the bird hatches Feral. No warning signs.
public class StormwingEggBlockEntity extends BlockEntity {
    private static final int CHECK_EVERY = 20;

    @Nullable private UUID owner;
    private final int[] tally = new int[12];
    private double safeChance = 1.0;   // chance it hatches loyal
    private int incubation;            // ticks spent warm
    private int target;                // ticks needed (0 = not rolled yet)
    @Nullable private BlockPos collider;

    public StormwingEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.STORMWING_EGG_BE.get(), pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public boolean hasProgress() {
        return target > 0 || safeChance < 1.0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StormwingEggBlockEntity egg) {
        if (level.getGameTime() % CHECK_EVERY != 0) return;
        if (!egg.colliderRunning(level)) return;
        if (egg.target <= 0) {
            int min = Config.EGG_INCUBATION_MIN_MINUTES.get();
            int max = Math.max(min, Config.EGG_INCUBATION_MAX_MINUTES.get());
            egg.target = (min * 1200) + level.getRandom().nextInt((max - min) * 1200 + 1);
        }
        egg.incubation += CHECK_EVERY;
        egg.setChanged();
        if (level.getRandom().nextInt(4) == 0) {
            ((ServerLevel) level).sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, 3, 0.2, 0.25, 0.2, 0.02);
        }
        if (egg.incubation >= egg.target) egg.hatch((ServerLevel) level);
    }

    // Is a Storm Collider in range and running (firing now, or fired in the last 2 minutes)?
    private boolean colliderRunning(Level level) {
        if (collider == null || !(level.getBlockEntity(collider) instanceof StormColliderBlockEntity)) {
            collider = null;
            if (level.getGameTime() % 100 != 0) return false; // look again every 5 seconds
            int r = Config.EGG_INCUBATION_RADIUS.get();
            for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-r, -r, -r), worldPosition.offset(r, r, r))) {
                if (level.getBlockEntity(p) instanceof StormColliderBlockEntity) {
                    collider = p.immutable();
                    break;
                }
            }
            if (collider == null) return false;
        }
        return level.getBlockEntity(collider) instanceof StormColliderBlockEntity c && c.isRunning();
    }

    // Called by a Storm Collider each time a pairing collides: every egg in range remembers it.
    public static void onPairing(ServerLevel level, BlockPos core, int a, int b, Tier tier) {
        int r = Config.EGG_INCUBATION_RADIUS.get();
        for (BlockPos p : BlockPos.betweenClosed(core.offset(-r, -r, -r), core.offset(r, r, r))) {
            if (level.getBlockEntity(p) instanceof StormwingEggBlockEntity egg) egg.record(a, b, tier);
        }
    }

    private void record(int a, int b, Tier tier) {
        if (a >= 0 && a < 12) tally[a]++;
        if (b >= 0 && b < 12) tally[b]++;
        if (tier == Tier.BAD) safeChance *= 1.0 - Config.FERAL_CHANCE_BAD.get();
        if (tier == Tier.CATASTROPHIC) safeChance *= 1.0 - Config.FERAL_CHANCE_CATASTROPHIC.get();
        setChanged();
    }

    private StormwingVariant variant(ServerLevel level) {
        int best = 0;
        List<Integer> top = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            if (tally[i] > best) {
                best = tally[i];
                top.clear();
                top.add(i);
            } else if (tally[i] == best && best > 0) {
                top.add(i);
            }
        }
        return top.isEmpty() ? StormwingVariant.PLAIN : StormwingVariant.fromMaterial(top.get(level.getRandom().nextInt(top.size())));
    }

    private void hatch(ServerLevel level) {
        BlockPos pos = worldPosition;
        StormwingVariant variant = variant(level);
        boolean feral = level.getRandom().nextDouble() >= safeChance;
        UUID hatchOwner = owner;
        level.removeBlock(pos, false);
        StormwingHatchlingEntity chick = ModEntities.STORMWING_HATCHLING.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (chick != null) {
            chick.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
            chick.setup(variant, feral, feral ? null : hatchOwner);
            level.addFreshEntity(chick);
        }
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1.0f, 0.8f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.2);
    }

    public CompoundTag saveProgress() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Tally", tally);
        tag.putDouble("Safe", safeChance);
        tag.putInt("Incubation", incubation);
        tag.putInt("Target", target);
        return tag;
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveStormwingEggBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveStormwingEggBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        tag.merge(saveProgress());
        if (owner != null) com.sideflowgames.fulguritefusion.util.NbtBridge.putUUID(tag, "Owner", owner);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadStormwingEggBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadStormwingEggBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        int[] t = tag.getIntArray("Tally").orElse(new int[0]);
        for (int i = 0; i < Math.min(12, t.length); i++) tally[i] = t[i];
        safeChance = tag.contains("Safe") ? tag.getDoubleOr("Safe", 0d) : 1.0;
        incubation = tag.getIntOr("Incubation", 0);
        target = tag.getIntOr("Target", 0);
        owner = com.sideflowgames.fulguritefusion.util.NbtBridge.hasUUID(tag, "Owner") ? com.sideflowgames.fulguritefusion.util.NbtBridge.getUUID(tag, "Owner") : null;
    }
}
