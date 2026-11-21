package com.sideflowgames.fulguritefusion.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.item.FusionFuelItem;
import com.sideflowgames.fulguritefusion.menu.ReactorConsoleMenu;
import com.sideflowgames.fulguritefusion.power.FmHandler;
import com.sideflowgames.fulguritefusion.power.FmSource;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// The Reactor Control Console's brain (in the control panel). Every second it checks the console around it
// and follows its two Data Cables out to the reactor's Ring Injection Ports.
//
// The console is 7 wide, 3 tall, 1 deep; the control panel is in the middle of the middle row. Seen from
// the front (columns -3..3, left to right; rows bottom to top):
//
//   top     frame   gauge   display  display  display  fuel     frame
//   middle  POWER   gauge   display  CONTROL  display  fuel     POWER
//   bottom  frame   FUEL 1  DATA 1   frame    DATA 2   FUEL 2   frame
//
// Every spot that isn't a port or the control panel is a Console Casing; forming turns each casing into its part.
// Data port 1 (left) carries fuel 1 to one Ring Injection Port, data port 2 (right) carries fuel 2 to the other.
// The console links the reactor (lets the rings form) while both cables are connected; once formed, the reactor
// stays formed on one cable (that injector just won't get its fuel).
public class ReactorConsoleBlockEntity extends BlockEntity implements MenuProvider {
    private static final int CHECK_EVERY = 20;

    // Layout rows, bottom to top; each is columns -3..3.
    // F frame, G/g gauge top/bottom, D display, S/s fuel screen top/bottom, P power port,
    // 1/2 fuel inputs, d data port, C the control panel itself.
    private static final String[] LAYOUT = {"F1dFd2F", "PgDCDsP", "FGDDDSF"};

    private boolean formed;
    private String problem = "not checked yet";
    private boolean cable1, cable2;
    private BlockPos colliderPos, port1, port2;

    // ---------- Running the reactor ----------
    // Menu data (big numbers split into two 16-bit halves).
    public static final int D_FM = 0, D_MAX = 2, D_STARTUP = 4, D_RUN = 6, D_STATE = 7, D_AUTO = 8, D_CABLES = 9,
            D_LAST = 10, DATA_COUNT = 12;
    public static final int S_READY = 0, S_RUNNING = 1, S_CONSOLE = 2, S_REACTOR = 3, S_FUEL = 4, S_POWER = 5, S_HEAT = 6;
    public static final int B_START = 0, B_STOP = 1, B_AUTO = 2;

    private long fm;
    private boolean auto, startRequested;
    private String lastResult = "No runs yet";
    private long lastYield;

    // Slot 0 = fuel 1, slot 1 = fuel 2: Fusion Fuel capsules or raw exotic materials.
    private final ItemStackHandler fuel = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isFuel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // Byproducts of the runs (twice what the Storm Collider gives). Taken out in the menu, or by a hopper or pipe
    // on a Fuel Input.
    private final ItemStackHandler byproducts = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public ItemStackHandler getByproducts() {
        return byproducts;
    }

