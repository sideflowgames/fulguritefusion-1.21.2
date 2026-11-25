package com.sideflowgames.fulguritefusion.dungeon;

import com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast;
import com.sideflowgames.fulguritefusion.entity.stormreach.StormDamage;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DungeonHazardBlockEntity extends BlockEntity {
    private int timer;
    @Nullable private Vec3 mark;

    public DungeonHazardBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonBlocks.HAZARD_BE.get(), pos, state);
    }

    private static boolean target(Player p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DungeonHazardBlockEntity be) {
        if (!(level instanceof ServerLevel server) || !(state.getBlock() instanceof DungeonHazardBlock block)) return;
        switch (block.hazard) {
            case DungeonHazardBlock.CONDUCTOR -> be.conductor(server, pos);
            case DungeonHazardBlock.CRUSHER -> be.crusher(server, pos);
            case DungeonHazardBlock.TOTEM -> be.totem(server, pos);
            default -> be.prism(server, pos);
        }
    }

    // Storm Conductor: marks the ground under a player within 7 blocks (a ring of sparks for 1.5 seconds), then a
    // bolt strikes there: 8 storm damage within 2.5 blocks. Every 3-6 seconds.
    private void conductor(ServerLevel server, BlockPos pos) {
        if (mark != null) {
            for (int i = 0; i < 6; i++) {
                double a = timer * 0.4 + i * Math.PI / 3;
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, mark.x + Math.cos(a) * 2, mark.y + 0.1, mark.z + Math.sin(a) * 2, 1, 0, 0.05, 0, 0);
            }
            if (--timer <= 0) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
                if (bolt != null) {
                    bolt.moveTo(mark);
                    bolt.setVisualOnly(true);
                    server.addFreshEntity(bolt);
                }
                for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(mark, mark).inflate(2.5),
                        e -> !(e instanceof StormBeast) && !(e instanceof Player p && !target(p)))) {
                    e.hurt(StormDamage.trap(server), 8.0f);
                }
                mark = null;
                timer = 60 + server.random.nextInt(60);
            }
            return;
        }
        if (timer > 0) {
            timer--;
            return;
        }
        Player p = server.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 7, e -> target((Player) e));
        if (p == null) {
            timer = 10;
            return;
        }
        mark = p.position();
        timer = 30;
        server.playSound(null, pos, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.BLOCKS, 0.4f, 2.0f);
    }

    // Crusher: every 4 seconds; dust falls a second before it slams. 12 damage to anything in the 3x3 column below
    // (6 blocks deep), and a moment of Slowness.
    private void crusher(ServerLevel server, BlockPos pos) {
        long phase = (server.getGameTime() + Math.abs(pos.hashCode())) % 80;
        if (phase != 50 && phase != 70) return;
        if (server.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 20, e -> true) == null) return;
        AABB below = new AABB(pos.getX() - 1, pos.getY() - 6, pos.getZ() - 1, pos.getX() + 2, pos.getY(), pos.getZ() + 2);
        if (phase == 50) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.BLACKSTONE.defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() - 0.1, pos.getZ() + 0.5, 20, 1.0, 0, 1.0, 0);
            server.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0f, 0.5f);
            return;
        }
        int floor = pos.getY() - 1;
        while (floor > pos.getY() - 7 && server.getBlockState(new BlockPos(pos.getX(), floor, pos.getZ())).isAir()) floor--;
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, below,
                e -> !(e instanceof StormBeast) && !(e instanceof Player p && !target(p)))) {
            e.hurt(server.damageSources().generic(), 12.0f);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
        }
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACKSTONE.defaultBlockState()),
                pos.getX() + 0.5, floor + 1.1, pos.getZ() + 0.5, 40, 1.0, 0.2, 1.0, 0.2);
        server.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, floor + 1.2, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        server.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.2f, 0.5f);
    }

    // Rime Totem: everyone within 6 blocks freezes (as in powder snow) and is slowed.
    private void totem(ServerLevel server, BlockPos pos) {
        if (server.getGameTime() % 10 != 0) return;
        for (Player p : server.getEntitiesOfClass(Player.class, new AABB(pos).inflate(6), DungeonHazardBlockEntity::target)) {
            if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 36) continue;
            p.setTicksFrozen(Math.min(p.getTicksFrozen() + 40, p.getTicksRequiredToFreeze() + 80));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            server.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY(1.0), p.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
        }
    }

    // Hanging Prism: when someone walks underneath it shakes for half a second and falls (one time only).
    private void prism(ServerLevel server, BlockPos pos) {
        if (timer > 0) {
            server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.01);
            if (--timer == 0) {
                FallingBlockEntity f = FallingBlockEntity.fall(server, pos, Blocks.AMETHYST_BLOCK.defaultBlockState());
                f.setHurtsEntities(2.5f, 24);
                server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.5f, 0.6f);
            }
            return;
        }
        if (server.getGameTime() % 5 != 0) return;
        AABB below = new AABB(pos.getX() - 0.6, pos.getY() - 10, pos.getZ() - 0.6, pos.getX() + 1.6, pos.getY(), pos.getZ() + 1.6);
        if (!server.getEntitiesOfClass(Player.class, below, DungeonHazardBlockEntity::target).isEmpty()) {
            timer = 10;
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5f, 1.4f);
        }
    }
}
