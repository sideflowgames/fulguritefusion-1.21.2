package com.sideflowgames.fulguritefusion.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Farm Collector: a 27-slot store for everything a Shock Farm's mobs drop, plus their XP. Hoppers and pipes can
// pull from it like a chest.
public class FarmCollectorBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 27;
    public static final int MAX_XP = 1_000_000;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int xp;

    public FarmCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(FarmBlocks.COLLECTOR_BE.get(), pos, state);
    }

    // Puts as much of stack in as fits; returns what's left over.
    public ItemStack insert(ItemStack stack) {
        ItemStack left = stack.copy();
        for (int i = 0; i < SIZE && !left.isEmpty(); i++) {
            ItemStack slot = items.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, left)) {
                int move = Math.min(left.getCount(), slot.getMaxStackSize() - slot.getCount());
                if (move > 0) {
                    slot.grow(move);
                    left.shrink(move);
                }
            }
        }
        for (int i = 0; i < SIZE && !left.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, left.copy());
                left = ItemStack.EMPTY;
            }
        }
        if (left.getCount() != stack.getCount()) setChanged();
        return left;
    }

    public boolean isFull() {
        for (ItemStack s : items) if (s.isEmpty() || s.getCount() < s.getMaxStackSize()) return false;
        return true;
    }

    public void addXp(int amount) {
        xp = (int) Math.min(MAX_XP, (long) xp + amount);
        setChanged();
    }

    public int xp() {
        return xp;
    }

    public int takeXp() {
        int out = xp;
        xp = 0;
        setChanged();
        return out;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.fulguritefusion.farm_collector");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveFarmCollectorBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveFarmCollectorBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        com.sideflowgames.fulguritefusion.util.NbtBridge.saveAllItems(tag, items, registries);
        tag.putInt("Xp", xp);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadFarmCollectorBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadFarmCollectorBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        com.sideflowgames.fulguritefusion.util.NbtBridge.loadAllItems(tag, items, registries);
        xp = tag.getIntOr("Xp", 0);
    }

    // 26.1: runs the block's removal logic while this block entity still exists.
    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (level == null) return;
        if (state.getBlock() instanceof FarmCollectorBlock block) block.onRemoveLegacy(state, level, pos, level.getBlockState(pos), false);
    }
}
