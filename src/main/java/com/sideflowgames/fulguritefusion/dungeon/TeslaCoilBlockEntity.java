package com.sideflowgames.fulguritefusion.dungeon;

import com.sideflowgames.fulguritefusion.entity.stormreach.StormDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class TeslaCoilBlockEntity extends BlockEntity {
    private static final double RANGE = 8.0;

    public TeslaCoilBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonBlocks.TESLA_COIL_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TeslaCoilBlockEntity coil) {
        if (!(level instanceof ServerLevel server) || (level.getGameTime() + pos.hashCode()) % 30 != 0) return;
        Vec3 top = Vec3.atCenterOf(pos).add(0, 0.5, 0);
        Player target = null;
        double best = RANGE * RANGE;
        for (Player p : server.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            double d = p.distanceToSqr(top);
            if (d > best) continue;
            Vec3 eye = p.position().add(0, p.getBbHeight() * 0.6, 0);
            var hit = server.clip(new ClipContext(top.add(eye.subtract(top).normalize().scale(0.6)), eye,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (hit.getType() == HitResult.Type.MISS) {
                best = d;
                target = p;
            }
        }
        if (target == null) return;
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
        int steps = (int) Math.max(6, top.distanceTo(to) * 4);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = top.lerp(to, i / (double) steps);
            double j = (i == 0 || i == steps) ? 0 : 0.15;
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x + (server.getRandom().nextDouble() - 0.5) * j,
                    p.y + (server.getRandom().nextDouble() - 0.5) * j, p.z + (server.getRandom().nextDouble() - 0.5) * j, 1, 0, 0, 0, 0);
        }
        target.hurtOrSimulate(StormDamage.trap(server), 5.0f);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1));
        server.playSound(null, pos, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.BLOCKS, 0.4f, 1.8f);
    }
}