    public void addByproduct(ItemStack stack) {
        ItemStack left = byproducts.insertItem(0, stack, false);
        if (!left.isEmpty() && level != null) {
            // Slot full of something else: drop the rest at the console.
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, left);
        }
    }

    // Void Collapse: the console's store, and anything giving Fm behind its power ports, is drained dry.
    public void voidCollapse() {
        fm = 0;
        for (BlockPos p : powerBacks()) ReactorReactions.drain(level, p);
        setChanged();
    }

    public static boolean isFuel(ItemStack stack) {
        if (stack.getItem() instanceof FusionFuelItem) return FusionFuelItem.result(stack) != null;
        return ModItems.materialIndex(stack) >= 0;
    }

    public ItemStackHandler getFuel() {
        return fuel;
    }

    public String lastResult() {
        return lastResult;
    }

    public ReactorConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.REACTOR_CONSOLE_BE.get(), pos, state);
    }

    public boolean isFormed() {
        return formed;
    }

    // For 3b: which Ring Injection Port each fuel goes to (null = that cable isn't connected).
    public BlockPos fuelPort(int fuel) {
        return fuel == 1 ? (cable1 ? port1 : null) : (cable2 ? port2 : null);
    }

    public BlockPos colliderPos() {
        return colliderPos;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReactorConsoleBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % CHECK_EVERY == 0) be.check();
        if (!be.formed) return;
        be.topUpStartup();
        be.pushPower();
        if ((be.startRequested || be.auto) && level.getGameTime() % 10 == 0) be.tryStart();
    }

    private FusionColliderBlockEntity reactor() {
        return colliderPos != null && level.getBlockEntity(colliderPos) instanceof FusionColliderBlockEntity r ? r : null;
    }

    private BlockPos[] powerBacks() {
        Direction back = front().getOpposite();
        return new BlockPos[]{spot(-3, 1).relative(back), spot(3, 1).relative(back)};
    }

    // Keep the startup charge topped up from anything that gives Fm (battery, generator) behind a power port.
    private void topUpStartup() {
        int startup = Config.REACTOR_STARTUP_FM.get();
        for (BlockPos p : powerBacks()) {
            if (fm >= startup) return;
            if (level.getBlockEntity(p) instanceof FmSource src) {
                int got = src.extractFm((int) (startup - fm), false);
                if (got > 0) {
                    fm += got;
                    setChanged();
                }
            }
        }
    }

    // The rear power ports push the store out into whatever is behind them (battery, machine).
    private void pushPower() {
        if (fm <= 0) return;
        int rate = Config.REACTOR_OUTPUT_RATE.get();
        for (BlockPos p : powerBacks()) {
            if (fm <= 0) break;
            if (level.getBlockEntity(p) instanceof FmHandler h) {
                int sent = h.receiveFm((int) Math.min(fm, rate), false);
                if (sent > 0) {
                    fm -= sent;
                    setChanged();
                }
            }
        }
    }

    public int state() {
        if (!formed) return S_CONSOLE;
        FusionColliderBlockEntity r = reactor();
        if (r != null && r.isRunning()) return S_RUNNING;
        if (r == null || !r.isFormed()) return S_REACTOR;
        if (r.isHot()) return S_HEAT;
        if (fuel.getStackInSlot(0).isEmpty() || fuel.getStackInSlot(1).isEmpty()) return S_FUEL;
        if (fm < Config.REACTOR_STARTUP_FM.get()) return S_POWER;
        return S_READY;
    }

    private void tryStart() {
        FusionColliderBlockEntity r = reactor();
        if (r == null || !r.isFormed() || r.isRunning()) return;
        if (fuel.getStackInSlot(0).isEmpty() || fuel.getStackInSlot(1).isEmpty()) {
            startRequested = false;
            return;
        }
        int startup = Config.REACTOR_STARTUP_FM.get();
        topUpStartup();
        if (fm < startup) return; // keep waiting: the charge tops up every tick
        // Each fuel goes down its own data cable to its Ring Injection Port; a cut cable's fuel stays here.
        ItemStack[] out = {ItemStack.EMPTY, ItemStack.EMPTY};
        BlockPos[] ports = {cable1 ? port1 : null, cable2 ? port2 : null};
        for (int f = 0; f < 2; f++) {
            int i = r.portIndex(ports[f]);
            if (i < 0 || !out[i].isEmpty()) continue;
            out[i] = fuel.getStackInSlot(f).copyWithCount(1);
        }
        if (out[0].isEmpty() && out[1].isEmpty()) {
            startRequested = false;
            return;
        }
        if (!r.beginRun(worldPosition, out[0], out[1])) return;
        for (int f = 0; f < 2; f++) {
            int i = r.portIndex(ports[f]);
            if (i >= 0) fuel.extractItem(f, 1, false);
        }
        fm -= startup;
        startRequested = false;
        setChanged();
    }

    // Called by the Fusion Collider when a run ends.
    public void deliver(String result, long amount) {
        lastResult = result;
        lastYield = amount;
        fm = Math.min(Config.REACTOR_MAX_FM.get(), fm + amount);
        if (amount <= 0) auto = false; // a lost or dead run stops auto-repeat
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void pressButton(int id) {
        switch (id) {
            case B_START -> startRequested = true;
            case B_STOP -> {
                startRequested = false;
                auto = false;
            }
            case B_AUTO -> auto = !auto;
            default -> {}
        }
        setChanged();
    }

    private static int lo(long v) {
        return (int) (Math.min(v, Integer.MAX_VALUE) & 0xFFFF);
    }

    private static int hi(long v) {
        return (int) ((Math.min(v, Integer.MAX_VALUE) >>> 16) & 0xFFFF);
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            int max = Config.REACTOR_MAX_FM.get(), startup = Config.REACTOR_STARTUP_FM.get();
            FusionColliderBlockEntity r = level != null ? reactor() : null;
            return switch (i) {
                case D_FM -> lo(fm);
                case D_FM + 1 -> hi(fm);
                case D_MAX -> lo(max);
                case D_MAX + 1 -> hi(max);
                case D_STARTUP -> lo(startup);
                case D_STARTUP + 1 -> hi(startup);
                case D_RUN -> r != null && r.isRunning() ? r.runTick(0) : -1;
                case D_STATE -> state();
                case D_AUTO -> auto ? 1 : 0;
                case D_CABLES -> (cable1 ? 1 : 0) | (cable2 ? 2 : 0);
                case D_LAST -> lo(lastYield);
                case D_LAST + 1 -> hi(lastYield);
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.fulguritefusion.reactor_console");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReactorConsoleMenu(id, inventory, this, data);
    }

    public void openMenu(ServerPlayer player) {
        player.openMenu(this, buf -> buf.writeBlockPos(worldPosition));
    }

    public void dropContents() {
        for (int i = 0; i < fuel.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    fuel.getStackInSlot(i));
            fuel.setStackInSlot(i, ItemStack.EMPTY);
        }
        Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                byproducts.getStackInSlot(0));
        byproducts.setStackInSlot(0, ItemStack.EMPTY);
    }

    // Hoppers and pipes: each Console Fuel Input feeds its fuel slot (front or back), insert only.
    public static IItemHandler fuelInputHandler(Level level, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos c = pos.relative(d, 2).above();
            if (level.getBlockEntity(c) instanceof ReactorConsoleBlockEntity console && console.formed) {
                if (console.spot(-2, 0).equals(pos)) return new InputPort(console.fuel, 0, console.byproducts);
                if (console.spot(2, 0).equals(pos)) return new InputPort(console.fuel, 1, console.byproducts);
            }
        }
        return null;
    }

    // A Fuel Input as hoppers see it: slot 0 = its fuel slot (insert only), slot 1 = the byproduct slot (take only).
    private record InputPort(ItemStackHandler fuel, int slot, ItemStackHandler byproducts) implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int s) {
            return s == 0 ? fuel.getStackInSlot(slot) : byproducts.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int s, ItemStack stack, boolean simulate) {
            return s == 0 ? fuel.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int s, int amount, boolean simulate) {
            return s == 1 ? byproducts.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int s) {
            return 64;
        }

        @Override
        public boolean isItemValid(int s, ItemStack stack) {
            return s == 0 && fuel.isItemValid(slot, stack);
        }
    }

    // ---------- Layout ----------

    private Direction front() {
        BlockState s = getBlockState();
        return s.hasProperty(ReactorConsoleBlock.FACING) ? s.getValue(ReactorConsoleBlock.FACING) : Direction.NORTH;
    }

    // Column -3..3 (left to right as you face the front), row 0..2 (bottom to top).
    private BlockPos spot(int col, int row) {
        Direction right = front().getOpposite().getClockWise();
        return worldPosition.relative(right, col).above(row - 1);
    }

    private static ConsolePart partFor(char c) {
        return switch (c) {
            case 'F' -> ConsolePart.FRAME;
            case 'G' -> ConsolePart.GAUGE_TOP;
            case 'g' -> ConsolePart.GAUGE_BOTTOM;
            case 'D' -> ConsolePart.DISPLAY;
            case 'S' -> ConsolePart.FUEL_SCREEN_TOP;
            case 's' -> ConsolePart.FUEL_SCREEN_BOTTOM;
            default -> null;
        };
    }

    private static Block portFor(char c) {
        return switch (c) {
            case 'P' -> ModBlocks.CONSOLE_POWER_PORT.get();
            case '1', '2' -> ModBlocks.CONSOLE_FUEL_INPUT.get();
            case 'd' -> ModBlocks.CONSOLE_DATA_PORT.get();
            default -> null;
        };
    }

    private static String nameFor(char c) {
        return switch (c) {
            case 'P' -> "a Console Power Port";
            case '1' -> "a Console Fuel Input (fuel 1)";
            case '2' -> "a Console Fuel Input (fuel 2)";
            case 'd' -> "a Console Data Port";
            default -> "a Console Casing";
        };
    }

    private String where(BlockPos p) {
        return p.getX() + " " + p.getY() + " " + p.getZ();
    }

    // Null when every block is in place, else what's missing.
    private String validateLayout() {
        for (int row = 0; row < 3; row++) {
            for (int col = -3; col <= 3; col++) {
                char c = LAYOUT[row].charAt(col + 3);
                if (c == 'C') continue;
                BlockPos p = spot(col, row);
                BlockState s = level.getBlockState(p);
                Block want = partFor(c) != null ? ModBlocks.CONSOLE_CASING.get() : portFor(c);
                if (!s.is(want)) return "needs " + nameFor(c) + " at " + where(p);
            }
        }
        return null;
    }

    // Turn every casing into its part (or back to plain) and every port to face the console's way.
    private void setConsoleFormed(boolean value) {
        Direction f = front();
        for (int row = 0; row < 3; row++) {
            for (int col = -3; col <= 3; col++) {
                char c = LAYOUT[row].charAt(col + 3);
                if (c == 'C') continue;
                BlockPos p = spot(col, row);
                BlockState s = level.getBlockState(p);
                BlockState want = s;
                if (s.is(ModBlocks.CONSOLE_CASING.get())) {
                    ConsolePart part = value ? partFor(c) : ConsolePart.NONE;
                    if (part == null) part = ConsolePart.NONE;
                    want = s.setValue(ConsoleCasingBlock.PART, part);
                    if (value) want = want.setValue(ConsoleCasingBlock.FACING, f);
                } else if (s.getBlock() instanceof ConsolePortBlock) {
                    want = s.setValue(FormableBlock.FORMED, value);
                    if (value) want = want.setValue(ConsolePortBlock.FACING, f);
                }
                if (want != s) {
                    level.setBlock(p, want, Block.UPDATE_ALL);
                    level.invalidateCapabilities(p);
                }
            }
        }
    }

    // ---------- Data cables ----------

    private record Trace(Set<BlockPos> ports, boolean tooLong, boolean any) {}

    // Follow the cable touching a data port; collect the Ring Injection Ports it reaches.
    private Trace trace(BlockPos dataPort) {
        Set<BlockPos> seen = new HashSet<>(), ports = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Block cable = ModBlocks.DATA_CABLE.get(), port = ModBlocks.RING_INJECTION_PORT.get();
        for (Direction d : Direction.values()) {
            BlockPos n = dataPort.relative(d);
            if (level.getBlockState(n).is(cable) && seen.add(n)) queue.add(n);
        }
        boolean any = !queue.isEmpty();
        int max = Config.DATA_CABLE_MAX_LENGTH.get();
        while (!queue.isEmpty()) {
            if (seen.size() > max) return new Trace(ports, true, true);
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos q = p.relative(d);
                if (!level.isLoaded(q)) continue;
                BlockState s = level.getBlockState(q);
                if (s.is(cable)) {
                    if (seen.add(q)) queue.add(q);
                } else if (s.is(port)) {
                    ports.add(q.immutable());
                }
            }
        }
        return new Trace(ports, false, any);
    }

    private static String cableProblem(int n, String side, Trace t) {
        if (!t.any()) return "run a Data Cable from data port " + n + " (" + side + ", bottom row) to a Ring Injection Port";
        if (t.tooLong()) return "data cable " + n + " is too long (over " + Config.DATA_CABLE_MAX_LENGTH.get() + " blocks)";
        return "data cable " + n + " (" + side + ") doesn't reach a Ring Injection Port";
    }

    // ---------- Check ----------

    public void check() {
        if (level == null || level.isClientSide) return;
        String layout = validateLayout();
        if (layout != null) {
            problem = layout;
            cable1 = cable2 = false;
            if (formed) {
                formed = false;
                setConsoleFormed(false);
            }
            setChanged();
            return;
        }
        if (!formed) formed = true;
        setConsoleFormed(true); // also catches a block swapped in while formed

        Trace t1 = trace(spot(-1, 0)), t2 = trace(spot(1, 0));
        cable1 = !t1.tooLong() && t1.ports().size() == 1;
        cable2 = !t2.tooLong() && t2.ports().size() == 1;
        port1 = cable1 ? t1.ports().iterator().next() : null;
        port2 = cable2 ? t2.ports().iterator().next() : null;
        problem = "";
        if (t1.ports().size() > 1 || t2.ports().size() > 1) {
            problem = "the two data cables touch: keep each fuel's cable separate";
            cable1 = cable2 = false;
        } else if (cable1 && cable2 && port1.equals(port2)) {
            problem = "both data cables go to the same Ring Injection Port: run one to each";
            cable2 = false;
        }

        // Find the reactor from the port(s) and link it.
        colliderPos = null;
        BlockPos c1 = cable1 ? FusionColliderBlockEntity.colliderForPort(level, port1) : null;
        BlockPos c2 = cable2 ? FusionColliderBlockEntity.colliderForPort(level, port2) : null;
        if (c1 != null && c2 != null && !c1.equals(c2)) {
            problem = "the data cables go to two different reactors";
            c2 = null;
            cable2 = false;
        }
        colliderPos = c1 != null ? c1 : c2;
        if (colliderPos != null && level.getBlockEntity(colliderPos) instanceof FusionColliderBlockEntity reactor) {
            reactor.link(level.getGameTime(), cable1 && c1 != null && cable2 && c2 != null);
        } else if (problem.isEmpty()) {
            if (!cable1) problem = cableProblem(1, "left", t1);
            else if (!cable2) problem = cableProblem(2, "right", t2);
            else problem = "the Ring Injection Ports aren't on a Fusion Reactor's outer ring corner";
        }
        if (problem.isEmpty() && !cable1) problem = cableProblem(1, "left", t1);
        if (problem.isEmpty() && !cable2) problem = cableProblem(2, "right", t2);
        setChanged();
    }

    public void unform() {
        if (formed) {
            formed = false;
            setConsoleFormed(false);
        }
    }

    public String statusText() {
        if (!formed) return "Reactor Console: " + problem;
        String cables = "cable 1 " + (cable1 ? "connected" : "cut") + ", cable 2 " + (cable2 ? "connected" : "cut");
        String reactor = "";
        if (colliderPos != null && level.getBlockEntity(colliderPos) instanceof FusionColliderBlockEntity r) {
            reactor = " | " + r.statusText();
        }
        return "Reactor Console: " + cables + (problem.isEmpty() ? "" : " (" + problem + ")") + reactor;
    }

    // Right-clicking any console block shows the console's status.
    public static InteractionResult showStatusFrom(Level level, BlockPos pos, Player player) {
        ReactorConsoleBlockEntity console = findNear(level, pos);
        if (console == null) return InteractionResult.PASS;
        console.interact(player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Click: the menu once the console is formed (sneak-click, or not formed yet: the status line).
    public void interact(Player player) {
        if (level.isClientSide) return;
        check();
        if (formed && !player.isShiftKeyDown() && player instanceof ServerPlayer sp) openMenu(sp);
        else player.displayClientMessage(Component.literal(statusText()), true);
    }

    private static ReactorConsoleBlockEntity findNear(Level level, BlockPos pos) {
        ReactorConsoleBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-6, -2, -6), pos.offset(6, 2, 6))) {
            if (level.getBlockEntity(p) instanceof ReactorConsoleBlockEntity c) {
                double d = p.distSqr(pos);
                if (d < bestDist) {
                    bestDist = d;
                    best = c;
                }
            }
        }
        return best;
    }

    // ---------- Save ----------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
        tag.putBoolean("Cable1", cable1);
        tag.putBoolean("Cable2", cable2);
        if (port1 != null) tag.put("Port1", NbtUtils.writeBlockPos(port1));
        if (port2 != null) tag.put("Port2", NbtUtils.writeBlockPos(port2));
        if (colliderPos != null) tag.put("Collider", NbtUtils.writeBlockPos(colliderPos));
        tag.putLong("Fm", fm);
        tag.putBoolean("Auto", auto);
        tag.putString("LastResult", lastResult);
        tag.putLong("LastYield", lastYield);
        tag.put("Fuel", fuel.serializeNBT(registries));
        tag.put("Byproducts", byproducts.serializeNBT(registries));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("LastResult", lastResult);
        tag.putLong("LastYield", lastYield);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        cable1 = tag.getBoolean("Cable1");
        cable2 = tag.getBoolean("Cable2");
        port1 = NbtUtils.readBlockPos(tag, "Port1").orElse(null);
        port2 = NbtUtils.readBlockPos(tag, "Port2").orElse(null);
        colliderPos = NbtUtils.readBlockPos(tag, "Collider").orElse(null);
        fm = tag.getLong("Fm");
        auto = tag.getBoolean("Auto");
        if (tag.contains("LastResult")) lastResult = tag.getString("LastResult");
        lastYield = tag.getLong("LastYield");
        if (tag.contains("Fuel")) fuel.deserializeNBT(registries, tag.getCompound("Fuel"));
        if (tag.contains("Byproducts")) byproducts.deserializeNBT(registries, tag.getCompound("Byproducts"));
    }
}
