package com.sideflowgames.fulguritefusion;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.IEventBus;

// Armor materials. In 26.1 these are plain data (no registry); the look comes from an equipment asset:
// assets/fulguritefusion/equipment/<name>.json, pointing at textures/entity/equipment/humanoid/<name>.png
// and humanoid_leggings/<name>.png.
public final class ModArmorMaterials {
    private ModArmorMaterials() {}

    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, name));
    }

    private static TagKey<Item> repair(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, name));
    }

    private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        return Map.of(ArmorType.BOOTS, boots, ArmorType.LEGGINGS, legs, ArmorType.CHESTPLATE, chest, ArmorType.HELMET, helmet, ArmorType.BODY, body);
    }

    // Stormforged: netherite-level protection. Its special effects come from FmChargeable + StormArmorEvents.
    public static final ArmorMaterial STORMFORGED = new ArmorMaterial(37, defense(3, 6, 8, 3, 11), 15,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0f, 0.0f, repair("repairs_stormforged_armor"), asset("stormforged"));

    // Prime: above netherite.
    public static final ArmorMaterial PRIME = new ArmorMaterial(37, defense(4, 7, 9, 4, 13), 22,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0f, 0.15f, repair("repairs_prime_armor"), asset("prime"));

    // Titan: dropped by the Storm Titan. Above Prime.
    public static final ArmorMaterial TITAN = new ArmorMaterial(37, defense(5, 8, 10, 5, 15), 25,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0f, 0.2f, repair("repairs_titan_armor"), asset("titan"));

    // Titan Prime: Prime and Titan armor fused in the Titan Forge.
    public static final ArmorMaterial TITAN_PRIME = new ArmorMaterial(37, defense(6, 9, 12, 6, 18), 30,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 7.0f, 0.25f, repair("repairs_titan_prime_armor"), asset("titan_prime"));

    // The armor settings for one piece. Like the 1.21.1 versions, the mod's armor never wears out, so no durability is set.
    public static Item.Properties armor(Item.Properties properties, ArmorMaterial material, ArmorType type) {
        return properties.attributes(material.createAttributes(type))
                .enchantable(material.enchantmentValue())
                .component(DataComponents.EQUIPPABLE, Equippable.builder(type.getSlot())
                        .setEquipSound(material.equipSound()).setAsset(material.assetId()).build());
    }

    public static void register(IEventBus modEventBus) {
        // Nothing to register any more: armor materials are plain data in 26.1.
    }
}
