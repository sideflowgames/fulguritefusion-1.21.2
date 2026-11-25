package com.sideflowgames.fulguritefusion.backpack;

import com.sideflowgames.fulguritefusion.FulguriteFusion;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// The backpack key (B by default): opens the backpack you're wearing (chestplate slot, then Curios' back
// slot), or else the first one in your inventory.
public final class BackpackNetwork {
    private BackpackNetwork() {}

    public record OpenPayload() implements CustomPacketPayload {
        public static final Type<OpenPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "open_backpack"));
        public static final StreamCodec<ByteBuf, OpenPayload> STREAM_CODEC = StreamCodec.unit(new OpenPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // Restock / Deposit buttons (see BackpackUpgrades.handleUtility for the action numbers).
    public record UtilityPayload(int action) implements CustomPacketPayload {
        public static final Type<UtilityPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "backpack_utility"));
        public static final StreamCodec<ByteBuf, UtilityPayload> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(UtilityPayload::new, UtilityPayload::action);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void openWorn(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof BackpackItem) {
            BackpackItem.open(player, chest, -1);
            return;
        }
        ItemStack back = com.sideflowgames.fulguritefusion.trinket.Trinkets.back(player);
        if (!back.isEmpty()) {
            BackpackItem.open(player, back, -1);
            return;
        }
        ItemStack curio = CuriosCompat.find(player, s -> s.getItem() instanceof BackpackItem);
        if (!curio.isEmpty()) {
            BackpackItem.open(player, curio, -1);
            return;
        }
        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).getItem() instanceof BackpackItem) {
                BackpackItem.open(player, inv.getItem(i), i);
                return;
            }
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(OpenPayload.TYPE, OpenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) openWorn(player);
                }));
        event.registrar("1").playToServer(UtilityPayload.TYPE, UtilityPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) BackpackUpgrades.handleUtility(player, payload.action());
                }));
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(BackpackNetwork::registerPayloads);
    }
}
