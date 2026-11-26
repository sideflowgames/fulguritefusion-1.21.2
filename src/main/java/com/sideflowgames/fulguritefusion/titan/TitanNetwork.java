package com.sideflowgames.fulguritefusion.titan;

import com.sideflowgames.fulguritefusion.FulguriteFusion;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// Titan armor messages from the client: a static dash in direction (dx, dz).
public final class TitanNetwork {
    private TitanNetwork() {}

    public record DashPayload(float dx, float dz) implements CustomPacketPayload {
        public static final Type<DashPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "titan_dash"));
        public static final StreamCodec<ByteBuf, DashPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, DashPayload::dx, ByteBufCodecs.FLOAT, DashPayload::dz, DashPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(DashPayload.TYPE, DashPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) TitanArmorEvents.dashed(player, payload.dx(), payload.dz());
                }));
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(TitanNetwork::registerPayloads);
    }
}
