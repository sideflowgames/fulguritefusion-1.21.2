package com.sideflowgames.fulguritefusion.entity;

import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

// Lumen Slug: a slow, glowing, harmless slug from the Lush Caves. As it crawls it leaves a glowing
// Lumen Trail behind; scoop the trail with a glass bottle to get Lumen Gel without hurting the slug.
public class LumenSlugEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.lumen_slug.idle");
    private static final RawAnimation CRAWL = RawAnimation.begin().thenLoop("animation.lumen_slug.crawl");
    private static final int TRAIL_INTERVAL = 40; // ticks between trail drops while moving

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LumenSlugEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.12);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 5.0f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Leave a glowing trail while crawling.
        if (!level().isClientSide && tickCount % TRAIL_INTERVAL == 0 && getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
            BlockPos pos = blockPosition();
            BlockState trail = ModBlocks.LUMEN_TRAIL.get().defaultBlockState();
            if (level().getBlockState(pos).isAir() && trail.canSurvive(level(), pos)) {
                level().setBlock(pos, trail, 3);
            }
        }
    }

    // Spawns on the ground in Lush Caves (moss, clay, grass and the like), never under open sky.
    public static boolean checkSpawn(EntityType<LumenSlugEntity> type, ServerLevelAccessor level, MobSpawnType reason,
                                     BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.LUSH_GROUND_REPLACEABLE) && !level.canSeeSky(pos);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 6,
                state -> state.setAndContinue(state.isMoving() ? CRAWL : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
