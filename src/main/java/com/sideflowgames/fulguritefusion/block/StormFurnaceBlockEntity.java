package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.machine.ItemTransfer;
import com.sideflowgames.fulguritefusion.machine.SideConfig;
import com.sideflowgames.fulguritefusion.machine.SideConfigurable;
import com.sideflowgames.fulguritefusion.machine.SidedItemHandler;
import com.sideflowgames.fulguritefusion.menu.StormFurnaceMenu;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

// Burns fuel to make Fm and pushes it into machines touching any face except the front.
//
// Power is scarce: every fuel is worth a fixed amount of Fm (coal = 5,120 Fm = 4 Spark Cells,
// other fuels scale with their vanilla burn time). Higher tiers don't get more out of a fuel,
// they deliver it faster: Weak 16 Fm/t, Enhanced 32 Fm/t, Charged 64 Fm/t.
public class StormFurnaceBlockEntity extends BlockEntity implements MenuProvider, SideConfigurable, FmSource {
    // Rates, fuel value and static damage come from the config (Config.java).
    public static final int BASE_CAPACITY = 16000;         // Fm stored by a Weak furnace (doubles per tier)
    public static final int COAL_BURN_TICKS = 1600;        // vanilla coal burn time
    public static final int STATIC_INTERVAL = 40;          // ticks between shocks (2 seconds)
    public static final int TRANSFER_INTERVAL = 8;         // ticks between item moves on Push/Pull faces

    // Menu data. Big numbers are split into two 16-bit halves because Minecraft only syncs 16 bits per value.
    public static final int D_FM = 0, D_MAX = 2, D_FUEL = 4, D_FUEL_TOTAL = 6, D_GROUNDED = 8, D_TIER = 9, D_RATE = 10, D_SIDES = 11;
    public static final int DATA_COUNT = D_SIDES + SideConfig.COUNT;

    // One slot: fuel. After a lava bucket burns, the empty bucket stays here (like a vanilla furnace).
    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return burnTimeOf(stack) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // Defaults like a vanilla furnace: fuel goes in from the top and sides, empty buckets come out the bottom.
    private final SideConfig sides = new SideConfig(SideConfig.PULL, SideConfig.PUSH,
            SideConfig.PULL, SideConfig.PULL, SideConfig.PULL);

