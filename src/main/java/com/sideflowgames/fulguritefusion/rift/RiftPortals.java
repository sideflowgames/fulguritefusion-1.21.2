package com.sideflowgames.fulguritefusion.rift;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.stormreach.Stormreach;
import com.sideflowgames.fulguritefusion.stormreach.StormreachTravel;
import com.sideflowgames.fulguritefusion.titan.TitanTrials;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

// Stormreach rifts: the only way into the Stormreach.
//   Build: a rectangle of Rift Frame blocks (inside from 2 wide x 3 tall up to 21 x 21) standing upright, with a
//   Rift Keystone as one of the blocks in its top edge and a Lightning Rod on top of the keystone. When lightning
//   strikes the rod, the rift opens. Rifts open in the Overworld and in the Stormreach.
//   Use: walk in. Only players who have defeated the Storm Titan pass; anyone else is thrown back.
//   Linking: the first crossing builds a matching rift on the other side and remembers the pair, so walking back
//   through returns you to the rift you came from.
public final class RiftPortals {
    private RiftPortals() {}

    private static final int MAX = 21;
    private static final int LINK_RANGE = 24;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RiftPortals::onJoin);
    }

    // ---- opening ----

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt bolt) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD && !Stormreach.is(level)) return;
        BlockPos at = bolt.blockPosition();
        for (int k = 0; k <= 2; k++) {
            BlockPos rod = at.below(k);
            BlockState s = level.getBlockState(rod);
            if (s.is(Blocks.LIGHTNING_ROD) && s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING) == Direction.UP
                    && level.getBlockState(rod.below()).is(ModBlocks.RIFT_KEYSTONE.get())) {
                tryOpen(level, rod.below());
                return;
            }
        }
    }

    // Fill the frame under this keystone with rift, if the frame is whole. True if it opened.
    public static boolean tryOpen(ServerLevel level, BlockPos keystone) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Set<BlockPos> inside = interior(level, keystone.below(), axis);
            if (inside == null) continue;
            BlockState portal = ModBlocks.RIFT_PORTAL.get().defaultBlockState().setValue(RiftPortalBlock.AXIS, axis);
            for (BlockPos p : inside) level.setBlock(p, portal, 2 | 16);
            level.playSound(null, keystone, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0f, 0.6f);
            level.playSound(null, keystone, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8f, 1.4f);
            for (BlockPos p : inside) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.1);
            }
            return true;
        }
        return false;
    }

    // The open space inside a frame standing along `axis`, starting from `start`; null if it isn't a closed frame.
    private static Set<BlockPos> interior(ServerLevel level, BlockPos start, Direction.Axis axis) {
        if (!empty(level.getBlockState(start))) return null;
        Direction side = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction[] dirs = {Direction.UP, Direction.DOWN, side, side.getOpposite()};
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        int minH = 0, maxH = 0, minY = start.getY(), maxY = start.getY();
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (Direction d : dirs) {
                BlockPos n = p.relative(d);
                if (seen.contains(n)) continue;
                BlockState s = level.getBlockState(n);
                if (s.is(ModBlocks.RIFT_FRAME.get()) || s.is(ModBlocks.RIFT_KEYSTONE.get())) continue;
                if (!empty(s)) return null;
                seen.add(n);
                queue.add(n);
                int h = axis == Direction.Axis.X ? n.getX() - start.getX() : n.getZ() - start.getZ();
                minH = Math.min(minH, h);
                maxH = Math.max(maxH, h);
                minY = Math.min(minY, n.getY());
                maxY = Math.max(maxY, n.getY());
                if (maxH - minH >= MAX || maxY - minY >= MAX) return null;
            }
        }
        if (maxH - minH + 1 < 2 || maxY - minY + 1 < 3) return null;
        return seen;
    }

    private static boolean empty(BlockState s) {
        return s.isAir() || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || (s.canBeReplaced() && s.getFluidState().isEmpty());
    }

    // ---- crossing ----

    public static void enter(ServerPlayer player, BlockPos at) {
        if (player.isOnPortalCooldown() || player.isPassenger()) return;
        ServerLevel level = player.serverLevel();
        if (!Stormreach.allowed(player)) {
            player.displayClientMessage(Component.literal("The Stormreach is still in development. It's coming in Fulgurite Fusion 2.0.")
                    .withStyle(ChatFormatting.GRAY), true);
            player.setPortalCooldown(60);
            return;
        }
        if (!TitanTrials.defeated(player)) {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(-v.x * 3 - 0.2 * Math.signum(v.x), 0.35, -v.z * 3 - 0.2 * Math.signum(v.z));
            player.hurtMarked = true;
            level.playSound(null, at, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.8f, 1.6f);
            player.displayClientMessage(Component.literal("The rift crackles and throws you back. The storm doesn't know you yet.")
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
            player.setPortalCooldown(40);
            return;
        }
        ResourceKey<Level> otherKey = Stormreach.is(level) ? Level.OVERWORLD : level.dimension() == Level.OVERWORLD ? Stormreach.LEVEL : null;
        if (otherKey == null) return;
        ServerLevel other = player.server.getLevel(otherKey);
        if (other == null) return;
        BlockPos keystone = keystoneOf(level, at);
        if (keystone == null) return;
        Direction.Axis axis = level.getBlockState(at).getValue(RiftPortalBlock.AXIS);

        Links links = player.server.overworld().getDataStorage().computeIfAbsent(Links.FACTORY, "fulguritefusion_rifts");
        BlockPos dest = links.find(level.dimension(), keystone);
        if (dest == null) {
            dest = buildCounterpart(other, keystone.getX(), keystone.getZ(), axis);
            links.add(level.dimension(), keystone, otherKey, dest);
        } else if (!open(other, dest)) {
            rebuild(other, dest, axis);
        }
        BlockPos stand = dest.below();
        while (other.getBlockState(stand.below()).is(ModBlocks.RIFT_PORTAL.get())) stand = stand.below();
        player.changeDimension(new DimensionTransition(other, Vec3.atBottomCenterOf(stand), Vec3.ZERO, player.getYRot(), player.getXRot(),
                DimensionTransition.DO_NOTHING));
        player.setPortalCooldown(100);
        other.playSound(null, stand, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    private static boolean open(ServerLevel level, BlockPos keystone) {
        return level.getBlockState(keystone).is(ModBlocks.RIFT_KEYSTONE.get()) && level.getBlockState(keystone.below()).is(ModBlocks.RIFT_PORTAL.get());
    }

    // The keystone of the rift this portal block belongs to: walk the connected rift and look above each piece.
    private static BlockPos keystoneOf(ServerLevel level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < MAX * MAX) {
            BlockPos p = queue.poll();
            if (level.getBlockState(p.above()).is(ModBlocks.RIFT_KEYSTONE.get())) return p.above();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && level.getBlockState(n).is(ModBlocks.RIFT_PORTAL.get())) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        return null;
    }

    // A small rift (2 x 3 inside) standing on the ground near (x, z) on the other side. Returns its keystone.
    private static BlockPos buildCounterpart(ServerLevel level, int x, int z, Direction.Axis axis) {
        BlockPos stand = groundSpot(level, x, z);
        if (stand == null) stand = Stormreach.is(level) ? StormreachTravel.findLand(level, x, z) : new BlockPos(x, level.getSeaLevel() + 1, z);
        BlockPos keystone = stand.above(3);
        rebuild(level, keystone, axis);
        return keystone;
    }

    // Real ground to stand a rift on near (x, z): the top solid block that isn't a tree, plant or fluid, on dry land
    // and fairly level. Searches outward in rings; null if nothing suitable turns up.
    private static BlockPos groundSpot(ServerLevel level, int x, int z) {
        BlockPos fallback = null;
        for (int r = 0; r <= 48; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    BlockPos g = ground(level, x + dx, z + dz);
                    if (g == null) continue;
                    if (fallback == null) fallback = g.above();
                    if (!level.getFluidState(g).isEmpty() || !level.getFluidState(g.above()).isEmpty()) continue;
                    boolean level4 = true;
                    for (int ox = -2; ox <= 3 && level4; ox += 5) {
                        for (int oz = -2; oz <= 2 && level4; oz += 4) {
                            BlockPos o = ground(level, x + dx + ox, z + dz + oz);
                            if (o == null || Math.abs(o.getY() - g.getY()) > 2) level4 = false;
                        }
                    }
                    if (level4) return g.above();
                }
            }
        }
        return fallback;
    }

    // The highest block in this column that counts as ground (skips leaves, logs, plants, snow layers and air).
    private static BlockPos ground(ServerLevel level, int x, int z) {
        int top = level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, top, z);
        while (p.getY() > level.getMinBuildHeight() + 1) {
            BlockState s = level.getBlockState(p);
            boolean skip = s.isAir() || s.canBeReplaced() || s.is(net.minecraft.tags.BlockTags.LEAVES) || s.is(net.minecraft.tags.BlockTags.LOGS)
                    || !s.isSolid();
            if (!skip || !s.getFluidState().isEmpty()) return p.immutable();
            p.move(Direction.DOWN);
        }
        return null;
    }

    // Build (or rebuild) a 4 x 5 frame around the given keystone, clear room in front and behind, and open it.
    private static void rebuild(ServerLevel level, BlockPos keystone, Direction.Axis axis) {
        Direction side = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction front = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        BlockPos base = keystone.below(3);   // lowest inside block, on the keystone's side
        BlockState frame = ModBlocks.RIFT_FRAME.get().defaultBlockState();
        BlockState floor = Stormreach.is(level) ? ModBlocks.STORMSTONE.get().defaultBlockState() : Blocks.STONE.defaultBlockState();
        for (int i = -2; i <= 3; i++) {
            for (int f = -2; f <= 2; f++) {
                for (int j = -1; j <= 4; j++) {
                    BlockPos p = base.relative(side, i).relative(front, f).above(j);
                    if (f == 0 && i >= -1 && i <= 2 && j <= 3) continue;
                    if (j == -1) {
                        // Floor, propped up on a column down to the ground so nothing hangs in the air.
                        for (int d = 0; d < 8; d++) {
                            BlockPos q = p.below(d);
                            BlockState qs = level.getBlockState(q);
                            if (!(qs.canBeReplaced() || qs.is(net.minecraft.tags.BlockTags.LEAVES))) break;
                            level.setBlock(q, floor, 3);
                        }
                    } else if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        for (int i = -1; i <= 2; i++) {
            for (int j = -1; j <= 3; j++) {
                BlockPos p = base.relative(side, i).above(j);
                boolean edge = i == -1 || i == 2 || j == -1 || j == 3;
                level.setBlock(p, edge ? frame : Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
        level.setBlock(keystone, ModBlocks.RIFT_KEYSTONE.get().defaultBlockState(), 3);
        level.setBlock(keystone.above(), Blocks.LIGHTNING_ROD.defaultBlockState(), 3);
        tryOpen(level, keystone);
    }

    // ---- which rift leads to which ----

    public static class Links extends SavedData {
        static final SavedData.Factory<Links> FACTORY = new SavedData.Factory<>(Links::new, Links::load, null);

        record Link(ResourceKey<Level> aDim, BlockPos a, ResourceKey<Level> bDim, BlockPos b) {}

        private final List<Link> links = new ArrayList<>();

        // The keystone on the other side of the rift nearest this keystone, if they're linked.
        BlockPos find(ResourceKey<Level> dim, BlockPos keystone) {
            BlockPos best = null;
            double bestD = LINK_RANGE * LINK_RANGE;
            for (Link l : links) {
                if (l.aDim() == dim && l.a().distSqr(keystone) <= bestD) {
                    bestD = l.a().distSqr(keystone);
                    best = l.b();
                }
                if (l.bDim() == dim && l.b().distSqr(keystone) <= bestD) {
                    bestD = l.b().distSqr(keystone);
                    best = l.a();
                }
            }
            return best;
        }

        void add(ResourceKey<Level> aDim, BlockPos a, ResourceKey<Level> bDim, BlockPos b) {
            links.add(new Link(aDim, a.immutable(), bDim, b.immutable()));
            setDirty();
        }

        static Links load(CompoundTag tag, HolderLookup.Provider registries) {
            Links out = new Links();
            ListTag list = tag.getList("Links", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                ResourceLocation a = ResourceLocation.tryParse(t.getString("ADim")), b = ResourceLocation.tryParse(t.getString("BDim"));
                if (a == null || b == null) continue;
                out.links.add(new Link(ResourceKey.create(Registries.DIMENSION, a), BlockPos.of(t.getLong("A")),
                        ResourceKey.create(Registries.DIMENSION, b), BlockPos.of(t.getLong("B"))));
            }
            return out;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Link l : links) {
                CompoundTag t = new CompoundTag();
                t.putString("ADim", l.aDim().location().toString());
                t.putLong("A", l.a().asLong());
                t.putString("BDim", l.bDim().location().toString());
                t.putLong("B", l.b().asLong());
                list.add(t);
            }
            tag.put("Links", list);
            return tag;
        }
    }
}
