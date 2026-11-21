package com.sideflowgames.fulguritefusion.entity;

import com.sideflowgames.fulguritefusion.FulguriteFusion;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// Stormwing riding, the parts shared by client and server:
//   - DivePayload: the rider pressed the dive key (R); the server checks the cooldown and calls the lightning.
//   - Shift descends while flying, so Shift must not throw the rider off mid-air:
//     getting off is only allowed once the bird is down near the ground.
public final class StormwingRiding {
    private StormwingRiding() {}

    public record DivePayload() implements CustomPacketPayload {
        public static final Type<DivePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "stormwing_dive"));
        public static final StreamCodec<ByteBuf, DivePayload> STREAM_CODEC = StreamCodec.unit(new DivePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(DivePayload.TYPE, DivePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof StormwingEntity bird) {
                        bird.riderDive(player);
                    }
                }));
    }

    // Shift while flying means "go down", not "get off". Other ways off (the bird dying, the rider dying,
    // teleporting) still work.
    private static void onMount(EntityMountEvent event) {
        if (!event.isDismounting()) return;
        if (event.getEntityBeingMounted() instanceof StormwingEntity bird && bird.isAlive()
                && event.getEntityMounting() instanceof Player rider && rider.isAlive() && rider.isShiftKeyDown()
                && bird.getControllingPassenger() == rider && !bird.nearGround()) {
            event.setCanceled(true);
        }
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(StormwingRiding::registerPayloads);
        NeoForge.EVENT_BUS.addListener(StormwingRiding::onMount);
    }
}
