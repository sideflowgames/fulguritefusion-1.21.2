package com.sideflowgames.fulguritefusion.block;

import java.util.ArrayList;
import java.util.List;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.ModSounds;
import com.sideflowgames.fulguritefusion.item.FusionFuelItem;
import com.sideflowgames.fulguritefusion.pairing.Pairings;
import com.sideflowgames.fulguritefusion.pairing.Pairings.Pairing;
import com.sideflowgames.fulguritefusion.pairing.Pairings.Tier;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// The Fusion Reactor's brain. Every 2 seconds it checks the blast chamber and inner ring around it;
// when both are right the ring forms (its blocks hide and the smooth ring is drawn), otherwise it un-forms.
//
// Positions are relative to the Fusion Collider (dx, dy, dz). Works either way round: the two Fusion Injectors
// decide the axis (east/west or north/south), the two Receiver Portals pick their pair of corners.
//
// Blast chamber (|d| <= 2): edges = Blast Chamber Frame, face middles = Fulgurite Blast Glass,
// except the two face centres facing the injectors, which must be open (air).
//
// Inner ring: a diamond tube around the square channel line at 9 blocks out. For a block, L = how far it is
// out from the channel line (max(|dx|, |dz|) - 9) and dy = its height; then:
//   tips (L = +-2, dy = 0 / L = 0, dy = +-2)      Fulgurite Blast Glass
//   diagonals (L = +-1, dy = +-1)                 Reinforced Fulgurite
//   the channel (L, dy within the plus shape)      air
// with two Fusion Injectors at the inner tips on the injector axis (facing the chamber) and two Receiver Portals
// at outer tips on opposite corners (where a straight channel side ends).
//
// Outer ring: the same diamond tube round the channel line at 44 blocks out (L = max(|dx|, |dz|) - 44), with
//   - two Transfer Portals on its inside face, each straight in line with a Receiver Portal;
//   - two Ring Injection Ports on its outside face at one corner, each in line with the other side's channel
//     (at (46, 44) and (44, 46) for the south-east corner), so their two beams leave that corner going
//     opposite ways round the ring.
//
// The rings only form while the Reactor Control Console is linked through its Data Cables: forming needs both
// cables; once formed one is enough.
public class FusionColliderBlockEntity extends BlockEntity {
    public static final int CHAMBER = 2, RING = 9, TUBE = 2, INJECTOR = RING - TUBE; // 7
    public static final int OUTER = 44, PORTAL_IN = OUTER - TUBE, PORT_OUT = OUTER + TUBE; // 44, 42, 46
    private static final int CHECK_EVERY = 40;
    private static final int LINK_TIMEOUT = 100;
    // The outer ring's corners {sx, sz}.
    public static final int[][] CORNERS = {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}};

    private boolean formed;
    private boolean axisX = true;        // injectors east/west (true) or north/south
    private int receiverPair = 0;        // which pair of corners holds the Receiver Portals (RECEIVER_PAIRS index)
    private String problem = "not checked yet";
    private boolean chamberFormed;       // the blast chamber on its own (shown as the armoured sphere)
    private boolean chamberAxisX = true; // which pair of chamber faces is open (the beams' way in)
    private String chamberProblem = "not checked yet";
    private int injCorner = -1;          // which outer corner holds the Ring Injection Ports (CORNERS index)
    private boolean structureOk;
    private long linkedAny = -100000, linkedBoth = -100000; // game time the console last reached us

    // The run in progress (see ReactorPath for the timeline).
    private long runStart = -1;          // game time it started; -1 = no run
    private long clientRunStart = -1;    // client only: the same moment on the client's own clock
    private int runMask;                 // bit i: particle i was fired; bit 2 + i: particle i was lost at the portal jump
    private int runSeverity = -1;        // at impact: -1 nothing hit, 0 clean, 1 unstable, 2 bad, 3 catastrophic (flash colour)
    private final ItemStack[] carried = {ItemStack.EMPTY, ItemStack.EMPTY};
    private long heatUntil = -1;         // game time the reactor has cooled down (Twin Suns, Flare, Torn Sky)
    private long staticBloomUntil = -1;  // Overcharge: the area stays charged until then
    private BlockPos consolePos;

    public FusionColliderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FUSION_COLLIDER_BE.get(), pos, state);
    }

    public boolean isFormed() {
        return formed;
    }

    public boolean axisX() {
        return axisX;
    }

    public boolean isChamberFormed() {
        return chamberFormed;
    }

    public boolean chamberAxisX() {
        return chamberAxisX;
    }

    // The two Receiver Portal spots {dx, dz}, for drawing their fittings.
    public int[][] receiverSpots() {
        return RECEIVER_PAIRS[Math.floorMod(receiverPair, RECEIVER_PAIRS.length)];
    }

    // The two Ring Injection Port spots {dx, dz} at a corner.
    public static int[][] injectionSpots(int corner) {
        int[] c = CORNERS[corner];
        return new int[][]{{c[0] * PORT_OUT, c[1] * OUTER}, {c[0] * OUTER, c[1] * PORT_OUT}};
    }

    public int injectionCorner() {
        return injCorner;
    }

    // The Transfer Portal spot {dx, dz} straight out from a Receiver Portal.
    public static int[] transferFor(int[] r) {
        return Math.abs(r[1]) > Math.abs(r[0]) ? new int[]{r[0], Integer.signum(r[1]) * PORTAL_IN}
                : new int[]{Integer.signum(r[0]) * PORTAL_IN, r[1]};
    }

    // From a Ring Injection Port, the Fusion Collider whose outer ring it sits on (or null).
    public static BlockPos colliderForPort(Level level, BlockPos port) {
        for (int c = 0; c < CORNERS.length; c++) {
            for (int[] s : injectionSpots(c)) {
                BlockPos cand = port.offset(-s[0], 0, -s[1]);
                if (level.isLoaded(cand) && level.getBlockState(cand).is(ModBlocks.FUSION_COLLIDER.get())) return cand;
            }
        }
        return null;
    }

    // Called by the Reactor Control Console every second while it reaches us.
    public void link(long time, boolean bothCables) {
        linkedAny = time;
        if (bothCables) linkedBoth = time;
        if (!formed && structureOk && bothCables) check();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FusionColliderBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % CHECK_EVERY == 0) be.check();
        if (be.runStart >= 0) be.tickRun((ServerLevel) level);
        if (be.staticBloomUntil > 0) be.tickStaticBloom((ServerLevel) level);
    }

    // ---------- Runs ----------

    public boolean isRunning() {
        return runStart >= 0;
    }

    // ---------- Heat and Static Bloom (set by some pairings' reactions) ----------

    public boolean isHot() {
        return level != null && level.getGameTime() < heatUntil;
    }

    public int coolSeconds() {
        return level == null ? 0 : (int) Math.max(0, (heatUntil - level.getGameTime()) / 20);
    }

    public void overheat(int ticks) {
        heatUntil = Math.max(heatUntil, level.getGameTime() + ReactorPath.T_END - ReactorPath.T_IMPACT + ticks);
        setChanged();
    }

    public void coolDown() {
        heatUntil = -1;
        setChanged();
    }

    public void staticBloom(int ticks) {
        staticBloomUntil = level.getGameTime() + ticks;
        setChanged();
    }

    // Static Bloom: anyone within 24 blocks touching metal gets zapped.
    private void tickStaticBloom(ServerLevel level) {
        if (level.getGameTime() >= staticBloomUntil || level.getGameTime() % 40 != 0) return;
        for (net.minecraft.world.entity.player.Player p : level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                new AABB(worldPosition).inflate(24))) {
            BlockPos feet = p.blockPosition();
            boolean metal = false;
            for (BlockPos q : new BlockPos[]{feet.below(), feet.north(), feet.south(), feet.east(), feet.west(), feet.above().north(),
                    feet.above().south(), feet.above().east(), feet.above().west()}) {
                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(q).getBlock()).getPath();
                if (id.contains("iron") || id.contains("gold") || id.contains("copper") || id.contains("netherite")
                        || id.contains("chain") || id.contains("rail") || id.contains("anvil") || id.contains("lightning_rod")) {
                    metal = true;
                    break;
                }
            }
            if (metal) {
                p.hurtOrSimulate(level.damageSources().lightningBolt(), 2f);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 0.5, p.getZ(), 12, 0.3, 0.5, 0.3, 0.1);
            }
        }
        if (level.getRandom().nextInt(4) == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 3, worldPosition.getZ() + 0.5,
                    30, 12, 2, 12, 0.05);
        }
    }

    public ReactorConsoleBlockEntity consoleEntity() {
        return console();
    }

    // Ticks into the current run (-1 = none). Works on the client too (for the renderer).
    public int runTick(float partialTick) {
        return runStart < 0 || level == null ? -1 : (int) (level.getGameTime() - runStart);
    }

    public long runStart() {
        return runStart;
    }

    // The run's start on the client's clock (client and server game times can drift a few ticks apart,
    // which at full speed would put the trace well away from the portals when the beam fires).
    public long renderStart() {
        return clientRunStart >= 0 ? clientRunStart : runStart;
    }

    public int runMask() {
        return runMask;
    }

    public int runSeverity() {
        return runSeverity;
    }

    public int[][] receivers() {
        return receiverSpots();
    }

    // Which particle (0 / 1) a Ring Injection Port fires, or -1 if it isn't one of ours.
    public int portIndex(BlockPos port) {
        if (injCorner < 0 || port == null) return -1;
        int[][] spots = injectionSpots(injCorner);
        for (int i = 0; i < 2; i++) if (at(spots[i][0], 0, spots[i][1]).equals(port)) return i;
        return -1;
    }

    // Start a run: fuel[i] goes to particle i (empty = that port gets nothing, e.g. its cable is cut).
    public boolean beginRun(BlockPos console, ItemStack fuel0, ItemStack fuel1) {
        if (!(level instanceof ServerLevel server) || !formed || runStart >= 0 || injCorner < 0 || isHot()) return false;
        if (fuel0.isEmpty() && fuel1.isEmpty()) return false;
        consolePos = console;
        carried[0] = fuel0.copy();
        carried[1] = fuel1.copy();
        runMask = (fuel0.isEmpty() ? 0 : 1) | (fuel1.isEmpty() ? 0 : 2);
        runSeverity = -1;
        runStart = server.getGameTime();
        server.playSound(null, worldPosition, ModSounds.REACTOR_HUM.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        setChanged();
        sync();
        return true;
    }

    private void sync() {
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private ReactorConsoleBlockEntity console() {
        return consolePos != null && level.getBlockEntity(consolePos) instanceof ReactorConsoleBlockEntity c ? c : null;
    }

    private void tickRun(ServerLevel level) {
        int t = (int) (level.getGameTime() - runStart);
        if (!formed && t < ReactorPath.T_IMPACT) {
            finish("run lost: the reactor came apart mid-run", 0);
            return;
        }
        if (t == ReactorPath.T_JUMP) portalJump(level);
        if (t == ReactorPath.T_INNER && (live(0) || live(1))) {
            level.playSound(null, worldPosition, ModSounds.REACTOR_INNER.get(), SoundSource.BLOCKS, 1.5f, 1.0f);
        }
        if (t == ReactorPath.T_IMPACT) impact(level);
        if (t >= ReactorPath.T_END) {
            runStart = -1;
            runMask = 0;
            setChanged();
            sync();
        }
    }

    private boolean live(int i) {
        return (runMask & (1 << i)) != 0 && (runMask & (4 << i)) == 0;
    }

    // Each charge leaves its Transfer Portal as a beam; anything in the way between the rings and it's lost.
    private void portalJump(ServerLevel level) {
        int[][] receivers = receiverSpots();
        for (int i = 0; i < 2; i++) {
            if (!live(i)) continue;
            int[] t = transferFor(receivers[i]), r = receivers[i];
            int sx = Integer.signum(r[0] - t[0]), sz = Integer.signum(r[1] - t[1]);
            int steps = Math.abs(r[0] - t[0]) + Math.abs(r[1] - t[1]);
            BlockPos portal = at(t[0], 0, t[1]);
            level.playSound(null, portal, ModSounds.REACTOR_BEAM.get(), SoundSource.BLOCKS, 1.5f, 1.0f);
            for (int k = 1; k < steps; k++) {
                BlockPos p = at(t[0] + sx * k, 0, t[1] + sz * k);
                if (!level.getBlockState(p).isAir()) {
                    runMask |= 4 << i;
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 25, 0.4, 0.4, 0.4, 0.02);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 30, 0.5, 0.5, 0.5, 0.2);
                    level.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.5f, 0.6f);
                    break;
                }
            }
        }
        setChanged();
        sync();
    }

    // What a fuel item is: a Fusion Fuel capsule (its result) or a raw exotic material.
    private record Fuel(Pairing capsule, int raw) {
        static Fuel of(ItemStack stack) {
            if (stack.getItem() instanceof FusionFuelItem) return new Fuel(FusionFuelItem.result(stack), -1);
            return new Fuel(null, ModItems.materialIndex(stack));
        }

        String name() {
            return capsule != null ? capsule.name() : raw >= 0 ? Pairings.MATERIAL_NAMES[raw] : "?";
        }
    }

    private static int severity(Tier tier) {
        return switch (tier) {
            case CATASTROPHIC -> 3;
            case BAD -> 2;
            case UNSTABLE -> 1;
            default -> 0;
        };
    }

    private void impact(ServerLevel level) {
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
        if (!live(0) || !live(1)) {
            // One charge (or none) arrives: nothing to hit, it fizzles in the chamber.
            if (live(0) || live(1)) {
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 30, 0.5, 0.5, 0.5, 0.02);
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.5f, 0.8f);
            }
            String why = (runMask & 12) != 0 ? "something blocked a portal beam" : "only one fuel was fired (is a data cable cut?)";
            finish("run lost: " + why + ", nothing to collide with", 0);
            return;
        }

        // Fusion! Work out the result(s) burned, the yield and any byproducts.
        Fuel a = Fuel.of(carried[0]), b = Fuel.of(carried[1]);
        List<Pairing> results = new ArrayList<>();
        List<ItemStack> byproducts = new ArrayList<>();
        double yield;
        String text;
        if (a.capsule() != null && b.capsule() != null) {
            yield = (a.capsule().fuelFm() + (double) b.capsule().fuelFm()) * 2;
            results.add(a.capsule());
            results.add(b.capsule());
            text = a.name() + " + " + b.name();
        } else if (a.capsule() == null && b.capsule() == null && a.raw() >= 0 && b.raw() >= 0) {
            Pairing pairing = Pairings.of(a.raw(), b.raw());
            Pairing result = StormColliderBlockEntity.resolve(pairing, level.getRandom());
            yield = result.fuelFm();
            results.add(result);
            byproducts.add(StormColliderBlockEntity.byproduct(pairing, result, level.getRandom()));
            text = result.name();
        } else {
            Fuel cap = a.capsule() != null ? a : b, raw = a.capsule() != null ? b : a;
            if (cap.capsule() == null || raw.raw() < 0) {
                finish("run lost: unknown fuel", 0);
                return;
            }
            yield = cap.capsule().fuelFm() * 1.5
                    + Pairings.ENERGY[raw.raw()] * Pairings.FM_PER_ENERGY * cap.capsule().tier().fuelMultiplier;
            results.add(cap.capsule());
            text = cap.name() + " + " + raw.name();
        }
        for (Pairing r : results) {
            if (results.size() > 1 || byproducts.isEmpty()) byproducts.add(StormColliderBlockEntity.byproduct(r, r, level.getRandom()));
        }
        long fm = Math.round(yield * Config.REACTOR_FM_MULTIPLIER.get());
        int worst = 0;
        for (Pairing r : results) worst = Math.max(worst, severity(r.tier()));
        runSeverity = fm <= 0 ? -1 : worst;

        // The flash.
        if (results.stream().noneMatch(r -> r.id() == 46)) { // Silence: no sound at all
            level.playSound(null, worldPosition, ModSounds.REACTOR_IMPACT.get(), SoundSource.BLOCKS, 2.0f, 1.0f);
        }
        level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, -1), x, y, z, 2, 0, 0, 0, 0);

        // Each result's tier effect, then its own reaction; byproducts doubled into the console.
        for (Pairing r : results) {
            sideEffect(level, r.tier(), x, y, z);
            ReactorReactions.react(level, this, r);
        }
        ReactorConsoleBlockEntity console = console();
        for (ItemStack bp : byproducts) {
            if (bp.isEmpty()) continue;
            ItemStack twice = bp.copyWithCount(bp.getCount() * 2);
            if (console != null) console.addByproduct(twice);
            else Block.popResource(level, worldPosition.above(3), twice);
        }

        // Everyone watching learns what these fuels do.
        java.util.Set<net.minecraft.server.level.ServerPlayer> watchers = new java.util.HashSet<>();
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            if (p.blockPosition().closerThan(worldPosition, 64)
                    || (consolePos != null && p.blockPosition().closerThan(consolePos, 32))) watchers.add(p);
        }
        for (net.minecraft.server.level.ServerPlayer p : watchers) {
            for (Pairing r : results) com.sideflowgames.fulguritefusion.pairing.PairingDiscoveries.discover(p, r.id());
        }

        StringBuilder tierText = new StringBuilder(" (");
        for (int i = 0; i < results.size(); i++) tierText.append(i > 0 ? " / " : "").append(results.get(i).tier().display());
        tierText.append(")");
        finish(text + tierText + (fm > 0 ? ": " + String.format("%,d", fm) + " Fm" : ": no reaction"), fm);
    }

    private void finish(String result, long fm) {
        carried[0] = ItemStack.EMPTY;
        carried[1] = ItemStack.EMPTY;
        ReactorConsoleBlockEntity console = console();
        if (console != null) console.deliver(result, fm);
        if (runStart >= 0 && level.getGameTime() - runStart < ReactorPath.T_IMPACT) {
            runStart = -1; // lost before impact: end at once
            runMask = 0;
        }
        setChanged();
        sync();
    }

    // The general effect of each tier, reactor-sized (each pairing's own reaction comes on top: ReactorReactions).
    private void sideEffect(ServerLevel level, Tier tier, double x, double y, double z) {
        switch (tier) {
            case UNSTABLE -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 1, z, 200, 6, 2, 6, 0.3);
                shock(level, 10, 3f);
            }
            case BAD -> {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1, z, 120, 5, 2, 5, 0.03);
                shock(level, 12, 5f);
            }
            case CATASTROPHIC -> {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
                shock(level, 16, 8f);
            }
            case JACKPOT -> level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 3, z, 150, 4, 2, 4, 0.4);
            default -> {}
        }
    }

    private void shock(ServerLevel level, double range, float damage) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(range))) {
            e.hurtOrSimulate(level.damageSources().lightningBolt(), damage);
        }
    }

    // Ring Melt (Solar Flare, Meltdown): a stretch of the inner ring's tube melts back into Raw Fulgurite.
    public void meltRing(ServerLevel level, int count) {
        Block glass = ModBlocks.FULGURITE_BLAST_GLASS.get(), solid = ModBlocks.REINFORCED_FULGURITE.get();
        // Pick a spot on the inner channel line and melt the tube blocks round it.
        float[] at = ReactorPath.point(RING, level.getRandom().nextFloat() * ReactorPath.perimeter(RING));
        int cx = Math.round(at[0]), cz = Math.round(at[1]);
        int melted = 0;
        for (int r = 0; r <= 4 && melted < count; r++) {
            for (int dx = -r; dx <= r && melted < count; dx++) {
                for (int dz = -r; dz <= r && melted < count; dz++) {
                    for (int dy = -TUBE; dy <= TUBE && melted < count; dy++) {
                        BlockPos p = at(cx + dx, dy, cz + dz);
                        BlockState s = level.getBlockState(p);
                        if (!s.is(glass) && !s.is(solid)) continue;
                        level.setBlock(p, ModBlocks.RAW_FULGURITE.get().defaultBlockState(), Block.UPDATE_ALL);
                        level.sendParticles(ParticleTypes.LAVA, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
                        melted++;
                    }
                }
            }
        }
        if (melted > 0) {
            level.playSound(null, at((int) at[0], 0, (int) at[1]), SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 3f, 0.6f);
            check();
        }
    }

    // A Catastrophic result blasts a handful of the rings' and chamber's blocks out (half of them drop as items).
    // The reactor un-forms until they're replaced.
    public void blastRingBlocks(ServerLevel level, int count) {
        Block glass = ModBlocks.FULGURITE_BLAST_GLASS.get(), solid = ModBlocks.REINFORCED_FULGURITE.get(),
                frame = ModBlocks.BLAST_CHAMBER_FRAME.get();
        int removed = 0;
        for (int tries = 0; tries < 600 && removed < count; tries++) {
            int which = level.getRandom().nextInt(3), dx, dy, dz;
            if (which == 2) {
                dx = level.getRandom().nextInt(5) - 2;
                dy = level.getRandom().nextInt(5) - 2;
                dz = level.getRandom().nextInt(5) - 2;
            } else {
                int reach = (which == 0 ? RING : OUTER) + TUBE;
                dx = level.getRandom().nextInt(2 * reach + 1) - reach;
                dz = level.getRandom().nextInt(2 * reach + 1) - reach;
                dy = level.getRandom().nextInt(5) - 2;
            }
            BlockPos p = at(dx, dy, dz);
            BlockState s = level.getBlockState(p);
            if (!s.is(glass) && !s.is(solid) && !s.is(frame)) continue;
            level.destroyBlock(p, level.getRandom().nextBoolean());
            level.sendParticles(ParticleTypes.EXPLOSION, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0);
            removed++;
        }
        if (removed > 0) check();
    }

    public String statusText() {
        if (formed) return "Fusion Reactor: formed and linked to its control console";
        return "Fusion Reactor: " + (chamberFormed && !problem.startsWith("blast chamber") ? "blast chamber formed | " : "") + problem;
    }

    // ---------- Structure ----------

    private BlockPos at(int dx, int dy, int dz) {
        return worldPosition.offset(dx, dy, dz);
    }

    private boolean is(int dx, int dy, int dz, Block block) {
        return level.getBlockState(at(dx, dy, dz)).is(block);
    }

    private String where(int dx, int dy, int dz) {
        BlockPos p = at(dx, dy, dz);
        return p.getX() + " " + p.getY() + " " + p.getZ();
    }

    // Receiver pairs: opposite outer-tip spots where a straight side of the channel ends.
    public static final int[][][] RECEIVER_PAIRS = {
            {{-RING, -RING - TUBE}, {RING, RING + TUBE}},
            {{RING, -RING - TUBE}, {-RING, RING + TUBE}},
            {{RING + TUBE, -RING}, {-RING - TUBE, RING}},
            {{RING + TUBE, RING}, {-RING - TUBE, -RING}},
    };

    // Returns null when everything is right, or what's wrong.
    private String validate() {
        if (chamberProblem != null) return "blast chamber: " + chamberProblem;
        axisX = chamberAxisX;
        Block injector = ModBlocks.FUSION_INJECTOR.get();
        if (!(axisX ? is(INJECTOR, 0, 0, injector) && is(-INJECTOR, 0, 0, injector)
                : is(0, 0, INJECTOR, injector) && is(0, 0, -INJECTOR, injector))) {
            return "place two Fusion Injectors on the inner ring's inside face, " + INJECTOR + " blocks out from the collider, "
                    + "in line with the chamber's open faces (" + (axisX ? "east and west" : "north and south") + ")";
        }

        // Injectors must face the chamber.
        if (!injectorFacesIn(axisX ? INJECTOR : 0, axisX ? 0 : INJECTOR) || !injectorFacesIn(axisX ? -INJECTOR : 0, axisX ? 0 : -INJECTOR)) {
            return "turn the Fusion Injectors so their fronts face the chamber";
        }

        // Receivers.
        int[][] receivers = null;
        Block receiver = ModBlocks.RECEIVER_PORTAL.get();
        for (int i = 0; i < RECEIVER_PAIRS.length; i++) {
            int[][] pair = RECEIVER_PAIRS[i];
            if (is(pair[0][0], 0, pair[0][1], receiver) && is(pair[1][0], 0, pair[1][1], receiver)) {
                receivers = pair;
                receiverPair = i;
                break;
            }
        }
        if (receivers == null) return "place two Receiver Portals on the inner ring's outside face, at opposite corners";

        // The inner ring.
        int[][] innerSpecials = {receivers[0], receivers[1],
                axisX ? new int[]{INJECTOR, 0} : new int[]{0, INJECTOR}, axisX ? new int[]{-INJECTOR, 0} : new int[]{0, -INJECTOR}};
        String inner = checkRing(RING, "inner ring", innerSpecials);
        if (inner != null) return inner;

        // Outer ring: Transfer Portals in line with the receivers, Ring Injection Ports at one corner.
        int[][] portals = {transferFor(receivers[0]), transferFor(receivers[1])};
        Block portal = ModBlocks.TRANSFER_PORTAL.get();
        for (int[] p : portals) {
            if (!is(p[0], 0, p[1], portal)) return "place a Transfer Portal on the outer ring's inside face at "
                    + where(p[0], 0, p[1]) + " (straight in line with a Receiver Portal)";
        }
        Block port = ModBlocks.RING_INJECTION_PORT.get();
        injCorner = -1;
        for (int c = 0; c < CORNERS.length; c++) {
            int[][] spots = injectionSpots(c);
            if (is(spots[0][0], 0, spots[0][1], port) && is(spots[1][0], 0, spots[1][1], port)) {
                injCorner = c;
                break;
            }
        }
        if (injCorner < 0) return "place two Ring Injection Ports on the outer ring's outside face at one corner, "
                + "each in line with the channel of the other side";
        int[][] spots = injectionSpots(injCorner);
        int[][] outerSpecials = {portals[0], portals[1], spots[0], spots[1]};
        return checkRing(OUTER, "outer ring", outerSpecials);
    }

    // The blast chamber (5 x 5 x 5 round the collider): edges = Blast Chamber Frame, face middles = Fulgurite Blast
    // Glass, except the centres of two opposite side faces, which are open (the beams' way in). Null when right.
    private String checkChamber() {
        boolean openX = level.getBlockState(at(CHAMBER, 0, 0)).isAir() && level.getBlockState(at(-CHAMBER, 0, 0)).isAir();
        boolean openZ = level.getBlockState(at(0, 0, CHAMBER)).isAir() && level.getBlockState(at(0, 0, -CHAMBER)).isAir();
        if (openX == openZ) {
            return openX ? "only two opposite side faces may have their middle open"
                    : "leave the middle of two opposite side faces open (east and west, or north and south)";
        }
        chamberAxisX = openX;
        Block frame = ModBlocks.BLAST_CHAMBER_FRAME.get(), glass = ModBlocks.FULGURITE_BLAST_GLASS.get();
        for (int dx = -CHAMBER; dx <= CHAMBER; dx++) {
            for (int dy = -CHAMBER; dy <= CHAMBER; dy++) {
                for (int dz = -CHAMBER; dz <= CHAMBER; dz++) {
                    int outer = (Math.abs(dx) == CHAMBER ? 1 : 0) + (Math.abs(dy) == CHAMBER ? 1 : 0) + (Math.abs(dz) == CHAMBER ? 1 : 0);
                    if (outer == 0) continue;
                    if (outer >= 2) {
                        if (!is(dx, dy, dz, frame)) return "needs a Blast Chamber Frame at " + where(dx, dy, dz);
                    } else {
                        boolean opening = dy == 0 && (openX ? (dz == 0 && Math.abs(dx) == CHAMBER) : (dx == 0 && Math.abs(dz) == CHAMBER));
                        if (!opening && !is(dx, dy, dz, glass)) return "needs Fulgurite Blast Glass at " + where(dx, dy, dz);
                    }
                }
            }
        }
        return null;
    }

    private void setChamberFormed(boolean value) {
        BlockState self = level.getBlockState(worldPosition);
        if (self.hasProperty(FormableBlock.FORMED) && self.getValue(FormableBlock.FORMED) != value) {
            level.setBlock(worldPosition, self.setValue(FormableBlock.FORMED, value), Block.UPDATE_CLIENTS);
        }
        for (int dx = -CHAMBER; dx <= CHAMBER; dx++) {
            for (int dy = -CHAMBER; dy <= CHAMBER; dy++) {
                for (int dz = -CHAMBER; dz <= CHAMBER; dz++) {
                    if (Math.abs(dx) != CHAMBER && Math.abs(dy) != CHAMBER && Math.abs(dz) != CHAMBER) continue;
                    BlockPos p = at(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.hasProperty(FormableBlock.FORMED) && s.getValue(FormableBlock.FORMED) != value) {
                        level.setBlock(p, s.setValue(FormableBlock.FORMED, value), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }
    }

    // Checks one diamond ring tube round the channel square 'radius' blocks out. Specials ({dx, dz} at dy = 0)
    // are skipped (checked separately).
    private String checkRing(int radius, String name, int[][] specials) {
        Block glass = ModBlocks.FULGURITE_BLAST_GLASS.get(), solid = ModBlocks.REINFORCED_FULGURITE.get();
        int reach = radius + TUBE;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                int l = Math.max(Math.abs(dx), Math.abs(dz)) - radius;
                if (Math.abs(l) > TUBE) continue;
                for (int dy = -TUBE; dy <= TUBE; dy++) {
                    int al = Math.abs(l), ay = Math.abs(dy);
                    if (dy == 0 && isSpecial(dx, dz, specials)) continue;
                    if ((al == TUBE && dy == 0) || (l == 0 && ay == TUBE)) {
                        if (!is(dx, dy, dz, glass)) return name + " needs Fulgurite Blast Glass at " + where(dx, dy, dz);
                    } else if (al == 1 && ay == 1) {
                        if (!is(dx, dy, dz, solid)) return name + " needs Reinforced Fulgurite at " + where(dx, dy, dz);
                    } else if (al + ay <= 1) {
                        if (!level.getBlockState(at(dx, dy, dz)).isAir()) return name + "'s channel must be clear at " + where(dx, dy, dz);
                    }
                }
            }
        }
        return null;
    }

    private boolean injectorFacesIn(int dx, int dz) {
        BlockState s = level.getBlockState(at(dx, 0, dz));
        return s.hasProperty(ReactorFacingBlock.FACING)
                && s.getValue(ReactorFacingBlock.FACING).getStepX() == -Integer.signum(dx)
                && s.getValue(ReactorFacingBlock.FACING).getStepZ() == -Integer.signum(dz);
    }

    private static boolean isSpecial(int dx, int dz, int[][] specials) {
        for (int[] p : specials) if (p[0] == dx && p[1] == dz) return true;
        return false;
    }

    public void check() {
        if (level == null || level.isClientSide()) return;
        // The blast chamber forms on its own, as soon as it's right.
        chamberProblem = checkChamber();
        boolean chamberNow = chamberProblem == null;
        if (chamberNow) setChamberFormed(true);
        if (chamberNow != chamberFormed) {
            chamberFormed = chamberNow;
            if (!chamberNow) setChamberFormed(false);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        // The outer ring is big: wait until all four of its corners are loaded.
        for (int[] c : CORNERS) {
            if (!level.isLoaded(at(c[0] * PORT_OUT, 0, c[1] * PORT_OUT))) return;
        }
        String result = validate();
        structureOk = result == null;
        if (structureOk) {
            long now = level.getGameTime();
            boolean linked = formed ? now - linkedAny <= LINK_TIMEOUT : now - linkedBoth <= LINK_TIMEOUT;
            if (!linked) {
                result = formed ? "the control console lost both data cables"
                        : "structure complete: build the Reactor Control Console and run a Data Cable from each of "
                        + "its data ports to a Ring Injection Port";
            }
        }
        problem = result == null ? "" : result;
        boolean nowFormed = result == null;
        if (nowFormed) setRingFormed(true); // also catches a block swapped in while formed
        if (nowFormed != formed) {
            formed = nowFormed;
            if (!formed) setRingFormed(false);
            setChanged();
        }
        // Keep clients' copy (formed, axis, receiver and port corners) up to date for the renderer.
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void unform() {
        if (formed) {
            formed = false;
            setRingFormed(false);
        }
        if (chamberFormed) {
            chamberFormed = false;
            setChamberFormed(false);
        }
    }

    // Hide (or show again) both rings' blocks: glass, Reinforced Fulgurite, injectors, portals and ports.
    // The chamber stays as it is.
    private void setRingFormed(boolean value) {
        setRingFormed(RING, value);
        setRingFormed(OUTER, value);
    }

    private void setRingFormed(int radius, boolean value) {
        int reach = radius + TUBE;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                int l = Math.max(Math.abs(dx), Math.abs(dz)) - radius;
                if (Math.abs(l) > TUBE) continue;
                for (int dy = -TUBE; dy <= TUBE; dy++) {
                    BlockPos p = at(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.hasProperty(FormableBlock.FORMED) && s.getValue(FormableBlock.FORMED) != value) {
                        level.setBlock(p, s.setValue(FormableBlock.FORMED, value), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }
    }

    // ---------- Save / sync ----------

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveFusionColliderBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveFusionColliderBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        tag.putBoolean("Formed", formed);
        tag.putBoolean("AxisX", axisX);
        tag.putInt("Receivers", receiverPair);
        tag.putInt("InjCorner", injCorner);
        tag.putBoolean("Chamber", chamberFormed);
        tag.putBoolean("ChamberX", chamberAxisX);
        tag.putLong("RunStart", runStart);
        tag.putInt("RunMask", runMask);
        tag.putInt("RunSeverity", runSeverity);
        tag.putLong("HeatUntil", heatUntil);
        tag.putLong("StaticBloom", staticBloomUntil);
        if (consolePos != null) tag.putLong("Console", consolePos.asLong());
        if (!carried[0].isEmpty()) tag.put("Carried0", com.sideflowgames.fulguritefusion.util.NbtBridge.saveStack(carried[0], registries));
        if (!carried[1].isEmpty()) tag.put("Carried1", com.sideflowgames.fulguritefusion.util.NbtBridge.saveStack(carried[1], registries));
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadFusionColliderBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadFusionColliderBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        formed = tag.getBooleanOr("Formed", false);
        axisX = !tag.contains("AxisX") || tag.getBooleanOr("AxisX", false);
        receiverPair = tag.getIntOr("Receivers", 0);
        injCorner = tag.contains("InjCorner") ? tag.getIntOr("InjCorner", 0) : -1;
        chamberFormed = tag.getBooleanOr("Chamber", false);
        chamberAxisX = !tag.contains("ChamberX") || tag.getBooleanOr("ChamberX", false);
        runStart = tag.contains("RunStart") ? tag.getLongOr("RunStart", 0L) : -1;
        if (tag.contains("Now") && level != null && level.isClientSide()) {
            clientRunStart = runStart < 0 ? -1 : level.getGameTime() - (tag.getLongOr("Now", 0L) - runStart);
        }
        runMask = tag.getIntOr("RunMask", 0);
        if (tag.contains("HeatUntil")) heatUntil = tag.getLongOr("HeatUntil", 0L);
        if (tag.contains("StaticBloom")) staticBloomUntil = tag.getLongOr("StaticBloom", 0L);
        runSeverity = tag.contains("RunSeverity") ? tag.getIntOr("RunSeverity", 0) : -1;
        if (tag.contains("Console")) consolePos = BlockPos.of(tag.getLongOr("Console", 0L));
        carried[0] = com.sideflowgames.fulguritefusion.util.NbtBridge.parseStack(registries, tag.getCompoundOrEmpty("Carried0"));
        carried[1] = com.sideflowgames.fulguritefusion.util.NbtBridge.parseStack(registries, tag.getCompoundOrEmpty("Carried1"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Formed", formed);
        tag.putBoolean("AxisX", axisX);
        tag.putInt("Receivers", receiverPair);
        tag.putInt("InjCorner", injCorner);
        tag.putLong("RunStart", runStart);
        tag.putInt("RunMask", runMask);
        tag.putInt("RunSeverity", runSeverity);
        tag.putBoolean("Chamber", chamberFormed);
        tag.putBoolean("ChamberX", chamberAxisX);
        tag.putLong("Now", level != null ? level.getGameTime() : 0);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // 26.1: runs the block's removal logic while this block entity still exists.
    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (level == null) return;
        if (state.getBlock() instanceof FusionColliderBlock block) block.onRemoveLegacy(state, level, pos, level.getBlockState(pos), false);
    }
}
