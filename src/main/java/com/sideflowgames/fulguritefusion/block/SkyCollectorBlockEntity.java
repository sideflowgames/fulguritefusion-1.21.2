package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

// See SkyCollectorBlock. Checks the sky once a second; while conditions hold it fills up and drops an item
// into its 9-slot store every so often (Aurora Dust every 25 s of clear night, Sunmote every 15 s of clear midday).
public class SkyCollectorBlockEntity extends BlockEntity {
    public static final int AURORA_MIN_Y = 200;
    private static final int AURORA_SECONDS = 25, SUN_SECONDS = 15;

    private int progress;
    private String status = "";

    private final ItemStackHandler store = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // Hoppers and pipes may only take out.
    private final IItemHandler output = new IItemHandler() {
        @Override
        public int getSlots() {
            return store.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return store.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return store.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return store.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    public SkyCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SKY_COLLECTOR_BE.get(), pos, state);
    }

    public IItemHandler getOutput() {
        return output;
    }

    public SkyCollectorBlock.Mode currentMode() {
        Level l = level;
        if (l == null || !l.dimensionType().hasSkyLight() || l.dimensionType().hasCeiling()) {
            status = "needs the open sky of the Overworld";
            return SkyCollectorBlock.Mode.IDLE;
        }
        if (!l.canSeeSky(worldPosition.above())) {
            status = "needs open sky above it";
            return SkyCollectorBlock.Mode.IDLE;
        }
        if (l.isRaining()) {
            status = "waiting for clear weather";
            return SkyCollectorBlock.Mode.IDLE;
        }
        long time = l.getDayTime() % 24000L;
        boolean night = time >= 13000 && time <= 23000, midday = time >= 4500 && time <= 7500;
        if (night) {
            if (worldPosition.getY() >= AURORA_MIN_Y) {
                status = "gathering Aurora Dust";
                return SkyCollectorBlock.Mode.AURORA;
            }
            status = "Aurora Dust only falls at Y " + AURORA_MIN_Y + " or higher";
            return SkyCollectorBlock.Mode.IDLE;
        }
        if (midday) {
            var biome = l.getBiome(worldPosition);
            if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BADLANDS)) {
                status = "gathering Sunmote";
                return SkyCollectorBlock.Mode.SUN;
            }
            status = "Sunmote only falls in a desert or badlands";
            return SkyCollectorBlock.Mode.IDLE;
        }
        status = "waiting: Aurora Dust on clear nights up high, Sunmote at midday in deserts and badlands";
        return SkyCollectorBlock.Mode.IDLE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SkyCollectorBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        SkyCollectorBlock.Mode mode = be.currentMode();
        if (state.getValue(SkyCollectorBlock.MODE) != mode) {
            level.setBlock(pos, state.setValue(SkyCollectorBlock.MODE, mode), Block.UPDATE_CLIENTS);
        }
        if (mode == SkyCollectorBlock.Mode.IDLE) return;
        be.progress++;
        int need = mode == SkyCollectorBlock.Mode.AURORA ? AURORA_SECONDS : SUN_SECONDS;
        if (be.progress >= need) {
            be.progress = 0;
            ItemStack item = new ItemStack(mode == SkyCollectorBlock.Mode.AURORA ? ModItems.AURORA_DUST.get() : ModItems.SUNMOTE.get());
            for (int i = 0; i < be.store.getSlots() && !item.isEmpty(); i++) item = be.store.insertItem(i, item, false);
        }
        be.setChanged();
    }

    public void interact(Player player) {
        boolean gave = false;
        for (int i = 0; i < store.getSlots(); i++) {
            ItemStack s = store.extractItem(i, 64, false);
            if (!s.isEmpty()) {
                gave = true;
                if (!player.getInventory().add(s)) player.drop(s, false);
            }
        }
        if (!gave) {
            currentMode();
            player.displayClientMessage(Component.literal("Sky Collector: " + status), true);
        }
    }

    public void dropContents() {
        for (int i = 0; i < store.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    store.getStackInSlot(i));
            store.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Store", store.serializeNBT(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Store")) store.deserializeNBT(registries, tag.getCompound("Store"));
        progress = tag.getInt("Progress");
    }
}
