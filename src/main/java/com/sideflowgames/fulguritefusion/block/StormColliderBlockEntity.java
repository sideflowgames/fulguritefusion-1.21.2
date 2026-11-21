package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.item.FusionFuelItem;
import com.sideflowgames.fulguritefusion.pairing.Pairings;
import com.sideflowgames.fulguritefusion.pairing.Pairings.Pairing;
import com.sideflowgames.fulguritefusion.pairing.Pairings.Tier;
import com.sideflowgames.fulguritefusion.power.FmHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import com.sideflowgames.fulguritefusion.menu.StormColliderMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

// Storm Collider: swap it in for the Mini Collider to turn the ring into a pairing machine.
// Each injector holds one material; every shot carries one from each into the middle, and the pair decides:
//   - Crafting pairs make late-game items (Prime Spark, Refined Fulgurite, higher Spark Cell tiers).
//   - Two exotic materials make a Fusion Fuel capsule for the big reactor, plus any byproduct.
// This is the safe way to pair: bad results only shock, smoke or knock back, they never break blocks.
// It costs Fm per shot (fed from any side, or a connector on top). Products come out of the bottom into a
// hopper/pipe, or sneak + right-click to take them.
public class StormColliderBlockEntity extends ColliderCoreBlockEntity implements FmHandler, MenuProvider {
    // Menu data (big numbers split into two 16-bit halves).
    public static final int D_FM = 0, D_MAX = 2, D_COST = 4, D_SHOT = 6, D_LENGTH = 7, D_CELLS = 8, D_MASK = 9,
            D_COMPLETE = 11, D_STATE = 12, DATA_COUNT = 13;
    public static final int S_READY = 0, S_FLIGHT = 1, S_POWER = 2, S_LOAD = 3, S_NO_PAIR = 4, S_CELLS = 5, S_FULL = 6, S_RING = 7;

    public static final int MAX_FM = 20000;
    public static final int OUTPUT_SLOTS = 9;

    private record Recipe(Supplier<Item> a, Supplier<Item> b, Supplier<Item> result) {
        boolean matches(ItemStack x, ItemStack y) {
            return (x.is(a.get()) && y.is(b.get())) || (x.is(b.get()) && y.is(a.get()));
        }
    }

    private static final List<Recipe> RECIPES = List.of(
            new Recipe(ModItems.PRIME_SPARK_FRAGMENT::get, ModItems.PRIME_SPARK_FRAGMENT::get, ModItems.PRIME_SPARK::get),
            new Recipe(ModItems.FULGURITE::get, ModItems.FULGURITE::get, ModItems.REFINED_FULGURITE::get),
            new Recipe(ModItems.SPARK_CELL::get, () -> Items.REDSTONE_BLOCK, ModItems.CHARGED_SPARK_CELL::get),
            new Recipe(ModItems.CHARGED_SPARK_CELL::get, ModItems.ARC_CORE::get, ModItems.OVERCHARGED_SPARK_CELL::get));

    private int fm;
    private ItemStack carriedA = ItemStack.EMPTY; // materials riding the cells in flight
    private ItemStack carriedB = ItemStack.EMPTY;
    private String lastResult = "none yet";
    private long lastShotTime = Long.MIN_VALUE / 2; // game time of the last firing (not saved)

    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // Hoppers and pipes can only take products out.
    private final IItemHandler extractOnly = new IItemHandler() {
        @Override
        public int getSlots() {
            return OUTPUT_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return output.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return output.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return output.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    public StormColliderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.STORM_COLLIDER_BE.get(), pos, state);
    }

    private static int cost() {
        return Config.STORM_FM_PER_SHOT.get();
    }

    @Nullable
    private ColliderInjectorBlockEntity injector(int mirror) {
        return level != null && inputDir != null
                && level.getBlockEntity(injectorPos(mirror)) instanceof ColliderInjectorBlockEntity inj ? inj : null;
    }

    private static boolean validPair(ItemStack x, ItemStack y) {
        if (x.isEmpty() || y.isEmpty()) return false;
        for (Recipe r : RECIPES) if (r.matches(x, y)) return true;
        return ModItems.materialIndex(x) >= 0 && ModItems.materialIndex(y) >= 0;
    }

    private int freeSlots() {
        int free = 0;
        for (int i = 0; i < OUTPUT_SLOTS; i++) if (output.getStackInSlot(i).isEmpty()) free++;
        return free;
    }

