package com.sideflowgames.fulguritefusion.backpack;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModDataComponents;
import com.sideflowgames.fulguritefusion.item.FmChargeable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// What backpack upgrades do. They work in any backpack the player is wearing or carrying (inventory,
// chestplate slot, offhand or Curios back slot). Stormforged upgrades take their boost from the Stormforged
// Backpack's own Fm; with too little Fm they behave like the basic upgrade.
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class BackpackUpgrades {
    private BackpackUpgrades() {}

    public static final double MAGNET_RANGE = 6, STORM_MAGNET_RANGE = 12;
    public static final int MAGNET_FM = 2;          // per pull (every 2 ticks) while reaching past 6 blocks
    public static final double PICKUP_REACH = 3;    // Storm Pickup grabs items this far away
    public static final int PICKUP_REACH_FM = 5;    // per stack grabbed
    public static final double VOID_XP_RANGE = 6;
    public static final int VOID_XP_FM = 5;         // per orb
    public static final int COMPACT_FM = 50;        // per item type packed 3x3
    public static final int HEAL_FM = 100;          // Storm Feeding, per half heart
    public static final int REFILL_FM = 10;         // Storm Refill, per refill
    public static final int REMOTE_FM = 500;        // Storm Restock / Deposit from the backpack screen
    public static final double REMOTE_RANGE = 8;
    public static final int WEAPON_FM = 50;         // Storm Tool Swapper, per weapon swap
    private static final double PULL = 0.45;

    // ---------- Finding backpacks and upgrades ----------

    public static List<ItemStack> packs(Player player) {
        List<ItemStack> list = new ArrayList<>();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof BackpackItem) list.add(s);
        }
        ItemStack back = com.sideflowgames.fulguritefusion.trinket.Trinkets.back(player);
        if (!back.isEmpty() && list.stream().noneMatch(s -> s == back)) list.add(back);
        ItemStack curio = CuriosCompat.find(player, s -> s.getItem() instanceof BackpackItem);
        if (!curio.isEmpty() && list.stream().noneMatch(s -> s == curio)) list.add(curio);
        return list;
    }

    public static List<ItemStack> upgrades(ItemStack pack) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack s : pack.getOrDefault(ModDataComponents.BACKPACK_UPGRADES.get(), ItemContainerContents.EMPTY).nonEmptyItems()) {
            if (s.getItem() instanceof BackpackUpgradeItem) list.add(s);
        }
        return list;
    }

    static ItemStack find(ItemStack pack, UpgradeKind kind) {
        for (ItemStack u : upgrades(pack)) {
            if (((BackpackUpgradeItem) u.getItem()).kind == kind) return u;
        }
        return ItemStack.EMPTY;
    }

    private static boolean storm(ItemStack upgrade) {
        return upgrade.getItem() instanceof BackpackUpgradeItem u && u.storm;
    }

    private static boolean powered(ItemStack pack, ItemStack upgrade, int cost) {
        return storm(upgrade) && FmChargeable.get(pack) >= cost;
    }

    // The pack's storage. If the player has this pack open, the open screen's copy is used, so the two
    // never disagree.
    static Container storage(Player player, ItemStack pack) {
        if (player.containerMenu instanceof BackpackMenu m && m.backpackItem() == pack) return m.storage();
        return new BackpackItemContainer(pack, ((BackpackItem) pack.getItem()).tier.slots());
    }

    // Puts as much of the stack as fits into the container; returns what's left.
    public static ItemStack insert(Container c, ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof BackpackItem || !stack.getItem().canFitInsideContainerItems()) return stack;
        int n = c.getContainerSize();
        boolean changed = false;
        for (int i = 0; i < n && !stack.isEmpty(); i++) {
            ItemStack s = c.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)) {
                int move = Math.min(stack.getCount(), c.getMaxStackSize(s) - s.getCount());
                if (move > 0) {
                    s.grow(move);
                    stack.shrink(move);
                    changed = true;
                }
            }
        }
        for (int i = 0; i < n && !stack.isEmpty(); i++) {
            if (c.getItem(i).isEmpty()) {
                c.setItem(i, stack.split(Math.min(stack.getCount(), c.getMaxStackSize(stack))));
                changed = true;
            }
        }
        if (changed) c.setChanged();
        return stack;
    }

    // ---------- Pickup and Void ----------

    private enum Result { NONE, PICKED, VOIDED }

    // Runs Void, then Pickup, on a stack the player is picking up. Changes the stack's count in place.
    private static Result absorb(Player player, List<ItemStack> packs, ItemStack stack) {
        for (ItemStack pack : packs) {
            ItemStack v = find(pack, UpgradeKind.VOID);
            if (!v.isEmpty() && BackpackUpgradeItem.matches(v, stack)) {
                stack.setCount(0);
                return Result.VOIDED;
            }
        }
        Result r = Result.NONE;
        for (ItemStack pack : packs) {
            ItemStack p = find(pack, UpgradeKind.PICKUP);
            if (p.isEmpty() || !BackpackUpgradeItem.matches(p, stack)) continue;
            ItemStack rest = insert(storage(player, pack), stack.copy());
            if (rest.getCount() < stack.getCount()) {
                stack.setCount(rest.getCount());
                r = Result.PICKED;
            }
            if (stack.isEmpty()) break;
        }
        return r;
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        if (player.level().isClientSide || player.isSpectator() || !entity.isAlive() || entity.hasPickUpDelay()) return;
        List<ItemStack> packs = packs(player);
        if (packs.isEmpty()) return;
        // The stack is changed in place: vanilla then picks up whatever is left of this same stack.
        ItemStack stack = entity.getItem();
        int before = stack.getCount();
        Result r = absorb(player, packs, stack);
        if (r == Result.NONE) return;
        if (r == Result.PICKED) player.take(entity, before - stack.getCount());
        if (stack.isEmpty()) {
            entity.discard();
            event.setCanPickup(TriState.FALSE);
        }
    }

    // ---------- Ticking upgrades ----------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.isSpectator() || !player.isAlive()) return;
        long t = player.level().getGameTime() + player.getId();
        if (t % 2 != 0) return;
        List<ItemStack> packs = packs(player);
        if (packs.isEmpty()) return;
        magnet(player, packs);
        if (t % 6 == 0) reach(player, packs);
        if (t % 10 == 0) voidXp(player, packs);
        if (t % 40 == 0) compact(player, packs);
        if (t % 20 == 0) feed(player, packs);
        refill(player, packs, t % 10 == 0);
    }

    private static boolean pullable(ItemEntity e) {
        return e.isAlive() && !e.hasPickUpDelay() && !e.getPersistentData().getBoolean("PreventRemoteMovement");
    }

    private static void magnet(Player player, List<ItemStack> packs) {
        ItemStack bestPack = ItemStack.EMPTY, best = ItemStack.EMPTY;
        double range = 0;
        for (ItemStack pack : packs) {
            ItemStack u = find(pack, UpgradeKind.MAGNET);
            if (u.isEmpty()) continue;
            double r = powered(pack, u, MAGNET_FM) ? STORM_MAGNET_RANGE : MAGNET_RANGE;
            if (r > range) {
                range = r;
                best = u;
                bestPack = pack;
            }
        }
        if (best.isEmpty()) return;
        Vec3 target = player.position().add(0, 0.6, 0);
        AABB area = player.getBoundingBox().inflate(range);
        final ItemStack filter = best;
        boolean far = false;
        for (ItemEntity e : player.level().getEntitiesOfClass(ItemEntity.class, area,
                e -> pullable(e) && BackpackUpgradeItem.matches(filter, e.getItem()))) {
            if (e.distanceTo(player) > MAGNET_RANGE) far = true;
            pull(e, target);
        }
        for (ExperienceOrb orb : player.level().getEntitiesOfClass(ExperienceOrb.class, area, Entity::isAlive)) {
            pull(orb, target);
        }
        if (far) FmChargeable.use(bestPack, MAGNET_FM);
    }

    private static void pull(Entity e, Vec3 target) {
        Vec3 to = target.subtract(e.position());
        if (to.lengthSqr() < 0.25) return;
        e.setDeltaMovement(to.normalize().scale(PULL));
        e.hasImpulse = true;
    }

    // Storm Pickup: grabs items within 3 blocks straight into the pack.
    private static void reach(Player player, List<ItemStack> packs) {
        ItemStack powerPack = ItemStack.EMPTY;
        for (ItemStack pack : packs) {
            if (powered(pack, find(pack, UpgradeKind.PICKUP), PICKUP_REACH_FM)) powerPack = pack;
        }
        if (powerPack.isEmpty()) return;
        AABB area = player.getBoundingBox().inflate(PICKUP_REACH);
        for (ItemEntity e : player.level().getEntitiesOfClass(ItemEntity.class, area, BackpackUpgrades::pullable)) {
            if (FmChargeable.get(powerPack) < PICKUP_REACH_FM) break;
            ItemStack stack = e.getItem().copy();
            int before = stack.getCount();
            Result r = absorb(player, packs, stack);
            if (r == Result.NONE) continue;
            FmChargeable.use(powerPack, PICKUP_REACH_FM);
            if (r == Result.PICKED) player.take(e, before - stack.getCount());
            if (stack.isEmpty()) e.discard();
            else e.setItem(stack);
        }
    }

    // Storm Void with "Void XP" on: removes XP orbs around you.
    private static void voidXp(Player player, List<ItemStack> packs) {
        for (ItemStack pack : packs) {
            ItemStack v = find(pack, UpgradeKind.VOID);
            if (v.isEmpty() || !storm(v) || !BackpackUpgradeItem.voidXp(v)) continue;
            AABB area = player.getBoundingBox().inflate(VOID_XP_RANGE);
            for (ExperienceOrb orb : player.level().getEntitiesOfClass(ExperienceOrb.class, area, Entity::isAlive)) {
                if (FmChargeable.use(pack, VOID_XP_FM) < VOID_XP_FM) return;
                orb.discard();
            }
            return;
        }
    }

    private static void compact(Player player, List<ItemStack> packs) {
        for (ItemStack pack : packs) {
            ItemStack u = find(pack, UpgradeKind.COMPACTING);
            if (u.isEmpty()) continue;
            boolean big = powered(pack, u, COMPACT_FM);
            int packed = Compactor.compact(player.level(), storage(player, pack), u, big);
            if (packed > 0) FmChargeable.use(pack, packed * COMPACT_FM);
        }
    }

    // ---------- Feeding ----------

    private static boolean harmful(ItemStack s, FoodProperties food) {
        if (s.is(Items.CHORUS_FRUIT)) return true;
        for (FoodProperties.PossibleEffect e : food.effects()) {
            if (e.effect().getEffect().value().getCategory() == MobEffectCategory.HARMFUL) return true;
        }
        return false;
    }

    // Harmful food only when it's on an allow list.
    private static boolean edible(ItemStack upgrade, ItemStack s, FoodProperties food) {
        if (!BackpackUpgradeItem.matches(upgrade, s)) return false;
        return !harmful(s, food) || BackpackUpgradeItem.allowList(upgrade);
    }

    private static void feed(Player player, List<ItemStack> packs) {
        FoodData fd = player.getFoodData();
        for (ItemStack pack : packs) {
            ItemStack u = find(pack, UpgradeKind.FEEDING);
            if (u.isEmpty()) continue;
            if (fd.getFoodLevel() < 20) {
                Container c = storage(player, pack);
                for (int i = 0; i < c.getContainerSize(); i++) {
                    ItemStack s = c.getItem(i);
                    if (s.isEmpty()) continue;
                    FoodProperties food = s.getFoodProperties(player);
                    // Only when none of it would go to waste.
                    if (food == null || 20 - fd.getFoodLevel() < food.nutrition() || !edible(u, s, food)) continue;
                    ItemStack one = s.split(1);
                    c.setItem(i, s);
                    ItemStack left = one.finishUsingItem(player.level(), player);
                    if (!left.isEmpty()) {
                        ItemStack r = insert(c, left);
                        if (!r.isEmpty() && !player.getInventory().add(r)) player.drop(r, false);
                    }
                    c.setChanged();
                    return;
                }
            } else if (player.getHealth() < player.getMaxHealth() && powered(pack, u, HEAL_FM)) {
                player.heal(1);
                FmChargeable.use(pack, HEAL_FM);
                return;
            }
        }
    }

    // ---------- Refill ----------

    // Inventory slot -> what it held at the last check (count kept), per player.
    private static final Map<UUID, ItemStack[]> LAST = new ConcurrentHashMap<>();
    private static final int[] HOTBAR = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] STORM_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 40, 36, 37, 38, 39};

    private static void refill(Player player, List<ItemStack> packs, boolean basicTick) {
        ItemStack pack = ItemStack.EMPTY, u = ItemStack.EMPTY;
        for (ItemStack p : packs) {
            ItemStack r = find(p, UpgradeKind.REFILL);
            if (r.isEmpty()) continue;
            if (u.isEmpty() || powered(p, r, REFILL_FM)) {
                pack = p;
                u = r;
            }
        }
        if (u.isEmpty()) {
            LAST.remove(player.getUUID());
            return;
        }
        boolean storm = powered(pack, u, REFILL_FM);
        if (!storm && !basicTick) return;
        var inv = player.getInventory();
        ItemStack[] last = LAST.computeIfAbsent(player.getUUID(), k -> new ItemStack[41]);
        // Not while another screen is open: don't fight the player moving things around.
        boolean busy = player.containerMenu != player.inventoryMenu;
        for (int slot : storm ? STORM_SLOTS : HOTBAR) {
            ItemStack cur = inv.getItem(slot);
            ItemStack was = last[slot];
            if (!busy && refillSlot(player, pack, u, slot, cur, was) && storm) FmChargeable.use(pack, REFILL_FM);
            last[slot] = inv.getItem(slot).copy();
        }
    }

    private static boolean refillSlot(Player player, ItemStack pack, ItemStack u, int slot, ItemStack cur, ItemStack was) {
        var inv = player.getInventory();
        boolean armor = slot >= 36 && slot <= 39;
        Container c = null;
        if (cur.isEmpty()) {
            if (was == null || was.isEmpty()) return false;
            // Armor: only a piece that was about to break. Other slots: only a stack that was running low.
            if (armor ? !(was.isDamageableItem() && was.getDamageValue() >= was.getMaxDamage() * 0.9) : was.getCount() > 8) return false;
            if (!BackpackUpgradeItem.matches(u, was)) return false;
            c = storage(player, pack);
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                boolean same = armor ? ItemStack.isSameItem(s, was) : ItemStack.isSameItemSameComponents(s, was);
                if (!s.isEmpty() && same) {
                    inv.setItem(slot, s.split(armor ? 1 : s.getMaxStackSize()));
                    c.setItem(i, s);
                    c.setChanged();
                    return true;
                }
            }
            return false;
        }
        if (armor || !cur.isStackable() || cur.getCount() >= cur.getMaxStackSize() || !BackpackUpgradeItem.matches(u, cur)) return false;
        c = storage(player, pack);
        boolean changed = false;
        for (int i = 0; i < c.getContainerSize() && cur.getCount() < cur.getMaxStackSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty() || !ItemStack.isSameItemSameComponents(s, cur)) continue;
            int n = Math.min(s.getCount(), cur.getMaxStackSize() - cur.getCount());
            cur.grow(n);
            s.shrink(n);
            c.setItem(i, s);
            changed = true;
        }
        if (changed) {
            inv.setItem(slot, cur);
            c.setChanged();
        }
        return changed;
    }

    // ---------- Tool Swapper ----------

    private static double toolScore(ItemStack s, BlockState state) {
        if (s.isDamageableItem() && s.getMaxDamage() - s.getDamageValue() <= 1) return -1;
        return (s.isCorrectToolForDrops(state) ? 1000 : 0) + s.getDestroySpeed(state);
    }

    private static double weaponScore(ItemStack s) {
        if (s.isDamageableItem() && s.getMaxDamage() - s.getDamageValue() <= 1) return -1;
        double[] d = {0};
        s.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.value() == Attributes.ATTACK_DAMAGE.value() && mod.operation() == AttributeModifier.Operation.ADD_VALUE) d[0] += mod.amount();
        });
        return d[0];
    }

    // Swaps the best-scoring item in the pack with the one in the player's hand. True if it swapped.
    private static boolean swapBest(Player player, ItemStack pack, ItemStack u, java.util.function.ToDoubleFunction<ItemStack> score) {
        var inv = player.getInventory();
        ItemStack held = inv.getSelected();
        if (held.getItem() instanceof BackpackItem || !held.getItem().canFitInsideContainerItems()) return false;
        Container c = storage(player, pack);
        double best = held.isEmpty() ? 0 : score.applyAsDouble(held);
        int bestSlot = -1;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty() || !BackpackUpgradeItem.matches(u, s)) continue;
            double sc = score.applyAsDouble(s);
            if (sc > best + 0.01) {
                best = sc;
                bestSlot = i;
            }
        }
        if (bestSlot < 0) return false;
        ItemStack tool = c.getItem(bestSlot);
        c.setItem(bestSlot, held);
        inv.setItem(inv.selected, tool);
        c.setChanged();
        return true;
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.isCreative() || player.isSpectator()
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        BlockState state = player.level().getBlockState(event.getPos());
        if (state.isAir() || state.getDestroySpeed(player.level(), event.getPos()) < 0) return;
        for (ItemStack pack : packs(player)) {
            ItemStack u = find(pack, UpgradeKind.TOOL_SWAPPER);
            if (!u.isEmpty() && swapBest(player, pack, u, s -> toolScore(s, state))) return;
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.isSpectator() || !(event.getTarget() instanceof LivingEntity)) return;
        for (ItemStack pack : packs(player)) {
            ItemStack u = find(pack, UpgradeKind.TOOL_SWAPPER);
            if (u.isEmpty() || !powered(pack, u, WEAPON_FM)) continue;
            if (swapBest(player, pack, u, BackpackUpgrades::weaponScore)) {
                FmChargeable.use(pack, WEAPON_FM);
                return;
            }
        }
    }

    // ---------- Restock / Deposit buttons ----------

    // 0 = deposit into the open container, 1 = restock from it,
    // 2 = deposit into the container you're looking at (Storm, from the backpack screen), 3 = restock from it.
    public static void handleUtility(Player player, int action) {
        boolean deposit = action == 0 || action == 2;
        UpgradeKind kind = deposit ? UpgradeKind.DEPOSIT : UpgradeKind.RESTOCK;
        int moved = 0;
        if (action == 0 || action == 1) {
            var menu = player.containerMenu;
            if (menu instanceof InventoryMenu || menu instanceof BackpackMenu || menu instanceof UpgradeFilterMenu) return;
            UtilityTransfer.Target target = UtilityTransfer.slots(menu.slots, player);
            if (target.size() == 0) return;
            boolean found = false;
            for (ItemStack pack : packs(player)) {
                ItemStack u = find(pack, kind);
                if (u.isEmpty()) continue;
                found = true;
                Container c = storage(player, pack);
                moved += deposit ? UtilityTransfer.deposit(c, target, u) : UtilityTransfer.restock(c, target, u);
            }
            if (!found) return;
            menu.broadcastChanges();
        } else if (action == 2 || action == 3) {
            if (!(player.containerMenu instanceof BackpackMenu m) || m.backpackItem().isEmpty()) return;
            ItemStack pack = m.backpackItem();
            ItemStack u = find(pack, kind);
            if (u.isEmpty() || !powered(pack, u, REMOTE_FM)) {
                player.displayClientMessage(Component.literal("Not enough Fm (needs " + REMOTE_FM + ")").withStyle(ChatFormatting.RED), true);
                return;
            }
            HitResult hit = player.pick(REMOTE_RANGE, 1f, false);
            var handler = hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK
                    ? player.level().getCapability(Capabilities.ItemHandler.BLOCK, bh.getBlockPos(), bh.getDirection()) : null;
            if (handler == null) {
                player.displayClientMessage(Component.literal("Look at a container within 8 blocks when you open the pack")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            UtilityTransfer.Target target = UtilityTransfer.handler(handler);
            moved = deposit ? UtilityTransfer.deposit(m.storage(), target, u) : UtilityTransfer.restock(m.storage(), target, u);
            if (moved > 0) FmChargeable.use(pack, REMOTE_FM);
            m.broadcastChanges();
        }
        player.displayClientMessage(Component.literal((deposit ? "Deposited " : "Restocked ") + moved + " item" + (moved == 1 ? "" : "s"))
                .withStyle(ChatFormatting.AQUA), true);
    }
}
