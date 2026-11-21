package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.power.FmHandler;
import com.sideflowgames.fulguritefusion.power.FmSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import com.sideflowgames.fulguritefusion.menu.MiniColliderMenu;
import net.minecraft.world.level.block.state.BlockState;

// Mini Collider (early-mid game): the ring's power mode. Each collision of two Spark Cells releases Fm
// (5,120 by default) over 40 seconds. Injector materials are ignored. Power comes out of the TOP only:
// a machine/battery on top, or a Machine Connector for wires.
public class MiniColliderBlockEntity extends ColliderCoreBlockEntity implements FmSource, MenuProvider {
    // Menu data (big numbers split into two 16-bit halves).
    public static final int D_FM = 0, D_MAX = 2, D_PENDING = 4, D_RATE = 6, D_SHOT = 7, D_LENGTH = 8, D_CELLS = 9,
            D_MASK = 10, D_COMPLETE = 12, D_STATE = 13, DATA_COUNT = 14;
    public static final int S_IDLE = 0, S_FLIGHT = 1, S_RELEASING = 2, S_RING = 3, S_CELLS = 4, S_FULL = 5;

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
        if (fm > 0 && level.getBlockEntity(worldPosition.above()) instanceof FmHandler machine
                && machine.acceptsFrom(net.minecraft.core.Direction.DOWN)) {
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

    private int state() {
        if (problem != null) return S_RING;
        if (shotTimer >= 0) return S_FLIGHT;
        if (pendingFm > 0) return S_RELEASING;
        if (fm >= MAX_FM) return S_FULL;
        if (loadedCells() < 2) return S_CELLS;
        return S_IDLE;
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case D_FM -> fm & 0xFFFF;
                case D_FM + 1 -> fm >>> 16;
                case D_MAX -> MAX_FM & 0xFFFF;
                case D_MAX + 1 -> MAX_FM >>> 16;
                case D_PENDING -> pendingFm & 0xFFFF;
                case D_PENDING + 1 -> pendingFm >>> 16;
                case D_RATE -> releaseRate;
                case D_SHOT -> shotTimer;
                case D_LENGTH -> shotLength();
                case D_CELLS -> loadedCells();
                case D_MASK -> ringMask() & 0xFFF;
                case D_MASK + 1 -> ringMask() >>> 12;
                case D_COMPLETE -> isComplete() ? 1 : 0;
                case D_STATE -> state();
                default -> 0;
            };
        }

        @Override
        public void set(int i, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fulguritefusion.mini_collider");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MiniColliderMenu(id, inventory, this, data);
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
