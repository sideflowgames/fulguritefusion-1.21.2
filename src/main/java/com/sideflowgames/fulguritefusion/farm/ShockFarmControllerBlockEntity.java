package com.sideflowgames.fulguritefusion.farm;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.power.FmHandler;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;

// Shock Farm. Two sizes, both built on one floor level with the controller in the middle of an edge:
//
//   Tier 1 (5x5): a 3x3 pad of Galvanic Soil, ringed by Tempered Arc Casing. Each corner is a pillar of 2 Reinforced
//                 Fulgurite with a Tesla Prong on top.
//   Tier 2 (7x7): a 5x5 pad of Galvanic Soil, ringed by Charged Arc Casing. Corner pillars are 3 Reinforced
//                 Fulgurite with a Tesla Prong on top.
//
// At least one edge casing is swapped for a Farm Collector; Farm Power Ports may stand in for any other edge casing.
// A wall of glass 2 blocks high sits on top of every edge block (not the corner pillars) so mobs can't walk off.
// Leave the space over the pad clear.
// The farm spawns the mobs of the biome it stands in (monsters and animals): every vanilla mob except bosses and
// rare ones, plus Fulgurite Fusion's overworld creatures (see the shock_farm_banned / shock_farm_allowed entity tags).
// The prongs electrocute whatever is on the pad, and the drops and XP go into the collector.
// Upgrade cards in the controller menu (2 slots on a 5x5, 4 on a 7x7) filter the mob and change speed, cost and loot.
public class ShockFarmControllerBlockEntity extends BlockEntity implements FmHandler, net.minecraft.world.MenuProvider,
        software.bernie.geckolib.animatable.GeoBlockEntity {
    public static final int CAPACITY = 50_000;
    public static final int SPAWN_FM = 2_000;
    public static final int ZAP_FM = 100;
    public static final float ZAP_DAMAGE = 10.0f;

    public static final TagKey<EntityType<?>> BANNED = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "shock_farm_banned"));
    public static final TagKey<EntityType<?>> ALLOWED = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "shock_farm_allowed"));

    private int fm;
    private int tier;                       // 0 = not formed
    private String problem = "not checked yet";
    private final List<BlockPos> collectors = new ArrayList<>();
    private int spawnTimer;
    private String lastNote = "";
    private boolean enabled = true;
    private int formedTier;                 // the size whose parts are currently hidden (drawn as one model)
    private int mobCount;

    // Upgrade slots. Slots 2 and 3 only count on a 7x7 farm.
    public static final int UPGRADE_SLOTS = 4;
    public final net.minecraft.world.SimpleContainer upgrades = new net.minecraft.world.SimpleContainer(UPGRADE_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            ShockFarmControllerBlockEntity.this.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    // Menu status codes.
    public static final int ST_UNFORMED = 0, ST_RUNNING = 1, ST_OFF = 2, ST_NO_POWER = 3, ST_FULL = 4, ST_NOTHING = 5, ST_FILTER = 6;
    public static final int DATA_COUNT = 11;

    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> fm & 0xFFFF;
                case 1 -> (fm >>> 16) & 0xFFFF;
                case 2 -> CAPACITY & 0xFFFF;
                case 3 -> (CAPACITY >>> 16) & 0xFFFF;
                case 4 -> enabled ? 1 : 0;
                case 5 -> statusCode();
                case 6 -> tier;
                case 7 -> mobCount;
                case 8 -> tier == 0 ? 0 : cap(tier);
                case 9 -> Math.min(0xFFFF, spawnCost());
                case 10 -> spawnInterval();
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

    public ShockFarmControllerBlockEntity(BlockPos pos, BlockState state) {
        super(FarmBlocks.CONTROLLER_BE.get(), pos, state);
    }

    // ---------- tier settings ----------

    private static int radius(int tier) {
        return tier == 2 ? 3 : 2;
    }

    private static int pillar(int tier) {
        return tier == 2 ? 3 : 2;
    }

    private static int interval(int tier) {
        return tier == 2 ? 40 : 80;
    }

    private static int perWave(int tier) {
        return tier == 2 ? 2 : 1;
    }

    private static int cap(int tier) {
        return tier == 2 ? 10 : 4;
    }

    // ---------- upgrades ----------

    public static int unlockedSlots(int tier) {
        return tier == 2 ? 4 : 2;
    }

    private int sum(FarmUpgradeItem.Kind kind) {
        int total = 0, n = unlockedSlots(tier);
        for (int i = 0; i < n; i++) {
            if (upgrades.getItem(i).getItem() instanceof FarmUpgradeItem u && u.kind == kind) total += u.tier;
        }
        return total;
    }

    private java.util.Set<EntityType<?>> filters() {
        java.util.Set<EntityType<?>> out = new java.util.HashSet<>();
        int n = unlockedSlots(tier);
        for (int i = 0; i < n; i++) {
            ItemStack st = upgrades.getItem(i);
            if (st.getItem() instanceof FarmUpgradeItem u && u.kind == FarmUpgradeItem.Kind.MOB_FILTER) {
                EntityType<?> t = FarmUpgradeItem.filterMob(st);
                if (t != null) out.add(t);
            }
        }
        return out;
    }

    // Efficiency: -15% per tier, at most -75%.
    private double efficiency() {
        return 1.0 - Math.min(0.75, 0.15 * sum(FarmUpgradeItem.Kind.EFFICIENCY));
    }

    public int spawnCost() {
        return (int) Math.round(SPAWN_FM * (1.0 + 0.25 * sum(FarmUpgradeItem.Kind.SPEED)) * efficiency());
    }

    public int zapCost() {
        return Math.max(1, (int) Math.round(ZAP_FM * efficiency()));
    }

    public int spawnInterval() {
        return Math.max(5, (int) Math.round(interval(Math.max(1, tier)) / (1.0 + 0.5 * sum(FarmUpgradeItem.Kind.SPEED))));
    }

    private int looting() {
        return Math.min(3, sum(FarmUpgradeItem.Kind.LOOTING));
    }

    private double xpBonus() {
        return 1.0 + 0.25 * Math.min(8, sum(FarmUpgradeItem.Kind.LOOTING));
    }

    private static Block casing(int tier) {
        return tier == 2 ? ModBlocks.CHARGED_ARC_CASING.get() : ModBlocks.TEMPERED_ARC_CASING.get();
    }

    private Direction facing() {
        return getBlockState().getValue(ShockFarmControllerBlock.FACING);
    }

    private BlockPos center(int tier) {
        return worldPosition.relative(facing(), radius(tier));
    }

    // ---------- structure ----------

    private record Check(int matched, @Nullable String problem, List<BlockPos> collectors) {}

    private Check check(int tier) {
        int r = radius(tier), h = pillar(tier), matched = 0;
        BlockPos c = center(tier);
        List<BlockPos> found = new ArrayList<>();
        Block casing = casing(tier);
        String name = tier == 2 ? "Charged Arc Casing" : "Tempered Arc Casing";
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos p = c.offset(dx, 0, dz);
                boolean edgeX = Math.abs(dx) == r, edgeZ = Math.abs(dz) == r;
                if (!edgeX && !edgeZ) {
                    if (!level.getBlockState(p).is(FarmBlocks.GALVANIC_SOIL.get())) {
                        return new Check(matched, "the pad needs Galvanic Soil (" + (2 * r - 1) + "x" + (2 * r - 1) + ")", found);
                    }
                    if (!level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                            || !level.getBlockState(p.above(2)).getCollisionShape(level, p.above(2)).isEmpty()) {
                        return new Check(matched, "keep the space over the pad clear", found);
                    }
                } else if (edgeX && edgeZ) {
                    for (int y = 0; y < h; y++) {
                        if (!level.getBlockState(p.above(y)).is(ModBlocks.REINFORCED_FULGURITE.get())) {
                            return new Check(matched, "each corner needs a pillar of " + h + " Reinforced Fulgurite", found);
                        }
                    }
                    if (!level.getBlockState(p.above(h)).is(FarmBlocks.TESLA_PRONG.get())) {
                        return new Check(matched, "each corner pillar needs a Tesla Prong on top", found);
                    }
                } else {
                    if (!p.equals(worldPosition)) {
                        BlockState s = level.getBlockState(p);
                        if (s.is(FarmBlocks.COLLECTOR.get())) found.add(p.immutable());
                        else if (!s.is(casing) && !s.is(FarmBlocks.POWER_PORT.get())) return new Check(matched, "the edge needs " + name, found);
                    }
                    if (!isGlass(level.getBlockState(p.above())) || !isGlass(level.getBlockState(p.above(2)))) {
                        return new Check(matched, "the edge needs a glass wall 2 blocks high on top", found);
                    }
                }
                matched++;
            }
        }
        if (found.isEmpty()) return new Check(matched, "swap one edge casing for a Farm Collector", found);
        return new Check(matched, null, found);
    }

    public static boolean isGlass(BlockState s) {
        return s.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) || s.is(net.minecraft.world.level.block.Blocks.TINTED_GLASS)
                || s.is(ModBlocks.FULGURITE_GLASS.get()) || s.is(ModBlocks.FULGURITE_BLAST_GLASS.get());
    }

    // Whether a Farm Power Port at p sits on the edge of this farm (either size).
    public boolean acceptsPort(BlockPos p) {
        if (p.getY() != worldPosition.getY() || p.equals(worldPosition)) return false;
        for (int t = 1; t <= 2; t++) {
            if (tier != 0 && tier != t) continue;
            int r = radius(t);
            BlockPos c = center(t);
            int dx = Math.abs(p.getX() - c.getX()), dz = Math.abs(p.getZ() - c.getZ());
            if ((dx == r && dz < r) || (dz == r && dx < r)) return true;
        }
        return false;
    }

    private void checkStructure() {
        Check big = check(2);
        if (big.problem() == null) {
            setTier(2, big);
            return;
        }
        Check small = check(1);
        if (small.problem() == null) {
            setTier(1, small);
            return;
        }
        tier = 0;
        collectors.clear();
        problem = big.matched() > small.matched() ? "7x7: " + big.problem() : small.problem();
        applyForm(0);
    }

    // ---------- forming: hide the parts, draw the farm as one model ----------

    private void applyForm(int newTier) {
        if (formedTier == newTier) {
            // Already formed: still merge any collector or power port added since.
            if (newTier > 0) setParts(newTier, true);
            return;
        }
        if (formedTier > 0) setParts(formedTier, false);
        if (newTier > 0) setParts(newTier, true);
        formedTier = newTier;
        BlockState s = getBlockState();
        if (s.hasProperty(ShockFarmControllerBlock.SIZE) && s.getValue(ShockFarmControllerBlock.SIZE) != newTier) {
            level.setBlock(worldPosition, s.setValue(ShockFarmControllerBlock.SIZE, newTier), 3);
        }
        setChanged();
    }

    // Called when the controller is broken.
    public void unform() {
        if (level == null || level.isClientSide || formedTier == 0) return;
        setParts(formedTier, false);
        formedTier = 0;
    }

    private void setParts(int t, boolean on) {
        int r = radius(t), h = pillar(t);
        BlockPos c = center(t);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos p = c.offset(dx, 0, dz);
                boolean ex = Math.abs(dx) == r, ez = Math.abs(dz) == r;
                if (!ex && !ez) {
                    setFlag(p, GalvanicSoilBlock.FORMED, FarmBlocks.GALVANIC_SOIL.get(), on);
                } else if (ex && ez) {
                    for (int y = 0; y < h; y++) {
                        setFlag(p.above(y), com.sideflowgames.fulguritefusion.block.FormableBlock.FORMED, ModBlocks.REINFORCED_FULGURITE.get(), on);
                    }
                    setFlag(p.above(h), GalvanicSoilBlock.FORMED, FarmBlocks.TESLA_PRONG.get(), on);
                } else if (level.getBlockState(p).is(FarmBlocks.COLLECTOR.get())) {
                    setFlag(p, GalvanicSoilBlock.FORMED, FarmBlocks.COLLECTOR.get(), on);
                } else if (level.getBlockState(p).is(FarmBlocks.POWER_PORT.get())) {
                    setFlag(p, GalvanicSoilBlock.FORMED, FarmBlocks.POWER_PORT.get(), on);
                } else if (level.getBlockState(p).getBlock() instanceof com.sideflowgames.fulguritefusion.block.ArcCasingBlock
                        && level.getBlockEntity(p) instanceof com.sideflowgames.fulguritefusion.block.ArcCasingBlockEntity casing
                        && casing.controllerPos() == null) {
                    BlockState s = level.getBlockState(p);
                    if (s.getValue(com.sideflowgames.fulguritefusion.block.ArcCasingBlock.FORMED) != on) {
                        level.setBlock(p, s.setValue(com.sideflowgames.fulguritefusion.block.ArcCasingBlock.FORMED, on), 3);
                    }
                }
            }
        }
    }

    private void setFlag(BlockPos p, net.minecraft.world.level.block.state.properties.BooleanProperty prop, Block block, boolean on) {
        BlockState s = level.getBlockState(p);
        if (s.is(block) && s.hasProperty(prop) && s.getValue(prop) != on) level.setBlock(p, s.setValue(prop, on), 3);
    }

    private void setTier(int t, Check c) {
        tier = t;
        problem = null;
        collectors.clear();
        collectors.addAll(c.collectors());
        applyForm(t);
    }

    // ---------- ticking ----------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShockFarmControllerBlockEntity be) {
        if (!(level instanceof ServerLevel server)) return;
        long t = level.getGameTime();
        if (t % 20 == 0) be.checkStructure();
        boolean active = be.tier > 0 && be.enabled && be.fm >= be.zapCost();
        BlockState now = level.getBlockState(pos);
        if (now.hasProperty(ShockFarmControllerBlock.ACTIVE) && now.getValue(ShockFarmControllerBlock.ACTIVE) != active) {
            level.setBlock(pos, now.setValue(ShockFarmControllerBlock.ACTIVE, active), 3);
        }
        if (be.tier == 0) return;
        AABB area = be.area();
        if (t % 10 == 0) {
            be.mobCount = server.getEntitiesOfClass(Mob.class, area.inflate(1.5, 1, 1.5), Entity::isAlive).size();
            if (be.enabled) be.zap(server, area);
            be.sweep(server, area.inflate(1.5, 1, 1.5));
        }
        if (!be.enabled) return;
        if (++be.spawnTimer >= be.spawnInterval()) {
            be.spawnTimer = 0;
            be.spawnWave(server, area);
        }
    }

    private AABB area() {
        int r = radius(tier) - 1;
        BlockPos c = center(tier);
        return new AABB(c.getX() - r, c.getY() + 1, c.getZ() - r, c.getX() + r + 1, c.getY() + 4, c.getZ() + r + 1);

    }

    private List<Vec3> prongTips() {
        int r = radius(tier), h = pillar(tier);
        BlockPos c = center(tier);
        List<Vec3> out = new ArrayList<>();
        for (int sx : new int[] {-r, r}) for (int sz : new int[] {-r, r}) {
            out.add(Vec3.atCenterOf(c.offset(sx, h, sz)).add(0, 0.4, 0));
        }
        return out;
    }

    private FakePlayer killer(ServerLevel level) {
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        Vec3 c = Vec3.atCenterOf(center(tier));
        fake.setPos(c.x, c.y, c.z);
        return fake;
    }

    private void zap(ServerLevel level, AABB area) {
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, area.inflate(0.5, 0, 0.5), m -> m.isAlive());
        if (mobs.isEmpty()) return;
        List<Vec3> tips = prongTips();
        FakePlayer fake = killer(level);
        int loot = looting(), cost = zapCost();
        ItemStack weapon = ItemStack.EMPTY;
        if (loot > 0) {
            weapon = new ItemStack(net.minecraft.world.item.Items.STICK);
            weapon.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), loot);
        }
        fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
        int i = 0;
        for (Mob m : mobs) {
            if (fm < cost) {
                lastNote = "needs power";
                break;
            }
            fm -= cost;
            arc(level, tips.get(i++ % tips.size()), m.position().add(0, m.getBbHeight() * 0.6, 0));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4, false, false));
            m.invulnerableTime = 0;
            m.hurt(level.damageSources().playerAttack(fake), ZAP_DAMAGE);
        }
        fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.BLOCKS, 0.25f, 1.9f);
    }

    private static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        int steps = (int) Math.max(6, from.distanceTo(to) * 4);
        for (int i = 0; i <= steps; i++) {
            double f = i / (double) steps;
            Vec3 p = from.lerp(to, f);
            double j = Math.sin(f * Math.PI) * 0.3;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x + (level.random.nextDouble() - 0.5) * j,
                    p.y + (level.random.nextDouble() - 0.5) * j, p.z + (level.random.nextDouble() - 0.5) * j, 1, 0, 0, 0, 0);
        }
    }

    // Drops and XP orbs on and around the pad go into the collectors.
    private void sweep(ServerLevel level, AABB box) {
        List<FarmCollectorBlockEntity> bins = new ArrayList<>();
        for (BlockPos p : collectors) if (level.getBlockEntity(p) instanceof FarmCollectorBlockEntity b) bins.add(b);
        if (bins.isEmpty()) return;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive())) {
            ItemStack left = item.getItem();
            for (FarmCollectorBlockEntity b : bins) {
                if (left.isEmpty()) break;
                left = b.insert(left);
            }
            if (left.isEmpty()) item.discard();
            else item.setItem(left);
        }
        double xp = xpBonus();
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box, e -> e.isAlive())) {
            bins.get(0).addXp((int) Math.round(orb.getValue() * xp));
            orb.discard();
        }
        boolean full = true;
        for (FarmCollectorBlockEntity b : bins) full &= b.isFull();
        if (full) lastNote = "collector full";
        else if (lastNote.equals("collector full")) lastNote = "";
    }

    private void spawnWave(ServerLevel level, AABB area) {
        int alive = level.getEntitiesOfClass(Mob.class, area.inflate(1.5, 1, 1.5), Entity::isAlive).size();
        List<MobSpawnSettings.SpawnerData> choices = choices(level);
        if (choices.isEmpty()) {
            lastNote = "nothing can spawn in this biome";
            return;
        }
        if (lastNote.equals("nothing can spawn in this biome")) lastNote = "";
        java.util.Set<EntityType<?>> filter = filters();
        if (!filter.isEmpty()) {
            choices.removeIf(d -> !filter.contains(d.type));
            if (choices.isEmpty()) {
                lastNote = "filter mob can't spawn here";
                return;
            }
        }
        if (lastNote.equals("filter mob can't spawn here")) lastNote = "";
        int cost = spawnCost();
        for (int n = 0; n < perWave(tier) && alive < cap(tier); n++) {
            if (fm < cost) {
                lastNote = "needs power";
                return;
            }
            if (spawnOne(level, pick(level, choices))) {
                fm -= cost;
                alive++;
                lastNote = "";
                setChanged();
            }
        }
    }

    private List<MobSpawnSettings.SpawnerData> choices(ServerLevel level) {
        BlockPos c = center(Math.max(1, tier)).above();
        MobSpawnSettings settings = level.getBiome(c).value().getMobSettings();
        List<MobSpawnSettings.SpawnerData> out = new ArrayList<>();
        for (MobCategory cat : new MobCategory[] {MobCategory.MONSTER, MobCategory.CREATURE}) {
            if (cat == MobCategory.MONSTER && level.getDifficulty() == Difficulty.PEACEFUL) continue;
            for (MobSpawnSettings.SpawnerData d : settings.getMobs(cat).unwrap()) if (allowed(d.type)) out.add(d);
        }
        return out;
    }

    public static boolean allowed(EntityType<?> type) {
        if (type.is(BANNED)) return false;
        if ("minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) return true;
        return type.is(ALLOWED);
    }

    private static MobSpawnSettings.SpawnerData pick(ServerLevel level, List<MobSpawnSettings.SpawnerData> list) {
        int total = 0;
        for (MobSpawnSettings.SpawnerData d : list) total += Math.max(1, d.getWeight().asInt());
        int roll = level.random.nextInt(total);
        for (MobSpawnSettings.SpawnerData d : list) {
            roll -= Math.max(1, d.getWeight().asInt());
            if (roll < 0) return d;
        }
        return list.get(0);
    }

    private boolean spawnOne(ServerLevel level, MobSpawnSettings.SpawnerData data) {
        int r = radius(tier) - 1;
        BlockPos c = center(tier);
        for (int attempt = 0; attempt < 4; attempt++) {
            BlockPos p = c.offset(level.random.nextInt(2 * r + 1) - r, 1, level.random.nextInt(2 * r + 1) - r);
            double x = p.getX() + 0.5, y = p.getY(), z = p.getZ() + 0.5;
            if (!level.noCollision(data.type.getSpawnAABB(x, y, z))) continue;
            Entity e = data.type.create(level);
            if (!(e instanceof Mob mob)) {
                if (e != null) e.discard();
                return false;
            }
            mob.moveTo(x, y, z, level.random.nextFloat() * 360f, 0f);
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(p), MobSpawnType.SPAWNER, null);
            mob.addTag("ff_shock_farm");
            level.addFreshEntityWithPassengers(mob);
            level.sendParticles(ParticleTypes.SOUL, x, y + 0.2, z, 10, 0.3, 0.1, 0.3, 0.02);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.5, z, 12, 0.3, 0.5, 0.3, 0.1);
            level.playSound(null, p, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 0.8f, 0.8f);
            return true;
        }
        return false;
    }

    // ---------- menu ----------

    public void toggle() {
        enabled = !enabled;
        setChanged();
    }

    public int statusCode() {
        if (tier == 0) return ST_UNFORMED;
        if (!enabled) return ST_OFF;
        return switch (lastNote) {
            case "needs power" -> ST_NO_POWER;
            case "collector full" -> ST_FULL;
            case "nothing can spawn in this biome" -> ST_NOTHING;
            case "filter mob can't spawn here" -> ST_FILTER;
            default -> fm < zapCost() ? ST_NO_POWER : ST_RUNNING;
        };
    }

    // Sent with the menu: where it is, what's wrong (if not formed), and what the biome can spawn here.
    public void writeMenuData(net.minecraft.network.RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        if (level instanceof ServerLevel server) checkStructure();
        buf.writeUtf(problem == null ? "" : problem);
        List<MobSpawnSettings.SpawnerData> list = level instanceof ServerLevel server ? choices(server) : List.of();
        java.util.Map<ResourceLocation, Integer> weights = new java.util.LinkedHashMap<>();
        for (MobSpawnSettings.SpawnerData d : list) {
            weights.merge(BuiltInRegistries.ENTITY_TYPE.getKey(d.type), Math.max(1, d.getWeight().asInt()), Integer::sum);
        }
        buf.writeVarInt(weights.size());
        weights.forEach((id, w) -> {
            buf.writeResourceLocation(id);
            buf.writeVarInt(w);
        });
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fulguritefusion.shock_farm_controller");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new ShockFarmMenu(id, inventory, this, data);
    }

    public int formedSize() {
        BlockState s = getBlockState();
        return s.hasProperty(ShockFarmControllerBlock.SIZE) ? s.getValue(ShockFarmControllerBlock.SIZE) : 0;
    }

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // ---------- status ----------

    public void status(Player player) {
        if (level instanceof ServerLevel server && tier == 0) checkStructure();
        Component line;
        if (tier == 0) {
            line = Component.literal("Shock Farm not formed: " + problem).withStyle(ChatFormatting.RED);
        } else {
            String size = tier == 2 ? "7x7" : "5x5";
            String extra = lastNote.isEmpty() ? "" : " | " + lastNote.toUpperCase();
            line = Component.literal("Shock Farm " + size + " | " + fm + " / " + CAPACITY + " Fm" + extra)
                    .withStyle(lastNote.isEmpty() ? ChatFormatting.AQUA : ChatFormatting.GOLD);
        }
        player.displayClientMessage(line, false);
    }

    // ---------- power ----------

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
        tag.putInt("SpawnTimer", spawnTimer);
        tag.putBoolean("Enabled", enabled);
        tag.putInt("FormedTier", formedTier);
        tag.put("Upgrades", net.minecraft.world.ContainerHelper.saveAllItems(new CompoundTag(), upgrades.getItems(), registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fm = tag.getInt("Fm");
        spawnTimer = tag.getInt("SpawnTimer");
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        formedTier = tag.getInt("FormedTier");
        upgrades.clearContent();
        if (tag.contains("Upgrades")) net.minecraft.world.ContainerHelper.loadAllItems(tag.getCompound("Upgrades"), upgrades.getItems(), registries);
    }
}
