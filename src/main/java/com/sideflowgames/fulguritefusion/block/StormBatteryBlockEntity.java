package com.sideflowgames.fulguritefusion.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.menu.StormBatteryMenu;
import com.sideflowgames.fulguritefusion.power.FmHandler;
import com.sideflowgames.fulguritefusion.power.FmSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// One Storm Battery block (64,000 Fm by default). Touching batteries form a bank that shares its charge:
// every tick each block evens out with its battery neighbours, so the bank fills and drains as one.
// Power comes in on any face (from a Storm Furnace or other source) and goes out to any touching machine.
public class StormBatteryBlockEntity extends BlockEntity implements FmHandler, FmSource, MenuProvider {
    // Menu data (big numbers split into two 16-bit halves).
    public static final int D_FM = 0, D_CAPACITY = 2, D_COUNT = 4, D_MAX = 5;
    public static final int DATA_COUNT = 6;

    private int fm;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            Set<BlockPos> bank = level != null ? findBank(level, worldPosition, null, 64) : Set.of(worldPosition);
            long total = 0, capacity = 0;
            for (BlockPos p : bank) {
                if (level != null && level.getBlockEntity(p) instanceof StormBatteryBlockEntity b) {
                    total += b.fm;
                    capacity += capacity();
                }
            }
            int t = (int) Math.min(Integer.MAX_VALUE, total), c = (int) Math.min(Integer.MAX_VALUE, capacity);
            return switch (index) {
                case D_FM -> t & 0xFFFF;
                case D_FM + 1 -> t >>> 16;
                case D_CAPACITY -> c & 0xFFFF;
                case D_CAPACITY + 1 -> c >>> 16;
                case D_COUNT -> bank.size();
                case D_MAX -> Config.BATTERY_MAX_BANK.get();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public StormBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.STORM_BATTERY_BE.get(), pos, state);
    }

    public static int capacity() {
        return Config.BATTERY_CAPACITY.get();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StormBatteryBlockEntity be) {
        boolean changed = false;
        int outputLimit = Config.BATTERY_OUTPUT.get();
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof StormBatteryBlockEntity other) {
                // Share charge across the bank.
                if (be.fm > other.fm + 1) {
                    int move = (be.fm - other.fm) / 2;
                    be.fm -= move;
                    other.fm += move;
                    other.setChanged();
                    changed = true;
                }
            } else if (be.fm > 0 && level.getBlockEntity(pos.relative(dir)) instanceof FmHandler machine && !machine.isStorage()) {
                // Feed touching machines.
                int sent = machine.receiveFm(Math.min(be.fm, outputLimit), false);
                if (sent > 0) {
                    be.fm -= sent;
                    changed = true;
                }
            }
        }
        if (changed) be.setChanged();
    }

    // All batteries connected to 'start' through touching faces (up to 'limit' blocks).
    // 'ignore' is treated as if it wasn't there (used when checking a block that was just placed).
    public static Set<BlockPos> findBank(Level level, BlockPos start, BlockPos ignore, int limit) {
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        found.add(start);
        queue.add(start);
        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (next.equals(ignore) || found.contains(next)) continue;
                if (level.getBlockState(next).is(ModBlocks.STORM_BATTERY.get())) {
                    found.add(next);
                    queue.add(next);
                }
            }
        }
        return found;
    }

    @Override
    public int receiveFm(int amount, boolean simulate) {
        int accepted = Math.max(0, Math.min(amount, capacity() - fm));
        if (!simulate && accepted > 0) {
            fm += accepted;
            setChanged();
        }
        return accepted;
    }

    // Wire networks draw stored Fm out through the Machine Connector.
    @Override
    public int extractFm(int max, boolean simulate) {
        int out = Math.max(0, Math.min(max, fm));
        if (!simulate && out > 0) {
            fm -= out;
            setChanged();
        }
        return out;
    }

    @Override
    public int getFm() {
        return fm;
    }

    @Override
    public int getMaxFm() {
        return capacity();
    }

    @Override
    public boolean isStorage() {
        return true;
    }

    public String statusText() {
        Set<BlockPos> bank = findBank(level, worldPosition, null, 64);
        long total = 0;
        for (BlockPos p : bank) {
            if (level.getBlockEntity(p) instanceof StormBatteryBlockEntity b) total += b.fm;
        }
        return "Storm Battery bank: " + bank.size() + " / " + Config.BATTERY_MAX_BANK.get() + " blocks | "
                + total + " / " + (long) bank.size() * capacity() + " Fm";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fulguritefusion.storm_battery");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StormBatteryMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fm", fm);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fm = tag.getInt("Fm");
    }
}
