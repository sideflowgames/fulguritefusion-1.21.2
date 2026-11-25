package com.sideflowgames.fulguritefusion.trinket;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.backpack.BackpackItem;
import com.sideflowgames.fulguritefusion.backpack.CuriosCompat;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// The equipment panel's slots: one per trinket (TrinketKind order) and a Back slot for a backpack. Saved on the
// player. They drop on death like the inventory (kept with keepInventory). Every change is sent to the player and
// to everyone who can see them, so worn backpacks show on other players and the client knows its own trinkets
// (double jump and blink start on the client).
// With Curios installed the mod's panel is hidden and trinkets go in Curios slots instead; worn() checks both.
public final class Trinkets {
    private Trinkets() {}

    public static final int BACK = TrinketKind.values().length;
    public static final int SIZE = BACK + 1;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FulguriteFusion.MODID);

    public static final Supplier<AttachmentType<Equipment>> EQUIPMENT = ATTACHMENTS.register("equipment",
            () -> AttachmentType.serializable(Equipment::new).copyOnDeath().build());

    public static class Equipment extends ItemStackHandler {
        private final IAttachmentHolder holder;

        public Equipment(IAttachmentHolder holder) {
            super(SIZE);
            this.holder = holder;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (holder instanceof ServerPlayer player) sync(player);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return fits(slot, stack);
        }
    }

    // What may go in a slot: the right trinket (plain or Empowered), or a backpack in the Back slot.
    public static boolean fits(int slot, ItemStack stack) {
        if (slot == BACK) return stack.getItem() instanceof BackpackItem;
        return stack.getItem() instanceof TrinketItem t && t.kind.slot() == slot;
    }

    // The trinket of that kind the player is wearing (panel or Curios), or EMPTY.
    public static ItemStack worn(Player player, TrinketKind kind) {
        ItemStack s = player.getData(EQUIPMENT).getStackInSlot(kind.slot());
        if (s.getItem() instanceof TrinketItem t && t.kind == kind) return s;
        return CuriosCompat.find(player, x -> x.getItem() instanceof TrinketItem t && t.kind == kind);
    }

    // 0 not worn, 1 worn, 2 the Empowered version worn, 3 a Relic (dungeon tier) worn.
    public static int level(Player player, TrinketKind kind) {
        ItemStack s = worn(player, kind);
        return s.getItem() instanceof TrinketItem t ? t.tier() : 0;
    }

    // The backpack in the Back slot, or EMPTY.
    public static ItemStack back(Player player) {
        ItemStack s = player.getData(EQUIPMENT).getStackInSlot(BACK);
        return s.getItem() instanceof BackpackItem ? s : ItemStack.EMPTY;
    }

    // ---- network ----

    public record SyncPayload(int entity, List<ItemStack> stacks) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "equipment_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SyncPayload::entity, ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(SIZE)), SyncPayload::stacks,
                SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenPayload() implements CustomPacketPayload {
        public static final Type<OpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "equipment_open"));
        public static final StreamCodec<ByteBuf, OpenPayload> STREAM_CODEC = StreamCodec.unit(new OpenPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DoubleJumpPayload() implements CustomPacketPayload {
        public static final Type<DoubleJumpPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "double_jump"));
        public static final StreamCodec<ByteBuf, DoubleJumpPayload> STREAM_CODEC = StreamCodec.unit(new DoubleJumpPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BlinkPayload(float dx, float dz) implements CustomPacketPayload {
        public static final Type<BlinkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "void_blink"));
        public static final StreamCodec<ByteBuf, BlinkPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, BlinkPayload::dx, ByteBufCodecs.FLOAT, BlinkPayload::dz, BlinkPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static SyncPayload payload(Player player) {
        Equipment eq = player.getData(EQUIPMENT);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) stacks.add(eq.getStackInSlot(i).copy());
        return new SyncPayload(player.getId(), stacks);
    }

    public static void sync(ServerPlayer player) {
        if (player.connection == null) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload(player));
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new EquipmentMenu(id, inv, p.getData(EQUIPMENT)),
                Component.translatable("container.fulguritefusion.equipment")));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player().level().getEntity(payload.entity()) instanceof Player p) {
                Equipment eq = p.getData(EQUIPMENT);
                for (int i = 0; i < SIZE && i < payload.stacks().size(); i++) eq.setStackInSlot(i, payload.stacks().get(i));
            }
        }));
        r.playToServer(OpenPayload.TYPE, OpenPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) open(player);
        }));
        r.playToServer(DoubleJumpPayload.TYPE, DoubleJumpPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) TrinketEvents.doubleJumped(player);
        }));
        r.playToServer(BlinkPayload.TYPE, BlinkPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) TrinketEvents.blink(player, payload.dx(), payload.dz());
        }));
    }

    // ---- keeping everyone in sync, and dropping on death ----

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p);
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p);
    }

    private static void onTrack(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player target && event.getEntity() instanceof ServerPlayer watcher) {
            PacketDistributor.sendToPlayer(watcher, payload(target));
        }
    }

    private static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        // Deaths in the Titan Trials keep everything, trinkets included.
        if (com.sideflowgames.fulguritefusion.titan.TitanTrials.is(player.level())) return;
        Equipment eq = player.getData(EQUIPMENT);
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = eq.getStackInSlot(i);
            if (s.isEmpty()) continue;
            event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), s.copy()));
            eq.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(Trinkets::registerPayloads);
        NeoForge.EVENT_BUS.addListener(Trinkets::onLogin);
        NeoForge.EVENT_BUS.addListener(Trinkets::onRespawn);
        NeoForge.EVENT_BUS.addListener(Trinkets::onDimension);
        NeoForge.EVENT_BUS.addListener(Trinkets::onTrack);
        NeoForge.EVENT_BUS.addListener(Trinkets::onDrops);
    }
}
