package com.sideflowgames.fulguritefusion.item;

import java.util.List;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

// Storm Caller: hold right-click for 2 seconds to call a thunderstorm (5 minutes by default).
//   - Costs 1 Charged Spark Cell from your inventory (free in creative).
//   - Only works from clear skies, under an open sky world (not the Nether or the End).
//   - Any thunderstorm wakes the Stormwings up: they turn aggressive while it lasts.
public class StormCallerItem extends Item {
    private static final int CHARGE_TICKS = 40;
    private static final int COOLDOWN = 100;

    public StormCallerItem(Properties properties) {
        super(properties);
    }

    // Why it can't be used right now, or null if it can.
    private static String problem(Level level, Player player) {
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) return "There's no sky here to call a storm from";
        if (level.isRaining() || level.isThundering()) return "The skies must be clear to call a storm";
        if (!player.getAbilities().instabuild && findCell(player) < 0) return "Needs a Charged Spark Cell";
        return null;
    }

    private static int findCell(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.CHARGED_SPARK_CELL.get())) return i;
        }
        return -1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String why = problem(level, player);
        if (why != null) {
            if (!level.isClientSide) player.displayClientMessage(Component.literal(why), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 0.6f);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CHARGE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    // Sparks gather while charging.
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level instanceof ServerLevel server && remaining % 4 == 0) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getEyeY() + 0.6, entity.getZ(),
                    6, 0.3, 0.3, 0.3, 0.05);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) return stack;
        String why = problem(level, player);
        if (why != null) {
            player.displayClientMessage(Component.literal(why), true);
            return stack;
        }
        if (!player.getAbilities().instabuild) player.getInventory().getItem(findCell(player)).shrink(1);

        int ticks = Config.STORM_CALLER_MINUTES.get() * 1200;
        server.setWeatherParameters(0, ticks, true, true);

        // A harmless bolt answers the call a little way off.
        double angle = level.random.nextDouble() * Math.PI * 2;
        BlockPos spot = BlockPos.containing(player.getX() + Math.cos(angle) * 12, player.getY(), player.getZ() + Math.sin(angle) * 12);
        spot = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, spot);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
            bolt.setVisualOnly(true);
            server.addFreshEntity(bolt);
        }
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getEyeY() + 0.6, player.getZ(), 60, 0.6, 0.6, 0.6, 0.3);
        player.displayClientMessage(Component.literal("The sky darkens..."), true);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Hold to call a thunderstorm (" + Config.STORM_CALLER_MINUTES.get() + " min)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Uses 1 Charged Spark Cell; skies must be clear").withStyle(ChatFormatting.DARK_AQUA));
    }
}
