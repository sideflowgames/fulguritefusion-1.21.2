package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.power.FmHandler;
import com.sideflowgames.fulguritefusion.power.FmSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// Mini Collider (early-mid game): the ring's power mode. Each collision of two Spark Cells releases Fm
// (5,120 by default) over 40 seconds. Injector materials are ignored. Power comes out of the TOP only:
// a machine/battery on top, or a Machine Connector for wires.
public class MiniColliderBlockEntity extends ColliderCoreBlockEntity implements FmSource {
    public static final int MAX_FM = 16000;
    public static final int PUSH_LIMIT = 256;

    private int fm;
    private int pendingFm;   // still to be released from the last collision
    private int releaseRate; // Fm per tick while releasing

    public MiniColliderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MINI_COLLIDER_BE.get(), pos, state);
    }

    // Fire the next pair early enough that output doesn't stop between shots.
    @Override
    protected boolean readyToFire() {
        return fm < MAX_FM && pendingFm <= releaseRate * travelTicks();
    }

    @Override
    protected void onCollide(ServerLevel level) {
        int amount = Config.COLLIDER_FM_PER_SHOT.get();
        int ticks = Config.COLLIDER_RELEASE_TICKS.get();
        pendingFm += amount;
        releaseRate = Math.max(1, (amount + ticks - 1) / ticks);
    }

    @Override
    protected void tickPower(Level level) {
        boolean changed = false;
        if (pendingFm > 0 && fm < MAX_FM) {
            int made = Math.min(Math.max(1, releaseRate), Math.min(pendingFm, MAX_FM - fm));
            pendingFm -= made;
            fm += made;
            changed = true;
        }
        // Power out of the top: the machine above counts as the 2nd in its daisy chain (the collider is 1st).
        if (fm > 0 && level.getBlockEntity(worldPosition.above()) instanceof FmHandler machine) {
            int sent = machine.receiveFmFromChain(Math.min(fm, PUSH_LIMIT), 1);
            if (sent > 0) {
                fm -= sent;
                changed = true;
            }
        }
        if (changed) setChanged();
    }

    @Override
    public int extractFm(int max, boolean simulate) {
        int out = Math.max(0, Math.min(max, fm));
        if (!simulate && out > 0) {
            fm -= out;
            setChanged();
        }
        return out;
    }

    public String statusText() {
        if (problem != null) return "Mini Collider: ring incomplete | " + problem;
        String state = shotTimer >= 0 ? "cells in flight" : pendingFm > 0 ? "releasing " + releaseRate + " Fm/t" : "idle";
        return "Mini Collider: " + state + " | " + fm + " / " + MAX_FM + " Fm | " + loadedCells() + " Spark Cells loaded";
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fm", fm);
        tag.putInt("PendingFm", pendingFm);
        tag.putInt("ReleaseRate", releaseRate);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fm = tag.getInt("Fm");
        pendingFm = tag.getInt("PendingFm");
        releaseRate = tag.getInt("ReleaseRate");
    }
}
