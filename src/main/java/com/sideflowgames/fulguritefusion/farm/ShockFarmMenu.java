package com.sideflowgames.fulguritefusion.farm;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Shock Farm Controller menu: power, status, what the biome spawns here, the On/Off button (button id 0),
// the upgrade slots (2 on a 5x5 farm, 4 on a 7x7) and the player's inventory.
public class ShockFarmMenu extends AbstractContainerMenu {
    public record SpawnEntry(Identifier id, int weight) {}

    // Slot positions, shared with ShockFarmScreen.
    public static final int[][] UPGRADE_POS = {{150, 20}, {170, 20}, {150, 40}, {170, 40}};
    public static final int INV_X = 30, INV_Y = 180;

    private final ContainerData data;
    private final BlockPos pos;
    @Nullable private final ShockFarmControllerBlockEntity farm; // server side only
    private final String problem;
    private final List<SpawnEntry> spawns = new ArrayList<>();
    private final Container upgrades;

    // Client.
    public ShockFarmMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        super(FarmMenus.SHOCK_FARM.get(), id);
        this.data = new SimpleContainerData(ShockFarmControllerBlockEntity.DATA_COUNT);
        this.farm = null;
        this.upgrades = new SimpleContainer(ShockFarmControllerBlockEntity.UPGRADE_SLOTS);
        this.pos = buf.readBlockPos();
        this.problem = buf.readUtf();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) spawns.add(new SpawnEntry(buf.readIdentifier(), buf.readVarInt()));
        addSlots(inventory);
        addDataSlots(data);
    }

    // Server.
    public ShockFarmMenu(int id, Inventory inventory, ShockFarmControllerBlockEntity farm, ContainerData data) {
        super(FarmMenus.SHOCK_FARM.get(), id);
        this.data = data;
        this.farm = farm;
        this.upgrades = farm.upgrades;
        this.pos = farm.getBlockPos();
        this.problem = "";
        addSlots(inventory);
        addDataSlots(data);
    }

    private void addSlots(Inventory inventory) {
        for (int i = 0; i < ShockFarmControllerBlockEntity.UPGRADE_SLOTS; i++) {
            addSlot(new UpgradeSlot(upgrades, i, UPGRADE_POS[i][0], UPGRADE_POS[i][1]));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INV_X + col * 18, INV_Y + 58));
    }

    public int unlockedSlots() {
        return ShockFarmControllerBlockEntity.unlockedSlots(tier());
    }

    public boolean isLocked(int slot) {
        return slot >= unlockedSlots();
    }

    public ItemStack upgrade(int slot) {
        return upgrades.getItem(slot);
    }

    private class UpgradeSlot extends Slot {
        UpgradeSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof FarmUpgradeItem u && u.kind != FarmUpgradeItem.Kind.BLANK && !isLocked(getContainerSlot());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        // A locked slot (5x5 farm) stays reachable while it still holds a card, so the card can be taken back out.
        @Override
        public boolean isActive() {
            return !isLocked(getContainerSlot()) || hasItem();
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && farm != null) {
            farm.toggle();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int up = ShockFarmControllerBlockEntity.UPGRADE_SLOTS, end = up + 36;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < up) {
            if (!moveItemStackTo(stack, up, end, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof FarmUpgradeItem) {
            boolean moved = false;
            for (int i = 0; i < up && !stack.isEmpty(); i++) {
                Slot s = slots.get(i);
                if (!s.hasItem() && s.mayPlace(stack)) {
                    s.setByPlayer(stack.split(1));
                    moved = true;
                }
            }
            if (!moved) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (farm != null && (farm.isRemoved() || farm.getLevel() != player.level())) return false;
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64 * 64 / 4.0;
    }

    private int big(int i) {
        return (data.get(i) & 0xFFFF) | ((data.get(i + 1) & 0xFFFF) << 16);
    }

    public int fm() { return big(0); }
    public int capacity() { return big(2); }
    public boolean enabled() { return data.get(4) != 0; }
    public int status() { return data.get(5); }
    public int tier() { return data.get(6); }
    public int mobs() { return data.get(7); }
    public int mobCap() { return data.get(8); }
    public int spawnCost() { return data.get(9); }
    public int spawnInterval() { return data.get(10); }
    public String problem() { return problem; }
    public List<SpawnEntry> spawns() { return spawns; }
}
