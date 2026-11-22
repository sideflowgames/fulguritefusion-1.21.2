package com.sideflowgames.fulguritefusion.harvester;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModFluids;
import com.sideflowgames.fulguritefusion.block.ArcCasingBlock;
import com.sideflowgames.fulguritefusion.block.ArcCasingBlockEntity;
import com.sideflowgames.fulguritefusion.machine.MachineShape;
import com.sideflowgames.fulguritefusion.power.DaisyChain;
import com.sideflowgames.fulguritefusion.power.FmHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

// Latex Harvester: a 3x3x2 ring around the bottom two blocks of a rubber tree's trunk. It taps the tree and
// fills its tank with latex.
//   - Front row, top layer: power port, controller (facing out), tank; the tank and port can swap corners.
//     Under the tank is a second tank (the tank column is 2 tall). The other 12 blocks are Arc Casings.
//   - Speed: 1.5 buckets a minute with Crude casings, x2 / x4 / x8 for Tempered / Charged / Prime (the lowest casing).
//     Uses 32 Fm/t for each x of speed while working.
//   - Tank: 128 buckets. When it's full the harvester stops until some is drained (pipes or a bucket on the tank).
//   - Each tree holds a random 1,000 to 4,000 buckets, rolled when the harvester first sees that tree. When it runs
//     dry the trunk turns into Dried Rubber Logs. Chop it down and plant a rubber sapling in the middle: while the
//     harvester is built and powered, every second it has a 1 in 4 chance to give the sapling an extra growth tick.
//     The new tree gets a new random amount.
public class LatexHarvesterBlockEntity extends BlockEntity implements FmHandler, GeoBlockEntity {
    public static final int CAPACITY = 128_000;          // mB (128 buckets)
    public static final int BUCKET = 1000;
    public static final int TICKS_PER_BUCKET = 800;      // at x1 speed: 1.5 buckets a minute
    public static final int FM_PER_TICK = 32;            // per x of speed
    public static final int MAX_FM = 100_000;
    public static final int MIN_TREE = 1000, MAX_TREE = 4000;
    public static final float GROW_CHANCE = 0.25f;
    public static final int GROW_FM = 500;
    private static final DustParticleOptions OUTLINE = new DustParticleOptions(new Vector3f(0.4f, 0.85f, 1.0f), 0.8f);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final DaisyChain chain = new DaisyChain();
    private final FluidTank tank = new FluidTank(CAPACITY, s -> s.is(ModFluids.LATEX.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncFill();
        }
    };

    private int tier;          // 0 = not built, 1-4 = casing tier
    private int fm;
    private int progress;
    private int treeTotal;     // 0 = no tree rolled yet
    private int treeLeft;
    private boolean checkRequested = true;
    private int checkTimer;
    private int previewTicks;
    private final List<BlockPos> missing = new ArrayList<>();
    private boolean tankCw = true; // the tank is on the controller's clockwise side
    private int sentFill = -1;
    private int shownTier;     // client copies
    private boolean shownCw = true;
    private int shownFill;     // 0-64

