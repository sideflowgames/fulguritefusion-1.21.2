package com.sideflowgames.fulguritefusion.item;

import net.minecraft.world.InteractionResult;

import com.sideflowgames.fulguritefusion.stormreach.Stormreach;
import com.sideflowgames.fulguritefusion.stormreach.StormreachTravel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

// Skyward Key: forged from the three guardians' key pieces (Storm Eye, Molten Heart, Void Shard) in the Storm Forge.
// Reusable, with a 5 minute cooldown.
//   Overworld, outdoors, during a thunderstorm: a lightning strike carries you up into the Stormreach.
//   In the Stormreach: takes you back to your bed or spawn point.
// While the Stormreach is still in development, only the developer can travel with it.
public class SkywardKeyItem extends Item {
    public static final int COOLDOWN = 20 * 60 * 5;

    public SkywardKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!Stormreach.allowed(player)) {
            hint(player, "The Stormreach is still in development. It's coming in Fulgurite Fusion 2.0.");
            return InteractionResult.FAIL;
        }
        if (Stormreach.is(level)) {
            strike(server, sp);
            StormreachTravel.home(sp);
        } else if (level.dimension() != Level.OVERWORLD) {
            hint(player, "The key only answers under the Overworld's sky.");
            return InteractionResult.FAIL;
        } else if (!level.isThundering()) {
            hint(player, "The key needs a thunderstorm to carry you up.");
            return InteractionResult.FAIL;
        } else if (!level.canSeeSky(player.blockPosition().above())) {
            hint(player, "The key needs open sky above you.");
            return InteractionResult.FAIL;
        } else {
            strike(server, sp);
            StormreachTravel.enter(sp);
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        return InteractionResult.CONSUME;
    }

    private static void strike(ServerLevel level, ServerPlayer player) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (bolt != null) {
            bolt.snapTo(player.getX(), player.getY(), player.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    private static void hint(Player player, String text) {
        player.sendOverlayMessage(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipOut, TooltipFlag flag) {
        java.util.List<Component> tooltip = new com.sideflowgames.fulguritefusion.util.TooltipLines(tooltipOut);
        tooltip.add(Component.literal("Use outdoors in a thunderstorm to ride the lightning").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("up into the Stormreach. Use it there to come home.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("5 minute cooldown").withStyle(ChatFormatting.DARK_GRAY));
    }
}
