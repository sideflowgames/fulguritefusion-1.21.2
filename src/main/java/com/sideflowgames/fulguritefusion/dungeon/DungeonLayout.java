package com.sideflowgames.fulguritefusion.dungeon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

// Lays out a dungeon on its cell grid and turns it into DungeonPieces.
//   1. reserve the boss arena, the treasure vault beside it, and (Stormspire) the hollow lightning core
//   2. grow a random tree of rooms from the entrance, with stairs between levels
//   3. connect the arena and the vault (the vault's doorway is a Storm Seal), add a few loops
//   4. find the main path from the entrance to the arena: only it is lit; everything else is a dark side branch
//   5. dead ends become loot vaults; the rest are mob rooms, trap rooms and halls; some floors are broken
final class DungeonLayout {
    static final int CELL = 15;
    private static final int[][] DIRS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}; // N S W E

    private final DungeonKind k;
    private final RandomSource r;
    private final int x0, z0, base, H;
    private final boolean[][][] used, boss, core;
    private final Room[][][] type;
    private final Set<Long> edges = new HashSet<>(), stairs = new HashSet<>(), sealed = new HashSet<>();
    private final Set<Long> holes = new HashSet<>();      // broken floors between vertically adjacent rooms
    private final Set<Long> pathEdges = new HashSet<>();  // links on the main path (doors and stairs)
    private final Set<Integer> pathCells = new HashSet<>();
    private final Set<Long> arenaLinks = new HashSet<>(); // links into the arena (door or stair)
    private int count;
    private int[] entrance, vault;
    private int bx, bz, bl;

    private DungeonLayout(DungeonKind k, RandomSource r, int x0, int z0, int base) {
        this.k = k;
        this.r = r;
        this.x0 = x0;
        this.z0 = z0;
        this.base = base;
        this.H = k.floorHeight();
        used = new boolean[k.levels][k.width][k.depth];
        boss = new boolean[k.levels][k.width][k.depth];
        core = new boolean[k.levels][k.width][k.depth];
        type = new Room[k.levels][k.width][k.depth];
    }

    static void generate(DungeonKind k, Structure.GenerationContext ctx, StructurePiecesBuilder builder, int cx, int cz) {
        RandomSource r = ctx.random();
        int x0 = cx - k.width * CELL / 2, z0 = cz - k.depth * CELL / 2;
        int wx = k.width * CELL, wz = k.depth * CELL;
        int base;
        if (k.tower) {
            // The Stormspire stands with its door at ground level: its base is the ground just outside the entrance.
            base = DungeonStructure.height(ctx, x0 + wx / 2, z0 + wz + 3) - 1;
        } else if (k.surface) {
            int min = Integer.MAX_VALUE;
            for (int[] p : new int[][] {{0, 0}, {wx - 1, 0}, {0, wz - 1}, {wx - 1, wz - 1}, {wx / 2, wz / 2}}) {
                min = Math.min(min, DungeonStructure.height(ctx, x0 + p[0], z0 + p[1]));
            }
            base = min - 1;
        } else {
            int ground = DungeonStructure.height(ctx, cx, cz);
            int minY = ctx.heightAccessor().getMinY() + 8 + k.levels * k.floorHeight();
            base = Mth.clamp(ground - 34, minY, 70);
        }
        DungeonLayout l = new DungeonLayout(k, r, x0, z0, base);
        l.plan();
        l.emit(ctx, builder);
    }

    int y(int level) {
        return k.tower ? base + level * H : base - level * H;
    }

    // ---- planning ----

    // The Stormspire is built by hand: a round tower of four floors, each a ring of eight rooms around the hollow
    // lightning core, one stair up per floor (alternating north and south of the core), the arena on top and the
    // vault on the east side of the top floor, sealed onto the core.
    private static final int[][] RING = {{0, 0}, {1, 0}, {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {0, 1}};

    private void planTower() {
        int W = k.width, D = k.depth, L = k.levels;
        bx = 0;
        bz = 0;
        bl = L - k.bossH;
        for (int l = bl; l < L; l++)
            for (int x = 0; x < W; x++)
                for (int z = 0; z < D; z++) boss[l][x][z] = true;
        for (int l = 0; l < bl; l++) core[l][1][1] = true;
        entrance = new int[] {1, 0, 2};
        vault = new int[] {2, bl - 1, 1};
        for (int l = 0; l < bl; l++) {
            for (int[] c : RING) if (!(l == bl - 1 && c[0] == 2 && c[1] == 1)) mark(new int[] {c[0], l, c[1]});
            List<Long> ring = new ArrayList<>();
            for (int i = 0; i < RING.length; i++) {
                int[] a = {RING[i][0], l, RING[i][1]}, b = {RING[(i + 1) % RING.length][0], l, RING[(i + 1) % RING.length][1]};
                if (used(a) && used(b)) ring.add(key(a, b));
            }
            // Break the loop once (twice on the big floors) so there are dead ends to hide vaults in.
            if (ring.size() == RING.length) ring.remove(r.nextInt(ring.size()));
            if (ring.size() > 5 && r.nextBoolean()) ring.remove(r.nextInt(ring.size()));
            edges.addAll(ring);
        }
        for (int l = 0; l < bl; l++) {
            int z = (l % 2 == 0) ? 0 : 2;
            long st = key(new int[] {1, l, z}, new int[] {1, l + 1, z});
            stairs.add(st);
            if (l + 1 == bl) arenaLinks.add(st);
        }
        sealed.add(key(vault, new int[] {1, bl - 1, 1}));
    }

    private void plan() {
        if (k.tower) {
            planTower();
            finishPlan();
            return;
        }
        int W = k.width, D = k.depth, L = k.levels;
        if (k.tower) {
            bx = 0;
            bz = 0;
        } else {
            bx = r.nextBoolean() ? 0 : W - k.bossW;
            bz = r.nextInt(D - k.bossD + 1);
        }
        bl = L - k.bossH;
        for (int l = bl; l < bl + k.bossH; l++)
            for (int x = bx; x < bx + k.bossW; x++)
                for (int z = bz; z < bz + k.bossD; z++) boss[l][x][z] = true;
        if (k.tower) for (int l = 0; l < bl; l++) core[l][W / 2][D / 2] = true;
        int deep = L - 1;
        if (k.tower) {
            entrance = new int[] {W / 2, 0, D - 1};
            vault = new int[] {W / 2, bl - 1, 0};
        } else {
            entrance = new int[] {bx == 0 ? W - 1 : 0, 0, k.surface ? D / 2 : r.nextInt(D)};
            List<int[]> c = new ArrayList<>();
            for (int x = 0; x < W; x++)
                for (int z = 0; z < D; z++) {
                    if (boss[deep][x][z]) continue;
                    for (int[] d : DIRS) {
                        int nx = x + d[0], nz = z + d[1];
                        if (in(nx, nz) && boss[deep][nx][nz]) c.add(new int[] {x, deep, z});
                    }
                }
            vault = c.get(r.nextInt(c.size()));
        }
        // Grow.
        mark(entrance);
        List<int[]> cells = new ArrayList<>();
        cells.add(entrance);
        int n = W * D * L;
        boolean[] stairUp = new boolean[n], stairDown = new boolean[n];
        for (int tries = 0; tries < 10000 && count < k.rooms; tries++) {
            int[] from = cells.get(r.nextInt(cells.size()));
            int roll = r.nextInt(14);
            int[] to;
            boolean vertical = roll >= 12;
            if (!vertical) {
                int[] d = DIRS[roll % 4];
                to = new int[] {from[0] + d[0], from[1], from[2] + d[1]};
            } else {
                to = new int[] {from[0], from[1] + (roll == 12 ? 1 : -1), from[2]};
            }
            if (!in(to[0], to[2]) || to[1] < 0 || to[1] >= L || !free(to)) continue;
            if (vertical) {
                if (same(from, entrance) || same(to, entrance)) continue;
                int[] lo = lower(from, to), hi = lo == from ? to : from;
                if (stairUp[idx(lo)] || stairDown[idx(hi)] || stairDown[idx(lo)] || stairUp[idx(hi)]) continue;
                stairUp[idx(lo)] = true;
                stairDown[idx(hi)] = true;
                stairs.add(key(from, to));
            } else {
                edges.add(key(from, to));
            }
            mark(to);
            cells.add(to);
        }
        // Connect the arena.
        if (k.tower) {
            int[] lo = null;
            for (int[] c : new int[][] {{0, bl - 1, 0}, {W - 1, bl - 1, 0}, {0, bl - 1, D - 1}, {W - 1, bl - 1, D - 1}}) {
                if (used(c) && !stairUp[idx(c)] && !stairDown[idx(c)]) {
                    lo = c;
                    break;
                }
            }
            if (lo == null) lo = new int[] {W - 1, bl - 1, D - 1};
            if (!used(lo)) connect(lo);
            long s = key(lo, new int[] {lo[0], bl, lo[2]});
            stairs.add(s);
            arenaLinks.add(s);
        } else {
            boolean linked = false;
            for (int x = 0; x < W; x++)
                for (int z = 0; z < D; z++) {
                    if (!used[deep][x][z]) continue;
                    for (int[] d : DIRS) {
                        int nx = x + d[0], nz = z + d[1];
                        if (in(nx, nz) && boss[deep][nx][nz] && (!linked || r.nextInt(3) == 0)) {
                            long e = key(new int[] {x, deep, z}, new int[] {nx, deep, nz});
                            edges.add(e);
                            arenaLinks.add(e);
                            linked = true;
                        }
                    }
                }
            if (!linked) {
                int[] near = firstFreeBeside(deep);
                connect(near);
                for (int[] d : DIRS) {
                    int nx = near[0] + d[0], nz = near[2] + d[1];
                    if (in(nx, nz) && boss[deep][nx][nz]) {
                        long e = key(near, new int[] {nx, deep, nz});
                        edges.add(e);
                        arenaLinks.add(e);
                        break;
                    }
                }
            }
        }
        // Seal the vault.
        if (k.tower) {
            int[] nb = {0, vault[1], vault[2]};
            if (!used(nb)) connect(nb);
            sealed.add(key(vault, nb));
        } else {
            for (int[] d : DIRS) {
                int nx = vault[0] + d[0], nz = vault[2] + d[1];
                if (in(nx, nz) && boss[deep][nx][nz]) {
                    sealed.add(key(vault, new int[] {nx, deep, nz}));
                    break;
                }
            }
        }
        // Loops.
        for (int l = 0; l < L; l++)
            for (int x = 0; x < W; x++)
                for (int z = 0; z < D; z++) {
                    if (!used[l][x][z]) continue;
                    for (int[] d : new int[][] {{1, 0}, {0, 1}}) {
                        int nx = x + d[0], nz = z + d[1];
                        if (in(nx, nz) && used[l][nx][nz] && r.nextInt(8) == 0) edges.add(key(new int[] {x, l, z}, new int[] {nx, l, nz}));
                    }
                }
        finishPlan();
    }

    private void finishPlan() {
        int W = k.width, D = k.depth, L = k.levels;
        findPath();
        // Broken floors (Stormspire) and chasms (Prism Vault) between stacked rooms with no stairs.
        if (k == DungeonKind.STORMSPIRE || k == DungeonKind.PRISM_VAULT) {
            for (int l = 0; l + 1 < L; l++)
                for (int x = 0; x < W; x++)
                    for (int z = 0; z < D; z++) {
                        int[] a = {x, l, z}, b = {x, l + 1, z};
                        if (!used(a) || !used(b) || stairs.contains(key(a, b))) continue;
                        if (same(a, entrance) || same(b, entrance)) continue;
                        if (r.nextInt(100) < 35) holes.add(key(a, b));
                    }
        }
        // Room types.
        int vaults = 0;
        for (int l = 0; l < L; l++)
            for (int x = 0; x < W; x++)
                for (int z = 0; z < D; z++) {
                    if (!used[l][x][z]) continue;
                    int[] c = {x, l, z};
                    Room t;
                    if (same(c, entrance)) t = Room.ENTRANCE;
                    else if (degree(c) == 1 && !hasStairs(c) && vaults < 6 && !pathCells.contains(idx(c))) {
                        t = Room.VAULT;
                        vaults++;
                    } else {
                        int roll = r.nextInt(100);
                        t = roll < 42 ? Room.MOB : roll < 66 ? Room.TRAP : Room.HALL;
                    }
                    type[l][x][z] = t;
                }
    }

    // Breadth-first search from the entrance to the arena over doors and stairs.
    private void findPath() {
        Map<Integer, Integer> prev = new HashMap<>();
        Map<Integer, Long> via = new HashMap<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(entrance);
        prev.put(idx(entrance), -1);
        int[] end = null;
        long endLink = 0;
        while (!q.isEmpty() && end == null) {
            int[] c = q.poll();
            for (int[] nb : neighbours(c)) {
                long link = key(c, nb);
                if (boss[nb[1]][nb[0]][nb[2]]) {
                    if (arenaLinks.contains(link)) {
                        end = c;
                        endLink = link;
                        break;
                    }
                    continue;
                }
                if (!used(nb) || prev.containsKey(idx(nb))) continue;
                prev.put(idx(nb), idx(c));
                via.put(idx(nb), link);
                q.add(nb);
            }
        }
        if (end == null) return;
        pathEdges.add(endLink);
        int at = idx(end);
        while (at != -1) {
            pathCells.add(at);
            Long v = via.get(at);
            if (v != null) pathEdges.add(v);
            at = prev.get(at);
        }
    }

    private List<int[]> neighbours(int[] c) {
        List<int[]> out = new ArrayList<>();
        for (int[] d : DIRS) {
            int nx = c[0] + d[0], nz = c[2] + d[1];
            if (!in(nx, nz)) continue;
            int[] nb = {nx, c[1], nz};
            if (edges.contains(key(c, nb))) out.add(nb);
        }
        for (int dl : new int[] {-1, 1}) {
            int nl = c[1] + dl;
            if (nl < 0 || nl >= k.levels) continue;
            int[] nb = {c[0], nl, c[2]};
            if (stairs.contains(key(c, nb))) out.add(nb);
        }
        return out;
    }

    private void connect(int[] c) {
        int l = c[1];
        int[] best = null;
        int bd = Integer.MAX_VALUE;
        for (int x = 0; x < k.width; x++)
            for (int z = 0; z < k.depth; z++)
                if (used[l][x][z]) {
                    int d = Math.abs(x - c[0]) + Math.abs(z - c[2]);
                    if (d < bd) {
                        bd = d;
                        best = new int[] {x, l, z};
                    }
                }
        if (best == null) {
            mark(c);
            int[] other = {c[0], l + (l > 0 ? -1 : 1), c[2]};
            if (!used(other)) connect(other);
            stairs.add(key(c, other));
            return;
        }
        int[] cur = best.clone();
        for (int guard = 0; guard < 64 && !same(cur, c); guard++) {
            int[] next = cur.clone();
            if (cur[0] != c[0]) next[0] += Integer.signum(c[0] - cur[0]);
            else next[2] += Integer.signum(c[2] - cur[2]);
            if (blockedCell(next)) {
                next = cur.clone();
                if (cur[2] != c[2]) next[2] += Integer.signum(c[2] - cur[2]);
                else next[0] += (next[0] + 1 < k.width ? 1 : -1);
                if (!in(next[0], next[2]) || blockedCell(next)) break;
            }
            if (!used(next)) mark(next);
            edges.add(key(cur, next));
            cur = next;
        }
    }

    private boolean blockedCell(int[] c) {
        return boss[c[1]][c[0]][c[2]] || core[c[1]][c[0]][c[2]] || same(c, vault);
    }

    private int[] firstFreeBeside(int l) {
        for (int x = 0; x < k.width; x++)
            for (int z = 0; z < k.depth; z++)
                if (!boss[l][x][z] && !same(new int[] {x, l, z}, vault)) {
                    for (int[] d : DIRS) {
                        int nx = x + d[0], nz = z + d[1];
                        if (in(nx, nz) && boss[l][nx][nz]) return new int[] {x, l, z};
                    }
                }
        return new int[] {0, l, 0};
    }

    private boolean free(int[] c) {
        return !used(c) && !blockedCell(c);
    }

    private void mark(int[] c) {
        if (!used[c[1]][c[0]][c[2]]) {
            used[c[1]][c[0]][c[2]] = true;
            count++;
        }
    }

    private boolean used(int[] c) {
        return used[c[1]][c[0]][c[2]];
    }

    private boolean in(int x, int z) {
        return x >= 0 && z >= 0 && x < k.width && z < k.depth;
    }

    private int idx(int[] c) {
        return (c[1] * k.width + c[0]) * k.depth + c[2];
    }

    private static boolean same(int[] a, int[] b) {
        return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
    }

    private int[] lower(int[] a, int[] b) {
        return y(a[1]) < y(b[1]) ? a : b;
    }

    private long key(int[] a, int[] b) {
        long ia = idx(a), ib = idx(b);
        return Math.min(ia, ib) * 100000L + Math.max(ia, ib);
    }

    private int degree(int[] c) {
        return neighbours(c).size();
    }

    private boolean hasStairs(int[] c) {
        for (int dl : new int[] {-1, 1}) {
            int nl = c[1] + dl;
            if (nl >= 0 && nl < k.levels && stairs.contains(key(c, new int[] {c[0], nl, c[2]}))) return true;
        }
        return false;
    }

    // ---- pieces ----

    private int[] shell(int[] c) {
        int ox = x0 + c[0] * CELL, oy = y(c[1]), oz = z0 + c[2] * CELL;
        return new int[] {ox, oy, oz, ox + CELL - 1, oy + H - 1, oz + CELL - 1};
    }

    private int[] door(int[] a, int[] b, int floorY) {
        int[] lo = (a[0] < b[0] || a[2] < b[2]) ? a : b;
        int ox = x0 + lo[0] * CELL, oz = z0 + lo[2] * CELL;
        if (a[0] != b[0]) return new int[] {ox + CELL - 1, floorY + 1, oz + 6, ox + CELL, floorY + 4, oz + 8};
        return new int[] {ox + 6, floorY + 1, oz + CELL - 1, ox + 8, floorY + 4, oz + CELL};
    }

    private int[] stairOf(int[] a, int[] b) {
        int[] lo = lower(a, b);
        int phys = k.tower ? lo[1] : (k.levels - 1 - lo[1]);
        int strip = k.tower ? (lo[2] == 0 ? 1 : 0) : (lo[0] + lo[2] + phys) & 1; // the tower's stairs run beside the core
        return new int[] {x0 + lo[0] * CELL, y(lo[1]), z0 + lo[2] * CELL, strip};
    }

    // The Stormspire's axis (the middle block of the core); null for the other dungeons.
    private int[] center() {
        return k.tower ? new int[] {x0 + k.width * CELL / 2, z0 + k.depth * CELL / 2} : null;
    }

    private void emit(Structure.GenerationContext ctx, StructurePiecesBuilder builder) {
        long seed = r.nextLong();
        int L = k.levels, deep = L - 1;
        int ax0 = x0 + bx * CELL, az0 = z0 + bz * CELL;
        int ayBottom = k.tower ? y(bl) : y(deep);
        int[] arena = {ax0, ayBottom, az0, ax0 + k.bossW * CELL - 1, ayBottom + k.bossH * H - 1, az0 + k.bossD * CELL - 1};
        int[] vaultShell = shell(vault);
        List<int[]> arenaDoors = new ArrayList<>(), arenaStairs = new ArrayList<>();
        for (int l = 0; l < L; l++)
            for (int x = 0; x < k.width; x++)
                for (int z = 0; z < k.depth; z++) {
                    if (!used[l][x][z]) continue;
                    int[] c = {x, l, z};
                    List<int[]> doors = new ArrayList<>(), st = new ArrayList<>(), pDoors = new ArrayList<>(), pStairs = new ArrayList<>();
                    int ext = 0, coreMask = 0;
                    for (int i = 0; i < 4; i++) {
                        int nx = x + DIRS[i][0], nz = z + DIRS[i][1];
                        int[] nb = {nx, l, nz};
                        boolean inside = in(nx, nz);
                        long link = inside ? key(c, nb) : -1;
                        if (inside && (edges.contains(link) || sealed.contains(link))) {
                            int[] d = door(c, nb, y(l));
                            doors.add(d);
                            if (edges.contains(link) && boss[l][nx][nz]) arenaDoors.add(d);
                            if (pathEdges.contains(link)) pDoors.add(d);
                        }
                        if (inside && core[l][nx][nz]) coreMask |= 1 << i;
                        else if (!inside || (!used[l][nx][nz] && !boss[l][nx][nz] && !same(nb, vault))) ext |= 1 << i;
                    }
                    int flags = pathCells.contains(idx(c)) ? DungeonPiece.MAIN_PATH : 0;
                    for (int dl : new int[] {-1, 1}) {
                        int nl = l + dl;
                        if (nl < 0 || nl >= L) continue;
                        int[] nb = {x, nl, z};
                        long link = key(c, nb);
                        if (stairs.contains(link)) {
                            int[] s = stairOf(c, nb);
                            st.add(s);
                            if (boss[nl][x][z]) arenaStairs.add(s);
                            if (pathEdges.contains(link)) pStairs.add(s);
                        }
                        if (holes.contains(link)) flags |= lower(c, nb) == c ? DungeonPiece.HOLE_UP : DungeonPiece.HOLE_DOWN;
                    }
                    flags |= coreMask << 4;
                    Room t = type[l][x][z];
                    int[] sh = shell(c);
                    boolean foundation = k.surface && (k.tower ? l == 0 : l == 0 && (L < 2 || !used[1][x][z]) && !boss[Math.min(1, L - 1)][x][z]);
                    int shaftTop = Integer.MIN_VALUE, outDoor = -1;
                    if (t == Room.ENTRANCE) {
                        if (k.surface) {
                            for (int i = 0; i < 4; i++) if (!in(x + DIRS[i][0], z + DIRS[i][1])) outDoor = i;
                            if (outDoor < 0) outDoor = 1;
                        } else {
                            shaftTop = Math.max(sh[4] + 4, DungeonStructure.height(ctx, sh[0] + 7, sh[2] + 7));
                        }
                    }
                    builder.addPiece(new DungeonPiece(k, t, sh, doors, List.of(), st, pDoors, pStairs, k.surface ? ext : 0, flags,
                            foundation, false, shaftTop, outDoor, seed ^ ((long) idx(c) * 0x9E3779B97F4A7C15L), l, null, center()));
                }
        // The vault.
        {
            List<int[]> seals = new ArrayList<>();
            for (int[] dir : DIRS) {
                int[] nb = {vault[0] + dir[0], vault[1], vault[2] + dir[1]};
                if (in(nb[0], nb[2]) && sealed.contains(key(vault, nb))) {
                    int[] d = door(vault, nb, y(vault[1]));
                    seals.add(d);
                    if (boss[nb[1]][nb[0]][nb[2]]) arenaDoors.add(d);
                }
            }
            builder.addPiece(new DungeonPiece(k, Room.TREASURE, vaultShell, List.of(), seals, List.of(), List.of(), List.of(), 0, 0,
                    false, false, Integer.MIN_VALUE, -1, seed ^ 0x51ED, 0, null, center()));
        }
        // The Stormspire's lightning core: one hollow shaft through every floor below the arena.
        if (k.tower) {
            int cx = k.width / 2, cz = k.depth / 2;
            int[] s = shell(new int[] {cx, 0, cz});
            int[] coreShell = {s[0], y(0), s[2], s[3], y(bl - 1) + H - 1, s[5]};
            List<int[]> windows = new ArrayList<>();
            for (int l = 0; l < bl; l++)
                for (int i = 0; i < 4; i++) {
                    int nx = cx + DIRS[i][0], nz = cz + DIRS[i][1];
                    if (!used[l][nx][nz]) continue;
                    int fy = y(l);
                    // The core's outer walkway opens fully onto each room around it.
                    int[] w = switch (i) {
                        case 0 -> new int[] {s[0] + 1, fy + 1, s[2], s[3] - 1, fy + H - 2, s[2]};
                        case 1 -> new int[] {s[0] + 1, fy + 1, s[5], s[3] - 1, fy + H - 2, s[5]};
                        case 2 -> new int[] {s[0], fy + 1, s[2] + 1, s[0], fy + H - 2, s[5] - 1};
                        default -> new int[] {s[3], fy + 1, s[2] + 1, s[3], fy + H - 2, s[5] - 1};
                    };
                    windows.add(w);
                }
            windows.add(door(vault, new int[] {cx, vault[1], cz}, y(vault[1])));
            builder.addPiece(new DungeonPiece(k, Room.CORE, coreShell, windows, List.of(), List.of(), List.of(), List.of(), 0, 0,
                    false, false, Integer.MIN_VALUE, -1, seed ^ 0xC0DE, 0, null, center()));
        }
        builder.addPiece(new DungeonPiece(k, Room.BOSS, arena, arenaDoors, List.of(), arenaStairs, arenaDoors, arenaStairs, 0,
                DungeonPiece.MAIN_PATH, false, k.tower, Integer.MIN_VALUE, -1, seed ^ 0xB055, 0, vaultShell, center()));
    }

    enum Room { ENTRANCE, HALL, MOB, TRAP, VAULT, BOSS, TREASURE, CORE }
}
