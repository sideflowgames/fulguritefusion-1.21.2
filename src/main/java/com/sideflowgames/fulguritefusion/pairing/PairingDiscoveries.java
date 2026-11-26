package com.sideflowgames.fulguritefusion.pairing;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import com.sideflowgames.fulguritefusion.FulguriteFusion;

import io.netty.buffer.ByteBuf;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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

// Which pairing results each player has discovered. A Fusion Fuel's reactor yield and side effects stay
// hidden ("???") until that player has burned that result in the big reactor. Saved on the player, kept on death.
//
// The big reactor calls PairingDiscoveries.discover(player, resultId) when it burns a fuel.
// Testing (operators): /fulguritefusion discover <player> all|<id>   and   /fulguritefusion forget <player>
public final class PairingDiscoveries {
    private PairingDiscoveries() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FulguriteFusion.MODID);

    public static final Supplier<AttachmentType<List<Integer>>> DISCOVERED = ATTACHMENTS.register("discovered_pairings",
            () -> AttachmentType.<List<Integer>>builder(() -> List.of())
                    .serialize(Codec.INT.listOf().fieldOf("value"))
                    .copyOnDeath()
                    .build());

    // What this client's player knows (filled from the server; empty on a dedicated server).
    private static final Set<Integer> CLIENT_KNOWN = new HashSet<>();

    public static boolean knownOnClient(int resultId) {
        return CLIENT_KNOWN.contains(resultId);
    }

    public static boolean knows(Player player, int resultId) {
        return player.getData(DISCOVERED).contains(resultId);
    }

    // Mark a result discovered and tell the player's client. Returns true if it was new.
    public static boolean discover(ServerPlayer player, int resultId) {
        List<Integer> known = player.getData(DISCOVERED);
        if (known.contains(resultId) || Pairings.byId(resultId) == null) return false;
        List<Integer> updated = new ArrayList<>(known);
        updated.add(resultId);
        player.setData(DISCOVERED, updated);
        sync(player);
        return true;
    }

    public static void forgetAll(ServerPlayer player) {
        player.setData(DISCOVERED, List.of());
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncPayload(List.copyOf(player.getData(DISCOVERED))));
    }

    // ---------- Networking ----------

    public record SyncPayload(List<Integer> ids) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "discovered_pairings"));
        public static final StreamCodec<ByteBuf, SyncPayload> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).map(SyncPayload::new, SyncPayload::ids);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    CLIENT_KNOWN.clear();
                    CLIENT_KNOWN.addAll(payload.ids());
                }));
    }

    // ---------- Events and commands ----------

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    private static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(FulguriteFusion.MODID)
                .requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("discover").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("all").executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            for (Pairings.Pairing p : Pairings.all()) discover(player, p.id());
                            ctx.getSource().sendSuccess(() -> Component.literal("Revealed all 78 pairings for " + player.getName().getString()), true);
                            return 1;
                        }))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1, 78)).executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            int id = IntegerArgumentType.getInteger(ctx, "id");
                            discover(player, id);
                            ctx.getSource().sendSuccess(() -> Component.literal("Revealed " + Pairings.byId(id).name()), true);
                            return 1;
                        }))))
                .then(Commands.literal("forget").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                    forgetAll(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Forgot all pairings for " + player.getName().getString()), true);
                    return 1;
                }))));
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(PairingDiscoveries::registerPayloads);
        NeoForge.EVENT_BUS.addListener(PairingDiscoveries::onLogin);
        NeoForge.EVENT_BUS.addListener(PairingDiscoveries::onCommands);
    }
}
