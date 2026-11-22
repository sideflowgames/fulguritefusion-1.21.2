package com.sideflowgames.fulguritefusion.backpack;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// A backpack set down in the world: opens like a chest. Its contents, upgrades and (Stormforged) Fm travel
// with it when it's picked back up, as item components (like a shulker box).
// A placed Stormforged Backpack with a powered Storm Pickup upgrade collects dropped items within 3 blocks.
public class BackpackBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items;
    private NonNullList<ItemStack> upgrades;
    private int fm;

    public BackpackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BACKPACK_BE.get(), pos, state);
        items = NonNullList.withSize(tier().slots(), ItemStack.EMPTY);
        upgrades = NonNullList.withSize(tier().upgradeSlots, ItemStack.EMPTY);
    }

    public BackpackTier tier() {
        return getBlockState().getBlock() instanceof BackpackBlock b ? b.tier : BackpackTier.COPPER;
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        items = NonNullList.withSize(tier().slots(), ItemStack.EMPTY);
        for (int i = 0; i < Math.min(list.size(), items.size()); i++) items.set(i, list.get(i));
    }

    // Hoppers and other automation can't put backpacks (or shulker boxes) inside a placed backpack either.
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !(stack.getItem() instanceof BackpackItem) && stack.getItem().canFitInsideContainerItems();
    }

    @Override
    public int getContainerSize() {
        return tier().slots();
    }

    // The upgrade slots, for the menu.
    private final Container upgradeContainer = new Container() {
        @Override public int getContainerSize() { return upgrades.size(); }
        @Override public boolean isEmpty() { return upgrades.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int slot) { return upgrades.get(slot); }
        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack s = ContainerHelper.removeItem(upgrades, slot, amount);
            if (!s.isEmpty()) setChanged();
            return s;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(upgrades, slot); }
        @Override public void setItem(int slot, ItemStack stack) { upgrades.set(slot, stack); setChanged(); }
        @Override public void setChanged() { BackpackBlockEntity.this.setChanged(); }
        @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(BackpackBlockEntity.this, player); }
        @Override public void clearContent() { upgrades.clear(); }
    };

    public NonNullList<ItemStack> upgradeItems() {
        return upgrades;
    }

    public int fm() {
        return fm;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        java.util.function.IntSupplier charge = tier().powered()
                ? () -> (int) (fm * 1000L / tier().maxFm) : null;
        return new BackpackMenu(id, inventory, tier(), this, upgradeContainer, -1, charge);
    }

    @Override
    public void startOpen(Player player) {
        if (!player.isSpectator() && level != null) {
            level.playSound(null, worldPosition, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 0.6f, 1.1f);
        }
    }

    // Placed Stormforged packs only: Storm Pickup collects nearby items once a second.
    public static void serverTick(Level level, BlockPos pos, BlockState state, BackpackBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0 || be.fm < BackpackUpgrades.PICKUP_REACH_FM) return;
        ItemStack pickup = ItemStack.EMPTY;
        for (ItemStack u : be.upgrades) {
            if (u.getItem() instanceof BackpackUpgradeItem up && up.storm && up.kind == UpgradeKind.PICKUP) pickup = u;
        }
        if (pickup.isEmpty()) return;
        AABB area = new AABB(pos).inflate(BackpackUpgrades.PICKUP_REACH);
        boolean changed = false;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.hasPickUpDelay()
                && !e.getPersistentData().getBoolean("PreventRemoteMovement"))) {
            if (be.fm < BackpackUpgrades.PICKUP_REACH_FM) break;
            ItemStack stack = e.getItem();
            if (!BackpackUpgradeItem.matches(pickup, stack)) continue;
            int before = stack.getCount();
            ItemStack rest = BackpackUpgrades.insert(be, stack.copy());
            if (rest.getCount() == before) continue;
            be.fm -= BackpackUpgrades.PICKUP_REACH_FM;
            changed = true;
            if (rest.isEmpty()) e.discard();
            else e.setItem(rest);
        }
        if (changed) be.setChanged();
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        upgrades = NonNullList.withSize(tier().upgradeSlots, ItemStack.EMPTY);
        input.getOrDefault(ModDataComponents.BACKPACK_UPGRADES.get(), ItemContainerContents.EMPTY).copyInto(upgrades);
        fm = input.getOrDefault(ModDataComponents.FM_CHARGE.get(), 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.BACKPACK_UPGRADES.get(), ItemContainerContents.fromItems(upgrades));
        if (fm > 0) components.set(ModDataComponents.FM_CHARGE.get(), fm);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Upgrades");
        tag.remove("Fm");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        CompoundTag ups = new CompoundTag();
        ContainerHelper.saveAllItems(ups, upgrades, registries);
        tag.put("Upgrades", ups);
        tag.putInt("Fm", fm);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(tier().slots(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        upgrades = NonNullList.withSize(tier().upgradeSlots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound("Upgrades"), upgrades, registries);
        fm = tag.getInt("Fm");
    }
}
