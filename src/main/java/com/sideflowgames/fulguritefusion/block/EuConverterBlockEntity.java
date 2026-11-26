package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.compat.EuCompat;
import com.sideflowgames.fulguritefusion.menu.EuConverterMenu;
import com.sideflowgames.fulguritefusion.power.DaisyChain;
import com.sideflowgames.fulguritefusion.power.FmHandler;

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

// Universal EU Converter: Fm in on any side, EU out of the front at 1 Fm = 1 EU.
// It detects the GregTech or Modern Industrialization cable/machine on its front and matches that voltage tier.
// It only sends full packets: if the stored Fm can't cover the tier, it sends nothing.
public class EuConverterBlockEntity extends BlockEntity implements FmHandler, MenuProvider {
    public static final int MAX_FM = 64000;
    public static final int STATUS_NOTHING = 0, STATUS_CONVERTING = 1, STATUS_LOW_FM = 2, STATUS_TARGET_FULL = 3;
    // Menu data (big numbers split into two 16-bit halves).
    public static final int D_FM = 0, D_MAX = 2, D_SOURCE = 4, D_TIER = 5, D_VOLTAGE = 6, D_OUTPUT = 8, D_STATUS = 10;
    public static final int DATA_COUNT = 11;

    private int fm;
    private final DaisyChain chain = new DaisyChain();
    private EuCompat.Target target = EuCompat.Target.NONE;
    private int lastOutput;
    private int status = STATUS_NOTHING;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int voltage = (int) Math.min(Integer.MAX_VALUE, target.voltage());
            return switch (index) {
                case D_FM -> fm & 0xFFFF;
                case D_FM + 1 -> fm >>> 16;
                case D_MAX -> MAX_FM & 0xFFFF;
                case D_MAX + 1 -> MAX_FM >>> 16;
                case D_SOURCE -> target.source().ordinal();
                case D_TIER -> target.tier() + 1;
                case D_VOLTAGE -> voltage & 0xFFFF;
                case D_VOLTAGE + 1 -> voltage >>> 16;
                case D_OUTPUT -> lastOutput & 0xFFFF;
                case D_OUTPUT + 1 -> lastOutput >>> 16;
                case D_STATUS -> status;
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

    public EuConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.EU_CONVERTER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EuConverterBlockEntity be) {
        Direction front = state.getValue(EuConverterBlock.FACING);
        // Re-check what's connected once a second (cables and machines can be swapped).
        if (level.getGameTime() % 20 == 0 || be.target.source() == EuCompat.Source.NONE) {
            be.target = EuCompat.detect(level, pos, front);
        }

        // Stored Fm turned into the EU it can buy (1 Fm = 1 EU by default, see config).
        long euPerFm = Config.EU_PER_FM.get();
        long affordable = (long) be.fm * euPerFm;
        long sent = 0;
        if (be.target.source() == EuCompat.Source.NONE) {
            be.status = STATUS_NOTHING;
        } else if (affordable < be.target.voltage()) {
            be.status = STATUS_LOW_FM;
        } else {
            sent = EuCompat.send(be.target, front, affordable);
            be.status = sent > 0 ? STATUS_CONVERTING : STATUS_TARGET_FULL;
        }
        if (sent > 0) {
            be.fm -= (int) Math.min(be.fm, (sent + euPerFm - 1) / euPerFm); // round up so EU is never free
            be.setChanged();
        }
        be.lastOutput = (int) sent;
        shareChain(level, pos, front, be);
    }

    @Override
    public int receiveFm(int amount, boolean simulate) {
        chain.touch(level, 0); // fed directly by a battery or wire: start of a daisy chain
        return accept(amount, simulate);
    }

    @Override
    public int receiveFmFromChain(int amount, int depth) {
        chain.touch(level, depth);
        return accept(amount, false);
    }

    @Override
    public void touchChain(int depth) {
        chain.touch(level, depth);
    }

    private int accept(int amount, boolean simulate) {
        int accepted = Math.max(0, Math.min(amount, MAX_FM - fm));
        if (!simulate && accepted > 0) {
            fm += accepted;
            setChanged();
        }
        return accepted;
    }

    // Daisy chain: pass power to touching machines (never out of the front, stops after 8 machines).
    private static void shareChain(Level level, BlockPos pos, Direction front, EuConverterBlockEntity be) {
        int shared = be.chain.share(level, pos, be, be.fm, front);
        if (shared > 0) {
            be.fm -= shared;
            be.setChanged();
        }
    }

    @Override
    public int getFm() {
        return fm;
    }

    @Override
    public int getMaxFm() {
        return MAX_FM;
    }

    public String statusText() {
        if (target.source() == EuCompat.Source.NONE) {
            return "EU Converter: " + fm + " Fm stored | no GregTech or Modern Industrialization block on the front";
        }
        return "EU Converter: " + EuCompat.sourceName(target.source()) + " " + EuCompat.TIER_NAMES[target.tier()]
                + " (" + target.voltage() + " EU/t) | output " + lastOutput + " EU/t | " + fm + " Fm stored";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fulguritefusion.eu_converter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EuConverterMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveEuConverterBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveEuConverterBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        tag.putInt("Fm", fm);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadEuConverterBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadEuConverterBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        fm = tag.getIntOr("Fm", 0);
    }
}
