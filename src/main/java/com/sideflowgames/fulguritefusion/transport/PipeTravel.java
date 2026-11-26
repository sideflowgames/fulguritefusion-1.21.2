package com.sideflowgames.fulguritefusion.transport;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// Items sliding through pipe windows: the server sends the route an item took (the pipes from the extracting pipe to
// the delivering one, which sides it came in and went out, the item and the pipe tier) and clients animate it
// (client/ItemPipeVisuals). Purely for show; the items have already arrived.
public final class PipeTravel {
    private PipeTravel() {}

    public static final int MAX_PATH = 512;

    // Set by the client; the server never calls it.
    public static Consumer<Payload> clientHandler = p -> {};

    public record Payload(List<BlockPos> path, int fromSide, int toSide, ItemStack item, int tier) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "pipe_travel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            int n = Math.min(p.path().size(), MAX_PATH);
            buf.writeVarInt(n);
            for (int i = 0; i < n; i++) buf.writeLong(p.path().get(i).asLong());
            buf.writeByte(p.fromSide());
            buf.writeByte(p.toSide());
            ItemStack.STREAM_CODEC.encode(buf, p.item());
            buf.writeByte(p.tier());
        }, buf -> {
            int n = Math.min(buf.readVarInt(), MAX_PATH);
            List<BlockPos> path = new ArrayList<>(n);
            for (int i = 0; i < n; i++) path.add(BlockPos.of(buf.readLong()));
            int from = buf.readByte(), to = buf.readByte();
            ItemStack item = ItemStack.STREAM_CODEC.decode(buf);
            return new Payload(path, from, to, item, buf.readByte());
        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Payload.TYPE, Payload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> clientHandler.accept(payload)));
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(PipeTravel::registerPayloads);
    }
}
