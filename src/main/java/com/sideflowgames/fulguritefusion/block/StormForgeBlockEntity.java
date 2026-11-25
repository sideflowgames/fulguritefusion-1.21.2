package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.forge.ForgeRecipes;
import com.sideflowgames.fulguritefusion.power.FmHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// Storm Forge: the Forge Anvil core. The structure (checked every second):
//   - a 5x5 floor of Tempered Arc Casing under the anvil (Forge Power Ports may stand in for any edge
//     casing except the corners),
//   - a Conductor Pylon on each floor corner: 2 Reinforced Fulgurite with a Lightning Rod on top.
// Right-click the anvil with an item to set one on it (up to 6 kinds). When the items match a forge recipe,
// the forge pours Fm into them (up to 8,192 Fm/t, fed through the anvil or a Forge Power Port) while arcs jump
// from the pylons. When the full cost is paid the items become the result.
// Empty hand: take the result, or the last item back. Sneak + empty hand: status.
public class StormForgeBlockEntity extends BlockEntity implements FmHandler {
    public static final int DRAW_RATE = 8192;           // Fm per tick poured into the work
    public static final int BUFFER = DRAW_RATE * 20;    // Fm the anvil can hold (one second of drawing)
    public static final int MAX_STACKS = 6;

    private final List<ItemStack> contents = new ArrayList<>();
    private ItemStack result = ItemStack.EMPTY;
    private int fm;
    private int progress;
    private boolean formed;
    private String problem = "not checked yet";
    private boolean working;      // synced: arcs and sound on the client
    @Nullable private ForgeRecipes.Recipe recipe;
    private boolean recipeDirty = true;

