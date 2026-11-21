package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

// First-person pose while winding up a spear throw. Vanilla's spear pose is made for the trident's 3D model,
// which points straight up; our spears are flat diagonal sprites, so the vanilla pose made them huge and
// pointed at the sky. Instead: raise the spear over the shoulder, tip pointing forward, and draw it back as the
// throw charges (fully drawn after half a second, then it trembles a little).
public class SpearHandTransform implements IClientItemExtensions {
    // Tuning: change these if the pose needs adjusting.
    private static final float TILT = -62f;      // tip forward (more negative = flatter)
    private static final float TURN = 12f;       // tip in toward the middle of the screen
    private static final float RAISE = 0.18f;    // how high the hand lifts
    private static final float PULL_BACK = 0.3f; // how far it draws back when fully charged

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        if (!player.isUsingItem() || player.getUseItem() != stack) return false;
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float held = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0f);
        float charge = Mth.clamp(held / 10.0f, 0f, 1f);
        // The normal hand spot (as vanilla places a held item), then lifted and drawn back.
        pose.translate(side * 0.56f, -0.52f + RAISE + 0.06f * charge, -0.72f + PULL_BACK * charge);
        if (charge >= 1f) {
            float shake = Mth.sin((held - 10f) * 1.3f) * 0.004f;
            pose.translate(shake, shake, 0);
        }
        pose.mulPose(Axis.XP.rotationDegrees(TILT));
        pose.mulPose(Axis.YP.rotationDegrees(side * TURN));
        return true;
    }
}
