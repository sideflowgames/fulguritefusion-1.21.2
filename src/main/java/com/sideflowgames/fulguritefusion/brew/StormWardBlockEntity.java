package com.sideflowgames.fulguritefusion.brew;

import com.sideflowgames.fulguritefusion.power.FmHandler;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StormWardBlockEntity extends BlockEntity implements FmHandler {
    public static final int CAPACITY = 100_000;
    public static final int DRAW = 20;
    public static final int RANGE = 32;

    // Active wards per level, so the spawn checks don't scan block entities.
    private static final Map<LevelReader, Set<BlockPos>> ACTIVE = new WeakHashMap<>();

    private int fm;

    public StormWardBlockEntity(BlockPos pos, BlockState state) {
        super(UtilityBlocks.STORM_WARD_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StormWardBlockEntity ward) {
        boolean on = ward.fm >= DRAW;
        if (on) {
            ward.fm -= DRAW;
            ward.setChanged();
        }
        synchronized (ACTIVE) {
            Set<BlockPos> set = ACTIVE.computeIfAbsent(level, l -> new HashSet<>());
            if (on) set.add(pos.immutable());
            else set.remove(pos);
        }
        if (state.getValue(StormWardBlock.ACTIVE) != on) {
            level.setBlock(pos, state.setValue(StormWardBlock.ACTIVE, on), 3);
        }
    }

    static void forget(Level level, BlockPos pos) {
        synchronized (ACTIVE) {
            Set<BlockPos> set = ACTIVE.get(level);
            if (set != null) set.remove(pos);
        }
    }

    // True when a powered ward is within 32 blocks of pos.
    public static boolean warded(LevelReader level, BlockPos pos) {
        synchronized (ACTIVE) {
            Set<BlockPos> set = ACTIVE.get(level);
            if (set == null || set.isEmpty()) return false;
            for (BlockPos w : set) if (w.distSqr(pos) <= RANGE * RANGE) return true;
        }
        return false;
    }

    @Override
    public void setRemoved() {
        if (level != null) forget(level, worldPosition);
        super.setRemoved();
    }

    @Override
    public int receiveFm(int amount, boolean simulate) {
        int took = Math.max(0, Math.min(CAPACITY - fm, amount));
        if (!simulate && took > 0) {
            fm += took;
            setChanged();
        }
        return took;
    }

    @Override
    public int getFm() {
        return fm;
    }

    @Override
    public int getMaxFm() {
        return CAPACITY;
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveStormWardBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveStormWardBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        tag.putInt("Fm", fm);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadStormWardBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadStormWardBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        fm = tag.getIntOr("Fm", 0);
    }
}
