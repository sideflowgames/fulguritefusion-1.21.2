package com.sideflowgames.fulguritefusion.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.block.MachineConnectorBlock;
import com.sideflowgames.fulguritefusion.block.MachineConnectorBlockEntity;
import com.sideflowgames.fulguritefusion.block.StormBatteryBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A wire network: Machine Connectors joined by hanging wires.
//
// Every network has one "master" connector (the one with the lowest position) that runs it each tick:
//  1. machines that use power are fed first, from furnaces, then from batteries
//  2. leftover furnace power charges the batteries
// The network carries as much as its weakest wire: 256 Fm/t (Raw, Insulated), 512 (Reinforced), 1024 (Heavy).
// A network powers every machine on it: the 8-machine limit is only for daisy chains (machines touching).
public final class FmNetwork {
    private static final int SCAN_LIMIT = 1024;
    private static final int HAZARD_INTERVAL = 40; // ticks (2 seconds)

    // Last time each creature was shocked by wires, so it's at most one shock every 2 seconds.
    private static final Map<UUID, Long> LAST_SHOCK = new HashMap<>();

    // A raw (uninsulated) wire, from one connector cap to another.
    public record RawSpan(Vec3 from, Vec3 to, WireTier tier) {}

    public record Snapshot(BlockPos master, List<BlockPos> connectors, List<RawSpan> rawSpans,
                           List<BlockPos> generators, List<BlockPos> batteries,
                           List<BlockPos> linkedMachines, Set<BlockPos> unlinkedMachines,
                           Set<BlockPos> batteryIn, Set<BlockPos> batteryOut, int rate) {}

    private FmNetwork() {}

    // Follow the wires from a connector and work out everything in its network.
    public static Snapshot scan(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> connectors = new ArrayList<>();
        List<RawSpan> rawSpans = new ArrayList<>();
        int rate = Integer.MAX_VALUE; // the weakest wire sets what the network can carry
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty() && visited.size() < SCAN_LIMIT) {
            BlockPos current = queue.poll();
            if (!(level.getBlockEntity(current) instanceof MachineConnectorBlockEntity connector)) continue;
            connectors.add(current);
            for (MachineConnectorBlockEntity.Link link : connector.getLinks()) {
                BlockPos other = link.other();
                if (!(level.getBlockEntity(other) instanceof MachineConnectorBlockEntity)) continue; // unloaded or gone
                rate = Math.min(rate, link.tier().rate());
                // Count each raw wire once (from the end with the lower position).
                if (!link.insulated() && current.asLong() < other.asLong()) {
                    rawSpans.add(new RawSpan(WireMath.attachPoint(level, current), WireMath.attachPoint(level, other), link.tier()));
                }
                if (visited.add(other)) queue.add(other);
            }
        }

        // Each connector belongs to the block it's stuck to. Connectors on plain blocks are just relays.
        // (Sets, so two connectors on the same machine still count it once.)
        Set<BlockPos> generatorSet = new LinkedHashSet<>();
        Set<BlockPos> batterySet = new LinkedHashSet<>();
        Set<BlockPos> machineSet = new LinkedHashSet<>();
        Set<BlockPos> batteryIn = new HashSet<>(), batteryOut = new HashSet<>();
        for (BlockPos c : connectors) {
            BlockPos on = MachineConnectorBlock.attachedPos(level.getBlockState(c), c);
            BlockEntity be = level.getBlockEntity(on);
            if (be instanceof StormBatteryBlockEntity battery) {
                batterySet.add(on);
                // The bank's setting for the face the connector sits on decides which way power may go.
                net.minecraft.core.Direction face = net.minecraft.core.Direction.fromDelta(
                        c.getX() - on.getX(), c.getY() - on.getY(), c.getZ() - on.getZ());
                if (face == null || battery.canInput(face)) batteryIn.add(on);
                if (face == null || battery.canOutput(face)) batteryOut.add(on);
            } else if (level.getBlockState(on).is(com.sideflowgames.fulguritefusion.ModBlocks.CONSOLE_POWER_PORT.get())) {
                // A connector on a reactor Console Power Port draws from the console's store.
                com.sideflowgames.fulguritefusion.block.ReactorConsoleBlockEntity console =
                        com.sideflowgames.fulguritefusion.block.ReactorConsoleBlockEntity.forPowerPort(level, on);
                if (console != null) generatorSet.add(console.getBlockPos());
            } else if (be instanceof FmSource) {
                generatorSet.add(on);
            } else if (be instanceof FmHandler handler && handler.acceptsConnector()) {
                machineSet.add(on);
            }
        }
        List<BlockPos> generators = new ArrayList<>(generatorSet);
        List<BlockPos> batteries = new ArrayList<>(batterySet);
        List<BlockPos> machines = new ArrayList<>(machineSet);
        // Same order everywhere, so every connector shares power out the same way.
        machines.sort(Comparator.comparingLong(BlockPos::asLong));
        List<BlockPos> linked = machines;
        Set<BlockPos> unlinked = Set.of();

