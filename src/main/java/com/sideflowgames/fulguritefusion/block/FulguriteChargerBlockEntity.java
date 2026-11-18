package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.power.FmHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Charges Empty Spark Cells: 400 Fm each, 1 Fm per tick = 20 seconds per cell.
// Also shares power with neighbouring machines so they can be daisy-chained.
public class FulguriteChargerBlockEntity extends BlockEntity implements FmHandler {
    public static final int MAX_FM = 1000;
    public static final int FM_PER_CELL = 400;
    public static final int MAX_CELLS = 16;
    public static final int SHARE_PER_TICK = 20;

    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int fm;
    private int progress;

    public FulguriteChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FULGURITE_CHARGER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FulguriteChargerBlockEntity be) {
        boolean changed = false;

        // Charge the current cell.
        if (!be.input.isEmpty() && be.output.getCount() < MAX_CELLS && be.fm > 0) {
            be.fm--;
            be.progress++;
            if (be.progress >= FM_PER_CELL) {
                be.progress = 0;
                be.input.shrink(1);
                if (be.output.isEmpty()) {
                    be.output = new ItemStack(ModItems.SPARK_CELL.get());
                } else {
                    be.output.grow(1);
                }
            }
            changed = true;
        } else if (be.input.isEmpty() && be.progress != 0) {
            be.progress = 0;
            changed = true;
        }

        // Daisy chain: even out power with any touching machine that has less (by fill level).
        for (Direction dir : Direction.values()) {
            if (be.fm <= 0) break;
            if (level.getBlockEntity(pos.relative(dir)) instanceof FmHandler other && other != be) {
                double mine = (double) be.fm / MAX_FM;
                double theirs = (double) other.getFm() / other.getMaxFm();
                if (mine > theirs) {
                    int gap = (int) ((mine - theirs) * Math.min(MAX_FM, other.getMaxFm()) / 2);
                    int offer = Math.min(Math.min(gap, SHARE_PER_TICK), be.fm);
                    if (offer > 0) {
                        int sent = other.receiveFm(offer, false);
                        be.fm -= sent;
                        if (sent > 0) changed = true;
                    }
                }
            }
        }

        if (changed) be.setChanged();
    }

    @Override
    public int receiveFm(int amount, boolean simulate) {
        int accepted = Math.min(amount, MAX_FM - fm);
        if (!simulate && accepted > 0) {
            fm += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public int getFm() {
        return fm;
    }

    @Override
    public int getMaxFm() {
        return MAX_FM;
    }

    public void insertCells(ItemStack held) {
        int room = MAX_CELLS - input.getCount();
        if (room <= 0) return;
        ItemStack moved = held.split(room);
        if (input.isEmpty()) {
            input = moved;
        } else {
            input.grow(moved.getCount());
        }
        setChanged();
    }

    public ItemStack takeOutput() {
        ItemStack out = output;
        output = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    public ItemStack takeInput() {
        ItemStack out = input;
        input = ItemStack.EMPTY;
        progress = 0;
        setChanged();
        return out;
    }

    public String statusText() {
        int percent = progress * 100 / FM_PER_CELL;
        return "Charger: " + fm + " / " + MAX_FM + " Fm | Empty: " + input.getCount()
                + " | Charged: " + output.getCount() + " | Current cell: " + percent + "%";
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fm", fm);
        tag.putInt("Progress", progress);
        if (!input.isEmpty()) tag.put("Input", input.save(registries));
        if (!output.isEmpty()) tag.put("Output", output.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fm = tag.getInt("Fm");
        progress = tag.getInt("Progress");
        input = tag.contains("Input") ? ItemStack.parseOptional(registries, tag.getCompound("Input")) : ItemStack.EMPTY;
        output = tag.contains("Output") ? ItemStack.parseOptional(registries, tag.getCompound("Output")) : ItemStack.EMPTY;
    }
}