    @Override
    protected boolean readyToFire() {
        ColliderInjectorBlockEntity a = injector(1), b = injector(-1);
        return fm >= cost() && a != null && b != null && validPair(a.peek(), b.peek()) && freeSlots() >= 2;
    }

    @Override
    protected void onFire() {
        fm -= cost();
        if (level != null) lastShotTime = level.getGameTime();
        carriedA = injector(1).takeOne();
        carriedB = injector(-1).takeOne();
    }

    @Override
    protected void onShotLost() {
        carriedA = ItemStack.EMPTY;
        carriedB = ItemStack.EMPTY;
        lastResult = "shot lost (the ring broke)";
    }

    @Override
    protected void onCollide(ServerLevel level) {
        ItemStack a = carriedA, b = carriedB;
        carriedA = ItemStack.EMPTY;
        carriedB = ItemStack.EMPTY;

        for (Recipe r : RECIPES) {
            if (r.matches(a, b)) {
                ItemStack made = new ItemStack(r.result().get());
                give(made);
                lastResult = made.getHoverName().getString();
                return;
            }
        }

        int ia = ModItems.materialIndex(a), ib = ModItems.materialIndex(b);
        if (ia < 0 || ib < 0) {
            lastResult = "nothing (unknown pairing)";
            fizzleEffects(level);
            return;
        }
        Pairing pairing = Pairings.of(ia, ib);
        Pairing result = resolve(pairing, level.random);
        // Stormwing Eggs incubating nearby remember what was fired.
        StormwingEggBlockEntity.onPairing(level, worldPosition, pairing.a(), pairing.b(), result.tier());
        lastResult = result == pairing ? result.name() + " (" + result.tier().display() + ")"
                : pairing.name() + " -> " + result.name() + " (" + result.tier().display() + ")";

        if (result.tier() == Tier.FIZZLE) {
            fizzleEffects(level); // no reaction: the materials are wasted
            return;
        }
        give(FusionFuelItem.create(pairing.id(), result.id()));
        ItemStack byproduct = byproduct(pairing, result, level.random);
        if (!byproduct.isEmpty()) {
            give(byproduct);
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
        }
        sideEffects(level, result.tier());
    }

    // Running = a pairing is in flight, or one was fired in the last 2 minutes. Warms Stormwing Eggs.
    public boolean isRunning() {
        return level != null && inputDir != null && (shotTimer >= 0 || level.getGameTime() - lastShotTime < 2400);
    }

    // Struck Spore has no fixed result: it rolls one of its partner's other pairings.
    public static Pairing resolve(Pairing p, RandomSource random) {
        if (p.tier() != Tier.WILDCARD) return p;
        if (p.id() == 78) { // Chain Mutation: Jackpot with a fragment, or a Catastrophe
            if (random.nextBoolean()) return Pairings.byId(18);
            List<Pairing> cat = Pairings.all().stream().filter(x -> x.tier() == Tier.CATASTROPHIC).toList();
            return cat.get(random.nextInt(cat.size()));
        }
        int partner = p.a() == Pairings.STRUCK_SPORE ? p.b() : p.a();
        List<Pairing> options = Pairings.all().stream()
                .filter(x -> x.involves(partner) && !x.involves(Pairings.STRUCK_SPORE))
                .filter(x -> p.id() != 68 || x.tier() != Tier.CATASTROPHIC) // Muted Mutation: never Catastrophic
                .filter(x -> p.id() != 77 || x.tier() == Tier.BAD || x.tier() == Tier.CATASTROPHIC
                        || x.tier() == Tier.UNSTABLE) // Wild Rot: always harmful
                .toList();
        return options.get(random.nextInt(options.size()));
    }

    public static ItemStack byproduct(Pairing pairing, Pairing result, RandomSource random) {
        if (pairing.id() == 72 && random.nextFloat() < 0.05f) { // Mutation: rare Jackpot chance
            return new ItemStack(ModItems.PRIME_SPARK_FRAGMENT.get());
        }
        return switch (result.id()) {
            case 5 -> new ItemStack(ModItems.RESONANT_SHARD.get());
            case 14 -> new ItemStack(ModItems.PEARL_EMBER.get());
            case 18 -> new ItemStack(ModItems.PRIME_SPARK_FRAGMENT.get());
            case 60 -> new ItemStack(ModItems.LIVING_SPARK.get());
            default -> ItemStack.EMPTY;
        };
    }

