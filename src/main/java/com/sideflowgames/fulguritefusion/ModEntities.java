package com.sideflowgames.fulguritefusion;

import com.sideflowgames.fulguritefusion.entity.ArcSerpentEntity;
import com.sideflowgames.fulguritefusion.entity.LumenSlugEntity;
import com.sideflowgames.fulguritefusion.entity.StormwingEntity;
import com.sideflowgames.fulguritefusion.entity.StormwingHatchlingEntity;
import com.sideflowgames.fulguritefusion.entity.VoidmothEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Fulgurite Fusion's mobs. Spawn biomes are in data/fulguritefusion/neoforge/biome_modifier.
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, FulguriteFusion.MODID);

    public static final Supplier<EntityType<LumenSlugEntity>> LUMEN_SLUG = ENTITIES.register("lumen_slug",
            () -> EntityType.Builder.of(LumenSlugEntity::new, MobCategory.AMBIENT)
                    .sized(0.6f, 0.45f)
                    .clientTrackingRange(8)
                    .build("lumen_slug"));

    public static final Supplier<EntityType<VoidmothEntity>> VOIDMOTH = ENTITIES.register("voidmoth",
            () -> EntityType.Builder.of(VoidmothEntity::new, MobCategory.AMBIENT)
                    .sized(0.7f, 0.6f)
                    .clientTrackingRange(8)
                    .build("voidmoth"));

    public static final Supplier<EntityType<StormwingEntity>> STORMWING = ENTITIES.register("stormwing",
            () -> EntityType.Builder.of(StormwingEntity::new, MobCategory.CREATURE)
                    .sized(1.2f, 1.3f) // hitbox hugs the body; the 6-block wings are visual only
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build("stormwing"));

    // Hatches from an incubated Stormwing Egg; grows into an adult after a Minecraft day.
    public static final Supplier<EntityType<StormwingHatchlingEntity>> STORMWING_HATCHLING = ENTITIES.register("stormwing_hatchling",
            () -> EntityType.Builder.of(StormwingHatchlingEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 0.6f)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build("stormwing_hatchling"));

    // A thrown Fulgurite Spear or Storm Spear.
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.SpearEntity>> SPEAR = ENTITIES.register("spear",
            () -> EntityType.Builder.<com.sideflowgames.fulguritefusion.entity.SpearEntity>of(
                            com.sideflowgames.fulguritefusion.entity.SpearEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("spear"));

    // Not a natural spawn: appears at a nest while its Stormwing is away hunting.
    public static final Supplier<EntityType<ArcSerpentEntity>> ARC_SERPENT = ENTITIES.register("arc_serpent",
            () -> EntityType.Builder.of(ArcSerpentEntity::new, MobCategory.MONSTER)
                    .sized(1.6f, 1.4f)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build("arc_serpent"));

    private ModEntities() {}

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(LUMEN_SLUG.get(), LumenSlugEntity.createAttributes().build());
        event.put(VOIDMOTH.get(), VoidmothEntity.createAttributes().build());
        event.put(STORMWING.get(), StormwingEntity.createAttributes().build());
        event.put(ARC_SERPENT.get(), ArcSerpentEntity.createAttributes().build());
        event.put(STORMWING_HATCHLING.get(), StormwingHatchlingEntity.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(LUMEN_SLUG.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                LumenSlugEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(VOIDMOTH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                VoidmothEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(STORMWING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                StormwingEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(ModEntities::attributes);
        modEventBus.addListener(ModEntities::spawnPlacements);
    }
}
