package com.sideflowgames.fulguritefusion.backpack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

// Optional Curios support (the 1.21 successor to Baubles), done by reflection so Curios isn't needed to build or
// play. When Curios is installed a backpack can sit in its "back" slot (see data/fulguritefusion/curios and the
// curios:back item tag); this finds it there.
public final class CuriosCompat {
    private CuriosCompat() {}

    private static Boolean loaded;
    private static Method getInventory, findFirst, stackOf;

    public static boolean loaded() {
        if (loaded == null) loaded = ModList.get().isLoaded("curios");
        return loaded;
    }

    // The first curio slot item matching the test, or EMPTY.
    public static ItemStack find(LivingEntity entity, Predicate<ItemStack> test) {
        if (!loaded()) return ItemStack.EMPTY;
        try {
            if (getInventory == null) {
                Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                getInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
            }
            Optional<?> inv = (Optional<?>) getInventory.invoke(null, entity);
            if (inv.isEmpty()) return ItemStack.EMPTY;
            Object handler = inv.get();
            if (findFirst == null) findFirst = handler.getClass().getMethod("findFirstCurio", Predicate.class);
            Optional<?> result = (Optional<?>) findFirst.invoke(handler, test);
            if (result.isEmpty()) return ItemStack.EMPTY;
            Object slot = result.get();
            if (stackOf == null) stackOf = slot.getClass().getMethod("stack");
            return (ItemStack) stackOf.invoke(slot);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            loaded = false; // a Curios version we don't understand: stop trying
            return ItemStack.EMPTY;
        }
    }
}
