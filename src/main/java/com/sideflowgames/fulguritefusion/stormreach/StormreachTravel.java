package com.sideflowgames.fulguritefusion.stormreach;

import com.mojang.brigadier.CommandDispatcher;
import com.sideflowgames.fulguritefusion.ModBlocks;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Developer-only travel to the Stormreach (see Stormreach.developer: the player named Dev, development build only):
//   /stormreach       go there, or back to your spawn point if you're already there
//   (RELEASE 2.0: "/stormreach rift" and the Stormreach Rift item are taken out for the CurseForge jar; put them
//   back in the next revision for testing. See ModItems.STORMREACH_RIFT.)
// The command only exists in the development build; a built jar doesn't have it. The Stormreach rift and the Storm
// Titan's armor piece will replace all of this.
public final class StormreachTravel {
    private StormreachTravel() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(StormreachTravel::onCommands);
    }

    private static void onCommands(RegisterCommandsEvent event) {
        if (!Stormreach.devBuild()) return;
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("stormreach")
                .requires(s -> Stormreach.developer(s.getEntity()))
                .executes(ctx -> {
                    travel(ctx.getSource().getPlayerOrException());
                    return 1;
                }));
        // RELEASE 2.0: restore next revision.
        // .then(Commands.literal("rift").executes(ctx -> {
        //     ServerPlayer player = ctx.getSource().getPlayerOrException();
        //     player.getInventory().add(new net.minecraft.world.item.ItemStack(com.sideflowgames.fulguritefusion.ModItems.STORMREACH_RIFT.get()));
        //     return 1;
        // }))
    }

    // There, or home again.
    public static void travel(ServerPlayer player) {
        if (!Stormreach.developer(player)) return;
        if (Stormreach.is(player.level())) home(player);
        else enter(player);
    }

    public static void enter(ServerPlayer player) {
        ServerLevel reach = player.server.getLevel(Stormreach.LEVEL);
        if (reach == null) {
            player.sendSystemMessage(Component.literal("The Stormreach isn't loaded in this world."));
            return;
        }
        BlockPos spot = findLand(reach, player.getBlockX(), player.getBlockZ());
        player.changeDimension(new DimensionTransition(reach, Vec3.atBottomCenterOf(spot), Vec3.ZERO,
                player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
        reach.playSound(null, spot, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    // Back to the bed or respawn point if there is one, otherwise world spawn.
    public static void home(ServerPlayer player) {
        player.changeDimension(player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING));
    }

    // Somewhere solid to stand near (x, z): the column itself, then rings further out. Over the void it builds a
    // small Stormstone platform.
    public static BlockPos findLand(ServerLevel level, int x, int z) {
        for (int r = 0; r <= 96; r += 16) {
            for (int dx = -r; dx <= r; dx += 16) {
                for (int dz = -r; dz <= r; dz += 16) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int top = level.getChunk((x + dx) >> 4, (z + dz) >> 4)
                            .getHeight(Heightmap.Types.MOTION_BLOCKING, (x + dx) & 15, (z + dz) & 15);
                    if (top > level.getMinBuildHeight() + 1) return new BlockPos(x + dx, top + 1, z + dz);
                }
            }
        }
        BlockPos base = new BlockPos(x, 130, z);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(base.offset(dx, -1, dz), ModBlocks.STORMSTONE.get().defaultBlockState());
                for (int dy = 0; dy < 3; dy++) {
                    if (!level.getBlockState(base.offset(dx, dy, dz)).isAir()) level.setBlockAndUpdate(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        return base;
    }
}
