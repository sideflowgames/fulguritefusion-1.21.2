package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.machine.MachineShape;
import com.sideflowgames.fulguritefusion.machine.MachineTier;
import com.sideflowgames.fulguritefusion.machine.MachineType;
import com.sideflowgames.fulguritefusion.menu.ProcessingMachineMenu;
import com.sideflowgames.fulguritefusion.power.DaisyChain;
import com.sideflowgames.fulguritefusion.power.FmHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

// The brain of a multiblock machine. Checks its Arc Casings once a second, forms or un-forms the machine,
// and processes items: slots 0-1 are inputs, 2-3 are outputs. Runs at the tier of its lowest casing.
public class MachineControllerBlockEntity extends BlockEntity implements FmHandler, MenuProvider, GeoBlockEntity {
    public static final int MAX_FM = 16000;
    public static final int SLOTS = 4;
    public static final int DATA_COUNT = 7; // fm, progress, ticks needed, tier, Fm/t, formed, latex (mB + 1; 0 = no tank)
    public static final int LATEX_CAPACITY = 16_000; // Latex Vulcanizer: 16 buckets
    public static final int LATEX_PER_RUN = 1000;
    public static final int RUBBER_PER_RUN = 3;
    private static final DustParticleOptions OUTLINE = new DustParticleOptions(new Vector3f(0.4f, 0.85f, 1.0f), 0.8f);

    private final MachineType type;
    private final DaisyChain chain = new DaisyChain();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation idle, working;

    private int fm;
    private int progress;
    private int tier; // 0 = not formed
    private boolean checkRequested = true;
    private int checkTimer;
    private int previewTicks;
    private final List<BlockPos> missing = new ArrayList<>();

    // What clients draw inside the formed machine (synced when it changes; see syncDisplay).
    private ItemStack shownInput = ItemStack.EMPTY, shownOutput = ItemStack.EMPTY;
    private int shownTier;
    private int displayHash;
    private int shownLatex; // 0-16

    // Latex Vulcanizer only: liquid latex piped (or poured) in.
    private final net.neoforged.neoforge.fluids.capability.templates.FluidTank latex =
            new net.neoforged.neoforge.fluids.capability.templates.FluidTank(LATEX_CAPACITY,
                    s -> s.is(com.sideflowgames.fulguritefusion.ModFluids.LATEX.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < 2; // outputs are filled by the machine only
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> fm;
                case 1 -> progress;
                case 2 -> tier > 0 ? MachineTier.ticksPerItem(type, tier) : 0;
                case 3 -> tier;
                case 4 -> MachineTier.fmPerTick(tier);
                case 5 -> tier > 0 ? 1 : 0;
                case 6 -> type.usesLatex() ? latex.getFluidAmount() + 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MachineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MACHINE_CONTROLLER_BE.get(), pos, state);
        this.type = ((MachineControllerBlock) state.getBlock()).type();
        this.idle = RawAnimation.begin().thenLoop("animation." + type.id + ".idle");
        this.working = RawAnimation.begin().thenLoop("animation." + type.id + ".working");
    }

