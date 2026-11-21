package com.sideflowgames.fulguritefusion.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.block.NacreClusterBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

// Where the exotic materials come from in survival (the Sky Collector and the mobs handle the rest):
//   Abyssal Nacre  - clusters grow on prismarine touching water near an active Conduit in a deep ocean.
//   Soul Frost     - smother soul fire with a bucket of powder snow; break the Frozen Soul Fire.
//   Echo Resin     - scrape sculk with an axe in the Deep Dark (makes a noise the sculk can hear).
//   Gale Core      - dropped by Breezes.
//   Cinder Heart   - dropped by large Magma Cubes in the Basalt Deltas.
//   Wither Ash     - dropped by Wither Skeletons killed by (or soon after) a lightning strike.
//   Struck Spore   - shear a Mooshroom within a minute of it being struck by lightning.
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class MaterialSourceEvents {
    private static final String STRUCK = "fulguritefusion:struck";
    private static final int STRUCK_WINDOW = 1200; // ticks after a strike that still count
    private static final int NACRE_EVERY = 200, NACRE_CAP = 16;

    private MaterialSourceEvents() {}

    // ---------- Soul Frost and Echo Resin ----------

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        BlockPos pos = event.getPos();
        Direction face = event.getFace() != null ? event.getFace() : Direction.UP;

        // Powder snow on soul fire: it freezes mid-flicker.
        if (held.is(Items.POWDER_SNOW_BUCKET)) {
            BlockPos fire = level.getBlockState(pos).is(Blocks.SOUL_FIRE) ? pos : pos.relative(face);
            if (!level.getBlockState(fire).is(Blocks.SOUL_FIRE)) return;
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (level.isClientSide) return;
            level.setBlock(fire, ModBlocks.FROZEN_SOUL_FIRE.get().defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, fire, SoundEvents.POWDER_SNOW_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
            level.playSound(null, fire, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.4f);
            if (!player.getAbilities().instabuild) player.setItemInHand(event.getHand(), new ItemStack(Items.BUCKET));
            return;
        }

        // Axe on sculk in the Deep Dark: scrape off Echo Resin. Loud enough for the sculk to hear.
        if (held.is(ItemTags.AXES) && level.getBlockState(pos).is(Blocks.SCULK)) {
            if (!level.getBiome(pos).is(Biomes.DEEP_DARK)) return;
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (level.isClientSide || player.getCooldowns().isOnCooldown(held.getItem())) return;
            player.getCooldowns().addCooldown(held.getItem(), 20);
            level.playSound(null, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0f, 0.7f);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
            if (level.random.nextFloat() < 0.6f) {
                Block.popResourceFromFace(level, pos, face, new ItemStack(ModItems.ECHO_RESIN.get()));
            }
        }
    }

    // ---------- Lightning marks (Struck Spore, Wither Ash) ----------

    @SubscribeEvent
    public static void onStruck(EntityStruckByLightningEvent event) {
        Entity e = event.getEntity();
        if (e instanceof MushroomCow || e instanceof WitherSkeleton) {
            e.getPersistentData().putLong(STRUCK, e.level().getGameTime());
        }
    }

    private static boolean recentlyStruck(Entity e) {
        if (!e.getPersistentData().contains(STRUCK)) return false;
        return e.level().getGameTime() - e.getPersistentData().getLong(STRUCK) <= STRUCK_WINDOW;
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getTarget() instanceof MushroomCow cow) || !event.getItemStack().is(Items.SHEARS)) return;
        if (!cow.readyForShearing() || !recentlyStruck(cow)) return;
        cow.getPersistentData().remove(STRUCK);
        int count = 2 + cow.getRandom().nextInt(2);
        cow.spawnAtLocation(new ItemStack(ModItems.STRUCK_SPORE.get(), count), 1.0f);
        ((ServerLevel) event.getLevel()).sendParticles(ParticleTypes.ELECTRIC_SPARK, cow.getX(), cow.getY() + 1, cow.getZ(),
                20, 0.4, 0.4, 0.4, 0.1);
    }

    // ---------- Mob drops ----------

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        Level level = e.level();
        if (level.isClientSide) return;
        ItemStack drop = ItemStack.EMPTY;
        if (e.getType() == EntityType.BREEZE) {
            if (level.random.nextFloat() < 0.7f) drop = new ItemStack(ModItems.GALE_CORE.get());
        } else if (e instanceof MagmaCube cube && cube.getSize() >= 4 && level.getBiome(e.blockPosition()).is(Biomes.BASALT_DELTAS)) {
            if (level.random.nextFloat() < 0.4f) drop = new ItemStack(ModItems.CINDER_HEART.get());
        } else if (e instanceof WitherSkeleton && (event.getSource().is(DamageTypeTags.IS_LIGHTNING) || recentlyStruck(e))) {
            drop = new ItemStack(ModItems.WITHER_ASH.get(), 2 + level.random.nextInt(3));
        }
        if (!drop.isEmpty()) event.getDrops().add(new ItemEntity(level, e.getX(), e.getY() + 0.5, e.getZ(), drop));
    }

    // ---------- Abyssal Nacre ----------

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % NACRE_EVERY != 0) return;
        Set<BlockPos> conduits = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            int pcx = player.blockPosition().getX() >> 4, pcz = player.blockPosition().getZ() >> 4;
            for (int cx = pcx - 3; cx <= pcx + 3; cx++) {
                for (int cz = pcz - 3; cz <= pcz + 3; cz++) {
                    if (!level.hasChunk(cx, cz)) continue;
                    LevelChunk chunk = level.getChunk(cx, cz);
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof ConduitBlockEntity conduit && conduit.isActive()
                                && level.getBiome(be.getBlockPos()).is(BiomeTags.IS_DEEP_OCEAN)) {
                            conduits.add(be.getBlockPos());
                        }
                    }
                }
            }
        }
        for (BlockPos c : conduits) {
            if (level.random.nextFloat() < 0.4f) growNacre(level, c);
        }
    }

    private static void growNacre(ServerLevel level, BlockPos conduit) {
        Block cluster = ModBlocks.NACRE_CLUSTER.get();
        int existing = 0;
        List<BlockPos[]> spots = new ArrayList<>(); // {prismarine block, where the cluster goes}
        int r = 4;
        for (BlockPos p : BlockPos.betweenClosed(conduit.offset(-r, -r, -r), conduit.offset(r, r, r))) {
            BlockState s = level.getBlockState(p);
            if (s.is(cluster)) {
                existing++;
                continue;
            }
            if (!NacreClusterBlock.growsOn(s)) continue;
            for (Direction d : Direction.values()) {
                BlockPos q = p.relative(d);
                if (level.getBlockState(q).is(Blocks.WATER) && level.getFluidState(q).isSource()) {
                    spots.add(new BlockPos[]{p.immutable(), q.immutable()});
                }
            }
        }
        if (existing >= NACRE_CAP || spots.isEmpty()) return;
        BlockPos[] pick = spots.get(level.random.nextInt(spots.size()));
        Direction facing = Direction.fromDelta(pick[1].getX() - pick[0].getX(), pick[1].getY() - pick[0].getY(), pick[1].getZ() - pick[0].getZ());
        if (facing == null) return;
        level.setBlock(pick[1], cluster.defaultBlockState().setValue(NacreClusterBlock.FACING, facing)
                .setValue(NacreClusterBlock.WATERLOGGED, true), Block.UPDATE_ALL);
        level.playSound(null, pick[1], SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.6f, 1.2f);
    }
}
