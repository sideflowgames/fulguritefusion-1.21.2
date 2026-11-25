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


    // ---------- Stormreach creatures (entity/stormreach). Spawns are listed in each Stormreach biome. ----------
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.SparkhornEntity>> SPARKHORN = ENTITIES.register("sparkhorn",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.SparkhornEntity::new, MobCategory.CREATURE)
                    .sized(0.9f, 1.3f)
                    .clientTrackingRange(10)
                    .build("sparkhorn"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.PufflingEntity>> PUFFLING = ENTITIES.register("puffling",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.PufflingEntity::new, MobCategory.CREATURE)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .build("puffling"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.StaticJellyEntity>> STATIC_JELLY = ENTITIES.register("static_jelly",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.StaticJellyEntity::new, MobCategory.AMBIENT)
                    .sized(0.8f, 1.0f)
                    .clientTrackingRange(8)
                    .build("static_jelly"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.IonEelEntity>> ION_EEL = ENTITIES.register("ion_eel",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.IonEelEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.6f, 0.4f)
                    .clientTrackingRange(8)
                    .build("ion_eel"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.RubberbackSlothEntity>> RUBBERBACK_SLOTH = ENTITIES.register("rubberback_sloth",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.RubberbackSlothEntity::new, MobCategory.CREATURE)
                    .sized(0.9f, 1.2f)
                    .clientTrackingRange(10)
                    .build("rubberback_sloth"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.GlimmerwingEntity>> GLIMMERWING = ENTITIES.register("glimmerwing",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.GlimmerwingEntity::new, MobCategory.AMBIENT)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(8)
                    .build("glimmerwing"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.VoltcapSnailEntity>> VOLTCAP_SNAIL = ENTITIES.register("voltcap_snail",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.VoltcapSnailEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 0.7f)
                    .clientTrackingRange(8)
                    .build("voltcap_snail"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.GlasscrabEntity>> GLASSCRAB = ENTITIES.register("glasscrab",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.GlasscrabEntity::new, MobCategory.CREATURE)
                    .sized(0.8f, 0.5f)
                    .clientTrackingRange(8)
                    .build("glasscrab"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.ArcwurmEntity>> ARCWURM = ENTITIES.register("arcwurm",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.ArcwurmEntity::new, MobCategory.MONSTER)
                    .sized(1.4f, 1.2f)
                    .clientTrackingRange(10)
                    .build("arcwurm"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.CrystalStalkerEntity>> CRYSTAL_STALKER = ENTITIES.register("crystal_stalker",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.CrystalStalkerEntity::new, MobCategory.MONSTER)
                    .sized(1.4f, 0.9f)
                    .clientTrackingRange(10)
                    .build("crystal_stalker"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.StaticWraithEntity>> STATIC_WRAITH = ENTITIES.register("static_wraith",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.StaticWraithEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 1.6f)
                    .clientTrackingRange(10)
                    .build("static_wraith"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.ShockroachEntity>> SHOCKROACH = ENTITIES.register("shockroach",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.ShockroachEntity::new, MobCategory.MONSTER)
                    .sized(0.5f, 0.3f)
                    .clientTrackingRange(8)
                    .build("shockroach"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.ArcReaverEntity>> ARC_REAVER = ENTITIES.register("arc_reaver",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.ArcReaverEntity::new, MobCategory.MONSTER)
                    .sized(0.8f, 2.2f)
                    .clientTrackingRange(10)
                    .build("arc_reaver"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.ThunderhulkEntity>> THUNDERHULK = ENTITIES.register("thunderhulk",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.ThunderhulkEntity::new, MobCategory.MONSTER)
                    .sized(1.6f, 3.0f)
                    .clientTrackingRange(10)
                    .build("thunderhulk"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.StormcallerShadeEntity>> STORMCALLER_SHADE = ENTITIES.register("stormcaller_shade",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.StormcallerShadeEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 1.9f)
                    .clientTrackingRange(10)
                    .build("stormcaller_shade"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.StaticLeechEntity>> STATIC_LEECH = ENTITIES.register("static_leech",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.StaticLeechEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 0.45f)
                    .clientTrackingRange(10)
                    .build("static_leech"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.IonSwarmEntity>> ION_SWARM = ENTITIES.register("ion_swarm",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.IonSwarmEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 0.9f)
                    .clientTrackingRange(10)
                    .build("ion_swarm"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.VoltspitterEntity>> VOLTSPITTER = ENTITIES.register("voltspitter",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.VoltspitterEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 0.7f)
                    .clientTrackingRange(10)
                    .build("voltspitter"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.TempestRaptorEntity>> TEMPEST_RAPTOR = ENTITIES.register("tempest_raptor",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.TempestRaptorEntity::new, MobCategory.MONSTER)
                    .sized(1.2f, 0.9f)
                    .clientTrackingRange(10)
                    .build("tempest_raptor"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.GloomarcSpiderEntity>> GLOOMARC_SPIDER = ENTITIES.register("gloomarc_spider",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.GloomarcSpiderEntity::new, MobCategory.MONSTER)
                    .sized(1.5f, 0.9f)
                    .clientTrackingRange(10)
                    .build("gloomarc_spider"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.PeakColossusEntity>> PEAK_COLOSSUS = ENTITIES.register("peak_colossus",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.PeakColossusEntity::new, MobCategory.MONSTER)
                    .sized(2.4f, 5.0f)
                    .clientTrackingRange(12)
                    .build("peak_colossus"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.CinderBehemothEntity>> CINDER_BEHEMOTH = ENTITIES.register("cinder_behemoth",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.CinderBehemothEntity::new, MobCategory.MONSTER)
                    .sized(2.6f, 2.6f)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build("cinder_behemoth"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.AuroraWraithEntity>> AURORA_WRAITH = ENTITIES.register("aurora_wraith",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.AuroraWraithEntity::new, MobCategory.MONSTER)
                    .sized(1.4f, 3.0f)
                    .clientTrackingRange(12)
                    .build("aurora_wraith"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.PrismSentinelEntity>> PRISM_SENTINEL = ENTITIES.register("prism_sentinel",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.PrismSentinelEntity::new, MobCategory.MONSTER)
                    .sized(2.0f, 3.4f)
                    .clientTrackingRange(12)
                    .build("prism_sentinel"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.ChargedOrbEntity>> CHARGED_ORB = ENTITIES.register("charged_orb",
            () -> EntityType.Builder.<com.sideflowgames.fulguritefusion.entity.stormreach.ChargedOrbEntity>of(com.sideflowgames.fulguritefusion.entity.stormreach.ChargedOrbEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(6)
                    .updateInterval(10)
                    .build("charged_orb"));


    // ---------- More Stormreach hostiles, and the three mini-bosses (entity/boss) ----------
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.StormhoundEntity>> STORMHOUND = ENTITIES.register("stormhound",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.StormhoundEntity::new, MobCategory.MONSTER)
                    .sized(0.8f, 0.9f)
                    .clientTrackingRange(10)
                    .build("stormhound"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.stormreach.VoltcapCrawlerEntity>> VOLTCAP_CRAWLER = ENTITIES.register("voltcap_crawler",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.stormreach.VoltcapCrawlerEntity::new, MobCategory.MONSTER)
                    .sized(0.8f, 0.6f)
                    .clientTrackingRange(8)
                    .build("voltcap_crawler"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.boss.ThunderheadEntity>> THUNDERHEAD = ENTITIES.register("thunderhead",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.boss.ThunderheadEntity::new, MobCategory.MONSTER)
                    .sized(6.0f, 3.5f)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build("thunderhead"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.boss.MagmaConduitEntity>> MAGMA_CONDUIT = ENTITIES.register("magma_conduit",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.boss.MagmaConduitEntity::new, MobCategory.MONSTER)
                    .sized(3.0f, 6.8f)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build("magma_conduit"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.boss.VoidArcEntity>> VOID_ARC = ENTITIES.register("void_arc",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.boss.VoidArcEntity::new, MobCategory.MONSTER)
                    .sized(2.0f, 6.0f)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build("void_arc"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.boss.StormTitanEntity>> STORM_TITAN = ENTITIES.register("storm_titan",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.boss.StormTitanEntity::new, MobCategory.MONSTER)
                    .sized(5.0f, 14.0f)
                    .clientTrackingRange(20)
                    .fireImmune()
                    .build("storm_titan"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.boss.TitanConduitEntity>> TITAN_CONDUIT = ENTITIES.register("titan_conduit",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.boss.TitanConduitEntity::new, MobCategory.MONSTER)
                    .sized(1.2f, 3.4f)
                    .clientTrackingRange(14)
                    .fireImmune()
                    .build("titan_conduit"));
    // Mini-boss shield anchors.
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.boss.StormCoreEntity>> STORM_CORE = ENTITIES.register("storm_core",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.boss.StormCoreEntity::new, MobCategory.MONSTER)
                    .sized(1.2f, 1.2f)
                    .clientTrackingRange(12)
                    .fireImmune()
                    .build("storm_core"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.boss.MagmaPylonEntity>> MAGMA_PYLON = ENTITIES.register("magma_pylon",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.boss.MagmaPylonEntity::new, MobCategory.MONSTER)
                    .sized(1.4f, 3.2f)
                    .clientTrackingRange(12)
                    .fireImmune()
                    .build("magma_pylon"));
    public static final Supplier<EntityType<com.sideflowgames.fulguritefusion.entity.boss.VoidEchoEntity>> VOID_ECHO = ENTITIES.register("void_echo",
            () -> EntityType.Builder.of(com.sideflowgames.fulguritefusion.entity.boss.VoidEchoEntity::new, MobCategory.MONSTER)
                    .sized(2.0f, 6.0f)
                    .clientTrackingRange(12)
                    .fireImmune()
                    .build("void_echo"));

    private ModEntities() {}

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(LUMEN_SLUG.get(), LumenSlugEntity.createAttributes().build());
        event.put(VOIDMOTH.get(), VoidmothEntity.createAttributes().build());
        event.put(STORMWING.get(), StormwingEntity.createAttributes().build());
        event.put(ARC_SERPENT.get(), ArcSerpentEntity.createAttributes().build());
        event.put(STORMWING_HATCHLING.get(), StormwingHatchlingEntity.createAttributes().build());
        event.put(STORMHOUND.get(), com.sideflowgames.fulguritefusion.entity.stormreach.StormhoundEntity.createAttributes().build());
        event.put(VOLTCAP_CRAWLER.get(), com.sideflowgames.fulguritefusion.entity.stormreach.VoltcapCrawlerEntity.createAttributes().build());
        event.put(THUNDERHEAD.get(), com.sideflowgames.fulguritefusion.entity.boss.ThunderheadEntity.createAttributes().build());
        event.put(MAGMA_CONDUIT.get(), com.sideflowgames.fulguritefusion.entity.boss.MagmaConduitEntity.createAttributes().build());
        event.put(VOID_ARC.get(), com.sideflowgames.fulguritefusion.entity.boss.VoidArcEntity.createAttributes().build());
        event.put(STORM_TITAN.get(), com.sideflowgames.fulguritefusion.entity.boss.StormTitanEntity.createAttributes().build());
        event.put(TITAN_CONDUIT.get(), com.sideflowgames.fulguritefusion.entity.boss.TitanConduitEntity.createAttributes().build());
        event.put(STORM_CORE.get(), com.sideflowgames.fulguritefusion.entity.boss.StormCoreEntity.createAttributes().build());
        event.put(MAGMA_PYLON.get(), com.sideflowgames.fulguritefusion.entity.boss.MagmaPylonEntity.createAttributes().build());
        event.put(VOID_ECHO.get(), com.sideflowgames.fulguritefusion.entity.boss.VoidEchoEntity.createAttributes().build());
        event.put(SPARKHORN.get(), com.sideflowgames.fulguritefusion.entity.stormreach.SparkhornEntity.createAttributes().build());
        event.put(PUFFLING.get(), com.sideflowgames.fulguritefusion.entity.stormreach.PufflingEntity.createAttributes().build());
        event.put(STATIC_JELLY.get(), com.sideflowgames.fulguritefusion.entity.stormreach.StaticJellyEntity.createAttributes().build());
        event.put(ION_EEL.get(), com.sideflowgames.fulguritefusion.entity.stormreach.IonEelEntity.createAttributes().build());
        event.put(RUBBERBACK_SLOTH.get(), com.sideflowgames.fulguritefusion.entity.stormreach.RubberbackSlothEntity.createAttributes().build());
        event.put(GLIMMERWING.get(), com.sideflowgames.fulguritefusion.entity.stormreach.GlimmerwingEntity.createAttributes().build());
        event.put(VOLTCAP_SNAIL.get(), com.sideflowgames.fulguritefusion.entity.stormreach.VoltcapSnailEntity.createAttributes().build());
        event.put(GLASSCRAB.get(), com.sideflowgames.fulguritefusion.entity.stormreach.GlasscrabEntity.createAttributes().build());
        event.put(ARCWURM.get(), com.sideflowgames.fulguritefusion.entity.stormreach.ArcwurmEntity.createAttributes().build());
        event.put(CRYSTAL_STALKER.get(), com.sideflowgames.fulguritefusion.entity.stormreach.CrystalStalkerEntity.createAttributes().build());
        event.put(STATIC_WRAITH.get(), com.sideflowgames.fulguritefusion.entity.stormreach.StaticWraithEntity.createAttributes().build());
        event.put(SHOCKROACH.get(), com.sideflowgames.fulguritefusion.entity.stormreach.ShockroachEntity.createAttributes().build());
        event.put(ARC_REAVER.get(), com.sideflowgames.fulguritefusion.entity.stormreach.ArcReaverEntity.createAttributes().build());
        event.put(THUNDERHULK.get(), com.sideflowgames.fulguritefusion.entity.stormreach.ThunderhulkEntity.createAttributes().build());
        event.put(STORMCALLER_SHADE.get(), com.sideflowgames.fulguritefusion.entity.stormreach.StormcallerShadeEntity.createAttributes().build());
        event.put(STATIC_LEECH.get(), com.sideflowgames.fulguritefusion.entity.stormreach.StaticLeechEntity.createAttributes().build());
        event.put(ION_SWARM.get(), com.sideflowgames.fulguritefusion.entity.stormreach.IonSwarmEntity.createAttributes().build());
        event.put(VOLTSPITTER.get(), com.sideflowgames.fulguritefusion.entity.stormreach.VoltspitterEntity.createAttributes().build());
        event.put(TEMPEST_RAPTOR.get(), com.sideflowgames.fulguritefusion.entity.stormreach.TempestRaptorEntity.createAttributes().build());
        event.put(GLOOMARC_SPIDER.get(), com.sideflowgames.fulguritefusion.entity.stormreach.GloomarcSpiderEntity.createAttributes().build());
        event.put(PEAK_COLOSSUS.get(), com.sideflowgames.fulguritefusion.entity.stormreach.PeakColossusEntity.createAttributes().build());
        event.put(CINDER_BEHEMOTH.get(), com.sideflowgames.fulguritefusion.entity.stormreach.CinderBehemothEntity.createAttributes().build());
        event.put(AURORA_WRAITH.get(), com.sideflowgames.fulguritefusion.entity.stormreach.AuroraWraithEntity.createAttributes().build());
        event.put(PRISM_SENTINEL.get(), com.sideflowgames.fulguritefusion.entity.stormreach.PrismSentinelEntity.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(LUMEN_SLUG.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                LumenSlugEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(VOIDMOTH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                VoidmothEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(STORMWING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                StormwingEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(STORMHOUND.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormhoundEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(VOLTCAP_CRAWLER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.VoltcapCrawlerEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SPARKHORN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::ground, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PUFFLING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::ground, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(STATIC_JELLY.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::air, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ION_EEL.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::water, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(RUBBERBACK_SLOTH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::ground, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GLIMMERWING.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::air, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(VOLTCAP_SNAIL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::ground, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GLASSCRAB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::ground, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ARCWURM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::cave, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CRYSTAL_STALKER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::cave, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(STATIC_WRAITH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::cave, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SHOCKROACH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormreachSpawns::cave, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ARC_REAVER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(THUNDERHULK.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(STORMCALLER_SHADE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(STATIC_LEECH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ION_SWARM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(VOLTSPITTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(TEMPEST_RAPTOR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GLOOMARC_SPIDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PEAK_COLOSSUS.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormElite::checkEliteSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CINDER_BEHEMOTH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormElite::checkEliteSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(AURORA_WRAITH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormElite::checkEliteSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PRISM_SENTINEL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.sideflowgames.fulguritefusion.entity.stormreach.StormElite::checkEliteSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(ModEntities::attributes);
        modEventBus.addListener(ModEntities::spawnPlacements);
    }
}