    // The small-scale versions of the reactor's side effects. Never breaks blocks.
    private void sideEffects(ServerLevel level, Tier tier) {
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.0, z = worldPosition.getZ() + 0.5;
        switch (tier) {
            case UNSTABLE -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 60, 2.0, 0.5, 2.0, 0.2);
                shock(level, 4.0, 2.0f);
            }
            case BAD -> {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 30, 1.0, 0.5, 1.0, 0.03);
                shock(level, 5.0, 4.0f);
            }
            case CATASTROPHIC -> {
                level.explode(null, x, y, z, 2.0f, Level.ExplosionInteraction.NONE);
                shock(level, 6.0, 4.0f);
            }
            default -> {}
        }
    }

    private void shock(ServerLevel level, double range, float damage) {
        AABB area = new AABB(worldPosition).inflate(range);
        for (Player player : level.getEntitiesOfClass(Player.class, area)) {
            player.hurt(level.damageSources().lightningBolt(), damage);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 10, 0.3, 0.5, 0.3, 0.1);
        }
    }

    private void give(ItemStack stack) {
        ItemStack left = ItemHandlerHelper.insertItem(output, stack, false);
        if (!left.isEmpty() && level != null) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, left);
        }
    }

    // ---------- Power and items ----------

    @Override
    public int receiveFm(int amount, boolean simulate) {
        int accepted = Math.max(0, Math.min(amount, MAX_FM - fm));
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

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return side == Direction.DOWN ? extractOnly : null;
    }

    public void takeOutputs(Player player) {
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack out = output.extractItem(i, 64, false);
            if (!out.isEmpty() && !player.getInventory().add(out)) player.drop(out, false);
        }
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), output.getStackInSlot(i));
        }
    }

    private int state() {
        if (problem != null) return S_RING;
        ColliderInjectorBlockEntity a = injector(1), b = injector(-1);
        if (shotTimer >= 0) return S_FLIGHT;
        if (fm < cost()) return S_POWER;
        if (a == null || b == null || a.peek().isEmpty() || b.peek().isEmpty()) return S_LOAD;
        if (!validPair(a.peek(), b.peek())) return S_NO_PAIR;
        if (loadedCells() < 2) return S_CELLS;
        if (freeSlots() < 2) return S_FULL;
        return S_READY;
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            int cost = cost();
            return switch (i) {
                case D_FM -> fm & 0xFFFF;
                case D_FM + 1 -> fm >>> 16;
                case D_MAX -> MAX_FM & 0xFFFF;
                case D_MAX + 1 -> MAX_FM >>> 16;
                case D_COST -> cost & 0xFFFF;
                case D_COST + 1 -> cost >>> 16;
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

    public ItemStackHandler getOutput() {
        return output;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fulguritefusion.storm_collider");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StormColliderMenu(id, inventory, this, data);
    }

    public String statusText() {
        if (problem != null) return "Storm Collider: ring incomplete | " + problem;
        int stored = 0;
        for (int i = 0; i < OUTPUT_SLOTS; i++) stored += output.getStackInSlot(i).getCount();
        String state = switch (state()) {
            case S_FLIGHT -> "pairing in flight";
            case S_POWER -> "needs " + cost() + " Fm per shot";
            case S_LOAD -> "load both injectors";
            case S_NO_PAIR -> "those two can't be paired";
            case S_CELLS -> "needs 2 Spark Cells in the input";
            case S_FULL -> "output full";
            default -> "ready";
        };
        return "Storm Collider: " + state + " | " + fm + " / " + MAX_FM + " Fm | " + stored
                + " items waiting | last: " + lastResult;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fm", fm);
        tag.put("Output", output.serializeNBT(registries));
        if (!carriedA.isEmpty()) tag.put("CarriedA", carriedA.save(registries));
        if (!carriedB.isEmpty()) tag.put("CarriedB", carriedB.save(registries));
        tag.putString("LastResult", lastResult);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fm = tag.getInt("Fm");
        output.deserializeNBT(registries, tag.getCompound("Output"));
        carriedA = ItemStack.parseOptional(registries, tag.getCompound("CarriedA"));
        carriedB = ItemStack.parseOptional(registries, tag.getCompound("CarriedB"));
        if (tag.contains("LastResult")) lastResult = tag.getString("LastResult");
    }
}
