package com.sideflowgames.fulguritefusion.dungeon;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

// One room of a Stormreach dungeon: a 15x15x9 cell (the arena and the Stormspire core: several cells). It rebuilds
// itself deterministically in every chunk it touches, so every choice is a pure function of position.
//
// Each dungeon carves its own shapes:
//   Stormspire     square rooms around a hollow lightning core, balconies open to the storm, broken floors
//   Ashen Foundry  halls floored with lava, crossed by narrow walkways; furnace halls; crushers overhead
//   Aurora Sanctum vaulted halls with colonnades, frozen statues and ice aisles; Rime Totems
//   Prism Vault    natural caverns and twisting tunnels, crystal growths, chasms with crystal bridges
// Only the main path (entrance to arena) is lit: sconces beside its doorways and lanterns along the way. Side
// branches stay dark.
public class DungeonPiece extends StructurePiece {
    static final int MAIN_PATH = 1, HOLE_UP = 2, HOLE_DOWN = 4;

    private final DungeonKind kind;
    private final DungeonLayout.Room room;
    private final int[] shell;            // minX minY minZ maxX maxY maxZ
    private final int[] doors, seals;     // 6 ints each
    private final int[] stairs;           // lower-cell origin x y z, strip
    private final int[] pathDoors, pathStairs;
    private final int ext;                // sides with nothing beyond (N S W E bits), surface dungeons only
    private final int flags;              // MAIN_PATH, HOLE_UP, HOLE_DOWN; bits 4-7: sides facing the Stormspire core
    private final boolean foundation, roofOpen;
    private final int shaftTop, outDoor, level;
    private final long seed;
    private final int[] vault;
    private final int[] center;           // Stormspire: the tower's axis (x z); otherwise empty

    DungeonPiece(DungeonKind kind, DungeonLayout.Room room, int[] shell, List<int[]> doors, List<int[]> seals, List<int[]> stairs,
                 List<int[]> pathDoors, List<int[]> pathStairs, int ext, int flags, boolean foundation, boolean roofOpen,
                 int shaftTop, int outDoor, long seed, int level, @Nullable int[] vault, @Nullable int[] center) {
        super(DungeonRegistry.ROOM.get(), 0, box(shell, shaftTop, outDoor));
        this.kind = kind;
        this.room = room;
        this.shell = shell;
        this.doors = flat(doors);
        this.seals = flat(seals);
        this.stairs = flat(stairs);
        this.pathDoors = flat(pathDoors);
        this.pathStairs = flat(pathStairs);
        this.ext = ext;
        this.flags = flags;
        this.foundation = foundation;
        this.roofOpen = roofOpen;
        this.shaftTop = shaftTop;
        this.outDoor = outDoor;
        this.seed = seed;
        this.level = level;
        this.vault = vault == null ? new int[0] : vault;
        this.center = center == null ? new int[0] : center;
        setOrientation(null);
    }

    public DungeonPiece(CompoundTag tag) {
        super(DungeonRegistry.ROOM.get(), tag);
        kind = DungeonKind.byId(tag.getStringOr("Kind", ""));
        room = DungeonLayout.Room.values()[tag.getIntOr("Room", 0)];
        shell = tag.getIntArray("Shell").orElse(new int[0]);
        doors = tag.getIntArray("Doors").orElse(new int[0]);
        seals = tag.getIntArray("Seals").orElse(new int[0]);
        stairs = tag.getIntArray("Stairs").orElse(new int[0]);
        pathDoors = tag.getIntArray("PathDoors").orElse(new int[0]);
        pathStairs = tag.getIntArray("PathStairs").orElse(new int[0]);
        ext = tag.getIntOr("Ext", 0);
        flags = tag.getIntOr("Flags", 0);
        foundation = tag.getBooleanOr("Foundation", false);
        roofOpen = tag.getBooleanOr("RoofOpen", false);
        shaftTop = tag.getIntOr("ShaftTop", 0);
        outDoor = tag.getIntOr("OutDoor", 0);
        seed = tag.getLongOr("Seed", 0L);
        level = tag.getIntOr("Level", 0);
        vault = tag.getIntArray("Vault").orElse(new int[0]);
        center = tag.getIntArray("Center").orElse(new int[0]);
        setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Kind", kind.id);
        tag.putInt("Room", room.ordinal());
        tag.putIntArray("Shell", shell);
        tag.putIntArray("Doors", doors);
        tag.putIntArray("Seals", seals);
        tag.putIntArray("Stairs", stairs);
        tag.putIntArray("PathDoors", pathDoors);
        tag.putIntArray("PathStairs", pathStairs);
        tag.putInt("Ext", ext);
        tag.putInt("Flags", flags);
        tag.putBoolean("Foundation", foundation);
        tag.putBoolean("RoofOpen", roofOpen);
        tag.putInt("ShaftTop", shaftTop);
        tag.putInt("OutDoor", outDoor);
        tag.putLong("Seed", seed);
        tag.putInt("Level", level);
        tag.putIntArray("Vault", vault);
        tag.putIntArray("Center", center);
    }

    private static BoundingBox box(int[] s, int shaftTop, int outDoor) {
        int maxY = s[4];
        if (shaftTop != Integer.MIN_VALUE) maxY = Math.max(maxY, shaftTop + 6);
        int grow = outDoor >= 0 ? 10 : 0;
        return new BoundingBox(s[0] - grow, s[1] - (grow > 0 ? 1 : 0), s[2] - grow, s[3] + grow, maxY, s[5] + grow);
    }

    private static int[] flat(List<int[]> list) {
        int n = 0;
        for (int[] a : list) n += a.length;
        int[] out = new int[n];
        int i = 0;
        for (int[] a : list) for (int v : a) out[i++] = v;
        return out;
    }

    // ---- building ----

