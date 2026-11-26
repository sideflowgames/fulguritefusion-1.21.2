package com.sideflowgames.fulguritefusion.backpack;

import com.sideflowgames.fulguritefusion.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntSupplier;

// A backpack's storage (from the item or a placed backpack) above the player's inventory, with its upgrade
// slots in a column on the left. Backpacks can't go inside backpacks, and the backpack being used can't be
// moved while it's open.
// Slot order: storage, then upgrades, then the player's inventory.
public class BackpackMenu extends AbstractContainerMenu {
    public static final int SLOT = 18, TOP = 18, GAP = 14, UPGRADE_X = -19;

    public final BackpackTier tier;
    private final Container storage;
    private final Container upgrades;
    private final int lockedSlot; // inventory slot holding the open backpack, or -1
    private final DataSlot charge; // Stormforged: stored Fm, in thousandths of full

    // Client side.
    public BackpackMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, BackpackTier.values()[buf.readVarInt()], null, null, buf.readVarInt(), null);
    }

    public BackpackMenu(int id, Inventory inventory, BackpackTier tier, Container storage, Container upgrades,
                        int lockedSlot, IntSupplier chargePermille) {
        super(ModMenus.BACKPACK.get(), id);
        this.tier = tier;
        this.storage = storage != null ? storage : new SimpleContainer(tier.slots());
        this.upgrades = upgrades != null ? upgrades : new SimpleContainer(tier.upgradeSlots);
        this.lockedSlot = lockedSlot;
        this.storage.startOpen(inventory.player);
        int w = width();
        int x0 = (w - tier.columns * SLOT) / 2 + 1;
        for (int r = 0; r < tier.rows; r++) {
            for (int c = 0; c < tier.columns; c++) {
                addSlot(new Slot(this.storage, c + r * tier.columns, x0 + c * SLOT, TOP + r * SLOT) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !(stack.getItem() instanceof BackpackItem) && stack.getItem().canFitInsideContainerItems();
                    }
                });
            }
        }
        for (int i = 0; i < tier.upgradeSlots; i++) {
            final int index = i;
            addSlot(new Slot(this.upgrades, i, UPGRADE_X, TOP + i * SLOT) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (!(stack.getItem() instanceof BackpackUpgradeItem u) || !u.fits(tier)) return false;
                    // One of each kind (a basic and a Stormforged one of the same kind count as the same).
                    for (int j = 0; j < upgrades().getContainerSize(); j++) {
                        if (j != index && upgrades().getItem(j).getItem() instanceof BackpackUpgradeItem o && o.kind == u.kind) return false;
                    }
                    return true;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        int px = (w - 9 * SLOT) / 2 + 1, py = playerInventoryY();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) addSlot(lockable(inventory, c + r * 9 + 9, px + c * SLOT, py + r * SLOT));
        }
        for (int c = 0; c < 9; c++) addSlot(lockable(inventory, c, px + c * SLOT, py + 58));
        charge = chargePermille == null ? DataSlot.standalone() : new DataSlot() {
            @Override public int get() { return chargePermille.getAsInt(); }
            @Override public void set(int value) {}
        };
        addDataSlot(charge);
    }

    private Slot lockable(Inventory inv, int index, int x, int y) {
        return new Slot(inv, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return index != lockedSlot;
            }
        };
    }

    public Container storage() {
        return storage;
    }

    public Container upgrades() {
        return upgrades;
    }

    // The backpack item this menu shows, or empty for a placed backpack (and on the client).
    public ItemStack backpackItem() {
        return storage instanceof BackpackItemContainer c ? c.backpack() : ItemStack.EMPTY;
    }

    public float charge() {
        return charge.get() / 1000f;
    }

    public int width() {
        return Math.max(176, tier.columns * SLOT + 14);
    }

    public int playerInventoryY() {
        return TOP + tier.rows * SLOT + GAP;
    }

    public int height() {
        return playerInventoryY() + 58 + SLOT + 6;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput type, Player player) {
        // Don't let number keys swap the open backpack out of its hotbar slot.
        if (type == ContainerInput.SWAP && button == lockedSlot) return;
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int size = tier.slots();
        int ups = size + tier.upgradeSlots;
        if (index < ups) {
            if (!moveItemStackTo(stack, ups, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (stack.getItem() instanceof BackpackItem) return ItemStack.EMPTY;
            boolean moved = stack.getItem() instanceof BackpackUpgradeItem && moveItemStackTo(stack, size, ups, false);
            if (!moved && !moveItemStackTo(stack, 0, size, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return storage.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        storage.stopOpen(player);
    }
}
