package com.sideflowgames.fulguritefusion.titan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.entity.boss.StormTitanEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// The Titan Trials: a void dimension where each player fights the Storm Titan in an arena of their own.
//   - A Storm Sigil takes you (and anyone within 8 blocks of you) to your arena, remembering where you left from.
//     Using it again inside the Trials takes you back there.
//   - Each player's arena sits far apart from everyone else's, so it plays like a private instance. It's built the
//     first time someone goes there.
//   - Dying here sends you to your bed or world spawn as usual, but you keep your whole inventory and experience.
//   - When the Titan falls, every player in the arena is marked as having defeated it (that's what lets them
//     through a Stormreach rift), gets a Rift Core, and is sent home 60 seconds later.
public final class TitanTrials {
    private TitanTrials() {}

    public static final ResourceKey<Level> LEVEL =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "titan_trials"));

    public static final int ARENA_Y = 100;
    public static final int ARENA_RADIUS = 24;
    private static final int SPACING = 2048;
    private static final int PARTY_RANGE = 8;
    private static final int RETURN_DELAY = 20 * 60;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FulguriteFusion.MODID);

    // Has this player ever defeated the Storm Titan? Only they can pass through a Stormreach rift.
    public static final Supplier<AttachmentType<Boolean>> DEFEATED = ATTACHMENTS.register("titan_defeated",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("value")).copyOnDeath().build());
    // Where to send this player when they leave the Trials.
    public static final Supplier<AttachmentType<CompoundTag>> RETURN = ATTACHMENTS.register("trials_return",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC.fieldOf("value")).copyOnDeath().build());
    // Inventory and experience held over from a death in the Trials, given back on respawn.
    public static final Supplier<AttachmentType<CompoundTag>> STASH = ATTACHMENTS.register("trials_stash",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC.fieldOf("value")).copyOnDeath().build());

    // Players due to be sent home after a victory, with the game time to do it at.
    private static final Map<UUID, Long> pendingReturn = new HashMap<>();

    public static boolean is(Level level) {
        return level != null && level.dimension() == LEVEL;
    }

    public static boolean defeated(Player player) {
        return player.getData(DEFEATED);
    }

    // ---- going there and coming back ----

    // The Storm Sigil: take this player and everyone standing near them to the player's own arena.
    public static void enter(ServerPlayer leader) {
        ServerLevel trials = leader.level().getServer().getLevel(LEVEL);
        if (trials == null) {
            leader.sendSystemMessage(Component.literal("The Titan Trials aren't loaded in this world."));
            return;
        }
        BlockPos center = arenaFor(trials, leader.getUUID());
        List<ServerPlayer> party = new ArrayList<>();
        party.add(leader);
        if (leader.level() instanceof ServerLevel here) {
            for (ServerPlayer p : here.players()) {
                if (p != leader && p.isAlive() && !p.isSpectator() && p.distanceTo(leader) <= PARTY_RANGE) party.add(p);
            }
        }
        for (int i = 0; i < party.size(); i++) {
            ServerPlayer p = party.get(i);
            strike(p);
            if (!is(p.level())) rememberReturn(p);
            double a = Math.PI * 2 * i / Math.max(1, party.size()) + Math.PI / 2;
            Vec3 at = new Vec3(center.getX() + 0.5 + Math.cos(a) * 14, center.getY() + 1, center.getZ() + 0.5 + Math.sin(a) * 14);
            float yaw = (float) (Math.atan2(center.getZ() + 0.5 - at.z, center.getX() + 0.5 - at.x) * 180 / Math.PI) - 90f;
            p.teleport(new TeleportTransition(trials, at, Vec3.ZERO, yaw, 0, TeleportTransition.DO_NOTHING));
            if (p != leader) {
                p.sendOverlayMessage(Component.literal(leader.getName().getString() + "'s sigil carries you into the Titan Trials.")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        trials.playSound(null, center, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.7f);
    }

    // Back to wherever the player came from, or their spawn point if that's gone.
    public static void home(ServerPlayer player) {
        CompoundTag back = player.getData(RETURN);
        player.setData(RETURN, new CompoundTag());
        ServerLevel level = null;
        if (back.contains("Dim")) {
            Identifier id = Identifier.tryParse(back.getStringOr("Dim", ""));
            if (id != null) level = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
        }
        if (level != null && !is(level)) {
            player.teleport(new TeleportTransition(level, new Vec3(back.getDoubleOr("X", 0d), back.getDoubleOr("Y", 0d), back.getDoubleOr("Z", 0d)),
                    Vec3.ZERO, back.getFloatOr("YRot", 0f), 0, TeleportTransition.DO_NOTHING));
        } else {
            player.teleport(player.findRespawnPositionAndUseSpawnBlock(true, TeleportTransition.DO_NOTHING));
        }
    }

    private static void rememberReturn(ServerPlayer p) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dim", p.level().dimension().identifier().toString());
        tag.putDouble("X", p.getX());
        tag.putDouble("Y", p.getY());
        tag.putDouble("Z", p.getZ());
        tag.putFloat("YRot", p.getYRot());
        p.setData(RETURN, tag);
    }

    private static void strike(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (bolt == null) return;
        bolt.snapTo(p.getX(), p.getY(), p.getZ());
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    // ---- the arenas ----

    // The centre of this player's arena (the floor block under the altar's dais), built on first use.
    private static BlockPos arenaFor(ServerLevel trials, UUID owner) {
        Slots slots = trials.getServer().overworld().getDataStorage().computeIfAbsent(Slots.FACTORY);
        int slot = slots.slotFor(owner);
        // Spread arenas along a square spiral so none of them is ever near another.
        int ring = (int) Math.ceil((Math.sqrt(slot + 1) - 1) / 2);
        int side = ring * 2, first = (side - 1) * (side - 1), x, z;
        int k = slot - first;
        if (ring == 0) { x = 0; z = 0; }
        else if (k < side) { x = ring; z = -ring + 1 + k; }
        else if (k < side * 2) { x = ring - 1 - (k - side); z = ring; }
        else if (k < side * 3) { x = -ring; z = ring - 1 - (k - side * 2); }
        else { x = -ring + 1 + (k - side * 3); z = -ring; }
        BlockPos center = new BlockPos(x * SPACING, ARENA_Y, z * SPACING);
        if (!trials.getBlockState(center.above(2)).is(ModBlocks.TITAN_ALTAR.get())) build(trials, center);
        return center;
    }

    // A floating island: a round Stormstone Brick floor with lit rings, eight crowned pillars and the altar on its
    // dais, over a rounded rocky underside.
    public static void build(ServerLevel level, BlockPos c) {
        RandomSource rand = RandomSource.create(c.asLong());
        BlockState bricks = ModBlocks.STORMSTONE_BRICKS.get().defaultBlockState();
        BlockState polished = ModBlocks.POLISHED_STORMSTONE.get().defaultBlockState();
        BlockState stone = ModBlocks.STORMSTONE.get().defaultBlockState();
        BlockState deep = ModBlocks.DEEP_STORMSTONE.get().defaultBlockState();
        BlockState light = Blocks.SEA_LANTERN.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r = ARENA_RADIUS;
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + 0.5) continue;
                boolean ring = (d >= 7.5 && d < 8.5) || (d >= 15.5 && d < 16.5) || d >= r - 0.5;
                level.setBlock(p.set(c.getX() + dx, c.getY(), c.getZ() + dz), ring || d < 3.0 ? polished : bricks, 2);
                int depth = 2 + (int) Math.round((1 - (d / (r + 1)) * (d / (r + 1))) * 14) + rand.nextInt(2);
                for (int y = 1; y <= depth; y++) {
                    level.setBlock(p.set(c.getX() + dx, c.getY() - y, c.getZ() + dz), y > depth * 0.55 ? deep : stone, 2);
                }
            }
        }
        // Lights set into the inner and middle rings.
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            level.setBlock(p.set(c.getX() + (int) Math.round(Math.cos(a) * 16), c.getY(), c.getZ() + (int) Math.round(Math.sin(a) * 16)), light, 2);
            if (i % 2 == 0) {
                level.setBlock(p.set(c.getX() + (int) Math.round(Math.cos(a) * 8), c.getY(), c.getZ() + (int) Math.round(Math.sin(a) * 8)), light, 2);
            }
        }
        // Eight pillars near the edge.
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = c.getX() + (int) Math.round(Math.cos(a) * (r - 2)), z = c.getZ() + (int) Math.round(Math.sin(a) * (r - 2));
            for (int y = 1; y <= 9; y++) level.setBlock(p.set(x, c.getY() + y, z), y == 9 ? polished : bricks, 2);
            level.setBlock(p.set(x, c.getY() + 10, z), Blocks.AMETHYST_CLUSTER.defaultBlockState(), 2);
        }
        // Dais and altar.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) level.setBlock(p.set(c.getX() + dx, c.getY() + 1, c.getZ() + dz), polished, 2);
        }
        level.setBlock(c.above(2), ModBlocks.TITAN_ALTAR.get().defaultBlockState(), 3);
    }

    // Which arena belongs to whom. Kept in the Overworld's saved data.
    public static class Slots extends SavedData {
        static final net.minecraft.world.level.saveddata.SavedDataType<Slots> FACTORY = com.sideflowgames.fulguritefusion.util.NbtBridge.savedData("fulguritefusion_titan_trials", Slots::new, Slots::load, (d, t) -> d.save(t, null));
        private final Map<UUID, Integer> owners = new HashMap<>();
        private int next;

        int slotFor(UUID id) {
            Integer s = owners.get(id);
            if (s == null) {
                s = next++;
                owners.put(id, s);
                setDirty();
            }
            return s;
        }

        static Slots load(CompoundTag tag, HolderLookup.Provider registries) {
            Slots s = new Slots();
            s.next = tag.getIntOr("Next", 0);
            CompoundTag map = tag.getCompoundOrEmpty("Owners");
            for (String key : map.keySet()) s.owners.put(UUID.fromString(key), map.getIntOr(key, 0));
            return s;
        }

                public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putInt("Next", next);
            CompoundTag map = new CompoundTag();
            owners.forEach((id, slot) -> map.putInt(id.toString(), slot));
            tag.put("Owners", map);
            return tag;
        }
    }

    // ---- victory ----

    // Called once when a Titan dies in the Trials.
    public static void victory(ServerLevel level, StormTitanEntity titan) {
        if (!is(level)) return;
        long at = level.getGameTime() + RETURN_DELAY;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.distanceTo(titan) > 48) continue;
            boolean first = !defeated(p);
            p.setData(DEFEATED, true);
            ItemStack core = new ItemStack(com.sideflowgames.fulguritefusion.ModItems.RIFT_CORE.get());
            if (!p.getInventory().add(core)) p.drop(core, false);
            p.sendSystemMessage(Component.literal(first
                    ? "The Storm Titan falls. The storm now knows you: a Stormreach rift will let you pass."
                    : "The Storm Titan falls again.").withStyle(ChatFormatting.LIGHT_PURPLE));
            p.sendSystemMessage(Component.literal("You'll be sent home in 60 seconds. Gather your rewards.").withStyle(ChatFormatting.GRAY));
            pendingReturn.put(p.getUUID(), at);
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (pendingReturn.isEmpty()) return;
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = pendingReturn.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                it.remove();
                continue;
            }
            ServerLevel trials = server.getLevel(LEVEL);
            long t = trials != null ? trials.getGameTime() : now;
            if (t < e.getValue()) continue;
            it.remove();
            if (is(p.level()) && p.isAlive()) home(p);
        }
    }

    // ---- keeping your things when you die here ----

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !is(p.level())) return;
        if (p.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) return;
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, p.registryAccess());
        p.getInventory().save(out.list("Inventory", net.minecraft.world.ItemStackWithSlot.CODEC));
        CompoundTag tag = out.buildResult();
        tag.putInt("XpLevel", p.experienceLevel);
        tag.putFloat("XpProgress", p.experienceProgress);
        tag.putInt("XpTotal", p.totalExperience);
        p.setData(STASH, tag);
        pendingReturn.remove(p.getUUID());
    }

    private static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && is(p.level()) && !p.getData(STASH).isEmpty()) event.setCanceled(true);
    }

    private static void onXp(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && is(p.level()) && !p.getData(STASH).isEmpty()) event.setCanceled(true);
    }

    private static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        CompoundTag tag = event.getOriginal().getData(STASH);
        Player now = event.getEntity();
        now.setData(STASH, new CompoundTag());
        if (tag.isEmpty()) return;
        now.getInventory().load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, now.registryAccess(), tag)
                .listOrEmpty("Inventory", net.minecraft.world.ItemStackWithSlot.CODEC));
        now.experienceLevel = tag.getIntOr("XpLevel", 0);
        now.experienceProgress = tag.getFloatOr("XpProgress", 0f);
        now.totalExperience = tag.getIntOr("XpTotal", 0);
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, TitanTrials::onDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TitanTrials::onDrops);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TitanTrials::onXp);
        NeoForge.EVENT_BUS.addListener(TitanTrials::onClone);
        NeoForge.EVENT_BUS.addListener(TitanTrials::onServerTick);
    }
}
