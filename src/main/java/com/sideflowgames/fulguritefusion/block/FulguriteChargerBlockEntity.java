package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.machine.ItemTransfer;
import com.sideflowgames.fulguritefusion.machine.SideConfig;
import com.sideflowgames.fulguritefusion.machine.SideConfigurable;
import com.sideflowgames.fulguritefusion.machine.SidedItemHandler;
import com.sideflowgames.fulguritefusion.menu.FulguriteChargerMenu;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

// Charges Empty Spark Cells: 1,280 Fm each, drawing 16 Fm per tick = 4 seconds per cell
// (a faster furnace doesn't speed it up).
// Also shares power with neighbouring machines so they can be daisy-chained.
public class FulguriteChargerBlockEntity extends BlockEntity implements FmHandler, MenuProvider, SideConfigurable {
    // Cell cost and draw rate come from the config (Config.java). The charger holds enough Fm for two cells.
    public static final int MAX_CELLS = 16;
    public static final int TRANSFER_INTERVAL = 8;
    public static final int GEAR_RATE = 4096; // Fm per tick into forged gear (limited by how fast power arrives)
    public static final int INPUT = 0, OUTPUT = 1;
    public static final int DATA_COUNT = 4 + SideConfig.COUNT;

    // Slot 0: Empty Spark Cells in. Slot 1: charged Spark Cells out (nothing can be put in by hand or hopper).
    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT && (stack.is(ModItems.EMPTY_SPARK_CELL.get())
                    || stack.getItem() instanceof com.sideflowgames.fulguritefusion.item.FmChargeable);
        }

        @Override
        public int getSlotLimit(int slot) {
            return MAX_CELLS;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // Defaults like a vanilla furnace: cells go in from the top, charged cells come out the bottom.
    private final SideConfig sides = new SideConfig(SideConfig.PULL, SideConfig.PUSH,
            SideConfig.NONE, SideConfig.NONE, SideConfig.NONE);

    private int fm;
    private int progress;

    private final DaisyChain chain = new DaisyChain();
    private int transferTimer;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> fm;
                case 1 -> maxFm();
                case 2 -> progress;
                case 3 -> fmPerCell();
                default -> sides.get(index - 4);
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FulguriteChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FULGURITE_CHARGER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FulguriteChargerBlockEntity be) {
        boolean changed = false;
        ItemStack input = be.items.getStackInSlot(INPUT);
        ItemStack output = be.items.getStackInSlot(OUTPUT);

        // Forged gear (FmChargeable): fill it straight from the charger's store, then move it to the output.
        if (input.getItem() instanceof com.sideflowgames.fulguritefusion.item.FmChargeable) {
            if (be.fm > 0) {
                int added = com.sideflowgames.fulguritefusion.item.FmChargeable.add(input, Math.min(be.fm, GEAR_RATE));
                if (added > 0) {
                    be.fm -= added;
                    changed = true;
                }
            }
            if (com.sideflowgames.fulguritefusion.item.FmChargeable.fraction(input) >= 1f && output.isEmpty()) {
                be.items.setStackInSlot(OUTPUT, input.copy());
                be.items.setStackInSlot(INPUT, ItemStack.EMPTY);
                changed = true;
            }
        } else
        // Charge the current cell.
        if (!input.isEmpty() && output.getCount() < MAX_CELLS && be.fm > 0) {
            int draw = Math.max(0, Math.min(Config.CHARGER_DRAW.get(), Math.min(be.fm, fmPerCell() - be.progress)));
            be.fm -= draw;
            be.progress += draw;
            if (be.progress >= fmPerCell()) {
                be.progress = 0;
                be.items.extractItem(INPUT, 1, false);
                if (output.isEmpty()) {
                    be.items.setStackInSlot(OUTPUT, new ItemStack(ModItems.SPARK_CELL.get()));
                } else {
                    be.items.setStackInSlot(OUTPUT, output.copyWithCount(output.getCount() + 1));
                }
            }
            changed = true;
        } else if (input.isEmpty() && be.progress != 0) {
            be.progress = 0;
            changed = true;
        }

        // Daisy chain: pass power to touching machines (stops after 8 machines).
        int shared = be.chain.share(level, pos, be, be.fm, null);
        if (shared > 0) {
            be.fm -= shared;
            changed = true;
        }

        // Items on Push / Pull faces.
        if (++be.transferTimer >= TRANSFER_INTERVAL) {
            be.transferTimer = 0;
            Direction front = state.getValue(FulguriteChargerBlock.FACING);
            for (int face = 0; face < SideConfig.COUNT; face++) {
                Direction dir = SideConfig.directionOf(front, face);
                int mode = be.sides.get(face);
                if (mode == SideConfig.PULL) {
                    ItemTransfer.pull(level, pos, dir, be.items, INPUT);
                } else if (mode == SideConfig.PUSH) {
                    ItemTransfer.push(level, pos, dir, be.items, OUTPUT);
                }
            }
        }

        if (changed) be.setChanged();
    }

    // Being offered power directly (by a battery or wire network) puts this machine at the start of a chain.
    @Override
    public int receiveFm(int amount, boolean simulate) {
        chain.touch(level, 0);
        int accepted = Math.max(0, Math.min(amount, maxFm() - fm));
        if (!simulate && accepted > 0) {
            fm += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public int receiveFmFromChain(int amount, int depth) {
        chain.touch(level, depth);
        int accepted = Math.max(0, Math.min(amount, maxFm() - fm));
        if (accepted > 0) {
            fm += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public void touchChain(int depth) {
        chain.touch(level, depth);
    }

    @Override
    public int getFm() {
        return fm;
    }

    @Override
    public int getMaxFm() {
        return maxFm();
    }

    public static int fmPerCell() {
        return Config.FM_PER_SPARK_CELL.get();
    }

    public static int maxFm() {
        return fmPerCell() * 2;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    // What hoppers and pipes see on a face. Null = nothing can connect there.
    public IItemHandler getItemHandler(Direction side) {
        if (side == null) return items;
        int mode = sides.modeFor(getBlockState().getValue(FulguriteChargerBlock.FACING), side);
        if (mode == SideConfig.PULL) {
            return new SidedItemHandler(items, true, false, (slot, stack) -> false);
        }
        if (mode == SideConfig.PUSH) {
            return new SidedItemHandler(items, false, true, (slot, stack) -> slot == OUTPUT);
        }
        return null;
    }

    @Override
    public void cycleSide(int face) {
        sides.cycle(face);
        setChanged();
        if (level != null) level.invalidateCapabilities(worldPosition);
    }

    public String statusText() {
        int percent = progress * 100 / fmPerCell();
        return "Charger: " + fm + " / " + maxFm() + " Fm | Empty: " + items.getStackInSlot(INPUT).getCount()
                + " | Charged: " + items.getStackInSlot(OUTPUT).getCount() + " | Current cell: " + percent + "%";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fulguritefusion.fulgurite_charger");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FulguriteChargerMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveFulguriteChargerBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveFulguriteChargerBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        tag.putInt("Fm", fm);
        tag.putInt("Progress", progress);
        tag.put("Inventory", com.sideflowgames.fulguritefusion.util.NbtBridge.serialize(items, registries));
        sides.save(tag);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadFulguriteChargerBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadFulguriteChargerBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        fm = tag.getIntOr("Fm", 0);
        progress = tag.getIntOr("Progress", 0);
        if (tag.contains("Inventory")) {
            com.sideflowgames.fulguritefusion.util.NbtBridge.deserialize(items, registries, tag.getCompoundOrEmpty("Inventory"));
        } else { // chargers saved before menus existed
            if (tag.contains("Input")) items.setStackInSlot(INPUT, com.sideflowgames.fulguritefusion.util.NbtBridge.parseStack(registries, tag.getCompoundOrEmpty("Input")));
            if (tag.contains("Output")) items.setStackInSlot(OUTPUT, com.sideflowgames.fulguritefusion.util.NbtBridge.parseStack(registries, tag.getCompoundOrEmpty("Output")));
        }
        sides.load(tag);
    }

    // 26.1: runs the block's removal logic while this block entity still exists.
    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (level == null) return;
        if (state.getBlock() instanceof FulguriteChargerBlock block) block.onRemoveLegacy(state, level, pos, level.getBlockState(pos), false);
    }

    // 26.1: the real storage behind the hopper/pipe view, so an undone transfer can be put back exactly (LegacyCaps).
    public net.neoforged.neoforge.items.IItemHandlerModifiable[] capStorage() {
        return new net.neoforged.neoforge.items.IItemHandlerModifiable[] {items};
    }

}
