package com.sideflowgames.fulguritefusion.item;

import com.sideflowgames.fulguritefusion.FulguriteFusion;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

// Chain lightning from a Storm or Prime Blade: the server sends the path (the struck enemy, then each enemy the
// lightning jumps to) and clients draw it as jagged bolts (client/ChainBolts).
public final class ChainBolt {
    private ChainBolt() {}

    // Set by the client; the server never calls it.
    public static BiConsumer<List<Vec3>, Boolean> clientHandler = (points, prime) -> {};

    public record Payload(List<Vec3> points, boolean prime) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "chain_bolt"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeBoolean(p.prime());
            buf.writeByte(p.points().size());
            for (Vec3 v : p.points()) {
                buf.writeFloat((float) v.x);
                buf.writeFloat((float) v.y);
                buf.writeFloat((float) v.z);
            }
        }, buf -> {
            boolean prime = buf.readBoolean();
            int n = Math.min(buf.readUnsignedByte(), 32);
            List<Vec3> pts = new ArrayList<>(n);
            for (int i = 0; i < n; i++) pts.add(new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()));
            return new Payload(pts, prime);
        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // Show the bolts to everyone who can see the struck enemy.
    public static void send(Entity target, List<Vec3> points, boolean prime) {
        if (points.size() < 2) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new Payload(List.copyOf(points), prime));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Payload.TYPE, Payload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> clientHandler.accept(payload.points(), payload.prime())));
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ChainBolt::registerPayloads);
    }
}