    private int fm;
    private int fuelFm;      // Fm still to come from the fuel currently burning
    private int fuelFmTotal; // Fm the current fuel item started with
    private int staticTimer;
    private int transferTimer;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case D_FM -> fm & 0xFFFF;
                case D_FM + 1 -> fm >>> 16;
                case D_MAX -> getMaxFm() & 0xFFFF;
                case D_MAX + 1 -> getMaxFm() >>> 16;
                case D_FUEL -> fuelFm & 0xFFFF;
                case D_FUEL + 1 -> fuelFm >>> 16;
                case D_FUEL_TOTAL -> fuelFmTotal & 0xFFFF;
                case D_FUEL_TOTAL + 1 -> fuelFmTotal >>> 16;
                case D_GROUNDED -> level != null && isGrounded(level, worldPosition) ? 1 : 0;
                case D_TIER -> getTier();
                case D_RATE -> getRate();
                default -> sides.get(index - D_SIDES);
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public StormFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.STORM_FURNACE_BE.get(), pos, state);
    }

    public static int burnTimeOf(ItemStack stack) {
        return com.sideflowgames.fulguritefusion.util.Fuel.burnTime(stack);
    }

    // How much Fm a fuel item is worth in total.
    public static int fmValueOf(ItemStack stack) {
        return (int) Math.min(Integer.MAX_VALUE, (long) burnTimeOf(stack) * Config.FM_PER_COAL.get() / COAL_BURN_TICKS);
    }

    public int getTier() {
        return getBlockState().getBlock() instanceof StormFurnaceBlock b ? b.getTier() : 1;
    }

    // Fm per tick for this tier (default 16 / 32 / 64).
    public int getRate() {
        return switch (getTier()) {
            case 3 -> Config.CHARGED_FURNACE_RATE.get();
            case 2 -> Config.ENHANCED_FURNACE_RATE.get();
            default -> Config.WEAK_FURNACE_RATE.get();
        };
    }

    // 16,000, 32,000 or 64,000 Fm.
    public int getMaxFm() {
        return BASE_CAPACITY << (getTier() - 1);
    }

    public float getStaticDamage() {
        return (float) (Config.STATIC_DAMAGE.get() * (1 << (getTier() - 1)));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StormFurnaceBlockEntity be) {
        boolean changed = false;
        Direction front = state.getValue(StormFurnaceBlock.FACING);
        int rate = be.getRate();
        int max = be.getMaxFm();

        // Start the next fuel item when the current one is used up.
        ItemStack fuel = be.items.getStackInSlot(0);
        if (be.fuelFm <= 0 && be.fm < max && !fuel.isEmpty()) {
            int value = fmValueOf(fuel);
            if (value > 0) {
                be.fuelFm = value;
                be.fuelFmTotal = value;
                if (fuel.is(Items.LAVA_BUCKET)) {
                    be.items.setStackInSlot(0, new ItemStack(Items.BUCKET));
                } else {
                    be.items.extractItem(0, 1, false);
                }
                changed = true;
            }
        }

        // Burn: turn up to 'rate' Fm of fuel into stored Fm. Pauses while the buffer is full.
        int made = Math.min(rate, Math.min(be.fuelFm, max - be.fm));
        boolean lit = made > 0;
        if (lit) {
            be.fuelFm -= made;
            be.fm += made;
            changed = true;
        }

        // Push power into machines on every face except the front.
        // The furnace counts as the first machine of a daisy chain, so the machines it touches are 2nd (position 1).
        int pushLimit = rate * 4;
        for (Direction dir : Direction.values()) {
            if (dir == front || be.fm <= 0) continue;
            if (level.getBlockEntity(pos.relative(dir)) instanceof FmHandler machine && machine.acceptsFrom(dir.getOpposite())) {
                int sent = machine.receiveFmFromChain(Math.min(be.fm, pushLimit), 1);
                if (sent > 0) {
                    be.fm -= sent;
                    changed = true;
                }
            }
        }

        // Items on Push / Pull faces.
        if (++be.transferTimer >= TRANSFER_INTERVAL) {
            be.transferTimer = 0;
            for (int face = 0; face < SideConfig.COUNT; face++) {
                Direction dir = SideConfig.directionOf(front, face);
                int mode = be.sides.get(face);
                if (mode == SideConfig.PULL) {
                    ItemTransfer.pull(level, pos, dir, be.items, 0);
                } else if (mode == SideConfig.PUSH && burnTimeOf(be.items.getStackInSlot(0)) <= 0) {
                    ItemTransfer.push(level, pos, dir, be.items, 0);
                }
            }
        }

        // Static discharge: shocks nearby players unless the furnace is grounded. Doubles per tier.
        if (++be.staticTimer >= STATIC_INTERVAL) {
            be.staticTimer = 0;
            if (Config.STATIC_ENABLED.get() && !isGrounded(level, pos)) {
                AABB area = new AABB(pos).inflate(Config.STATIC_RANGE.get());
                for (Player player : level.getEntitiesOfClass(Player.class, area)) {
                    player.hurtOrSimulate(level.damageSources().lightningBolt(), be.getStaticDamage());
                }
            }
        }

        if (state.getValue(StormFurnaceBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(StormFurnaceBlock.LIT, lit), Block.UPDATE_ALL);
        }
        if (changed) be.setChanged();
    }

    // Grounded = three lightning rods stacked straight down under the furnace.
    public static boolean isGrounded(Level level, BlockPos pos) {
        for (int i = 1; i <= 3; i++) {
            if (!level.getBlockState(pos.below(i)).is(Blocks.LIGHTNING_ROD)) return false;
        }
        return true;
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

    public ItemStackHandler getItems() {
        return items;
    }

    // What hoppers and pipes see on a face. Null = nothing can connect there.
    public IItemHandler getItemHandler(Direction side) {
        if (side == null) return items;
        int mode = sides.modeFor(getBlockState().getValue(StormFurnaceBlock.FACING), side);
        if (mode == SideConfig.PULL) {
            return new SidedItemHandler(items, true, false, (slot, stack) -> false);
        }
        if (mode == SideConfig.PUSH) {
            // Only non-fuel leftovers (empty buckets) can be taken out.
            return new SidedItemHandler(items, false, true, (slot, stack) -> burnTimeOf(stack) <= 0);
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
        ItemStack fuel = items.getStackInSlot(0);
        String fuelText = fuel.isEmpty() ? "none" : fuel.getCount() + "x " + fuel.getHoverName().getString();
        String ground = isGrounded(level, worldPosition) ? "grounded" : "NOT grounded";
        return getDisplayName().getString() + ": " + fm + " / " + getMaxFm() + " Fm (" + getRate()
                + " Fm/t) | Fuel: " + fuelText + " | " + ground;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StormFurnaceMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveStormFurnaceBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveStormFurnaceBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        tag.putInt("Fm", fm);
        tag.putInt("FuelFm", fuelFm);
        tag.putInt("FuelFmTotal", fuelFmTotal);
        tag.put("Inventory", com.sideflowgames.fulguritefusion.util.NbtBridge.serialize(items, registries));
        sides.save(tag);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadStormFurnaceBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadStormFurnaceBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        fm = tag.getIntOr("Fm", 0);
        fuelFm = tag.getIntOr("FuelFm", 0);
        fuelFmTotal = tag.getIntOr("FuelFmTotal", 0);
        if (tag.contains("Inventory")) com.sideflowgames.fulguritefusion.util.NbtBridge.deserialize(items, registries, tag.getCompoundOrEmpty("Inventory"));
        sides.load(tag);
    }

    // 26.1: runs the block's removal logic while this block entity still exists.
    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (level == null) return;
        if (state.getBlock() instanceof StormFurnaceBlock block) block.onRemoveLegacy(state, level, pos, level.getBlockState(pos), false);
    }

    // 26.1: the real storage behind the hopper/pipe view, so an undone transfer can be put back exactly (LegacyCaps).
    public net.neoforged.neoforge.items.IItemHandlerModifiable[] capStorage() {
        return new net.neoforged.neoforge.items.IItemHandlerModifiable[] {items};
    }

}