    public StormForgeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlocks.STORM_FORGE_BE.get(), pos, state);
    }

    // For larger forges built on the same anvil logic (see TitanForgeBlockEntity).
    protected StormForgeBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ---------- What a particular forge overrides ----------

    protected String title() {
        return "Storm Forge";
    }

    protected int drawRate() {
        return DRAW_RATE;
    }

    protected int buffer() {
        return BUFFER;
    }

    @Nullable
    protected ForgeRecipes.Recipe matchRecipe(List<ItemStack> items) {
        return ForgeRecipes.match(items);
    }

    protected ItemStack craft(ForgeRecipes.Recipe recipe, List<ItemStack> items) {
        return recipe.make(items);
    }

    // Where the arcs start from.
    protected List<Vec3> pylonTips() {
        List<Vec3> out = new ArrayList<>();
        for (int[] c : CORNERS) out.add(Vec3.atCenterOf(worldPosition.offset(c[0], 2, c[1])).add(0, 0.4, 0));
        return out;
    }

    // Whether a Forge Power Port at (dx, -1, dz) from the anvil feeds this forge.
    public boolean acceptsPort(int dx, int dz) {
        return isEdge(dx, dz);
    }

    // ---------- Structure ----------

    // Offsets of the pylon corners.
    public static final int[][] CORNERS = {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}};

    public static boolean isEdge(int dx, int dz) {
        return (Math.abs(dx) == 2 || Math.abs(dz) == 2) && !(Math.abs(dx) == 2 && Math.abs(dz) == 2);
    }

    @Nullable
    protected String checkStructure() {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockState s = level.getBlockState(worldPosition.offset(dx, -1, dz));
                boolean ok = s.is(ModBlocks.TEMPERED_ARC_CASING.get()) || (isEdge(dx, dz) && s.is(ModBlocks.FORGE_POWER_PORT.get()));
                if (!ok) return "floor at " + dx + ", " + dz + " needs Tempered Arc Casing";
            }
        }
        for (int[] c : CORNERS) {
            BlockPos base = worldPosition.offset(c[0], 0, c[1]);
            if (!level.getBlockState(base).is(ModBlocks.REINFORCED_FULGURITE.get())
                    || !level.getBlockState(base.above()).is(ModBlocks.REINFORCED_FULGURITE.get())) {
                return "a corner pylon needs 2 Reinforced Fulgurite";
            }
            if (!level.getBlockState(base.above(2)).is(Blocks.LIGHTNING_ROD)) return "a corner pylon needs a Lightning Rod on top";
        }
        return null;
    }

    public boolean isFormed() {
        return formed;
    }

    public boolean isWorking() {
        return working;
    }

    public List<ItemStack> contents() {
        return contents;
    }

    public ItemStack result() {
        return result;
    }

    // ---------- Ticking ----------

    public static void serverTick(Level level, BlockPos pos, BlockState state, StormForgeBlockEntity be) {
        if (level.getGameTime() % 20 == 0) {
            String p = be.checkStructure();
            be.formed = p == null;
            be.problem = p;
        }
        if (be.recipeDirty) {
            be.recipe = be.matchRecipe(be.contents);
            be.recipeDirty = false;
            be.progress = 0;
        }
        boolean nowWorking = false;
        if (be.formed && be.recipe != null && be.result.isEmpty() && be.fm > 0) {
            int draw = Math.min(be.drawRate(), Math.min(be.fm, be.recipe.fmCost() - be.progress));
            be.fm -= draw;
            be.progress += draw;
            nowWorking = draw > 0;
            be.setChanged();
            if (be.progress >= be.recipe.fmCost()) be.finish((ServerLevel) level);
        }
        if (nowWorking) be.arcs((ServerLevel) level);
        if (nowWorking != be.working) {
            be.working = nowWorking;
            be.sync();
        }
    }

    // Arcs from the pylon tips into the work, and a buzz.
    private void arcs(ServerLevel level) {
        long t = level.getGameTime();
        Vec3 top = Vec3.atCenterOf(worldPosition).add(0, 0.6, 0);
        if (t % 3 == 0) {
            List<Vec3> tips = pylonTips();
            Vec3 tip = tips.get((int) ((t / 3) % tips.size()));
            int steps = 14;
            for (int i = 0; i <= steps; i++) {
                double f = i / (double) steps;
                Vec3 p = tip.lerp(top, f);
                double jitter = Math.sin(f * Math.PI) * 0.35;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x + (level.random.nextDouble() - 0.5) * jitter,
                        p.y + (level.random.nextDouble() - 0.5) * jitter, p.z + (level.random.nextDouble() - 0.5) * jitter,
                        1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, top.x, top.y, top.z, 6, 0.15, 0.1, 0.15, 0.2);
        }
        if (t % 30 == 0) level.playSound(null, worldPosition, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.0f, 1.8f);
        if (t % 9 == 0) level.playSound(null, worldPosition, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.5f, 1.2f + level.random.nextFloat() * 0.6f);
    }

    private void finish(ServerLevel level) {
        result = craft(recipe, contents);
        contents.clear();
        progress = 0;
        recipeDirty = true;
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.1, z = worldPosition.getZ() + 0.5;
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 60, 0.4, 0.3, 0.4, 0.5);
        level.playSound(null, worldPosition, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 1.2f, 1.4f);
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 0.8f);
        sync();
    }

    // ---------- Using the anvil ----------

    // Set one of the held item on the anvil. Returns a message if it can't.
    @Nullable
    public String insert(ItemStack held) {
        if (!result.isEmpty()) return "Take the finished item first.";
        if (working) return "The forge is working.";
        ItemStack one = held.copyWithCount(1);
        for (ItemStack s : contents) {
            if (ItemStack.isSameItemSameComponents(s, one) && s.getCount() < s.getMaxStackSize()) {
                s.grow(1);
                held.shrink(1);
                changed();
                return null;
            }
        }
        if (contents.size() >= MAX_STACKS) return "The anvil is full.";
        contents.add(one);
        held.shrink(1);
        changed();
        return null;
    }

    // Take the result, or the last item set down. Returns what was taken.
    public ItemStack takeBack() {
        if (!result.isEmpty()) {
            ItemStack r = result;
            result = ItemStack.EMPTY;
            changed();
            return r;
        }
        if (contents.isEmpty()) return ItemStack.EMPTY;
        ItemStack last = contents.get(contents.size() - 1);
        ItemStack one = last.split(1);
        if (last.isEmpty()) contents.remove(contents.size() - 1);
        changed();
        return one;
    }

    public void showStatus(Player player) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(title() + ": " + (formed ? "formed" : "not formed (" + problem + ")"))
                .withStyle(formed ? ChatFormatting.AQUA : ChatFormatting.RED));
        if (recipe != null) {
            lines.add(Component.literal(String.format("Forging %s: %,d / %,d Fm (%d%%)", recipe.name(), progress, recipe.fmCost(),
                    (int) (100L * progress / recipe.fmCost()))));
        } else if (!contents.isEmpty()) {
            lines.add(Component.literal("These items don't make anything.").withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.literal(String.format("Stored: %,d Fm", fm)).withStyle(ChatFormatting.GRAY));
        for (Component c : lines) player.sendSystemMessage(c);
    }

    public void dropAll() {
        for (ItemStack s : contents) Block.popResource(level, worldPosition.above(), s);
        if (!result.isEmpty()) Block.popResource(level, worldPosition.above(), result);
        contents.clear();
        result = ItemStack.EMPTY;
    }

    private void changed() {
        recipeDirty = true;
        progress = 0;
        setChanged();
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ---------- Power ----------

    @Override
    public int receiveFm(int amount, boolean simulate) {
        int accepted = Math.max(0, Math.min(amount, buffer() - fm));
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
        return buffer();
    }

    @Override
    public boolean acceptsConnector() {
        return false;
    }

    // ---------- Saving and syncing ----------

    private void writeItems(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack s : contents) list.add(s.save(registries));
        tag.put("Contents", list);
        if (!result.isEmpty()) tag.put("Result", result.save(registries));
        tag.putBoolean("Working", working);
    }

    private void readItems(CompoundTag tag, HolderLookup.Provider registries) {
        contents.clear();
        ListTag list = tag.getList("Contents", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) ItemStack.parse(registries, list.getCompound(i)).ifPresent(contents::add);
        result = tag.contains("Result") ? ItemStack.parse(registries, tag.getCompound("Result")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        working = tag.getBoolean("Working");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeItems(tag, registries);
        tag.putInt("Fm", fm);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readItems(tag, registries);
        fm = tag.getInt("Fm");
        progress = tag.getInt("Progress");
        recipeDirty = true;
        int keep = progress;
        // Keep progress across a reload: match now so the dirty check doesn't wipe it.
        recipe = matchRecipe(contents);
        recipeDirty = false;
        progress = recipe != null ? keep : 0;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeItems(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
