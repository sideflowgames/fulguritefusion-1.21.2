package com.sideflowgames.fulguritefusion.block;

import java.util.ArrayList;
import java.util.List;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.power.FmNetwork;
import com.sideflowgames.fulguritefusion.power.WireMath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A Machine Connector. It remembers the hanging wires strung from it to other connectors.
// Each connector re-reads its network once a second; only the network's master connector moves power.
public class MachineConnectorBlockEntity extends BlockEntity {
    // One hanging wire from this connector to another one, and what kind of wire it is.
    public record Link(BlockPos other, com.sideflowgames.fulguritefusion.power.WireTier tier) {
        public boolean insulated() {
            return tier.insulated;
        }
    }

    private final List<Link> links = new ArrayList<>();
    private FmNetwork.Snapshot network;
    private int rescanTimer;
    private int lastFlow;     // Fm moved last tick (master only)
    private boolean powered;  // power is flowing through this connector's network (synced for sparks)

    public MachineConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MACHINE_CONNECTOR_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineConnectorBlockEntity be) {
        if (be.network == null || ++be.rescanTimer >= 20) {
            be.rescanTimer = 0;
            be.network = FmNetwork.scan(level, pos);
        }
        // Keep our own copy: switching the network's power on/off clears be.network for a rescan next tick.
        FmNetwork.Snapshot net = be.network;
        if (!net.master().equals(pos)) return;

        be.lastFlow = FmNetwork.tick(level, net);
        boolean flowing = be.lastFlow > 0;
        if (flowing != be.powered) {
            FmNetwork.setPowered(level, net, flowing);
        }
        if (flowing && level instanceof ServerLevel serverLevel) {
            FmNetwork.hazards(serverLevel, net);
        }
    }

    // --- links ---

    public List<Link> getLinks() {
        return links;
    }

    public boolean isLinkedTo(BlockPos other) {
        for (Link link : links) {
            if (link.other().equals(other)) return true;
        }
        return false;
    }

    public void addLink(BlockPos other, com.sideflowgames.fulguritefusion.power.WireTier tier) {
        links.add(new Link(other, tier));
        changedAndSync();
    }

    public void removeLink(BlockPos other) {
        if (links.removeIf(link -> link.other().equals(other))) changedAndSync();
    }

    public boolean isPowered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        if (this.powered != powered) {
            this.powered = powered;
            changedAndSync();
        }
    }

    private void changedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        network = null; // re-read the network next tick
    }

    // The box the wires are drawn in (so they don't vanish when this block is off-screen).
    public AABB wireBounds() {
        AABB box = new AABB(worldPosition);
        Vec3 a = WireMath.attachPoint(level, worldPosition);
        for (Link link : links) {
            Vec3 b = WireMath.attachPoint(level, link.other());
            box = box.minmax(new AABB(a, b).inflate(0.5).expandTowards(0, -WireMath.sag(a, b, link.tier()) - 0.5, 0));
        }
        return box;
    }

    public String statusText() {
        if (network == null || level == null) return "Machine Connector: starting up";
        if (links.isEmpty()) return "Machine Connector: no wires attached (right-click it with a Wire Roll)";
        int linked = network.linkedMachines().size();
        BlockPos machine = MachineConnectorBlock.attachedPos(getBlockState(), worldPosition);
        String self;
        if (level.getBlockState(machine).is(com.sideflowgames.fulguritefusion.ModBlocks.CONSOLE_POWER_PORT.get())) {
            self = com.sideflowgames.fulguritefusion.block.ReactorConsoleBlockEntity.forPowerPort(level, machine) != null
                    ? "reactor power port: supplies power" : "reactor power port (console not formed)";
        } else if (!MachineConnectorBlock.isMachine(level, machine)) {
            self = "relay (not on a machine)";
        } else if (network.generators().contains(machine)) {
            self = "this machine supplies power";
        } else if (network.batteries().contains(machine)) {
            self = "this battery stores power";
        } else {
            self = "this machine is linked";
        }
        int flow = 0;
        if (level.getBlockEntity(network.master()) instanceof MachineConnectorBlockEntity master) flow = master.lastFlow;
        return "Network: " + linked + " machine" + (linked == 1 ? "" : "s") + " | " + network.batteries().size() + " batteries | "
                + flow + " / " + network.rate() + " Fm/t | " + links.size() + " wires | " + self;
    }

    // --- saving and syncing to clients (so the wires can be drawn) ---

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveMachineConnectorBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveMachineConnectorBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        ListTag list = new ListTag();
        for (Link link : links) {
            CompoundTag l = new CompoundTag();
            l.putLong("Pos", link.other().asLong());
            l.putString("Tier", link.tier().id);
            list.add(l);
        }
        tag.put("Links", list);
        tag.putBoolean("Powered", powered);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadMachineConnectorBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadMachineConnectorBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        links.clear();
        ListTag list = tag.getListOrEmpty("Links");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag l = list.getCompoundOrEmpty(i);
            com.sideflowgames.fulguritefusion.power.WireTier tier;
            if (l.contains("Tier")) tier = com.sideflowgames.fulguritefusion.power.WireTier.byId(l.getStringOr("Tier", ""));
            else tier = l.getBooleanOr("Heavy", false) ? com.sideflowgames.fulguritefusion.power.WireTier.HEAVY   // older saves
                    : l.getBooleanOr("Insulated", false) ? com.sideflowgames.fulguritefusion.power.WireTier.INSULATED
                    : com.sideflowgames.fulguritefusion.power.WireTier.RAW;
            links.add(new Link(BlockPos.of(l.getLongOr("Pos", 0L)), tier));
        }
        powered = tag.getBooleanOr("Powered", false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // 26.1: runs the block's removal logic while this block entity still exists.
    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (level == null) return;
        if (state.getBlock() instanceof MachineConnectorBlock block) block.onRemoveLegacy(state, level, pos, level.getBlockState(pos), false);
    }
}