    private WorldGenLevel lvl;
    private BoundingBox chunk;
    private final List<double[]> segs = new ArrayList<>(); // connector segments: ax az bx bz path

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        lvl = level;
        chunk = chunkBox;
        segs.clear();
        buildConnectors();
        if (foundation) buildFoundation();
        carve();
        for (int i = 0; i + 5 < doors.length; i += 6) fillShell(doors, i, Blocks.AIR.defaultBlockState());
        for (int i = 0; i + 5 < seals.length; i += 6) fillShell(seals, i, DungeonBlocks.STORM_SEAL.get().defaultBlockState());
        switch (kind) {
            case STORMSPIRE -> spireFeatures();
            case ASHEN_FOUNDRY -> foundryFeatures();
            case AURORA_SANCTUM -> sanctumFeatures();
            case PRISM_VAULT -> prismFeatures();
        }
        switch (room) {
            case ENTRANCE -> entrance();
            case MOB -> mobRoom();
            case TRAP -> trapRoom();
            case VAULT -> vaultRoom();
            case BOSS -> arena();
            case TREASURE -> treasure();
            case CORE -> core();
            default -> {}
        }
        if (mainPath()) pathLights();
        for (int i = 0; i + 3 < stairs.length; i += 4) stairRun(stairs[i], stairs[i + 1], stairs[i + 2], stairs[i + 3]);
        lvl = null;
        chunk = null;
    }

    private boolean mainPath() {
        return (flags & MAIN_PATH) != 0;
    }

    private int coreMask() {
        return (flags >> 4) & 15;
    }

    private boolean is(DungeonLayout.Room... rooms) {
        for (var r : rooms) if (room == r) return true;
        return false;
    }

    private boolean inShell(int x, int y, int z) {
        return x >= shell[0] && x <= shell[3] && y >= shell[1] && y <= shell[4] && z >= shell[2] && z <= shell[5];
    }

    private void put(int x, int y, int z, BlockState s) {
        if (chunk.isInside(x, y, z)) lvl.setBlock(new BlockPos(x, y, z), s, 2);
    }

    private boolean set(int x, int y, int z, BlockState s) {
        if (!inShell(x, y, z) || !chunk.isInside(x, y, z)) return false;
        lvl.setBlock(new BlockPos(x, y, z), s, 2);
        return true;
    }

    private BlockState get(int x, int y, int z) {
        return lvl.getBlockState(new BlockPos(x, y, z));
    }

    // Deterministic noise in [0,1) for a position.
    private double h(int x, int y, int z, int salt) {
        return hash(seed, x, y, z, salt);
    }

    private static double hash(long seed, int x, int y, int z, int salt) {
        long v = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L) ^ (salt * 0x27D4EB2F165667C5L);
        v = (v ^ (v >>> 30)) * 0xBF58476D1CE4E5B9L;
        v = (v ^ (v >>> 27)) * 0x94D049BB133111EBL;
        v ^= v >>> 31;
        return (v >>> 11) * 0x1.0p-53;
    }

    // Smooth value noise in [0,1), cells of 4 blocks.
    private double smooth(double x, double y, double z, int salt) {
        double gx = x / 4.0, gy = y / 4.0, gz = z / 4.0;
        int ix = (int) Math.floor(gx), iy = (int) Math.floor(gy), iz = (int) Math.floor(gz);
        double fx = gx - ix, fy = gy - iy, fz = gz - iz;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        fz = fz * fz * (3 - 2 * fz);
        double v = 0;
        for (int a = 0; a <= 1; a++)
            for (int b = 0; b <= 1; b++)
                for (int c = 0; c <= 1; c++) {
                    double w = (a == 1 ? fx : 1 - fx) * (b == 1 ? fy : 1 - fy) * (c == 1 ? fz : 1 - fz);
                    v += w * h(ix + a, iy + b, iz + c, salt);
                }
        return v;
    }

    // The middle of the room. In the round Stormspire a corner room's middle would be outside the tower, so it's
    // pulled in toward the axis.
    private double cx() {
        double c = (shell[0] + shell[3]) / 2.0;
        return anchored() ? center[0] + (c - center[0]) * pull() : c;
    }

    private double cz() {
        double c = (shell[2] + shell[5]) / 2.0;
        return anchored() ? center[1] + (c - center[1]) * pull() : c;
    }

    private boolean anchored() {
        return round() && !is(DungeonLayout.Room.BOSS, DungeonLayout.Room.CORE);
    }

    private double pull() {
        double dx = (shell[0] + shell[3]) / 2.0 - center[0], dz = (shell[2] + shell[5]) / 2.0 - center[1];
        double r = Math.sqrt(dx * dx + dz * dz);
        return r > 13.5 ? 13.5 / r : 1;
    }

    private int icx() {
        return (int) Math.floor(cx());
    }

    private int icz() {
        return (int) Math.floor(cz());
    }

    private int fy() {
        return shell[1] + 1;
    }

    private int top() {
        return shell[4] - 1;
    }

    // ---- connectors: lines from each doorway and stair end to the middle of the room ----

    private boolean isPathDoor(int i) {
        for (int j = 0; j + 5 < pathDoors.length; j += 6) {
            boolean same = true;
            for (int k = 0; k < 6; k++) same &= pathDoors[j + k] == doors[i + k];
            if (same) return true;
        }
        return false;
    }

    private boolean isPathStair(int i) {
        for (int j = 0; j + 3 < pathStairs.length; j += 4) {
            boolean same = true;
            for (int k = 0; k < 4; k++) same &= pathStairs[j + k] == stairs[i + k];
            if (same) return true;
        }
        return false;
    }

    private void buildConnectors() {
        if (room == DungeonLayout.Room.CORE) return;
        double cx = cx() + 0.5, cz = cz() + 0.5;
        for (int i = 0; i + 5 < doors.length; i += 6) {
            double dx = (doors[i] + doors[i + 3]) / 2.0 + 0.5, dz = (doors[i + 2] + doors[i + 5]) / 2.0 + 0.5;
            double ex = Math.max(shell[0] + 1.5, Math.min(shell[3] - 0.5, dx));
            double ez = Math.max(shell[2] + 1.5, Math.min(shell[5] - 0.5, dz));
            boolean alongX = doors[i] <= shell[0] || doors[i + 3] >= shell[3];
            double ix = alongX ? ex + Math.signum(cx - ex) * 3 : ex, iz = alongX ? ez : ez + Math.signum(cz - ez) * 3;
            double p = isPathDoor(i) ? 1 : 0;
            segs.add(new double[] {ex, ez, ix, iz, p});
            segs.add(new double[] {ix, iz, cx, cz, p});
        }
        for (int i = 0; i + 3 < stairs.length; i += 4) {
            int ox = stairs[i], oy = stairs[i + 1], oz = stairs[i + 3] == 0 ? stairs[i + 2] + 2 : stairs[i + 2] + 11;
            double zm = oz + 1.0;
            double p = isPathStair(i) ? 1 : 0;
            if (oy == shell[1]) segs.add(new double[] {ox + 1.5, zm, cx, cz, p});
            else if (kind.floorHeight() > 9) segs.add(new double[] {ox + 1.5, (stairs[i + 3] == 0 ? stairs[i + 2] + 4 : stairs[i + 2] + 9) + 1.0, cx, cz, p});
            else segs.add(new double[] {ox + 12.0, zm, cx, cz, p});
        }
        if (outDoor >= 0) {
            double ex = outDoor == 2 ? shell[0] + 1.5 : outDoor == 3 ? shell[3] - 0.5 : cx;
            double ez = outDoor == 0 ? shell[2] + 1.5 : outDoor == 1 ? shell[5] - 0.5 : cz;
            segs.add(new double[] {ex, ez, cx, cz, 1});
        }
        if (shaftTop != Integer.MIN_VALUE) segs.add(new double[] {cx, cz - 1, cx, cz, 1});
    }

    private static double segDist(double px, double pz, double[] s) {
        double ax = s[0], az = s[1], bx = s[2], bz = s[3];
        double dx = bx - ax, dz = bz - az, len = dx * dx + dz * dz;
        double t = len < 1e-6 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / len));
        double qx = ax + t * dx - px, qz = az + t * dz - pz;
        return Math.sqrt(qx * qx + qz * qz);
    }

    // Distance from a block's centre to the nearest connector (or only path connectors).
    private double conn(int x, int z, boolean pathOnly) {
        double best = 99;
        for (double[] s : segs) if (!pathOnly || s[4] > 0) best = Math.min(best, segDist(x + 0.5, z + 0.5, s));
        return best;
    }

    private double conn(int x, int z) {
        return conn(x, z, false);
    }

    // Around stair runs nothing gets placed.
    private boolean blocked(int x, int z) {
        for (int i = 0; i + 3 < stairs.length; i += 4) {
            int ox = stairs[i], oz = stairs[i + 2], zs = stairs[i + 3] == 0 ? oz + 2 : oz + 11;
            if (kind.floorHeight() > 9) {
                int zlo = stairs[i + 3] == 0 ? oz + 2 : oz + 9;
                if (x >= ox + 1 && x <= ox + 13 && z >= zlo - 1 && z <= zlo + 4) return true;
            } else if (x >= ox + 1 && x <= ox + 12 && z >= zs - 1 && z <= zs + 2) return true;
        }
        return false;
    }

    // ---- shape ----

    // Static smooth noise (no seed): the same for every piece, so neighbouring rooms agree on shared openings.
    private static double staticNoise(double a, double b, int salt) {
        double ga = a / 3.0, gb = b / 3.0;
        int ia = (int) Math.floor(ga), ib = (int) Math.floor(gb);
        double fa = ga - ia, fb = gb - ib;
        fa = fa * fa * (3 - 2 * fa);
        fb = fb * fb * (3 - 2 * fb);
        double v00 = hash(0, ia, ib, 0, salt), v10 = hash(0, ia + 1, ib, 0, salt), v01 = hash(0, ia, ib + 1, 0, salt), v11 = hash(0, ia + 1, ib + 1, 0, salt);
        return (v00 * (1 - fa) + v10 * fa) * (1 - fb) + (v01 * (1 - fa) + v11 * fa) * fb;
    }

    private boolean round() {
        return kind == DungeonKind.STORMSPIRE && center.length == 2 && room != DungeonLayout.Room.TREASURE;
    }

    // The treasure vault's own box. In the round Stormspire it's pulled in from the outer wall so it doesn't poke
    // out of the tower; the rest of its cell is tower stone.
    private int[] vaultBox() {
        if (kind != DungeonKind.STORMSPIRE || center.length != 2 || room != DungeonLayout.Room.TREASURE) return shell;
        int[] v = shell.clone();
        double mx = (shell[0] + shell[3]) / 2.0 - center[0], mz = (shell[2] + shell[5]) / 2.0 - center[1];
        if (Math.abs(mx) >= Math.abs(mz)) {
            if (mx > 0) v[3] -= 5; else v[0] += 5;
        } else {
            if (mz > 0) v[5] -= 5; else v[2] += 5;
        }
        return v;
    }

    // Distance from the Stormspire's axis.
    private double radius(int x, int z) {
        double dx = x - center[0], dz = z - center[1];
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static final double SPIRE_OUT = 21.6, SPIRE_WALL = 20.2, CORE_SHAFT = 3.3, CORE_WALL = 4.7;

    // Is this interior position open (air)? Pure function of position.
    private boolean open(int x, int y, int z) {
        if (round()) {
            int fh = kind.floorHeight();
            int ly = (y - shell[1]) % fh;
            if (room == DungeonLayout.Room.BOSS) ly = y - shell[1];
            double r = radius(x, z);
            if (r > SPIRE_WALL) return false;
            if (room == DungeonLayout.Room.CORE) return r <= CORE_SHAFT ? y > shell[1] : r > CORE_WALL && ly >= 1 && ly <= fh - 2;
            return ly >= 1 && (room == DungeonLayout.Room.BOSS || ly <= fh - 2);
        }
        if (is(DungeonLayout.Room.TREASURE, DungeonLayout.Room.ENTRANCE, DungeonLayout.Room.CORE)) return true;
        return switch (kind) {
            case PRISM_VAULT -> cave(x, y, z);
            case ASHEN_FOUNDRY -> magma(x, y, z);
            case AURORA_SANCTUM -> dome(x, y, z) >= y - shell[1];
            default -> true;
        };
    }

    private boolean cave(int x, int y, int z) {
        int ly = y - shell[1], sy = shell[4] - shell[1] + 1;
        if (ly < 1 || ly > sy - 2) return false;
        double cd = conn(x, z);
        if (cd < 1.6 + smooth(x, 0, z, 41) * 1.0 && ly <= 3.6 + smooth(x, 50, z, 42) * 1.6) return true;
        boolean tunnelHall = room == DungeonLayout.Room.HALL && h(shell[0], 0, shell[2], 30) < 0.5;
        if (tunnelHall) return false;
        double rx = (shell[3] - shell[0]) / 2.0 - 0.8, rz = (shell[5] - shell[2]) / 2.0 - 0.8;
        if (room == DungeonLayout.Room.HALL) {
            rx *= 0.65;
            rz *= 0.65;
        }
        double ry = (sy - 2) * 0.95;
        double dx = (x - cx()) / rx, dz = (z - cz()) / rz, dy = (ly - 1) / ry;
        double d = dx * dx + dz * dz + dy * dy + (smooth(x, y, z, 43) - 0.5) * 0.9;
        if (d >= 1) return false;
        return !(ly <= 2 && cd > 1.6 && smooth(x, 0, z, 44) > 0.78);
    }

    // Ashen Foundry: wide, low magma caverns joined by lava tubes; some halls are only tubes.
    private boolean magma(int x, int y, int z) {
        int ly = y - shell[1], sy = shell[4] - shell[1] + 1;
        if (ly < 1 || ly > sy - 2) return false;
        double cd = conn(x, z);
        if (cd < 2.0 + smooth(x, 0, z, 45) * 1.2 && ly <= 4.0 + smooth(x, 60, z, 46) * 1.8) return true;
        if (room == DungeonLayout.Room.HALL && h(shell[0], 0, shell[2], 31) < 0.5) return false;
        double rx = (shell[3] - shell[0]) / 2.0 - 1.3, rz = (shell[5] - shell[2]) / 2.0 - 1.3;
        double ry = (sy - 2) * 0.85;
        double dx = (x - cx()) / rx, dz = (z - cz()) / rz, dy = (ly - 1) / ry;
        return dx * dx + dz * dz + dy * dy + (smooth(x, y, z, 47) - 0.5) * 1.5 + (smooth(x * 2, y, z * 2, 49) - 0.5) * 0.4 < 1;
    }

    // Aurora Sanctum: the height of the vault over a column, or -1 where it's solid. Each room is a round domed hall;
    // naves with pointed vaults run from it to every doorway.
    private double dome(int x, int z) {
        int sy = shell[4] - shell[1] + 1;
        boolean arena = room == DungeonLayout.Room.BOSS;
        double rx = (shell[3] - shell[0]) / 2.0 - (arena ? 1.2 : 0.8), rz = (shell[5] - shell[2]) / 2.0 - (arena ? 1.2 : 0.8);
        double dx = (x - cx()) / rx, dz = (z - cz()) / rz;
        double nd = Math.sqrt(dx * dx + dz * dz) + (smooth(x, 0, z, 48) - 0.5) * 0.08;
        double best = -1;
        if (nd <= 1) {
            double base = arena ? 9 : 5, topH = sy - 2;
            best = base + (topH - base) * Math.sqrt(1 - nd * nd);
        }
        double cd = conn(x, z);
        if (cd <= 2.4) best = Math.max(best, 3 + 3 * Math.sqrt(Math.max(0, 1 - (cd / 2.6) * (cd / 2.6))));
        return best;
    }

    private double dome(int x, int y, int z) {
        int ly = y - shell[1], sy = shell[4] - shell[1] + 1;
        if (ly < 1 || ly > sy - 2) return -1;
        return dome(x, z);
    }

    // Where a wall opening between two rooms is carved. Each dungeon has its own: plain doorways (Stormspire, whose
    // rooms join wide open), pointed arches (Aurora Sanctum), ragged cave mouths (Ashen Foundry, Prism Vault).
    private boolean mouth(int x, int y, int z) {
        if (room == DungeonLayout.Room.TREASURE || kind == DungeonKind.STORMSPIRE) return false;
        for (int i = 0; i + 5 < doors.length; i += 6) {
            int[] d = doors;
            boolean crossesX = d[i + 3] - d[i] == 1;
            if (crossesX ? (x < d[i] || x > d[i + 3]) : (z < d[i + 2] || z > d[i + 5])) continue;
            double along = crossesX ? z + 0.5 : x + 0.5;
            double mid = crossesX ? (d[i + 2] + d[i + 5]) / 2.0 + 0.5 : (d[i] + d[i + 3]) / 2.0 + 0.5;
            double a = Math.abs(along - mid);
            int ly = y - (d[i + 1] - 1);
            if (ly < 1) continue;
            if (kind == DungeonKind.AURORA_SANCTUM) {
                if (ly <= 6 && a <= (ly <= 3 ? 2.6 : 2.6 - (ly - 3) * 0.85)) return true;
            } else {
                double n = staticNoise(along, y, 70 + (crossesX ? 1 : 0));
                if (ly <= 5 && a <= 2.0 + n * 1.3 - Math.max(0, ly - 3) * 0.7) return true;
            }
        }
        return false;
    }

    // The Aurora Sanctum's ground-floor halls stand free as domed buildings: outside their walls is open air.
    private boolean freeStanding() {
        return kind == DungeonKind.AURORA_SANCTUM && level == 0 && !is(DungeonLayout.Room.TREASURE, DungeonLayout.Room.ENTRANCE, DungeonLayout.Room.CORE);
    }

    // For a free-standing hall: the top of its shell over a column (vault + 1), or -1 if the column is outside it.
    private double roofOver(int x, int z) {
        double best = -1;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (x + dx <= shell[0] || x + dx >= shell[3] || z + dz <= shell[2] || z + dz >= shell[5]) {
                    if (mouth(x + dx, shell[1] + 2, z + dz)) best = Math.max(best, 7);
                    continue;
                }
                best = Math.max(best, dome(x + dx, z + dz) + 1);
            }
        return best;
    }

    // ---- materials ----

    private BlockState rock(int x, int y, int z) {
        double n = h(x, y, z, 1);
        if (room == DungeonLayout.Room.TREASURE) return DungeonBlocks.WARDED_BRICK.get().defaultBlockState();
        boolean natural = !is(DungeonLayout.Room.ENTRANCE);
        if (kind == DungeonKind.PRISM_VAULT && natural) {
            double m = smooth(x, y, z, 2);
            if (n < 0.03) return Blocks.AMETHYST_BLOCK.defaultBlockState();
            if (m < 0.3) return Blocks.TUFF.defaultBlockState();
            if (m > 0.75) return Blocks.CALCITE.defaultBlockState();
            return n < 0.5 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        if (kind == DungeonKind.ASHEN_FOUNDRY && natural) {
            double m = smooth(x, y, z, 3);
            if (m > 0.68) return n < 0.3 ? kind.cracked() : kind.wall(); // ruined forge walls in the rock
            if (n < 0.03) return Blocks.MAGMA_BLOCK.defaultBlockState();
            if (m < 0.3) return Blocks.BASALT.defaultBlockState();
            if (m < 0.42) return Blocks.SMOOTH_BASALT.defaultBlockState();
            return n < 0.6 ? Blocks.BLACKSTONE.defaultBlockState() : n > 0.97 ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                    : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (kind == DungeonKind.AURORA_SANCTUM && natural) {
            double m = smooth(x, y, z, 4);
            if (m > 0.66) return Blocks.PACKED_ICE.defaultBlockState();
            if (m < 0.2) return Blocks.SNOW_BLOCK.defaultBlockState();
        }
        return n < 0.16 ? kind.cracked() : n < 0.2 ? kind.tile() : kind.wall();
    }

    private BlockState floorMat(int x, int y, int z) {
        if (room == DungeonLayout.Room.TREASURE) return DungeonBlocks.WARDED_BRICK.get().defaultBlockState();
        if ((kind == DungeonKind.PRISM_VAULT || kind == DungeonKind.ASHEN_FOUNDRY) && !is(DungeonLayout.Room.ENTRANCE)) return rock(x, y, z);
        if (round()) {
            double r = radius(x, z);
            if (Math.abs(r - 9) < 0.5 || Math.abs(r - 15) < 0.5) return kind.tile();
            return h(x, y, z, 2) < 0.1 ? kind.cracked() : kind.floor();
        }
        int lx = x - shell[0], lz = z - shell[2];
        boolean border = lx % 15 == 1 || lx % 15 == 13 || lz % 15 == 1 || lz % 15 == 13;
        if (border && kind != DungeonKind.AURORA_SANCTUM) return kind.tile();
        return h(x, y, z, 2) < 0.1 ? kind.cracked() : kind.floor();
    }

    private void carve() {
        if (round()) {
            carveRound();
            return;
        }
        if (vaultBox() != shell) {
            carveTowerVault();
            return;
        }
        int x0 = Math.max(shell[0], chunk.minX()), x1 = Math.min(shell[3], chunk.maxX());
        int z0 = Math.max(shell[2], chunk.minZ()), z1 = Math.min(shell[5], chunk.maxZ());
        BlockState air = Blocks.AIR.defaultBlockState();
        boolean free = freeStanding();
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                double roof = free ? roofOver(x, z) : 99;
                for (int y = shell[1]; y <= shell[4]; y++) {
                    int ly = y - shell[1];
                    int freeFrom = room == DungeonLayout.Room.BOSS ? 9 : 0; // the arena is sunk half into the ground
                    if (free && ly >= freeFrom && (roof < 0 || ly > roof)) {
                        // Outside the free-standing hall: open air (the floor too, unless the hall's floor is under it).
                        set(x, y, z, ly == freeFrom && roof >= 0 ? floorMat(x, y, z) : air);
                        continue;
                    }
                    boolean side = x == shell[0] || x == shell[3] || z == shell[2] || z == shell[5];
                    if (y == shell[4] && roofOpen) {
                        set(x, y, z, side && ((x + z) & 1) == 0 ? kind.wall() : air);
                    } else if (y == shell[1]) {
                        set(x, y, z, !side && open(x, y + 1, z) ? floorMat(x, y, z) : rock(x, y, z));
                    } else if (y == shell[4]) {
                        set(x, y, z, skylight(x, z) ? skylightMat(x, z) : rock(x, y, z));
                    } else if (side) {
                        set(x, y, z, mouth(x, y, z) ? air : rock(x, y, z));
                    } else if (open(x, y, z)) {
                        set(x, y, z, air);
                    } else {
                        set(x, y, z, open(x, y + 1, z) ? floorMat(x, y, z) : (free && ly == roof) ? kind.tile() : rock(x, y, z));
                    }
                }
            }
    }

    private void carveTowerVault() {
        int[] v = vaultBox();
        int x0 = Math.max(shell[0], chunk.minX()), x1 = Math.min(shell[3], chunk.maxX());
        int z0 = Math.max(shell[2], chunk.minZ()), z1 = Math.min(shell[5], chunk.maxZ());
        BlockState warded = DungeonBlocks.WARDED_BRICK.get().defaultBlockState();
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                double dx = x - center[0], dz = z - center[1], r = Math.sqrt(dx * dx + dz * dz);
                if (r > SPIRE_OUT) continue;
                boolean inVault = x >= v[0] && x <= v[3] && z >= v[2] && z <= v[5];
                for (int y = shell[1]; y <= shell[4]; y++) {
                    if (!inVault) {
                        set(x, y, z, kind.wall());
                        continue;
                    }
                    boolean edge = x == v[0] || x == v[3] || z == v[2] || z == v[5] || y == shell[1] || y == shell[4];
                    set(x, y, z, edge ? warded : Blocks.AIR.defaultBlockState());
                }
            }
    }

    // Aurora Sanctum ground-floor domes have a coloured skylight at the crown: the aurora shines through.
    private boolean skylight(int x, int z) {
        if (kind != DungeonKind.AURORA_SANCTUM || level != 0 || is(DungeonLayout.Room.TREASURE, DungeonLayout.Room.ENTRANCE)) return false;
        double dx = x - cx(), dz = z - cz();
        return dx * dx + dz * dz <= 5 && dome(x, z) >= shell[4] - shell[1] - 2;
    }

    private BlockState skylightMat(int x, int z) {
        double n = h(x, 0, z, 5);
        return (n < 0.33 ? Blocks.LIME_STAINED_GLASS : n < 0.66 ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.PURPLE_STAINED_GLASS).defaultBlockState();
    }

    // The Stormspire is round: a stone ring around the core, its outer wall broken by gaps that open over the
    // drop (a knee-high rail is all that stops you). Outside the ring is open air.
    private void carveRound() {
        int x0 = Math.max(shell[0], chunk.minX()), x1 = Math.min(shell[3], chunk.maxX());
        int z0 = Math.max(shell[2], chunk.minZ()), z1 = Math.min(shell[5], chunk.maxZ());
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                double r = radius(x, z);
                double ang = Math.toDegrees(Math.atan2(z - center[1], x - center[0])) + 360;
                for (int y = shell[1]; y <= shell[4]; y++) {
                    int fh = kind.floorHeight();
                    int ly = room == DungeonLayout.Room.BOSS ? y - shell[1] : (y - shell[1]) % fh;
                    boolean floorLayer = ly == 0, ceilLayer = room == DungeonLayout.Room.BOSS ? y == shell[4] : ly == fh - 1;
                    if (r > SPIRE_OUT) {
                        continue; // outside the tower: leave the land as it is
                    } else if (r > SPIRE_WALL) {
                        boolean gap = room != DungeonLayout.Room.BOSS && ly >= 2 && ly <= fh - 6 && (ang % 30) < 7;
                        if (roofOpen && y == shell[4]) set(x, y, z, ((int) (ang / 6) & 1) == 0 ? kind.wall() : air);
                        else set(x, y, z, gap ? air : rock(x, y, z));
                    } else if (room == DungeonLayout.Room.CORE && r > CORE_SHAFT && r <= CORE_WALL) {
                        boolean window = ly >= 2 && ly <= fh - 4 && (ang % 45) < 12;
                        set(x, y, z, floorLayer || ceilLayer || !window ? rock(x, y, z) : air);
                    } else if (room == DungeonLayout.Room.CORE && r <= CORE_SHAFT) {
                        set(x, y, z, y == shell[1] ? kind.accent() : air);
                    } else if (floorLayer) {
                        set(x, y, z, floorMat(x, y, z));
                    } else if (ceilLayer) {
                        set(x, y, z, roofOpen ? air : rock(x, y, z));
                    } else {
                        boolean side = x == shell[0] || x == shell[3] || z == shell[2] || z == shell[5];
                        set(x, y, z, side && !wideSide(x, y, z) ? rock(x, y, z) : air);
                    }
                }
            }
    }

    // In the Stormspire, rooms joined by a doorway (and every room beside the core) open fully onto each other.
    private boolean wideSide(int x, int y, int z) {
        if (room == DungeonLayout.Room.CORE || room == DungeonLayout.Room.BOSS) {
            for (int i = 0; i + 5 < doors.length; i += 6)
                if (x >= doors[i] && x <= doors[i + 3] && y >= doors[i + 1] && y <= doors[i + 4] && z >= doors[i + 2] && z <= doors[i + 5]) return true;
            return false;
        }
        int side = z == shell[2] ? 0 : z == shell[5] ? 1 : x == shell[0] ? 2 : 3;
        if ((coreMask() & (1 << side)) != 0) return true;
        for (int i = 0; i + 5 < doors.length; i += 6) {
            boolean onSide = switch (side) {
                case 0 -> doors[i + 2] <= shell[2];
                case 1 -> doors[i + 5] >= shell[5];
                case 2 -> doors[i] <= shell[0];
                default -> doors[i + 3] >= shell[3];
            };
            if (onSide) return true;
        }
        return false;
    }

    private BlockState windowMat() {
        return kind == DungeonKind.AURORA_SANCTUM ? Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()
                : ModBlocks.FULGURITE_GLASS.get().defaultBlockState();
    }

    // The Aurora Sanctum's outer walls get tall stained-glass windows.
    private boolean window(int x, int y, int z) {
        if (ext == 0 || kind != DungeonKind.AURORA_SANCTUM) return false;
        int ly = y - shell[1];
        if (ly < 2 || ly > 5) return false;
        int along = sideAlong(x, z, ext);
        if (along < 0) return false;
        int a = along % 15;
        return a == 4 || a == 5 || a == 9 || a == 10;
    }

    // If (x,z) is on one of the given walls, its position along that wall; otherwise -1.
    private int sideAlong(int x, int z, int mask) {
        int lx = x - shell[0], lz = z - shell[2];
        if (z == shell[2] && (mask & 1) != 0) return lx;
        if (z == shell[5] && (mask & 2) != 0) return lx;
        if (x == shell[0] && (mask & 4) != 0) return lz;
        if (x == shell[3] && (mask & 8) != 0) return lz;
        return -1;
    }

    private void fillShell(int[] arr, int i, BlockState s) {
        for (int x = arr[i]; x <= arr[i + 3]; x++)
            for (int y = arr[i + 1]; y <= arr[i + 4]; y++)
                for (int z = arr[i + 2]; z <= arr[i + 5]; z++) set(x, y, z, s);
    }

    private void buildFoundation() {
        int x0 = Math.max(shell[0], chunk.minX()), x1 = Math.min(shell[3], chunk.maxX());
        int z0 = Math.max(shell[2], chunk.minZ()), z1 = Math.min(shell[5], chunk.maxZ());
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                for (int y = shell[1] - 1; y > shell[1] - 40; y--) {
                    if (round() && radius(x, z) > SPIRE_OUT) break;
                    BlockState s = get(x, y, z);
                    if (!(s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS))) break;
                    put(x, y, z, h(x, y, z, 3) < 0.3 ? kind.cracked() : kind.wall());
                }
    }

    private void stairRun(int ox, int oy, int oz, int strip) {
        if (kind.floorHeight() > 9) {
            switchback(ox, oy, oz, strip);
            return;
        }
        int zs = strip == 0 ? oz + 2 : oz + 11;
        BlockState step = kind.stairs().setValue(StairBlock.FACING, Direction.EAST);
        for (int k = 0; k <= 8; k++) {
            int x = ox + 2 + k, y = oy + 1 + k;
            for (int z = zs; z <= zs + 1; z++) {
                set(x, y, z, step);
                for (int a = 1; a <= 3; a++) set(x, y + a, z, Blocks.AIR.defaultBlockState());
                if (k > 0) set(x, y - 1, z, kind.wall());
            }
        }
        // Landing space at both ends.
        for (int z = zs; z <= zs + 1; z++)
            for (int a = 1; a <= 3; a++) {
                set(ox + 1, oy + a, z, Blocks.AIR.defaultBlockState());
                set(ox + 11, oy + 9 + a, z, Blocks.AIR.defaultBlockState());
                set(ox + 12, oy + 9 + a, z, Blocks.AIR.defaultBlockState());
            }
    }

    // The Stormspire's tall floors need two flights: 9 steps up along +x, a landing, then 8 steps back along -x in
    // the next strip over, arriving on the floor above.
    private void switchback(int ox, int oy, int oz, int strip) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int zs = strip == 0 ? oz + 2 : oz + 11, zs2 = strip == 0 ? oz + 4 : oz + 9;
        BlockState up = kind.stairs().setValue(StairBlock.FACING, Direction.EAST), back = kind.stairs().setValue(StairBlock.FACING, Direction.WEST);
        for (int k = 0; k <= 8; k++) {
            int x = ox + 2 + k, y = oy + 1 + k;
            for (int z = zs; z <= zs + 1; z++) {
                set(x, y, z, up);
                for (int a = 1; a <= 3; a++) set(x, y + a, z, air);
                for (int b = y - 1; b > oy; b--) set(x, b, z, kind.wall());
            }
        }
        int zlo = Math.min(zs, zs2), zhi = Math.max(zs, zs2) + 1;
        for (int x = ox + 11; x <= ox + 12; x++)
            for (int z = zlo; z <= zhi; z++) {
                for (int b = oy + 9; b > oy; b--) set(x, b, z, kind.wall());
                for (int a = 1; a <= 3; a++) set(x, oy + 9 + a, z, air);
            }
        for (int j = 0; j <= 7; j++) {
            int x = ox + 10 - j, y = oy + 10 + j;
            for (int z = zs2; z <= zs2 + 1; z++) {
                set(x, y, z, back);
                for (int a = 1; a <= 3; a++) set(x, y + a, z, air);
                set(x, y - 1, z, kind.wall());
            }
        }
        for (int z = zs2; z <= zs2 + 1; z++)
            for (int a = 1; a <= 3; a++) {
                set(ox + 2, oy + 17 + a, z, air);
                set(ox + 1, oy + 17 + a, z, air);
                set(ox + 1, oy + a, zs + (z - zs2), air);
            }
    }

    // The lowest open block above solid ground in a column of this room, or -1.
    private int floorY(int x, int z) {
        for (int y = shell[1] + 1; y <= shell[4] - 2; y++) {
            if (open(x, y, z) && (y == shell[1] + 1 || !open(x, y - 1, z)) && open(x, y + 1, z)) return y;
        }
        return -1;
    }

    // ---- hole in a floor between stacked rooms (pure function of the column, so both rooms agree) ----

    private static boolean inHole(int x, int z, int ox, int oz) {
        double cx = ox + 7.5, cz = oz + 7.5;
        double a = Math.atan2(z + 0.5 - cz, x + 0.5 - cx);
        double r = 3.4 + 0.9 * Math.sin(a * 3 + ((ox * 31 + oz * 17) & 7));
        double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
        return dx * dx + dz * dz < r * r;
    }

    private void holes(BlockState bridge) {
        int ox = shell[0], oz = shell[2];
        for (int x = ox + 1; x < shell[3]; x++)
            for (int z = oz + 1; z < shell[5]; z++) {
                if (!inHole(x, z, ox, oz) || blocked(x, z)) continue;
                if ((flags & HOLE_DOWN) != 0) {
                    if (conn(x, z) <= 1.1) set(x, shell[1], z, bridge);
                    else if (open(x, shell[1] + 1, z)) set(x, shell[1], z, Blocks.AIR.defaultBlockState());
                }
                if ((flags & HOLE_UP) != 0) {
                    for (int y = shell[4]; y > shell[1]; y--) {
                        if (y < shell[4] && open(x, y, z)) break;
                        set(x, y, z, Blocks.AIR.defaultBlockState());
                    }
                }
            }
    }

    // ==== per-dungeon features ====

    // Stormspire: balconies open to the storm on outer walls (a knee-high rail and a long drop), windows into the
    // lightning core, broken floors, Storm Conductors on the balconies.
    private void spireFeatures() {
        BlockState conductor = DungeonBlocks.STORM_CONDUCTOR.get().defaultBlockState();
        if (room == DungeonLayout.Room.BOSS && center.length == 2) {
            for (int i = 0; i < 4; i++) {
                double ang = Math.PI / 4 + i * Math.PI / 2;
                int x = center[0] + (int) Math.round(Math.cos(ang) * 15), z = center[1] + (int) Math.round(Math.sin(ang) * 15);
                if (!blocked(x, z) && conn(x, z) > 1.5) set(x, fy(), z, conductor);
            }
            return;
        }
        if (is(DungeonLayout.Room.TREASURE, DungeonLayout.Room.CORE) || !round()) return;
        // A Storm Conductor near the outer wall, where the gaps open over the drop.
        if (h(shell[0], 1, shell[2], 50) < 0.6) {
            for (int t = 0; t < 12; t++) {
                int x = shell[0] + 1 + (int) (h(shell[0], t, shell[2], 58) * 13), z = shell[2] + 1 + (int) (h(shell[0], t, shell[2], 59) * 13);
                double r = radius(x, z);
                if (r < 17 || r > 19 || blocked(x, z) || conn(x, z) < 1.5 || floorY(x, z) != fy()) continue;
                set(x, fy(), z, conductor);
                break;
            }
        }
        holes(kind.cracked());
        rubble();
    }

    // Ashen Foundry: lava floors crossed by narrow walkways (the main path's walkways are glowing grates), furnace
    // halls, hanging chains and Crushers above the walkways.
    private boolean lavaRoom() {
        if (kind != DungeonKind.ASHEN_FOUNDRY) return false;
        if (is(DungeonLayout.Room.MOB, DungeonLayout.Room.TRAP, DungeonLayout.Room.VAULT, DungeonLayout.Room.BOSS)) return true;
        return room == DungeonLayout.Room.HALL && h(shell[0], 2, shell[2], 51) < 0.55;
    }

    private boolean walkable(int x, int z) {
        if (blocked(x, z) || conn(x, z) <= 1.25) return true;
        double dx = x + 0.5 - (cx() + 0.5), dz = z + 0.5 - (cz() + 0.5);
        double d = Math.sqrt(dx * dx + dz * dz);
        if (room == DungeonLayout.Room.BOSS) return d < 7.5 || d > 11.5;
        if (is(DungeonLayout.Room.MOB, DungeonLayout.Room.VAULT)) return d < 3.3;
        return false;
    }

    private void foundryFeatures() {
        if (is(DungeonLayout.Room.TREASURE, DungeonLayout.Room.ENTRANCE)) return;
        BlockState lava = Blocks.LAVA.defaultBlockState(), grate = DungeonBlocks.THEME.get(5).get().defaultBlockState();
        boolean lavaRoom = lavaRoom();
        for (int x = shell[0] + 1; x < shell[3]; x++)
            for (int z = shell[2] + 1; z < shell[5]; z++) {
                if (floorY(x, z) != fy()) continue;
                if (lavaRoom && !walkable(x, z)) set(x, shell[1], z, lava);
                else if (conn(x, z, true) <= 1.25) set(x, shell[1], z, grate);
            }
        if (lavaRoom) {
            // Crushers over the walkways.
            int crushers = room == DungeonLayout.Room.BOSS ? 4 : h(shell[0], 3, shell[2], 52) < 0.65 ? 1 + (h(shell[0], 4, shell[2], 53) < 0.4 ? 1 : 0) : 0;
            for (int i = 0; i < segs.size() && crushers > 0; i++) {
                double[] s = segs.get(i);
                if (i % 2 == 0 && room != DungeonLayout.Room.BOSS) continue;
                int x = (int) Math.floor((s[0] + s[2]) / 2), z = (int) Math.floor((s[1] + s[3]) / 2);
                int ceil = ceilingAbove(x, z);
                if (blocked(x, z) || ceil < 0) continue;
                set(x, ceil, z, DungeonBlocks.CRUSHER.get().defaultBlockState());
                crushers--;
            }
        }
        // Chains.
        for (int x = shell[0] + 2; x < shell[3] - 1; x++)
            for (int z = shell[2] + 2; z < shell[5] - 1; z++)
                if (h(x, 9, z, 54) < 0.025 && conn(x, z) > 1.5 && !blocked(x, z)) {
                    int ceil = ceilingAbove(x, z);
                    if (ceil < 0) continue;
                    int len = 2 + (int) (h(x, 8, z, 55) * 3);
                    for (int y = ceil - 1; y > ceil - 1 - len && y > fy(); y--) set(x, y, z, Blocks.IRON_CHAIN.defaultBlockState());
                }
    }

    // The first solid block above the open space of a column (the cave's ceiling), or -1.
    private int ceilingAbove(int x, int z) {
        int f = floorY(x, z);
        if (f < 0) return -1;
        for (int y = f; y < shell[4]; y++) if (!open(x, y, z)) return y;
        return shell[4];
    }

    // Great furnaces against the walls that have no doorway: a block of brick with a fire-filled mouth.
    private void furnaces() {
        for (int side = 0; side < 4; side++) {
            boolean hasDoor = false;
            for (int i = 0; i + 5 < doors.length; i += 6) {
                if ((side == 0 && doors[i + 2] <= shell[2]) || (side == 1 && doors[i + 5] >= shell[5])
                        || (side == 2 && doors[i] <= shell[0]) || (side == 3 && doors[i + 3] >= shell[3])) hasDoor = true;
            }
            if (hasDoor) continue;
            for (int a = 4; a <= 10; a++)
                for (int d = 1; d <= 2; d++)
                    for (int ly = 1; ly <= 5; ly++) {
                        int x = side == 2 ? shell[0] + d : side == 3 ? shell[3] - d : shell[0] + a;
                        int z = side == 0 ? shell[2] + d : side == 1 ? shell[5] - d : shell[2] + a;
                        if (blocked(x, z)) continue;
                        boolean mouth = a >= 6 && a <= 8 && ly <= 2 && d == 2;
                        BlockState s = mouth ? (ly == 1 ? Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true) : Blocks.AIR.defaultBlockState())
                                : (ly == 5 || a == 4 || a == 10) ? kind.wall() : DungeonBlocks.THEME.get(5).get().defaultBlockState();
                        if (mouth && ly == 1) set(x, shell[1], z, Blocks.MAGMA_BLOCK.defaultBlockState());
                        set(x, shell[1] + ly, z, s);
                    }
        }
    }

    // Aurora Sanctum: ice aisles (the trap rooms are all slippery blue ice), colonnades, frozen statues, Rime Totems.
    private void sanctumFeatures() {
        if (is(DungeonLayout.Room.TREASURE, DungeonLayout.Room.ENTRANCE)) return;
        for (int x = shell[0] + 1; x < shell[3]; x++)
            for (int z = shell[2] + 1; z < shell[5]; z++) {
                if (blocked(x, z) || floorY(x, z) != fy()) continue;
                if (room == DungeonLayout.Room.TRAP) set(x, shell[1], z, Blocks.BLUE_ICE.defaultBlockState());
                else if (conn(x, z) <= 1.6) set(x, shell[1], z, Blocks.PACKED_ICE.defaultBlockState());
            }
        boolean arena = room == DungeonLayout.Room.BOSS;
        double rx = (shell[3] - shell[0]) / 2.0 - (arena ? 1.2 : 0.8);
        // Columns around the hall, and frozen statues between them.
        int n = arena ? 16 : 8;
        for (int i = 0; i < n * 2; i++) {
            boolean statue = (i & 1) == 1;
            double ang = Math.PI * i / n + Math.PI / 8;
            double rr = rx - (statue ? 0.7 : 1.7);
            int x = (int) Math.floor(cx() + 0.5 + Math.cos(ang) * rr), z = (int) Math.floor(cz() + 0.5 + Math.sin(ang) * rr);
            if (blocked(x, z) || floorY(x, z) != fy() || conn(x, z) < (statue ? 2.5 : 2.2)) continue;
            if (arena && Math.hypot(x - cx(), z - cz()) < 10.5) continue;
            if (statue) {
                if (h(x, 0, z, 56) < 0.3 || room == DungeonLayout.Room.TRAP) continue;
                set(x, fy(), z, kind.tile());
                set(x, fy() + 1, z, Blocks.PACKED_ICE.defaultBlockState());
                set(x, fy() + 2, z, Blocks.PACKED_ICE.defaultBlockState());
                set(x, fy() + 3, z, Blocks.BLUE_ICE.defaultBlockState());
            } else {
                for (int y = fy(); y < shell[4] && open(x, y, z); y++) set(x, y, z, y == fy() || !open(x, y + 1, z) ? kind.accent() : kind.tile());
            }
        }
        if (arena) {
            for (int x = shell[0] + 1; x < shell[3]; x++)
                for (int z = shell[2] + 1; z < shell[5]; z++) {
                    double d = Math.hypot(x - cx(), z - cz());
                    if (floorY(x, z) != fy()) continue;
                    if (d < 8) set(x, shell[1], z, Blocks.BLUE_ICE.defaultBlockState());
                    else if (d < 10) set(x, shell[1], z, Blocks.PACKED_ICE.defaultBlockState());
                }
            set(icx() - 6, fy(), icz() - 6, DungeonBlocks.RIME_TOTEM.get().defaultBlockState());
            set(icx() + 6, fy(), icz() + 6, DungeonBlocks.RIME_TOTEM.get().defaultBlockState());
        } else if (is(DungeonLayout.Room.HALL, DungeonLayout.Room.TRAP) && h(shell[0], 5, shell[2], 57) < 0.55) {
            int x = icx() + 3, z = icz() + 3;
            if (!blocked(x, z) && conn(x, z) > 1.6 && floorY(x, z) == fy()) set(x, fy(), z, DungeonBlocks.RIME_TOTEM.get().defaultBlockState());
        }
    }

    // Prism Vault: crystal growths, prismite columns, chasms with crystal bridges, Hanging Prisms over the tunnels.
    private void prismFeatures() {
        if (is(DungeonLayout.Room.TREASURE, DungeonLayout.Room.ENTRANCE)) return;
        holes(DungeonBlocks.THEME.get(11).get().defaultBlockState());
        for (int x = shell[0] + 1; x < shell[3]; x++)
            for (int z = shell[2] + 1; z < shell[5]; z++) {
                if (blocked(x, z)) continue;
                int f = floorY(x, z);
                if (f > 0 && conn(x, z) > 1.5 && h(x, 0, z, 60) < 0.05)
                    set(x, f, z, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
                for (int y = shell[4] - 1; y > shell[1] + 2; y--) {
                    if (open(x, y, z)) {
                        if (!open(x, y + 1, z) && h(x, y, z, 61) < 0.05)
                            set(x, y, z, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN));
                        break;
                    }
                }
            }
        // Prismite columns in the caverns.
        int columns = room == DungeonLayout.Room.BOSS ? 6 : room == DungeonLayout.Room.HALL ? 0 : 2;
        for (int i = 0; i < columns * 4 && columns > 0; i++) {
            int x = shell[0] + 2 + (int) (h(shell[0], i, shell[2], 62) * (shell[3] - shell[0] - 3));
            int z = shell[2] + 2 + (int) (h(shell[0], i, shell[2], 63) * (shell[5] - shell[2] - 3));
            if (blocked(x, z) || conn(x, z) < 2.0 || floorY(x, z) < 0) continue;
            if (room == DungeonLayout.Room.BOSS && Math.abs(x - icx()) < 4 && Math.abs(z - icz() - 5) < 5) continue;
            for (int y = floorY(x, z); y < shell[4] && open(x, y, z); y++) set(x, y, z, ModBlocks.PRISMITE_BLOCK.get().defaultBlockState());
            columns--;
        }
        // Hanging Prisms above the tunnels.
        int prisms = room == DungeonLayout.Room.BOSS ? 6 : 1 + (int) (h(shell[0], 6, shell[2], 64) * 3);
        for (int i = 0; i < segs.size() && prisms > 0; i++) {
            double[] s = segs.get(i);
            double t = 0.3 + h(shell[0], i, shell[2], 65) * 0.4;
            int x = (int) Math.floor(s[0] + (s[2] - s[0]) * t), z = (int) Math.floor(s[1] + (s[3] - s[1]) * t);
            if (blocked(x, z)) continue;
            for (int y = shell[4] - 1; y > shell[1] + 3; y--) {
                if (open(x, y, z) && !open(x, y + 1, z)) {
                    set(x, y, z, DungeonBlocks.HANGING_PRISM.get().defaultBlockState());
                    prisms--;
                    break;
                }
            }
        }
    }

    private void rubble() {
        for (int x = shell[0] + 1; x < shell[3]; x++)
            for (int z = shell[2] + 1; z < shell[5]; z++) {
                if (blocked(x, z) || conn(x, z) < 1.3) continue;
                double n = h(x, 5, z, 14);
                if (n < 0.025) set(x, fy(), z, kind.cracked());
                else if (n < 0.04) set(x, top(), z, Blocks.COBWEB.defaultBlockState());
            }
    }

    // ==== the main path's lights ====

    private BlockState lantern() {
        return switch (kind) {
            case STORMSPIRE -> Blocks.SOUL_LANTERN.defaultBlockState();
            case ASHEN_FOUNDRY -> Blocks.LANTERN.defaultBlockState();
            case AURORA_SANCTUM -> Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP);
            case PRISM_VAULT -> Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP);
        };
    }

    private void pathLights() {
        if (room == DungeonLayout.Room.CORE) return;
        // Sconces beside the path's doorways (set into solid wall).
        for (int i = 0; i + 5 < pathDoors.length && kind != DungeonKind.STORMSPIRE; i += 6) {
            int[] d = pathDoors;
            boolean alongX = d[i + 3] - d[i] == 1; // the doorway crosses an x wall: it runs along z
            int y = d[i + 1] + 2;
            for (int wx = d[i]; wx <= d[i + 3]; wx++)
                for (int wz = d[i + 2]; wz <= d[i + 5]; wz++) {
                    if (!(inShell(wx, y, wz) && (wx == shell[0] || wx == shell[3] || wz == shell[2] || wz == shell[5]))) continue;
                    if (alongX) {
                        if (wz == d[i + 2] && !mouth(wx, y, wz - 2)) set(wx, y, wz - 2, kind.light());
                        if (wz == d[i + 5] && !mouth(wx, y, wz + 2)) set(wx, y, wz + 2, kind.light());
                    } else {
                        if (wx == d[i] && !mouth(wx - 2, y, wz)) set(wx - 2, y, wz, kind.light());
                        if (wx == d[i + 3] && !mouth(wx + 2, y, wz)) set(wx + 2, y, wz, kind.light());
                    }
                }
        }
        // Lanterns along the way, every few blocks, alternating sides.
        double off = kind == DungeonKind.ASHEN_FOUNDRY ? 1.0 : 2.0;
        int n = 0;
        for (double[] s : segs) {
            if (s[4] <= 0) continue;
            double dx = s[2] - s[0], dz = s[3] - s[1], len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1) continue;
            double px = -dz / len, pz = dx / len;
            for (double t = 2; t < len; t += 4, n++) {
                double side = (n & 1) == 0 ? off : -off;
                int x = (int) Math.floor(s[0] + dx * t / len + px * side), z = (int) Math.floor(s[1] + dz * t / len + pz * side);
                if (x <= shell[0] || x >= shell[3] || z <= shell[2] || z >= shell[5] || blocked(x, z)) continue;
                if (lavaRoom() && !walkable(x, z)) continue;
                if ((flags & HOLE_DOWN) != 0 && inHole(x, z, shell[0], shell[2])) continue;
                int y = floorY(x, z);
                if (y > 0) set(x, y, z, lantern());
            }
        }
        // Ceiling lights in the box-shaped rooms.
        if (!roofOpen && kind == DungeonKind.STORMSPIRE) {
            set(icx(), shell[4], icz(), kind.light());
            if (shell[3] - shell[0] > 20)
                for (int x = shell[0] + 7; x < shell[3]; x += 8)
                    for (int z = shell[2] + 7; z < shell[5]; z += 8) set(x, shell[4], z, kind.light());
        }
    }

    // ==== rooms ====

    private void cage(int x, int z, int count) {
        if (blocked(x, z)) return;
        int y = floorY(x, z);
        if (y < 0 || (lavaRoom() && !walkable(x, z))) return;
        if (!set(x, y, z, DungeonBlocks.DUNGEON_SPAWNER.get().defaultBlockState())) return;
        set(x, y - 1, z, kind.accent());
        if (lvl.getBlockEntity(new BlockPos(x, y, z)) instanceof DungeonSpawnerBlockEntity cage) {
            List<String> ids = new ArrayList<>();
            for (var t : kind.mobs()) ids.add(BuiltInRegistries.ENTITY_TYPE.getKey(t.get()).toString());
            cage.configure(ids, count, h(x, y, z, 15) < 0.25);
        }
    }

    private void chest(int x, int z, Direction facing, String table) {
        if (blocked(x, z)) return;
        int y = floorY(x, z);
        if (y < 0) return;
        if (!set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing))) return;
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "chests/" + kind.id + "_" + table));
        RandomizableContainer.setBlockEntityLootTable(lvl, RandomSource.create(seed ^ BlockPos.asLong(x, y, z)), new BlockPos(x, y, z), key);
    }

    private int mobs() {
        return 3 + level / 2 + (h(shell[0], 7, shell[2], 16) < 0.4 ? 1 : 0);
    }

    private void mobRoom() {
        cage(icx(), icz(), mobs());
        if (h(shell[0], 3, shell[2], 17) < 0.5) cage(shell[0] + 4, shell[2] + 10, Math.max(2, mobs() - 1));
    }

    private void trapRoom() {
        for (int x = shell[0] + 1; x < shell[3]; x++)
            for (int z = shell[2] + 1; z < shell[5]; z++) {
                if (blocked(x, z) || h(x, 0, z, 18) >= 0.32 || floorY(x, z) != fy()) continue;
                if (lavaRoom() && !walkable(x, z)) continue;
                set(x, shell[1], z, DungeonBlocks.ARC_TRAP.get().defaultBlockState());
            }
        int coils = 2 + (h(shell[0], 4, shell[2], 19) < 0.5 ? 1 : 0) + (level > 1 ? 1 : 0);
        int[][] spots = {{3, 3}, {11, 11}, {3, 11}, {11, 3}};
        for (int i = 0; i < Math.min(coils, 4); i++) {
            int x = shell[0] + spots[i][0], z = shell[2] + spots[i][1];
            if (blocked(x, z) || (lavaRoom() && !walkable(x, z))) continue;
            int y = floorY(x, z);
            if (y < 0) continue;
            set(x, y, z, kind.tile());
            set(x, y + 1, z, DungeonBlocks.TESLA_COIL.get().defaultBlockState());
        }
        if (h(shell[0], 6, shell[2], 20) < 0.4) cage(icx(), icz(), Math.max(2, mobs() - 1));
    }

    private void vaultRoom() {
        for (int x = icx() - 2; x <= icx() + 2; x++)
            for (int z = icz() - 2; z <= icz() + 2; z++)
                if (floorY(x, z) == fy() && !blocked(x, z)) set(x, shell[1], z, kind.accent());
        chest(icx() - 1, icz(), Direction.SOUTH, "vault");
        chest(icx() + 1, icz(), Direction.SOUTH, "vault");
        if (h(shell[0], 8, shell[2], 21) < 0.7) cage(icx(), icz() - 3, mobs());
    }

    private void entrance() {
        if (shaftTop != Integer.MIN_VALUE) shaft();
        if (outDoor >= 0) outsideDoor();
    }

    private void shaft() {
        int sx = icx(), sz = icz();
        BlockState air = Blocks.AIR.defaultBlockState();
        // A column inside the room to hold the ladder.
        for (int y = fy(); y < shell[4]; y++) set(sx, y, sz - 2, kind.wall());
        for (int y = shell[4]; y <= shaftTop + 5; y++)
            for (int x = sx - 2; x <= sx + 2; x++)
                for (int z = sz - 2; z <= sz + 2; z++) {
                    boolean ring = Math.abs(x - sx) == 2 || Math.abs(z - sz) == 2;
                    if (y < shaftTop) put(x, y, z, ring ? kind.wall() : air);
                }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = fy(); y <= shaftTop; y++) {
            if (y < shell[4]) set(sx, y, sz - 1, ladder);
            else put(sx, y, sz - 1, ladder);
        }
        // Gatehouse: 9x9, floor at shaftTop-1, walls 5 high with an opening on each side, a roof and lights.
        int fyG = shaftTop - 1;
        for (int x = sx - 4; x <= sx + 4; x++)
            for (int z = sz - 4; z <= sz + 4; z++) {
                boolean hole = Math.abs(x - sx) <= 1 && Math.abs(z - sz) <= 1;
                boolean edge = Math.abs(x - sx) == 4 || Math.abs(z - sz) == 4;
                boolean corner = Math.abs(x - sx) == 4 && Math.abs(z - sz) == 4;
                boolean doorway = (Math.abs(x - sx) <= 1 || Math.abs(z - sz) <= 1) && edge;
                if (!hole) put(x, fyG, z, kind.floor());
                for (int y = fyG - 4; y < fyG; y++) {
                    BlockState s = get(x, y, z);
                    if (!hole && (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty())) put(x, y, z, kind.wall());
                }
                for (int y = fyG + 1; y <= fyG + 5; y++) {
                    if (y == fyG + 5) put(x, y, z, edge ? kind.wall() : (x == sx && z == sz ? kind.light() : kind.tile()));
                    else if (corner) put(x, y, z, kind.accent());
                    else if (edge && !(doorway && y <= fyG + 3)) put(x, y, z, kind.wall());
                    else put(x, y, z, air);
                }
            }
        put(sx, shaftTop, sz - 2, kind.wall());
        put(sx, shaftTop, sz - 1, ladder);
        put(sx, fyG + 6, sz, Blocks.LIGHTNING_ROD.defaultBlockState());
    }

    private void outsideDoor() {
        BlockState air = Blocks.AIR.defaultBlockState();
        int cx = icx(), cz = icz();
        int nx = outDoor == 2 ? -1 : outDoor == 3 ? 1 : 0, nz = outDoor == 0 ? -1 : outDoor == 1 ? 1 : 0;
        int wx = nx < 0 ? shell[0] : nx > 0 ? shell[3] : cx, wz = nz < 0 ? shell[2] : nz > 0 ? shell[5] : cz;
        // A passage through the wall and a causeway out to open ground, cut through any hillside in the way.
        for (int a = -1; a <= 1; a++)
            for (int o = -6; o <= 9; o++) {
                int x = wx + (nz != 0 ? a : 0) + nx * o, z = wz + (nx != 0 ? a : 0) + nz * o;
                put(x, shell[1], z, o > 0 ? kind.tile() : kind.floor());
                for (int y = fy(); y <= fy() + 3; y++) put(x, y, z, air);
                if (o > 0) for (int y = shell[1] - 1; y > shell[1] - 6; y--) put(x, y, z, kind.wall());
            }
        // An archway at the wall, and two tall beacons at the end of the causeway, lit so the way in is easy to find.
        for (int y = fy(); y <= fy() + 4; y++)
            for (int a : new int[] {-2, 2}) put(wx + (nz != 0 ? a : 0) + nx, y, wz + (nx != 0 ? a : 0) + nz, kind.accent());
        for (int a = -2; a <= 2; a++) put(wx + (nz != 0 ? a : 0) + nx, fy() + 4, wz + (nx != 0 ? a : 0) + nz, kind.tile());
        for (int a : new int[] {-2, 2}) {
            int x = wx + (nz != 0 ? a : 0) + nx * 9, z = wz + (nx != 0 ? a : 0) + nz * 9;
            for (int y = fy(); y <= fy() + 6; y++) put(x, y, z, y == fy() + 6 ? kind.light() : kind.wall());
            put(x, fy() + 7, z, Blocks.LIGHTNING_ROD.defaultBlockState());
        }
    }

    private void arena() {
        int cx = icx(), cz = icz();
        int rx = (shell[3] - shell[0]) / 2, rz = (shell[5] - shell[2]) / 2;
        if (kind == DungeonKind.STORMSPIRE) {
            for (int x = shell[0] + 1; x < shell[3]; x++)
                for (int z = shell[2] + 1; z < shell[5]; z++) {
                    if (blocked(x, z)) continue;
                    double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                    if (Math.abs(d - 4) < 0.6 || Math.abs(d - 10) < 0.6) set(x, shell[1], z, kind.accent());
                }
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int x = cx + (int) Math.round(Math.cos(a) * (rx - 4)), z = cz + (int) Math.round(Math.sin(a) * (rz - 4));
                if (blocked(x, z) || conn(x, z) < 2.5) continue;
                for (int y = fy(); y <= fy() + 9; y++) {
                    set(x, y, z, y % 4 == 0 ? kind.tile() : kind.wall());
                    set(x + 1, y, z, kind.wall());
                    set(x, y, z + 1, kind.wall());
                    set(x + 1, y, z + 1, kind.wall());
                }
                set(x, fy() + 10, z, Blocks.LIGHTNING_ROD.defaultBlockState());
            }
        } else if (kind == DungeonKind.ASHEN_FOUNDRY) {
            for (int[] p : new int[][] {{3, 3}, {3, -3}, {-3, 3}, {-3, -3}}) {
                int x = p[0] > 0 ? shell[0] + p[0] : shell[3] + p[0], z = p[1] > 0 ? shell[2] + p[1] : shell[5] + p[1];
                if (blocked(x, z)) continue;
                for (int y = fy(); y < shell[4]; y++)
                    for (int a = 0; a <= 1; a++)
                        for (int b = 0; b <= 1; b++) set(x + a, y, z + b, y % 5 == 0 ? DungeonBlocks.THEME.get(5).get().defaultBlockState() : kind.wall());
            }
        }
        int ay = floorY(cx, cz);
        if (ay < 0) ay = fy();
        if (set(cx, ay, cz, DungeonBlocks.DUNGEON_ALTAR.get().defaultBlockState())
                && lvl.getBlockEntity(new BlockPos(cx, ay, cz)) instanceof DungeonAltarBlockEntity altar && vault.length == 6) {
            altar.configure(kind, new BoundingBox(vault[0], vault[1], vault[2], vault[3], vault[4], vault[5]));
        }
        // Clear room for the guardian to stand.
        for (int x = cx - 2; x <= cx + 2; x++)
            for (int z = cz + 3; z <= cz + 7; z++)
                for (int y = ay; y <= ay + 5; y++) if (inShell(x, y, z) && y < shell[4]) set(x, y, z, Blocks.AIR.defaultBlockState());
        cage(shell[0] + 4, shell[2] + 4, 2);
        cage(shell[3] - 4, shell[5] - 4, 2);
    }

    private void treasure() {
        int[] v = vaultBox();
        int mx = (v[0] + v[3]) / 2, mz = (v[2] + v[5]) / 2;
        for (int x = mx - 2; x <= mx + 2; x++)
            for (int z = mz - 3; z <= mz + 3; z++) set(x, shell[1], z, kind.accent());
        chest(mx - 1, mz - 2, Direction.SOUTH, "treasure");
        chest(mx + 1, mz, Direction.SOUTH, "treasure");
        chest(mx - 1, mz + 2, Direction.SOUTH, "treasure");
        for (int[] p : new int[][] {{v[0] + 2, v[2] + 2}, {v[0] + 2, v[5] - 2}, {v[3] - 2, v[2] + 2}, {v[3] - 2, v[5] - 2}}) {
            for (int y = fy(); y <= top(); y++) set(p[0], y, p[1], y == top() ? kind.light() : kind.accent());
        }
    }

    // The Stormspire's lightning core: a hollow shaft through every floor, with a column of storm quartz up the
    // middle studded with end rods, seen through the windows of the rooms around it.
    private void core() {
        int cx = icx(), cz = icz();
        for (int x = cx - 2; x <= cx + 2; x++)
            for (int z = cz - 2; z <= cz + 2; z++) set(x, shell[1], z, kind.accent());
        for (int y = fy(); y < shell[4]; y++) {
            set(cx, y, cz, ModBlocks.STORMQUARTZ_BLOCK.get().defaultBlockState());
            if ((y - fy()) % 4 == 2) {
                set(cx + 1, y, cz, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.EAST));
                set(cx - 1, y, cz, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.WEST));
                set(cx, y, cz + 1, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.SOUTH));
                set(cx, y, cz - 1, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.NORTH));
            }
            if ((y - fy()) % kind.floorHeight() == kind.floorHeight() / 2) {
                for (int[] p : new int[][] {{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
                    set(cx + p[0], y, cz + p[1], Blocks.IRON_CHAIN.defaultBlockState());
                    set(cx + p[0], y + 1, cz + p[1], Blocks.IRON_CHAIN.defaultBlockState());
                    set(cx + p[0], y - 1, cz + p[1], Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
                }
            }
        }
    }
}