        BlockPos master = connectors.stream().min(Comparator.comparingLong(BlockPos::asLong)).orElse(start);
        if (rate == Integer.MAX_VALUE) rate = Config.WIRE_RATE.get();
        return new Snapshot(master, connectors, rawSpans, generators, batteries, linked, unlinked, batteryIn, batteryOut, rate);
    }

    // Move power around the network for one tick. Returns how much Fm moved.
    public static int tick(Level level, Snapshot net) {
        int budget = net.rate();
        List<FmSource> generators = new ArrayList<>();
        for (BlockPos p : net.generators()) {
            if (level.getBlockEntity(p) instanceof FmSource s) generators.add(s);
        }
        // Batteries charge only through faces set to In, and feed the network only through faces set to Out.
        List<StormBatteryBlockEntity> batteries = new ArrayList<>(), sources = new ArrayList<>();
        for (BlockPos p : net.batteries()) {
            if (level.getBlockEntity(p) instanceof StormBatteryBlockEntity b) {
                if (net.batteryIn().contains(p)) batteries.add(b);
                if (net.batteryOut().contains(p)) sources.add(b);
            }
        }
        List<FmHandler> machines = new ArrayList<>();
        for (BlockPos p : net.linkedMachines()) {
            if (level.getBlockEntity(p) instanceof FmHandler h) machines.add(h);
        }

        int moved = 0;
        // 1. Feed machines: an even share each, furnaces first, then batteries.
        for (int i = 0; i < machines.size() && budget > 0; i++) {
            FmHandler machine = machines.get(i);
            int share = Math.max(1, budget / (machines.size() - i));
            int wanted = machine.receiveFm(share, true);
            if (wanted <= 0) continue;
            int got = pull(generators, wanted);
            if (got < wanted) got += pull(sources, wanted - got);
            if (got > 0) {
                machine.receiveFm(got, false);
                budget -= got;
                moved += got;
            }
        }
        // 2. Charge batteries with what's left from the furnaces.
        for (int i = 0; i < batteries.size() && budget > 0; i++) {
            StormBatteryBlockEntity battery = batteries.get(i);
            int share = Math.max(1, budget / (batteries.size() - i));
            int wanted = battery.receiveFm(share, true);
            if (wanted <= 0) continue;
            int got = pull(generators, wanted);
            if (got > 0) {
                battery.receiveFm(got, false);
                budget -= got;
                moved += got;
            }
        }
        return moved;
    }

    private static int pull(List<? extends FmSource> sources, int amount) {
        int got = 0;
        for (FmSource source : sources) {
            if (got >= amount) break;
            got += source.extractFm(amount - got, false);
        }
        return got;
    }

    // Raw wire hazards while power is flowing: shocks players near the wire and electrifies water it dips into.
    public static void hazards(ServerLevel level, Snapshot net) {
        if (level.getGameTime() % HAZARD_INTERVAL != 0) return;
        long now = level.getGameTime();
        boolean water = Config.WATER_ELECTRIFY.get();
        double waterRange = Config.WATER_RANGE.get();
        int checked = 0;
        for (RawSpan span : net.rawSpans()) {
            if (++checked > 128) break; // keep huge networks cheap
            WireTier tier = span.tier();
            float damage = tier.shockDamage();
            double range = tier.shockRange();
            List<Vec3> points = samples(span);
            if (Config.STATIC_ENABLED.get()) {
                AABB area = new AABB(span.from(), span.to()).inflate(range).expandTowards(0, -WireMath.sag(span.from(), span.to(), tier), 0);
                for (Player player : level.getEntitiesOfClass(Player.class, area)) {
                    if (closeTo(points, player.position().add(0, 1, 0), range)) shock(level, player, damage, tier, now);
                }
            }
            if (water) {
                for (Vec3 p : points) {
                    if (level.getFluidState(BlockPos.containing(p)).is(FluidTags.WATER)) {
                        for (LivingEntity creature : level.getEntitiesOfClass(LivingEntity.class,
                                new AABB(p, p).inflate(waterRange + tier.gauge), LivingEntity::isInWater)) {
                            shock(level, creature, damage, tier, now);
                        }
                    }
                }
            }
        }
        if (LAST_SHOCK.size() > 4096) LAST_SHOCK.clear();
    }

    // Points about one block apart along a wire.
    private static List<Vec3> samples(RawSpan span) {
        int n = Math.max(2, (int) Math.ceil(span.from().distanceTo(span.to())) + 1);
        List<Vec3> points = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) points.add(WireMath.point(span.from(), span.to(), (double) i / n, span.tier()));
        return points;
    }

    private static boolean closeTo(List<Vec3> points, Vec3 target, double range) {
        double r2 = (range + 0.5) * (range + 0.5);
        for (Vec3 p : points) {
            if (p.distanceToSqr(target) <= r2) return true;
        }
        return false;
    }

    private static void shock(ServerLevel level, LivingEntity target, float damage, WireTier tier, long now) {
        Long last = LAST_SHOCK.get(target.getUUID());
        if (last != null && now - last < HAZARD_INTERVAL) return;
        LAST_SHOCK.put(target.getUUID(), now);
        target.hurt(level.damageSources().lightningBolt(), damage);
        if (tier.shockBurns() && !target.isInWater()) target.igniteForSeconds(3);
    }

    // Tell every connector whether power is flowing (raw wires spark while it is).
    public static void setPowered(Level level, Snapshot net, boolean powered) {
        for (BlockPos c : net.connectors()) {
            if (level.getBlockEntity(c) instanceof MachineConnectorBlockEntity connector) connector.setPowered(powered);
        }
    }
}
