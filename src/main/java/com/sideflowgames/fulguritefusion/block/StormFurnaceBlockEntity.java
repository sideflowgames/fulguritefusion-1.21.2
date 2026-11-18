package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.power.FmHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// Burns fuel to make Fm (1 Fm per tick of burn time, so one coal = 1600 Fm = 4 Spark Cells)
// and pushes it into machines touching any face except the front.
public class StormFurnaceBlockEntity extends BlockEntity {
    public static final int MAX_FM = 4000;
    public static final int PUSH_PER_TICK = 40;
    public static final int STATIC_INTERVAL = 40;   // ticks between shocks (2 seconds)
    public static final float STATIC_DAMAGE = 1.0f; // half a heart
    public static final double STATIC_RANGE = 2.0;  // blocks

    private ItemStack fuel = ItemStack.EMPTY;
    private int burnTime;
    private int burnTotal;
    private int fm;
    private int staticTimer;

    public StormFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.STORM_FURNACE_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StormFurnaceBlockEntity be) {
        boolean changed = false;

        // Start burning the next fuel item when the current one is used up.
        if (be.burnTime <= 0 && be.fm < MAX_FM && !be.fuel.isEmpty()) {
            int duration = level.fuelValues().burnDuration(be.fuel);
            if (duration > 0) {
                be.burnTime = duration;
                be.burnTotal = duration;
                if (be.fuel.is(Items.LAVA_BUCKET)) {
                    be.fuel = ItemStack.EMPTY;
                    Block.popResource(level, pos.above(), new ItemStack(Items.BUCKET));
                } else {
                    be.fuel.shrink(1);
                }
                changed = true;
            }
        }

        // Burning makes 1 Fm per tick. It pauses while the buffer is full.
        boolean lit = be.burnTime > 0 && be.fm < MAX_FM;
        if (lit) {
            be.burnTime--;
            be.fm++;
            changed = true;
        }

        // Push power into machines on every face except the front.
        Direction front = state.getValue(StormFurnaceBlock.FACING);
        for (Direction dir : Direction.values()) {
            if (dir == front || be.fm <= 0) continue;
            if (level.getBlockEntity(pos.relative(dir)) instanceof FmHandler machine) {
                int sent = machine.receiveFm(Math.min(be.fm, PUSH_PER_TICK), false);
                if (sent > 0) {
                    be.fm -= sent;
                    changed = true;
                }
            }
        }

        // Static discharge: shocks nearby players unless the furnace is grounded.
        if (++be.staticTimer >= STATIC_INTERVAL) {
            be.staticTimer = 0;
            if (!isGrounded(level, pos) && level instanceof ServerLevel serverLevel) {
                AABB area = new AABB(pos).inflate(STATIC_RANGE);
                for (Player player : level.getEntitiesOfClass(Player.class, area)) {
                    player.hurtServer(serverLevel, serverLevel.damageSources().lightningBolt(), STATIC_DAMAGE);
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

    public void insertFuel(ItemStack held, Player player) {
        if (fuel.isEmpty()) {
            fuel = held.split(held.getMaxStackSize());
        } else if (ItemStack.isSameItemSameComponents(fuel, held)) {
            int room = fuel.getMaxStackSize() - fuel.getCount();
            fuel.grow(held.split(room).getCount());
        }
        setChanged();
    }

    public ItemStack takeFuel() {
        ItemStack out = fuel;
        fuel = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    public String statusText() {
        String fuelText = fuel.isEmpty() ? "none" : fuel.getCount() + "x " + fuel.getHoverName().getString();
        String ground = isGrounded(level, worldPosition) ? "grounded" : "NOT grounded";
        return "Storm Furnace: " + fm + " / " + MAX_FM + " Fm | Fuel: " + fuelText + " | " + ground;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fm", fm);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTotal", burnTotal);
        if (!fuel.isEmpty()) tag.put("Fuel", fuel.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fm = tag.getInt("Fm");
        burnTime = tag.getInt("BurnTime");
        burnTotal = tag.getInt("BurnTotal");
        fuel = tag.contains("Fuel") ? ItemStack.parseOptional(registries, tag.getCompound("Fuel")) : ItemStack.EMPTY;
    }
}
