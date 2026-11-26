package com.sideflowgames.fulguritefusion.transport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;

// An Item Pipe. Each side that meets an inventory is Insert, Extract or Off. Twice a second, every Extract side pulls
// up to (items a tick x 10) from its inventory and hands the items out among the Insert sides along all the pipes
// joined to it, taking turns so they share. Pipes hold nothing themselves, so nothing is lost when one breaks.
// On Fulgurite and Stormforged pipes, each Insert / Extract side can have a filter (9 items, allow or block list).
public class ItemPipeBlockEntity extends BlockEntity {
    public static final int IN = 0, OUT = 1, OFF = 2;
    public static final int INTERVAL = 10;
    public static final int MAX_PIPES = 4096;
    public static final int FILTER_SLOTS = 9;

    private final byte[] modes = new byte[6];
    private final boolean[] allow = new boolean[6];
    private final ItemStack[][] filters = new ItemStack[6][FILTER_SLOTS];
    private final int[] turn = new int[6];

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(TransportBlocks.ITEM_PIPE_BE.get(), pos, state);
        for (ItemStack[] f : filters) java.util.Arrays.fill(f, ItemStack.EMPTY);
    }

    public int tier() {
        return getBlockState().getBlock() instanceof ItemPipeBlock p ? p.tier() : 1;
    }

    public int mode(Direction d) {
        return modes[d.ordinal()];
    }

    public void setMode(Direction d, int mode) {
        modes[d.ordinal()] = (byte) mode;
        setChanged();
    }

    // ---------- filters ----------

    public boolean allowList(Direction d) {
        return allow[d.ordinal()];
    }

    public void setAllowList(Direction d, boolean value) {
        allow[d.ordinal()] = value;
        setChanged();
    }

    public ItemStack filter(Direction d, int i) {
        return filters[d.ordinal()][i];
    }

    public void setFilter(Direction d, int i, ItemStack stack) {
        filters[d.ordinal()][i] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    public boolean passes(Direction d, ItemStack stack) {
        if (tier() < 2) return true;
        boolean listed = false;
        for (ItemStack f : filters[d.ordinal()]) {
            if (!f.isEmpty() && ItemStack.isSameItem(f, stack)) {
                listed = true;
                break;
            }
        }
        return allow[d.ordinal()] == listed;
    }

    public MenuProvider filterMenu(Direction d) {
        return new SimpleMenuProvider((id, inv, player) -> new PipeFilterMenu(id, inv, this, d),
                Component.literal("Pipe Filter (" + d.getSerializedName() + ")"));
    }

    public void writeFilterMenu(RegistryFriendlyByteBuf buf, Direction d) {
        buf.writeBlockPos(worldPosition);
        buf.writeVarInt(d.ordinal());
    }

    // ---------- moving items ----------

    private record Dest(ItemPipeBlockEntity pipe, Direction dir, IItemHandler handler, List<BlockPos> path) {}

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % INTERVAL != 0) return;
        for (Direction d : Direction.values()) {
            if (state.getValue(ItemPipeBlock.SIDES.get(d)) == PipeSide.OUT) be.extract((ServerLevel) level, d);
        }
    }

    private void extract(ServerLevel level, Direction d) {
        BlockPos sourcePos = worldPosition.relative(d);
        IItemHandler source = com.sideflowgames.fulguritefusion.util.LegacyCaps.itemsAt(level, sourcePos, d.getOpposite());
        if (source == null) return;
        int budget = ItemPipeBlock.rate(tier()) * INTERVAL;
        // Anything to send at all?
        boolean any = false;
        for (int s = 0; s < source.getSlots() && !any; s++) {
            ItemStack peek = source.extractItem(s, budget, true);
            any = !peek.isEmpty() && passes(d, peek);
        }
        if (!any) return;
        List<Dest> dests = walk(level, sourcePos, d);
        if (dests.isEmpty()) return;
        int start = Math.floorMod(turn[d.ordinal()]++, dests.size());
        Map<Dest, ItemStack> shown = new HashMap<>();
        for (int s = 0; s < source.getSlots() && budget > 0; s++) {
            for (int k = 0; k < dests.size() && budget > 0; k++) {
                ItemStack peek = source.extractItem(s, budget, true);
                if (peek.isEmpty() || !passes(d, peek)) break;
                Dest dest = dests.get((start + k) % dests.size());
                if (!dest.pipe().passes(dest.dir(), peek)) continue;
                ItemStack rest = ItemHandlerHelper.insertItem(dest.handler(), peek, true);
                int fits = peek.getCount() - rest.getCount();
                if (fits <= 0) continue;
                ItemStack taken = source.extractItem(s, fits, false);
                if (taken.isEmpty()) break;
                ItemStack left = ItemHandlerHelper.insertItem(dest.handler(), taken, false);
                if (!left.isEmpty()) source.insertItem(s, left, false);
                int moved = taken.getCount() - left.getCount();
                budget -= moved;
                if (moved > 0) shown.putIfAbsent(dest, taken.copyWithCount(1));
            }
        }
        // Show a few of the deliveries sliding along the glass.
        int sent = 0;
        for (Map.Entry<Dest, ItemStack> e : shown.entrySet()) {
            if (sent++ >= 4) break;
            Dest dest = e.getKey();
            PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(worldPosition),
                    new PipeTravel.Payload(dest.path(), d.ordinal(), dest.dir().ordinal(), e.getValue(), tier()));
        }
    }

    // Every pipe joined to this one (any tier), and every Insert side along them, nearest first.
    private List<Dest> walk(Level level, BlockPos sourcePos, Direction fromSide) {
        List<Dest> out = new ArrayList<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        parent.put(worldPosition, worldPosition);
        queue.add(worldPosition);
        while (!queue.isEmpty() && parent.size() < MAX_PIPES) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof ItemPipeBlock) || !(level.getBlockEntity(p) instanceof ItemPipeBlockEntity pipe)) continue;
            for (Direction d : Direction.values()) {
                PipeSide side = s.getValue(ItemPipeBlock.SIDES.get(d));
                BlockPos n = p.relative(d);
                if (side == PipeSide.PIPE) {
                    if (!parent.containsKey(n)) {
                        parent.put(n, p);
                        queue.add(n);
                    }
                } else if (side == PipeSide.IN && !n.equals(sourcePos)) {
                    IItemHandler h = com.sideflowgames.fulguritefusion.util.LegacyCaps.itemsAt(level, n, d.getOpposite());
                    if (h != null) out.add(new Dest(pipe, d, h, path(parent, p)));
                }
            }
        }
        return out;
    }

    private List<BlockPos> path(Map<BlockPos, BlockPos> parent, BlockPos end) {
        ArrayDeque<BlockPos> stack = new ArrayDeque<>();
        BlockPos p = end;
        while (true) {
            stack.push(p);
            BlockPos up = parent.get(p);
            if (up == null || up.equals(p)) break;
            p = up;
        }
        return new ArrayList<>(stack);
    }

    // ---------- save ----------

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveItemPipeBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveItemPipeBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        tag.putByteArray("Modes", modes.clone());
        ListTag sides = new ListTag();
        for (int i = 0; i < 6; i++) {
            CompoundTag side = new CompoundTag();
            side.putBoolean("Allow", allow[i]);
            ListTag items = new ListTag();
            for (int j = 0; j < FILTER_SLOTS; j++) {
                if (!filters[i][j].isEmpty()) {
                    CompoundTag it = new CompoundTag();
                    it.putInt("Slot", j);
                    it.put("Item", com.sideflowgames.fulguritefusion.util.NbtBridge.saveStack(filters[i][j], registries));
                    items.add(it);
                }
            }
            side.put("Filter", items);
            sides.add(side);
        }
        tag.put("Sides", sides);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadItemPipeBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadItemPipeBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        byte[] m = tag.getByteArray("Modes").orElse(new byte[0]);
        for (int i = 0; i < 6 && i < m.length; i++) modes[i] = m[i];
        ListTag sides = tag.getListOrEmpty("Sides");
        for (int i = 0; i < 6 && i < sides.size(); i++) {
            CompoundTag side = sides.getCompoundOrEmpty(i);
            allow[i] = side.getBooleanOr("Allow", false);
            java.util.Arrays.fill(filters[i], ItemStack.EMPTY);
            ListTag items = side.getListOrEmpty("Filter");
            for (int k = 0; k < items.size(); k++) {
                CompoundTag it = items.getCompoundOrEmpty(k);
                int j = it.getIntOr("Slot", 0);
                if (j >= 0 && j < FILTER_SLOTS) filters[i][j] = com.sideflowgames.fulguritefusion.util.NbtBridge.parseStack(registries, it.getCompoundOrEmpty("Item"));
            }
        }
    }
}
