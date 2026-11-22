package com.sideflowgames.fulguritefusion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public final class ModArmorMaterials {
    private ModArmorMaterials() {}

    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, FulguriteFusion.MODID);

    // Stormforged: netherite-level protection. Its special effects come from FmChargeable + StormArmorEvents.
    // Textures: textures/models/armor/stormforged_layer_1.png and _layer_2.png.
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STORMFORGED = MATERIALS.register("stormforged",
            () -> new ArmorMaterial(
                    new EnumMap<>(java.util.Map.of(
                            ArmorItem.Type.BOOTS, 3,
                            ArmorItem.Type.LEGGINGS, 6,
                            ArmorItem.Type.CHESTPLATE, 8,
                            ArmorItem.Type.HELMET, 3,
                            ArmorItem.Type.BODY, 11)),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.REFINED_FULGURITE.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "stormforged"))),
                    3.0f,
                    0.0f));

    // Prime: above netherite. Textures: textures/models/armor/prime_layer_1.png and _layer_2.png.
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PRIME = MATERIALS.register("prime",
            () -> new ArmorMaterial(
                    new EnumMap<>(java.util.Map.of(
                            ArmorItem.Type.BOOTS, 4,
                            ArmorItem.Type.LEGGINGS, 7,
                            ArmorItem.Type.CHESTPLATE, 9,
                            ArmorItem.Type.HELMET, 4,
                            ArmorItem.Type.BODY, 13)),
                    22,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.EMPTY,
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "prime"))),
                    4.0f,
                    0.15f));

    public static void register(IEventBus modEventBus) {
        MATERIALS.register(modEventBus);
    }
}
