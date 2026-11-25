package com.sideflowgames.fulguritefusion.dungeon;

import com.sideflowgames.fulguritefusion.entity.stormreach.StormBeast;
import com.sideflowgames.fulguritefusion.entity.stormreach.StormDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Arc Trap: a charged floor plate. Step on it while it glows and it discharges: 6 storm damage and a jolt
// upward. Recharges in 2 seconds. Storm beasts don't set it off.
public class ArcTrapBlock extends Block {
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

    public ArcTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && state.getValue(CHARGED) && entity instanceof LivingEntity living
                && !(entity instanceof StormBeast) && !(entity instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            living.hurt(StormDamage.trap(server), 6.0f);
            living.push(0, 0.5, 0);
            living.hurtMarked = true;
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 25, 0.4, 0.3, 0.4, 0.3);
            server.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 1.0f, 0.6f);
            server.setBlock(pos, state.setValue(CHARGED, false), 3);
            server.scheduleTick(pos, this, 40);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CHARGED)) level.setBlock(pos, state.setValue(CHARGED, true), 3);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CHARGED) && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }
}
