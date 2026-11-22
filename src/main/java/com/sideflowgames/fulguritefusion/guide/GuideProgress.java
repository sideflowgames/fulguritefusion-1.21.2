package com.sideflowgames.fulguritefusion.guide;

import com.mojang.serialization.Codec;
import com.sideflowgames.fulguritefusion.FulguriteFusion;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

// Which guide topics each player has unlocked. A topic unlocks the first time the player holds one of its
// items (the client notices and tells the server). Saved on the player, kept on death, sent back on login.
public final class GuideProgress {
    private GuideProgress() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FulguriteFusion.MODID);

    public static final Supplier<AttachmentType<List<String>>> UNLOCKED = ATTACHMENTS.register("guide_topics",
            () -> AttachmentType.<List<String>>builder(() -> List.of())
                    .serialize(Codec.STRING.listOf())
                    .copyOnDeath()
                    .build());

    // This client's player (filled from the server on login).
    public static final Set<String> CLIENT_UNLOCKED = new HashSet<>();
    public static boolean clientSynced;

    public record UnlockPayload(String topic) implements CustomPacketPayload {
        public static final Type<UnlockPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "guide_unlock"));
        public static final StreamCodec<ByteBuf, UnlockPayload> STREAM_CODEC =
                ByteBufCodecs.stringUtf8(64).map(UnlockPayload::new, UnlockPayload::topic);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncPayload(List<String> topics) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "guide_topics"));
        public static final StreamCodec<ByteBuf, SyncPayload> STREAM_CODEC =
                ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(256)).map(SyncPayload::new, SyncPayload::topics);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void unlock(ServerPlayer player, String topic) {
        List<String> have = player.getData(UNLOCKED);
        if (have.contains(topic) || have.size() >= 256 || !topic.matches("[a-z0-9_]{1,64}")) return;
        List<String> updated = new ArrayList<>(have);
        updated.add(topic);
        player.setData(UNLOCKED, updated);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(UnlockPayload.TYPE, UnlockPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) unlock(player, payload.topic());
                }));
        event.registrar("1").playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    CLIENT_UNLOCKED.clear();
                    CLIENT_UNLOCKED.addAll(payload.topics());
                    clientSynced = true;
                }));
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new SyncPayload(List.copyOf(player.getData(UNLOCKED))));
        }
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(GuideProgress::registerPayloads);
        NeoForge.EVENT_BUS.addListener(GuideProgress::onLogin);
    }
}
