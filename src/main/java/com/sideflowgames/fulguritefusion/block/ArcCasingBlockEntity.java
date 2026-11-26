package com.sideflowgames.fulguritefusion.block;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.machine.MachineShape.Role;
import com.sideflowgames.fulguritefusion.power.FmHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

// Remembers which machine this casing belongs to and which port it is.
//   - Power port: a Machine Connector can sit on it, and touching machines can feed it. Power goes to the controller.
//   - Input port: hoppers and pipes put items into the machine's input slots.
//   - Output port: hoppers and pipes take items from the machine's output slots.
public class ArcCasingBlockEntity extends BlockEntity implements FmHandler {
    @Nullable private BlockPos controllerPos;
    private Role role = Role.NONE;

    public ArcCasingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ARC_CASING_BE.get(), pos, state);
    }

    public void link(BlockPos controller, Role role) {
        this.controllerPos = controller;
        this.role = role;
        setChanged();
    }

    public void unlink() {
        controllerPos = null;
        role = Role.NONE;
        setChanged();
    }

    @Nullable
    public BlockPos controllerPos() {
        return controllerPos;
    }

    public boolean belongsTo(BlockPos controller) {
        return controller.equals(controllerPos);
    }

    @Nullable
    private MachineControllerBlockEntity controller() {
        return level != null && controllerPos != null
                && level.getBlockEntity(controllerPos) instanceof MachineControllerBlockEntity c ? c : null;
    }

    public void notifyController() {
        MachineControllerBlockEntity c = controller();
        if (c != null) c.requestCheck();
        else if (level != null && controllerPos != null
                && level.getBlockEntity(controllerPos) instanceof com.sideflowgames.fulguritefusion.harvester.LatexHarvesterBlockEntity h) {
            h.requestCheck(); // a Latex Harvester's frame
        }
    }

    // ---------- Power port ----------

    private boolean isPowerPort() {
        return role == Role.POWER && controller() != null;
    }

    @Override
    public boolean acceptsConnector() {
        return role == Role.POWER;
    }

    @Override
    public int receiveFm(int amount, boolean simulate) {
        return isPowerPort() ? controller().receiveFm(amount, simulate) : 0;
    }

    @Override
    public int receiveFmFromChain(int amount, int depth) {
        return isPowerPort() ? controller().receiveFmFromChain(amount, depth) : 0;
    }

    @Override
    public void touchChain(int depth) {
        if (isPowerPort()) controller().touchChain(depth);
    }

    @Override
    public int getFm() {
        return isPowerPort() ? controller().getFm() : 0;
    }

    @Override
    public int getMaxFm() {
        return isPowerPort() ? controller().getMaxFm() : 1;
    }

    // ---------- Item ports ----------

    // Latex Vulcanizer: any casing takes latex in, from any face except the machine's front.
    @Nullable
    public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidHandler(@Nullable Direction side) {
        MachineControllerBlockEntity c = controller();
        if (c == null || !c.type().usesLatex() || (side != null && side == c.facingDir())) return null;
        return c.latexInlet();
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        MachineControllerBlockEntity c = controller();
        if (c == null) return null;
        return switch (role) {
            case INPUT -> c.inputPort();
            case OUTPUT -> c.outputPort();
            default -> null;
        };
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        legacySaveArcCasingBlockEntity(tag, com.sideflowgames.fulguritefusion.util.NbtBridge.registries(level));
        com.sideflowgames.fulguritefusion.util.NbtBridge.write(output, tag);
    }

    protected void legacySaveArcCasingBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        if (controllerPos != null) tag.put("Controller", com.sideflowgames.fulguritefusion.util.NbtBridge.writeBlockPos(controllerPos));
        tag.putString("Role", role.name());
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        legacyLoadArcCasingBlockEntity(com.sideflowgames.fulguritefusion.util.NbtBridge.toTag(input), input.lookup());
    }

    protected void legacyLoadArcCasingBlockEntity(CompoundTag tag, HolderLookup.Provider registries) {
        
        controllerPos = com.sideflowgames.fulguritefusion.util.NbtBridge.readBlockPos(tag, "Controller").orElse(null);
        try {
            role = Role.valueOf(tag.getStringOr("Role", ""));
        } catch (IllegalArgumentException e) {
            role = Role.NONE;
        }
    }

    // 26.1: runs the block's removal logic while this block entity still exists.
    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (level == null) return;
        if (state.getBlock() instanceof ArcCasingBlock block) block.onRemoveLegacy(state, level, pos, level.getBlockState(pos), false);
    }

    // 26.1: the controller's real storage behind this casing's ports (see LegacyCaps).
    public net.neoforged.neoforge.items.IItemHandlerModifiable[] capStorage() {
        MachineControllerBlockEntity c = controller();
        return c == null ? new net.neoforged.neoforge.items.IItemHandlerModifiable[0] : c.capStorage();
    }

    public net.neoforged.neoforge.fluids.capability.templates.FluidTank[] fluidStorage() {
        MachineControllerBlockEntity c = controller();
        return c == null ? new net.neoforged.neoforge.fluids.capability.templates.FluidTank[0]
                : new net.neoforged.neoforge.fluids.capability.templates.FluidTank[] {c.latexTank()};
    }

}
