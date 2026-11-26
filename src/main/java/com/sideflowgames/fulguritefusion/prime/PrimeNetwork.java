package com.sideflowgames.fulguritefusion.prime;

import com.sideflowgames.fulguritefusion.FulguriteFusion;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// Prime gear messages from the client: the hammer key, and boost ticks spent while gliding.
public final class PrimeNetwork {
    private PrimeNetwork() {}

    public record HammerPayload() implements CustomPacketPayload {
        public static final Type<HammerPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "prime_hammer"));
        public static final StreamCodec<ByteBuf, HammerPayload> STREAM_CODEC = StreamCodec.unit(new HammerPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BoostPayload(int ticks) implements CustomPacketPayload {
        public static final Type<BoostPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "prime_boost"));
        public static final StreamCodec<ByteBuf, BoostPayload> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(BoostPayload::new, BoostPayload::ticks);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(HammerPayload.TYPE, HammerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) PrimeEvents.toggleHammer(player);
                }));
        event.registrar("1").playToServer(BoostPayload.TYPE, BoostPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) PrimeEvents.boosted(player, payload.ticks());
                }));
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(PrimeNetwork::registerPayloads);
    }
}
