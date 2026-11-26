package com.sideflowgames.fulguritefusion.util;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

// 26.1 port helper. Minecraft now saves block entities and entities through ValueInput / ValueOutput
// instead of CompoundTag. The mod's save code still builds CompoundTags; these helpers move a whole tag
// in and out of the new system so the saved data keeps exactly the same layout as in 1.21.1.
public final class NbtBridge {
    private NbtBridge() {}

    // Registry access for saving item stacks etc.
    public static HolderLookup.Provider registries(@Nullable Level level) {
        if (level != null) return level.registryAccess();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        return RegistryAccess.EMPTY;
    }

    // Everything stored in a ValueInput, as one CompoundTag.
    public static CompoundTag toTag(ValueInput in) {
        CompoundTag tag = new CompoundTag();
        for (String key : in.keySet()) {
            in.read(key, ExtraCodecs.NBT).ifPresent(t -> tag.put(key, t));
        }
        return tag;
    }

    // Writes every key of a CompoundTag into a ValueOutput.
    public static void write(ValueOutput out, CompoundTag tag) {
        if (out instanceof TagValueOutput t) {
            t.store(tag);
            return;
        }
        for (String key : tag.keySet()) out.store(key, ExtraCodecs.NBT, tag.get(key));
    }

    // ---------- item lists (ContainerHelper format: "Items" list with "Slot" bytes) ----------

    public static CompoundTag saveAllItems(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        return saveAllItems(tag, items, true, registries);
    }

    public static CompoundTag saveAllItems(CompoundTag tag, NonNullList<ItemStack> items, boolean alsoWhenEmpty, HolderLookup.Provider registries) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        ContainerHelper.saveAllItems(out, items, alsoWhenEmpty);
        tag.merge(out.buildResult());
        return tag;
    }

    public static void loadAllItems(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag), items);
    }

    // ---------- single stacks ----------

    public static Tag saveStack(ItemStack stack, HolderLookup.Provider registries) {
        return ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
                .result().orElseGet(CompoundTag::new);
    }

    public static ItemStack parseStack(HolderLookup.Provider registries, @Nullable Tag tag) {
        if (tag == null) return ItemStack.EMPTY;
        return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                .result().orElse(ItemStack.EMPTY);
    }

    public static ItemStack parseStack(HolderLookup.Provider registries, Optional<CompoundTag> tag) {
        return tag.map(t -> parseStack(registries, (Tag) t)).orElse(ItemStack.EMPTY);
    }

    // ---------- NeoForge ValueIOSerializable (ItemStackHandler, FluidTank, ...) ----------

    public static CompoundTag serialize(ValueIOSerializable thing, HolderLookup.Provider registries) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        thing.serialize(out);
        return out.buildResult();
    }

    public static void deserialize(ValueIOSerializable thing, HolderLookup.Provider registries, CompoundTag tag) {
        thing.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
    }

    public static void deserialize(ValueIOSerializable thing, HolderLookup.Provider registries, Optional<CompoundTag> tag) {
        deserialize(thing, registries, tag.orElseGet(CompoundTag::new));
    }

    // ---------- UUIDs (stored as 4-int arrays, as before) ----------

    public static void putUUID(CompoundTag tag, String key, UUID id) {
        tag.put(key, new IntArrayTag(UUIDUtil.uuidToIntArray(id)));
    }

    public static boolean hasUUID(CompoundTag tag, String key) {
        return tag.getIntArray(key).map(a -> a.length == 4).orElse(false);
    }

    public static UUID getUUID(CompoundTag tag, String key) {
        return UUIDUtil.uuidFromIntArray(tag.getIntArray(key).orElse(new int[4]));
    }

    // ---------- block positions (int array, as NbtUtils used to write) ----------

    public static Tag writeBlockPos(BlockPos pos) {
        return new IntArrayTag(new int[] {pos.getX(), pos.getY(), pos.getZ()});
    }

    public static Optional<BlockPos> readBlockPos(CompoundTag tag, String key) {
        return tag.getIntArray(key).filter(a -> a.length == 3).map(a -> new BlockPos(a[0], a[1], a[2]));
    }

    // SavedData in 26.1 is saved through a codec. This keeps the old tag-based load/save methods working.
    // The id keeps the old file name (data/<name>.dat) so the saved state stays in the same place.
    public static <T extends net.minecraft.world.level.saveddata.SavedData> net.minecraft.world.level.saveddata.SavedDataType<T> savedData(
            String name, java.util.function.Supplier<T> constructor,
            java.util.function.BiFunction<CompoundTag, HolderLookup.Provider, T> load,
            java.util.function.BiFunction<T, CompoundTag, CompoundTag> save) {
        com.mojang.serialization.Codec<T> codec = CompoundTag.CODEC.xmap(tag -> load.apply(tag, null), data -> save.apply(data, new CompoundTag()));
        return new net.minecraft.world.level.saveddata.SavedDataType<>(net.minecraft.resources.Identifier.withDefaultNamespace(name), constructor, codec, null);
    }
}
