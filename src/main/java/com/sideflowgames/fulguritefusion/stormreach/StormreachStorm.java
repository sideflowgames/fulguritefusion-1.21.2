package com.sideflowgames.fulguritefusion.stormreach;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModBlocks;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// The Stormreach's weather: long storms (10-20 minutes) with short calm spells (2-4 minutes) between them.
// During a storm, lightning strikes around every player there (and can hit them), and a strike on Charged
// Stormsand fuses it into Fulgurite Glass. The storm runs on its own, separate from Overworld weather; clients are
// told its strength and draw the rain themselves (client/StormreachClient), with the thunder turned down.
public final class StormreachStorm {
    private StormreachStorm() {}

    public static final int STORM_MIN = 20 * 60 * 10, STORM_MAX = 20 * 60 * 20;
    public static final int CALM_MIN = 20 * 60 * 2, CALM_MAX = 20 * 60 * 4;
    public static final float STRIKE_CHANCE = 0.3f; // per player, per second, during a storm
    public static final int STRIKE_RANGE = 48;
    // Fire a strike starts fizzles out: a few seconds later, any fire near the strike is put out.
    public static final int FIZZLE_TICKS = 50, FIZZLE_RADIUS = 4;

    private record Fizzle(BlockPos pos, long at) {}
    private static final List<Fizzle> FIZZLES = new ArrayList<>();

    // Set by the client; the server never calls it.
    public static Consumer<Boolean> clientHandler = storming -> {};

    public record Payload(boolean storming) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "stormreach_storm"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = ByteBufCodecs.BOOL.map(Payload::new, Payload::storming);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // Saved with the Stormreach so a storm carries on after a restart.
    public static class State extends SavedData {
        boolean storming = true;
        int timer = STORM_MIN;

        static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State s = new State();
            s.storming = tag.getBoolean("Storming");
            s.timer = tag.getInt("Timer");
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("Storming", storming);
            tag.putInt("Timer", timer);
            return tag;
        }

        static State get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::load, null), "fulguritefusion_storm");
        }
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(StormreachStorm::registerPayloads);
        NeoForge.EVENT_BUS.addListener(StormreachStorm::onLevelTick);
        NeoForge.EVENT_BUS.addListener(StormreachStorm::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(StormreachStorm::onLogin);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Payload.TYPE, Payload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> clientHandler.accept(payload.storming())));
    }

    // Whether a storm is raging in the Stormreach right now.
    public static boolean isStorming(ServerLevel level) {
        return Stormreach.is(level) && State.get(level).storming;
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Stormreach.is(level)) return;
        State state = State.get(level);
        if (--state.timer <= 0) {
            state.storming = !state.storming;
            state.timer = state.storming ? between(level, STORM_MIN, STORM_MAX) : between(level, CALM_MIN, CALM_MAX);
            PacketDistributor.sendToPlayersInDimension(level, new Payload(state.storming));
        }
        state.setDirty();
        // Keep the level's own weather in step with the Stormreach storm, so anything that checks
        // isThundering() (Stormwings waking and hunting, rain on blocks) sees the storm too.
        level.setRainLevel(state.storming ? 1.0f : 0.0f);
        level.setThunderLevel(state.storming ? 1.0f : 0.0f);
        fizzle(level);
        if (!state.storming || level.getGameTime() % 20 != 0) return;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || level.random.nextFloat() > STRIKE_CHANCE) continue;
            int x = player.getBlockX() + level.random.nextInt(STRIKE_RANGE * 2 + 1) - STRIKE_RANGE;
            int z = player.getBlockZ() + level.random.nextInt(STRIKE_RANGE * 2 + 1) - STRIKE_RANGE;
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (top <= level.getMinBuildHeight() + 1) continue; // open sky over the void
            strike(level, new BlockPos(x, top, z));
        }
    }

    // Puts out the fire left by strikes whose time is up.
    private static void fizzle(ServerLevel level) {
        if (FIZZLES.isEmpty()) return;
        long now = level.getGameTime();
        FIZZLES.removeIf(f -> {
            if (f.at() > now) return false;
            if (!level.hasChunkAt(f.pos())) return true;
            BlockPos min = f.pos().offset(-FIZZLE_RADIUS, -FIZZLE_RADIUS, -FIZZLE_RADIUS);
            BlockPos max = f.pos().offset(FIZZLE_RADIUS, FIZZLE_RADIUS, FIZZLE_RADIUS);
            boolean any = false;
            for (BlockPos p : BlockPos.betweenClosed(min, max)) {
                if (level.getBlockState(p).getBlock() instanceof BaseFireBlock) {
                    level.removeBlock(p, false);
                    any = true;
                }
            }
            if (any) {
                level.playSound(null, f.pos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.4f);
                level.sendParticles(ParticleTypes.SMOKE, f.pos().getX() + 0.5, f.pos().getY() + 0.5, f.pos().getZ() + 0.5,
                        12, 1.5, 0.5, 1.5, 0.01);
            }
            return true;
        });
    }

    private static int between(ServerLevel level, int min, int max) {
        return min + level.random.nextInt(max - min + 1);
    }

    private static void strike(ServerLevel level, BlockPos pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(bolt);
        FIZZLES.add(new Fizzle(pos.immutable(), level.getGameTime() + FIZZLE_TICKS));
        // Fuse Charged Stormsand around where it lands.
        BlockPos ground = pos.below();
        for (BlockPos p : BlockPos.betweenClosed(ground.offset(-1, -1, -1), ground.offset(1, 0, 1))) {
            if (level.getBlockState(p).is(ModBlocks.CHARGED_STORMSAND.get()) && (p.equals(ground) || level.random.nextFloat() < 0.5f)) {
                level.setBlockAndUpdate(p, ModBlocks.FULGURITE_GLASS.get().defaultBlockState());
            }
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    private static void sync(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer sp && sp.level() instanceof ServerLevel level && Stormreach.is(level)) {
            PacketDistributor.sendToPlayer(sp, new Payload(State.get(level).storming));
        }
    }
}
