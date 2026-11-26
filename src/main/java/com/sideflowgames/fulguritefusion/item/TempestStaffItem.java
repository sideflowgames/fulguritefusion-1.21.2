package com.sideflowgames.fulguritefusion.item;

import net.minecraft.world.InteractionResult;

import java.util.List;

import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

// Tempest Staff: right-click to call a lightning strike where you aim (up to 32 blocks).
//   - Runs on stored Fm (10,000 per strike, 500,000 when full); recharge it in a Fulgurite Charger.
//   - The bolt hurts and sets fire to mobs around where it lands. It never sets blocks on fire and never hits
//     you or your tamed pets.
//   - It still counts as a lightning strike for the mod's lightning reactions (furnaces, sand veins, rituals).
public class TempestStaffItem extends Item implements FmChargeable, GeoItem {
    public static final int MAX_FM = 500_000;
    public static final int CAST_COST = 10_000;
    public static final double RANGE = 32.0;
    public static final int COOLDOWN = 40;
    public static final float DAMAGE = 8.0f;
    public static final double RADIUS = 2.5;
    public static final int BURN_SECONDS = 5;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.tempest_staff.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TempestStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public int maxFm() {
        return MAX_FM;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean free = player.getAbilities().instabuild;
        if (!free && FmChargeable.get(stack) < CAST_COST) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.literal("The staff is out of charge").withStyle(ChatFormatting.RED));
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 1.6f);
            }
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            Vec3 at = aim(server, player);
            if (at == null) {
                player.sendOverlayMessage(Component.literal("Nothing in reach to strike"));
                return InteractionResult.FAIL;
            }
            if (!free) FmChargeable.use(stack, CAST_COST);
            strike(server, player, at);
            player.getCooldowns().addCooldown(stack, COOLDOWN);
        }
        player.swing(hand, true);
        return InteractionResult.SUCCESS;
    }

    // Where the player is looking: the first mob or block within range, or null if there's nothing there.
    private static Vec3 aim(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 reach = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, player, eye, reach,
                new AABB(eye, reach).inflate(1.0), e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != player, 0.3f);
        if (entity != null) return entity.getEntity().position();
        if (block.getType() == HitResult.Type.MISS) return null;
        // Land on top of the block that was hit (a wall hit drops the bolt to the ground in front of it).
        BlockPos pos = block.getBlockPos().relative(block.getDirection());
        while (pos.getY() > level.getMinY() && level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
            pos = pos.below();
        }
        return new Vec3(block.getLocation().x, pos.getY(), block.getLocation().z);
    }

    private static void strike(ServerLevel level, Player caster, Vec3 at) {
        // The bolt itself is for show (so it can't start block fires or hit the caster); the damage is ours.
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (bolt != null) {
            bolt.snapTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        DamageSource lightning = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.LIGHTNING_BOLT), caster, caster);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(RADIUS, 3.0, RADIUS),
                e -> e.isAlive() && !spared(caster, e))) {
            if (e.hurtOrSimulate(lightning, DAMAGE)) e.igniteForSeconds(BURN_SECONDS);
        }
        // Charged Stormsand fuses under the strike, like in the Stormreach.
        BlockPos ground = BlockPos.containing(at).below();
        if (level.getBlockState(ground).is(ModBlocks.CHARGED_STORMSAND.get())) {
            level.setBlockAndUpdate(ground, ModBlocks.FULGURITE_GLASS.get().defaultBlockState());
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.3, at.z, 50, 1.2, 0.3, 1.2, 0.4);
        // A crackle at the staff as it fires.
        Vec3 tip = caster.getEyePosition().add(caster.getLookAngle().scale(0.8));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, tip.x, tip.y, tip.z, 12, 0.15, 0.15, 0.15, 0.15);
        level.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.5f,
                1.3f + level.getRandom().nextFloat() * 0.2f);
    }

    // Never the caster, and never the caster's tamed animals.
    private static boolean spared(Player caster, Entity e) {
        if (e == caster) return true;
        return e instanceof OwnableEntity pet && pet.getOwner() == caster;
    }

    // ---------- Charge bar and tooltip ----------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13 * FmChargeable.fraction(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return FmChargeable.fraction(stack) < 0.1f ? 0xFF4040 : Mth.hsvToRgb(0.55f, 0.75f, 1.0f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag flag) {
        java.util.List<Component> tooltip = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut);
        int fm = FmChargeable.get(stack);
        tooltip.add(Component.literal(String.format("Charge: %,d / %,d Fm", fm, MAX_FM)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Right-click: lightning where you aim (" + (int) RANGE + " blocks)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("%,d Fm per strike · %d strikes left", CAST_COST, fm / CAST_COST))
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("Burns mobs, never blocks; spares you and your pets").withStyle(ChatFormatting.DARK_GRAY));
    }

    // ---------- GeckoLib: the crystal turns slowly in its cradle ----------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("idle", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public void createGeoRenderer(java.util.function.Consumer<com.geckolib.animatable.client.GeoRenderProvider> consumer) {
        // GeckoLib 5: the item's renderer (used by the "geckolib:geckolib" special model in items/tempest_staff.json).
        consumer.accept(new com.geckolib.animatable.client.GeoRenderProvider() {
            private com.sideflowgames.fulguritefusion.client.TempestStaffRenderer renderer;

            @Override
            public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new com.sideflowgames.fulguritefusion.client.TempestStaffRenderer();
                return renderer;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