    public LatexHarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.LATEX_HARVESTER_BE.get(), pos, state);
    }

    public FluidTank tank() {
        return tank;
    }

    public boolean isFormed() {
        return tier > 0;
    }

    public void requestCheck() {
        checkRequested = true;
    }

    public int speed() {
        return 1 << Math.max(0, tier - 1);
    }

    public int shownTier() {
        return Math.max(1, shownTier);
    }

    public boolean shownTankClockwise() {
        return shownCw;
    }

    // How full the tank is, 0-1, for the latex in the window.
    public float shownFill() {
        return shownFill / 64f;
    }

    private Direction facing() {
        return getBlockState().getValue(LatexHarvesterBlock.FACING);
    }

    // The trunk beside the controller (the top layer): right behind it.
    public BlockPos center() {
        return worldPosition.relative(facing().getOpposite());
    }

    // The bottom of the trunk, where a sapling is replanted.
    public BlockPos base() {
        return center().below();
    }

    // ---------- Layout ----------

    private static List<BlockPos> ring(BlockPos mid) {
        List<BlockPos> list = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (dx != 0 || dz != 0) list.add(mid.offset(dx, 0, dz));
        return list;
    }

    // All 15 blocks around the trunk except the controller.
    private List<BlockPos> allSpots() {
        List<BlockPos> list = ring(center());
        list.addAll(ring(base()));
        list.remove(worldPosition);
        return list;
    }

    private BlockPos cornerA() {
        return worldPosition.relative(facing().getClockWise());
    }

    private BlockPos cornerB() {
        return worldPosition.relative(facing().getCounterClockWise());
    }

    private BlockPos tankTop() {
        return tankCw ? cornerA() : cornerB();
    }

    private BlockPos portTop() {
        return tankCw ? cornerB() : cornerA();
    }

    private List<BlockPos> tankSpots() {
        return List.of(tankTop(), tankTop().below());
    }

    private List<BlockPos> casingSpots() {
        List<BlockPos> list = allSpots();
        list.remove(portTop());
        list.removeAll(tankSpots());
        return list;
    }

    // ---------- Forming ----------

    private void checkStructure() {
        if (level == null) return;
        missing.clear();
        // Which corner holds the tank: go by what's been placed (keeps the last answer if it can't tell).
        BlockState a = level.getBlockState(cornerA()), b = level.getBlockState(cornerB());
        boolean aTank = a.is(ModBlocks.HARVESTER_TANK.get()), bTank = b.is(ModBlocks.HARVESTER_TANK.get());
        boolean aPort = a.is(ModBlocks.HARVESTER_POWER_PORT.get()), bPort = b.is(ModBlocks.HARVESTER_POWER_PORT.get());
        if (tier == 0) {
            if (aTank != bTank) tankCw = aTank;
            else if (aPort != bPort) tankCw = bPort;
        }
        int lowest = 4;
        for (BlockPos p : casingSpots()) {
            if (level.getBlockState(p).getBlock() instanceof ArcCasingBlock casing && level.getBlockEntity(p) instanceof ArcCasingBlockEntity be
                    && (be.controllerPos() == null || be.belongsTo(worldPosition))) {
                lowest = Math.min(lowest, casing.tier());
            } else {
                missing.add(p);
            }
        }
        // The tank column and the power port take the two front corners, either way round.
        for (BlockPos p : tankSpots()) {
            if (!level.getBlockState(p).is(ModBlocks.HARVESTER_TANK.get()) || !partFree(p)) missing.add(p);
        }
        if (!level.getBlockState(portTop()).is(ModBlocks.HARVESTER_POWER_PORT.get()) || !partFree(portTop())) missing.add(portTop());
        if (missing.isEmpty()) {
            if (tier != lowest) form(lowest);
        } else if (tier > 0) {
            unform();
        }
    }

    private boolean partFree(BlockPos p) {
        return level.getBlockEntity(p) instanceof HarvesterPartBlockEntity part && part.free(worldPosition);
    }

    private void form(int newTier) {
        boolean was = tier > 0;
        tier = newTier;
        for (BlockPos p : casingSpots()) {
            if (level.getBlockEntity(p) instanceof ArcCasingBlockEntity be) be.link(worldPosition, MachineShape.Role.NONE);
            BlockState s = level.getBlockState(p);
            if (s.hasProperty(ArcCasingBlock.FORMED) && !s.getValue(ArcCasingBlock.FORMED)) level.setBlock(p, s.setValue(ArcCasingBlock.FORMED, true), 3);
        }
        List<BlockPos> parts = new ArrayList<>(tankSpots());
        parts.add(portTop());
        for (BlockPos p : parts) {
            if (level.getBlockEntity(p) instanceof HarvesterPartBlockEntity part) part.link(worldPosition);
            BlockState s = level.getBlockState(p);
            if (s.hasProperty(HarvesterPart.FORMED) && !s.getValue(HarvesterPart.FORMED)) level.setBlock(p, s.setValue(HarvesterPart.FORMED, true), 3);
        }
        level.setBlock(worldPosition, getBlockState().setValue(LatexHarvesterBlock.FORMED, true), 3);
        sentFill = -1;
        syncFill();
        if (!was && level instanceof ServerLevel server) {
            server.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.4f);
            BlockPos c = center();
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 60, 1.2, 0.8, 1.2, 0.2);
        }
        sync();
        setChanged();
    }

    private void unform() {
        tier = 0;
        progress = 0;
        release();
        BlockState own = level.getBlockState(worldPosition);
        if (own.getBlock() instanceof LatexHarvesterBlock) {
            level.setBlock(worldPosition, own.setValue(LatexHarvesterBlock.FORMED, false).setValue(LatexHarvesterBlock.WORKING, false), 3);
        }
        sync();
        setChanged();
    }

    private void release() {
        for (BlockPos p : allSpots()) {
            BlockState s = level.getBlockState(p);
            if (level.getBlockEntity(p) instanceof ArcCasingBlockEntity be && be.belongsTo(worldPosition)) {
                be.unlink();
                if (s.hasProperty(ArcCasingBlock.FORMED)) level.setBlock(p, s.setValue(ArcCasingBlock.FORMED, false), 3);
            } else if (level.getBlockEntity(p) instanceof HarvesterPartBlockEntity part && worldPosition.equals(part.controllerPos())) {
                part.link(null);
                if (s.hasProperty(HarvesterPart.FORMED)) level.setBlock(p, s.setValue(HarvesterPart.FORMED, false), 3);
            }
        }
    }

    // The controller was broken: free the other blocks (the latex in the tank is lost).
    public void breakApart() {
        if (level == null || level.isClientSide) return;
        release();
    }

    private int fillNow() {
        return tank.isEmpty() ? 0 : Math.max(1, (int) Math.round(tank.getFluidAmount() * 64.0 / CAPACITY));
    }

    // The tank's window shows how full it is: tell clients when it changes by a 64th.
    private void syncFill() {
        if (level == null || level.isClientSide || tier == 0) return;
        int f = fillNow();
        if (f != sentFill) {
            sentFill = f;
            sync();
        }
    }

    // ---------- The tree ----------

    private boolean liveTrunk() {
        return level.getBlockState(base()).is(ModBlocks.RUBBER_LOG.get());
    }

    // Turn the whole trunk (the column of rubber logs from the base up) into Dried Rubber Logs.
    private void dryTree(ServerLevel server) {
        BlockPos p = base();
        while (server.getBlockState(p).is(ModBlocks.RUBBER_LOG.get())) {
            BlockState log = server.getBlockState(p);
            BlockState dried = ModBlocks.DRIED_RUBBER_LOG.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, log.getValue(RotatedPillarBlock.AXIS));
            server.setBlock(p, dried, Block.UPDATE_ALL);
            server.sendParticles(ParticleTypes.WHITE_ASH, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.02);
            p = p.above();
        }
        server.playSound(null, base(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8f, 0.6f);
        treeTotal = 0;
        treeLeft = 0;
    }

    // ---------- Ticking ----------

    public static void serverTick(Level level, BlockPos pos, BlockState state, LatexHarvesterBlockEntity be) {
        ServerLevel server = (ServerLevel) level;
        if (be.checkRequested || ++be.checkTimer >= 40) {
            be.checkRequested = false;
            be.checkTimer = 0;
            be.checkStructure();
        }
        if (be.previewTicks > 0) {
            be.previewTicks--;
            if (be.previewTicks % 10 == 0 && !be.missing.isEmpty()) be.drawPreview(server);
        }
        boolean working = false;
        if (be.tier > 0) {
            BlockPos c = be.base();
            BlockState centerState = level.getBlockState(c);
            if (be.liveTrunk()) {
                // A tree it hasn't seen yet: how much latex it holds is up to chance.
                if (be.treeTotal == 0) {
                    be.treeTotal = MIN_TREE + level.random.nextInt(MAX_TREE - MIN_TREE + 1);
                    be.treeLeft = be.treeTotal;
                    be.setChanged();
                }
                int speed = be.speed(), cost = FM_PER_TICK * speed;
                if (be.treeLeft > 0 && be.tank.getSpace() >= BUCKET && be.fm >= cost) {
                    working = true;
                    be.fm -= cost;
                    be.progress += speed;
                    if (be.progress >= TICKS_PER_BUCKET) {
                        be.progress = 0;
                        be.tank.fill(new FluidStack(ModFluids.LATEX.get(), BUCKET), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                        be.treeLeft--;
                        if (be.treeLeft <= 0) be.dryTree(server);
                    }
                    be.setChanged();
                }
            } else if (centerState.getBlock() instanceof SaplingBlock sapling && centerState.is(ModBlocks.RUBBER_SAPLING.get())) {
                // Help a replanted sapling along.
                if (level.getGameTime() % 20 == 0 && be.fm >= GROW_FM && level.random.nextFloat() < GROW_CHANCE) {
                    be.fm -= GROW_FM;
                    sapling.advanceTree(server, c, centerState, level.random);
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.getX() + 0.5, c.getY() + 0.4, c.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
                    be.setChanged();
                }
            }
        }
        if (state.getValue(LatexHarvesterBlock.WORKING) != working && state.getValue(LatexHarvesterBlock.FORMED) == (be.tier > 0)) {
            level.setBlock(pos, state.setValue(LatexHarvesterBlock.WORKING, working), 3);
        }
    }

    // ---------- Status ----------

    public void interact(Player player) {
        checkStructure();
        if (tier == 0) {
            previewTicks = 200;
            player.displayClientMessage(Component.literal(missing.size() + " part" + (missing.size() == 1 ? "" : "s")
                    + " missing (outlined). A 3x3x2 ring around the bottom of the trunk. Front row, top layer: the controller in the "
                    + "middle, a Harvester Tank in one corner (with another Harvester Tank under it) and a Harvester Power Port in "
                    + "the other. The rest are Arc Casings of one tier").withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        String tree;
        BlockState c = level.getBlockState(base());
        if (liveTrunk()) tree = String.format("%,d / %,d buckets left in this tree", treeLeft, treeTotal);
        else if (c.is(ModBlocks.RUBBER_SAPLING.get())) tree = "growing a sapling";
        else if (c.is(ModBlocks.DRIED_RUBBER_LOG.get())) tree = "tree is dry: chop it and plant a rubber sapling";
        else tree = "no rubber tree in the middle";
        String state = tank.getSpace() < BUCKET ? " | TANK FULL" : fm < FM_PER_TICK * speed() ? " | needs power" : "";
        player.displayClientMessage(Component.literal(String.format("Latex: %d / %d buckets | %s | %,d Fm | %dx speed%s",
                tank.getFluidAmount() / BUCKET, CAPACITY / BUCKET, tree, fm, speed(), state)).withStyle(ChatFormatting.AQUA), false);
    }

    private void drawPreview(ServerLevel server) {
        for (BlockPos p : missing) {
            for (int e = 0; e < 12; e++) {
                for (int k = 0; k <= 3; k++) {
                    double t = k / 3.0, x, y, z;
                    int a = e % 4, axis = e / 4;
                    double c1 = (a & 1), c2 = (a >> 1);
                    if (axis == 0) { x = t; y = c1; z = c2; }
                    else if (axis == 1) { x = c1; y = t; z = c2; }
                    else { x = c1; y = c2; z = t; }
                    server.sendParticles(OUTLINE, p.getX() + x, p.getY() + y, p.getZ() + z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    // ---------- Power ----------

    @Override
    public boolean acceptsConnector() {
        return false; // connectors go on the power port
    }

    @Override
    public int receiveFm(int amount, boolean simulate) {
        chain.touch(level, 0);
        return accept(amount, simulate);
    }

    @Override
    public int receiveFmFromChain(int amount, int depth) {
        chain.touch(level, depth);
        return accept(amount, false);
    }

    @Override
    public void touchChain(int depth) {
        chain.touch(level, depth);
    }

    private int accept(int amount, boolean simulate) {
        if (tier == 0) return 0;
        int accepted = Math.max(0, Math.min(amount, MAX_FM - fm));
        if (!simulate && accepted > 0) {
            fm += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override public int getFm() { return fm; }
    @Override public int getMaxFm() { return MAX_FM; }

    // ---------- GeckoLib ----------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ---------- Client sync (the tier, for the trim colour) ----------

    private void sync() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Tier", tier);
        tag.putBoolean("TankCw", tankCw);
        tag.putInt("Fill", tier > 0 ? fillNow() : 0);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        shownTier = tag.getInt("Tier");
        shownCw = !tag.contains("TankCw") || tag.getBoolean("TankCw");
        shownFill = tag.getInt("Fill");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    // ---------- Save ----------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Tier", tier);
        tag.putInt("Fm", fm);
        tag.putInt("Progress", progress);
        tag.putInt("TreeTotal", treeTotal);
        tag.putInt("TreeLeft", treeLeft);
        tag.putBoolean("TankCw", tankCw);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = tag.getInt("Tier");
        shownTier = tier;
        fm = tag.getInt("Fm");
        progress = tag.getInt("Progress");
        treeTotal = tag.getInt("TreeTotal");
        treeLeft = tag.getInt("TreeLeft");
        tankCw = !tag.contains("TankCw") || tag.getBoolean("TankCw");
        shownCw = tankCw;
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }
}
