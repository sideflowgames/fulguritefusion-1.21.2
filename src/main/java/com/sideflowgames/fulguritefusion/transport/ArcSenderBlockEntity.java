package com.sideflowgames.fulguritefusion.transport;

import com.sideflowgames.fulguritefusion.item.ChainBolt;
import com.sideflowgames.fulguritefusion.power.FmHandler;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;

public class ArcSenderBlockEntity extends BlockEntity implements FmHandler {
    public static final int CAPACITY = 20_000;
    public static final int BOLT_FM = 400;
    public static final int RANGE = 5;
    public static final int MAX_STACKS = 27;

    private int fm;
    @Nullable private BlockPos target;

    public ArcSenderBlockEntity(BlockPos pos, BlockState state) {
        super(TransportBlocks.ARC_SENDER_BE.get(), pos, state);
    }

    @Nullable
    public BlockPos target() {
        return target;
    }

    public void setTarget(@Nullable BlockPos target) {
        this.target = target == null ? null : target.immutable();
        setChanged();
    }

    public static boolean inRange(BlockPos a, BlockPos b) {
        return a.distSqr(b) <= RANGE * RANGE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcSenderBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0 || be.target == null || be.fm < BOLT_FM) return;
        BlockPos t = be.target;
        if (!inRange(pos, t) || !level.isLoaded(t)) return;
        BlockState ts = level.getBlockState(t);
        if (!(ts.getBlock() instanceof ArcNodeBlock node) || node.isSender()) return;
        Direction from = state.getValue(ArcNodeBlock.FACING), to = ts.getValue(ArcNodeBlock.FACING);
        IItemHandler src = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(from), from.getOpposite());
        IItemHandler dst = level.getCapability(Capabilities.ItemHandler.BLOCK, t.relative(to), to.getOpposite());
        if (src == null || dst == null) return;
        int stacks = 0;
        for (int s = 0; s < src.getSlots() && stacks < MAX_STACKS; s++) {
            ItemStack peek = src.extractItem(s, 64, true);
            if (peek.isEmpty()) continue;
            ItemStack rest = ItemHandlerHelper.insertItem(dst, peek, true);
            int fits = peek.getCount() - rest.getCount();
            if (fits <= 0) continue;
            ItemStack taken = src.extractItem(s, fits, false);
            ItemStack left = ItemHandlerHelper.insertItem(dst, taken, false);
            if (!left.isEmpty()) src.insertItem(s, left, false);
            if (left.getCount() < taken.getCount()) stacks++;
        }
        if (stacks == 0) return;
        be.fm -= BOLT_FM;
        be.setChanged();
        if (level instanceof ServerLevel server) {
            Vec3 a = Vec3.atCenterOf(pos), b = Vec3.atCenterOf(t);
            PacketDistributor.sendToPlayersTrackingChunk(server, new ChunkPos(pos),
                    new ChainBolt.Payload(List.of(a, a.lerp(b, 0.5).add(0, 0.4, 0), b), false));
            server.playSound(null, pos, com.sideflowgames.fulguritefusion.ModSounds.ARC_BUZZ.get(), SoundSource.BLOCKS, 0.45f,
                    0.9f + server.random.nextFloat() * 0.2f);
        }
    }

    @Override
    public int receiveFm(int amount, boolean simulate) {
        int took = Math.max(0, Math.min(CAPACITY - fm, amount));
        if (!simulate && took > 0) {
            fm += took;
            setChanged();
        }
        return took;
    }

    @Override
    public int getFm() {
        return fm;
    }

    @Override
    public int getMaxFm() {
        return CAPACITY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fm", fm);
        if (target != null) tag.putLong("Target", target.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fm = tag.getInt("Fm");
        target = tag.contains("Target") ? BlockPos.of(tag.getLong("Target")) : null;
    }
}
