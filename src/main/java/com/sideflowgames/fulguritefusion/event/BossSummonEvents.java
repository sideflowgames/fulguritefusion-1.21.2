package com.sideflowgames.fulguritefusion.event;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModEntities;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.entity.boss.StormBossEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

// Summoning the three mini-bosses:
//   Thunderhead    Overworld: use a Charged Spark Cell on a Lightning Rod under open sky during a thunderstorm.
//   Magma Conduit  Nether:    throw a Charged Spark Cell into lava.
//   Void Arc       End:       use an End Crystal on a Lightning Rod.
// Only one of each can be awake within 64 blocks.
@EventBusSubscriber(modid = FulguriteFusion.MODID)
public final class BossSummonEvents {
    private static final int NEARBY = 64;

    private BossSummonEvents() {}

    @SubscribeEvent
    public static void onUseRod(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos rod = event.getPos();
        if (!level.getBlockState(rod).is(Blocks.LIGHTNING_ROD)) return;
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        boolean cell = stack.is(ModItems.CHARGED_SPARK_CELL.get());
        boolean crystal = stack.is(Items.END_CRYSTAL);
        if (!cell && !crystal) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(level instanceof ServerLevel server)) return;

        if (cell) {
            if (level.dimension() != Level.OVERWORLD) {
                hint(player, "The Thunderhead only answers in the Overworld.");
                return;
            }
            if (!level.isThundering()) {
                hint(player, "Nothing happens. It needs a thunderstorm.");
                return;
            }
            if (!level.canSeeSky(rod.above())) {
                hint(player, "The rod needs open sky above it.");
                return;
            }
            summon(server, player, stack, ModEntities.THUNDERHEAD.get(), rod, Vec3.atBottomCenterOf(rod.above(8)),
                    "The storm gathers into a Thunderhead!");
        } else {
            if (level.dimension() != Level.END) {
                hint(player, "The crystal only resonates in the End.");
                return;
            }
            summon(server, player, stack, ModEntities.VOID_ARC.get(), rod, Vec3.atBottomCenterOf(rod.above(4)),
                    "The crystal shatters, and something steps out of the void...");
        }
    }

    private static void summon(ServerLevel level, Player player, ItemStack stack, EntityType<? extends StormBossEntity> type,
                               BlockPos at, Vec3 spawn, String message) {
        if (alreadyAwake(level, type, at)) {
            hint(player, "One is already awake nearby.");
            return;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(at.above()));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        Mob boss = type.create(level);
        if (boss == null) return;
        boss.moveTo(spawn.x, spawn.y, spawn.z, level.random.nextFloat() * 360f, 0);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        level.addFreshEntity(boss);
        level.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 1.2f);
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(at).inflate(32))) {
            p.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    private static boolean alreadyAwake(ServerLevel level, EntityType<?> type, BlockPos at) {
        return !level.getEntities(type, new AABB(at).inflate(NEARBY), e -> e.isAlive()).isEmpty();
    }

    private static void hint(Player player, String text) {
        player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GRAY), true);
    }

    // A Charged Spark Cell dropped into lava in the Nether wakes the Magma Conduit.
    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !(item.level() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.NETHER || !item.getItem().is(ModItems.CHARGED_SPARK_CELL.get())) return;
        BlockPos pos = item.blockPosition();
        if (!level.getFluidState(pos).is(FluidTags.LAVA)) return;
        if (alreadyAwake(level, ModEntities.MAGMA_CONDUIT.get(), pos)) return;
        item.getItem().shrink(1);
        if (item.getItem().isEmpty()) item.discard();
        Mob boss = ModEntities.MAGMA_CONDUIT.get().create(level);
        if (boss == null) return;
        BlockPos top = pos;
        while (level.getFluidState(top.above()).is(FluidTags.LAVA) && top.getY() < level.getMaxBuildHeight()) top = top.above();
        boss.moveTo(top.getX() + 0.5, top.getY() + 0.5, top.getZ() + 0.5, level.random.nextFloat() * 360f, 0);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(top), MobSpawnType.EVENT, null);
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.LAVA, top.getX() + 0.5, top.getY() + 1, top.getZ() + 0.5, 60, 1.5, 0.5, 1.5, 0.2);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(top.above()));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, top, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 0.7f);
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(top).inflate(32))) {
            p.displayClientMessage(Component.literal("The lava heaves, and the Magma Conduit rises!").withStyle(ChatFormatting.GOLD), true);
        }
    }
}