    public MachineType type() {
        return type;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public void requestCheck() {
        checkRequested = true;
    }

    public Direction facingDir() {
        return facing();
    }

    private Direction facing() {
        return getBlockState().getValue(MachineControllerBlock.FACING);
    }

    // ---------- Forming ----------

    private void checkStructure() {
        if (level == null) return;
        List<MachineShape.Part> parts = type.shape.parts(worldPosition, facing());
        missing.clear();
        int lowest = 4;
        for (MachineShape.Part part : parts) {
            BlockState s = level.getBlockState(part.pos());
            if (s.getBlock() instanceof ArcCasingBlock casing && level.getBlockEntity(part.pos()) instanceof ArcCasingBlockEntity be
                    && (be.controllerPos() == null || be.belongsTo(worldPosition))) {
                lowest = Math.min(lowest, casing.tier());
            } else {
                missing.add(part.pos());
            }
        }
        if (missing.isEmpty()) {
            if (tier != lowest) form(parts, lowest);
        } else if (tier > 0) {
            unform(parts);
        }
    }

    private void form(List<MachineShape.Part> parts, int newTier) {
        boolean wasFormed = tier > 0;
        tier = newTier;
        for (MachineShape.Part part : parts) {
            if (level.getBlockEntity(part.pos()) instanceof ArcCasingBlockEntity be) be.link(worldPosition, part.role());
            BlockState s = level.getBlockState(part.pos());
            if (s.hasProperty(ArcCasingBlock.FORMED) && !s.getValue(ArcCasingBlock.FORMED)) {
                level.setBlock(part.pos(), s.setValue(ArcCasingBlock.FORMED, true), 3);
            }
        }
        level.setBlock(worldPosition, getBlockState().setValue(MachineControllerBlock.FORMED, true), 3);
        if (!wasFormed && level instanceof ServerLevel server) {
            server.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.4f);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                    worldPosition.getZ() + 0.5, 40, 1.0, 1.0, 1.0, 0.2);
        }
        setChanged();
    }

    private void unform(List<MachineShape.Part> parts) {
        tier = 0;
        progress = 0;
        for (MachineShape.Part part : parts) {
            if (level.getBlockEntity(part.pos()) instanceof ArcCasingBlockEntity be && be.belongsTo(worldPosition)) {
                be.unlink();
                BlockState s = level.getBlockState(part.pos());
                if (s.hasProperty(ArcCasingBlock.FORMED)) level.setBlock(part.pos(), s.setValue(ArcCasingBlock.FORMED, false), 3);
            }
        }
        BlockState own = level.getBlockState(worldPosition);
        if (own.getBlock() instanceof MachineControllerBlock) {
            level.setBlock(worldPosition, own.setValue(MachineControllerBlock.FORMED, false).setValue(MachineControllerBlock.WORKING, false), 3);
        }
        setChanged();
    }

    // The controller was broken: free the casings and drop the items.
    public void breakApart() {
        if (level == null || level.isClientSide) return;
        for (MachineShape.Part part : type.shape.parts(worldPosition, facing())) {
            if (level.getBlockEntity(part.pos()) instanceof ArcCasingBlockEntity be && be.belongsTo(worldPosition)) {
                be.unlink();
                BlockState s = level.getBlockState(part.pos());
                if (s.hasProperty(ArcCasingBlock.FORMED)) level.setBlock(part.pos(), s.setValue(ArcCasingBlock.FORMED, false), 3);
            }
        }
        for (int i = 0; i < SLOTS; i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(i));
        }
    }

    // Outline the missing casings for 10 seconds. Returns how many are missing.
    public int showPreview() {
        checkStructure();
        previewTicks = 200;
        return missing.size();
    }

    private void drawPreview(ServerLevel server) {
        for (BlockPos p : missing) {
            for (int e = 0; e < 12; e++) {
                for (int k = 0; k <= 3; k++) {
                    double t = k / 3.0;
                    double x, y, z;
                    // 12 edges of the block: 4 along each axis.
                    int a = e % 4, axis = e / 4;
                    double c1 = (a & 1), c2 = (a >> 1);
                    if (axis == 0) { x = t; y = c1; z = c2; }
                    else if (axis == 1) { x = c1; y = t; z = c2; }
                    else { x = c1; y = c2; z = t; }
                    server.sendParticles(OUTLINE, p.getX() + x, p.getY() + y, p.getZ() + z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    // ---------- Ticking ----------

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineControllerBlockEntity be) {
        if (be.checkRequested || ++be.checkTimer >= 20) {
            be.checkRequested = false;
            be.checkTimer = 0;
            be.checkStructure();
        }
        if (be.previewTicks > 0) {
            be.previewTicks--;
            if (be.previewTicks % 10 == 0 && !be.missing.isEmpty()) be.drawPreview((ServerLevel) level);
        }

        boolean working = false;
        if (be.tier > 0) {
            if (be.type.usesLatex()) be.pourBuckets();
            MachineType.Result recipe = be.recipe(level);
            if (recipe != null && be.fits(recipe)) {
                int cost = MachineTier.fmPerTick(be.tier);
                if (be.fm >= cost) {
                    be.fm -= cost;
                    be.progress++;
                    working = true;
                    if (be.progress >= MachineTier.ticksPerItem(be.type, be.tier)) {
                        // Roll the final result now (the crusher's flint is a chance).
                        MachineType.Result done = be.recipe(level);
                        if (done != null && be.fits(done)) {
                            be.finish(done);
                            be.progress = 0;
                        }
                    }
                }
            } else {
                be.progress = 0;
            }
        }

        // Pass power on to touching machines (not its own casings), up to 8 machines down a chain.
        int shared = be.chain.share(level, pos, be, be.fm, null);
        if (shared > 0) be.fm -= shared;

        if (level.getGameTime() % 10 == 0) be.syncDisplay();

        BlockState current = level.getBlockState(pos);
        if (current.getBlock() instanceof MachineControllerBlock && current.getValue(MachineControllerBlock.WORKING) != working) {
            level.setBlock(pos, current.setValue(MachineControllerBlock.WORKING, working), 3);
        }
        be.setChanged();
    }

    private MachineType.Result recipe(Level level) {
        if (type.usesLatex()) {
            return latex.getFluidAmount() >= LATEX_PER_RUN
                    ? new MachineType.Result(0, 0, new ItemStack(com.sideflowgames.fulguritefusion.ModItems.DRIED_RUBBER.get(), RUBBER_PER_RUN), ItemStack.EMPTY)
                    : null;
        }
        return type.findRecipe(level, items.getStackInSlot(0), items.getStackInSlot(1), level.random);
    }

    // Latex Buckets put in the input slots are poured into the tank; the empty bucket goes to the second output.
    private void pourBuckets() {
        for (int slot = 0; slot < 2; slot++) {
            ItemStack s = items.getStackInSlot(slot);
            if (!s.is(com.sideflowgames.fulguritefusion.ModItems.LATEX_BUCKET.get())) continue;
            if (latex.getSpace() < LATEX_PER_RUN || !fitsIn(3, new ItemStack(net.minecraft.world.item.Items.BUCKET))) return;
            latex.fill(new net.neoforged.neoforge.fluids.FluidStack(com.sideflowgames.fulguritefusion.ModFluids.LATEX.get(), LATEX_PER_RUN),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            items.extractItem(slot, 1, false);
            put(3, new ItemStack(net.minecraft.world.item.Items.BUCKET));
        }
    }

    private boolean fits(MachineType.Result r) {
        return fitsIn(2, r.out0()) && fitsIn(3, r.out1());
    }

    private boolean fitsIn(int slot, ItemStack out) {
        if (out.isEmpty()) return true;
        ItemStack there = items.getStackInSlot(slot);
        return there.isEmpty() || (ItemStack.isSameItemSameComponents(there, out)
                && there.getCount() + out.getCount() <= there.getMaxStackSize());
    }

    private void finish(MachineType.Result r) {
        if (type.usesLatex()) latex.drain(LATEX_PER_RUN, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        items.extractItem(0, r.take0(), false);
        if (r.take1() > 0) items.extractItem(1, r.take1(), false);
        put(2, r.out0());
        put(3, r.out1());
    }

    private void put(int slot, ItemStack out) {
        if (out.isEmpty()) return;
        ItemStack there = items.getStackInSlot(slot);
        if (there.isEmpty()) items.setStackInSlot(slot, out.copy());
        else there.grow(out.getCount());
        setChanged();
    }

    // ---------- Ports ----------

    private final IItemHandler inputPort = new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return items.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    };

    private final IItemHandler outputPort = new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot + 2); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return items.extractItem(slot + 2, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot + 2); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };

    // Latex in only (pipes and buckets).
    private final net.neoforged.neoforge.fluids.capability.IFluidHandler latexInlet = new net.neoforged.neoforge.fluids.capability.IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) { return latex.getFluid(); }
        @Override public int getTankCapacity(int tank) { return LATEX_CAPACITY; }
        @Override public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) { return latex.isFluidValid(stack); }
        @Override public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) { return tier > 0 ? latex.fill(resource, action) : 0; }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) { return net.neoforged.neoforge.fluids.FluidStack.EMPTY; }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) { return net.neoforged.neoforge.fluids.FluidStack.EMPTY; }
    };

    @javax.annotation.Nullable
    public net.neoforged.neoforge.fluids.capability.IFluidHandler latexInlet() {
        return type.usesLatex() ? latexInlet : null;
    }

    public IItemHandler inputPort() {
        return inputPort;
    }

    public IItemHandler outputPort() {
        return outputPort;
    }

    // ---------- Power ----------

    @Override
    public boolean acceptsConnector() {
        return false; // connectors go on the power port
    }

    @Override
    public int receiveFm(int amount, boolean simulate) {
        chain.touch(level, 0);
        return accept(amount, simulate);
    }

    @Override
    public int receiveFmFromChain(int amount, int depth) {
        chain.touch(level, depth);
        return accept(amount, false);
    }

    @Override
    public void touchChain(int depth) {
        chain.touch(level, depth);
    }

    private int accept(int amount, boolean simulate) {
        int accepted = Math.max(0, Math.min(amount, MAX_FM - fm));
        if (!simulate && accepted > 0) {
            fm += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public int getFm() {
        return fm;
    }

    @Override
    public int getMaxFm() {
        return MAX_FM;
    }

    public String statusText() {
        if (tier == 0) return getBlockState().getBlock().getName().getString() + ": not built";
        return getBlockState().getBlock().getName().getString() + ": " + MachineTier.NAMES[tier] + " (tier " + tier + ") | "
                + fm + " / " + MAX_FM + " Fm | " + MachineTier.fmPerTick(tier) + " Fm/t | "
                + MachineTier.ticksPerItem(type, tier) / 20.0 + "s per item";
    }

    // ---------- Menu ----------

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ProcessingMachineMenu(containerId, inventory, this, data);
    }

    // ---------- Model ----------

    public boolean isFormed() {
        return getBlockState().getValue(MachineControllerBlock.FORMED);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state ->
                state.setAndContinue(getBlockState().getValue(MachineControllerBlock.WORKING) ? working : idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ---------- What clients see ----------

    private ItemStack displayIn() {
        ItemStack s = items.getStackInSlot(0);
        return s.isEmpty() ? items.getStackInSlot(1) : s;
    }

    private ItemStack displayOut() {
        ItemStack s = items.getStackInSlot(2);
        return s.isEmpty() ? items.getStackInSlot(3) : s;
    }

    // Sends the shown input/output items and tier to nearby clients when they change.
    private void syncDisplay() {
        if (level == null || level.isClientSide) return;
        ItemStack in = displayIn(), out = displayOut();
        int h = java.util.Objects.hash(tier, latexSixteenths(), ItemStack.hashItemAndComponents(in), in.isEmpty(), ItemStack.hashItemAndComponents(out), out.isEmpty());
        if (h == displayHash) return;
        displayHash = h;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public ItemStack shownInput() {
        return shownInput;
    }

    public ItemStack shownOutput() {
        return shownOutput;
    }

    public int shownTier() {
        return shownTier;
    }

    // Latex Vulcanizer: how full its vat is, 0-1.
    public float shownLatex() {
        return shownLatex / 16f;
    }

    private int latexSixteenths() {
        return latex.isEmpty() ? 0 : Math.max(1, Math.round(latex.getFluidAmount() * 16f / LATEX_CAPACITY));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Tier", tier);
        tag.putInt("Latex", latexSixteenths());
        ItemStack in = displayIn(), out = displayOut();
        if (!in.isEmpty()) tag.put("In", in.copyWithCount(1).save(registries));
        if (!out.isEmpty()) tag.put("Out", out.copyWithCount(1).save(registries));
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // Clients only take the display data (the machine's real contents stay on the server).
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        shownTier = tag.getInt("Tier");
        shownLatex = tag.getInt("Latex");
        shownInput = tag.contains("In") ? ItemStack.parseOptional(registries, tag.getCompound("In")) : ItemStack.EMPTY;
        shownOutput = tag.contains("Out") ? ItemStack.parseOptional(registries, tag.getCompound("Out")) : ItemStack.EMPTY;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    // ---------- Save ----------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Fm", fm);
        tag.putInt("Progress", progress);
        tag.putInt("Tier", tier);
        if (type.usesLatex()) tag.put("Latex", latex.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        fm = tag.getInt("Fm");
        progress = tag.getInt("Progress");
        tier = tag.getInt("Tier");
        if (tag.contains("Latex")) latex.readFromNBT(registries, tag.getCompound("Latex"));
    }
}
