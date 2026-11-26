package com.sideflowgames.fulguritefusion.client;

import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.block.StormBatteryBlockEntity;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

// Draws the Storm Battery bank's charge gauge. Every flat side of the bank gets one merged window
// (windows run across touching batteries), and the glowing liquid rises from the bank's lowest block
// to its highest, so the whole bank reads as one tank.
public class StormBatteryRenderer extends LegacyBlockEntityRenderer<StormBatteryBlockEntity> {
    private static final Identifier WHITE =
            Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "textures/block/storm_battery_gauge.png");
    private static final int SIDE_INSET = 4;   // pixels of casing left/right of the window at the bank's edges
    private static final int END_INSET = 3;    // pixels of casing above/below the window at the bank's edges
    private static final float LIFT = 0.0015f; // tiny gap off the face so the gauge doesn't flicker

    private static final int BORDER = 0xA0C8DC, EMPTY = 0x1E3C5A, LIQUID = 0x46AAF0, SURFACE = 0x96E6FF, SURFACE_BRIGHT = 0xD2F6FF;

    public StormBatteryRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void render(StormBatteryBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockPos pos = be.getBlockPos();

        // The whole bank's charge and height.
        Set<BlockPos> bank = StormBatteryBlockEntity.findBank(level, pos, null, 64);
        long fm = 0, cap = 0;
        int minY = pos.getY(), maxY = pos.getY();
        for (BlockPos p : bank) {
            if (level.getBlockEntity(p) instanceof StormBatteryBlockEntity b) {
                fm += b.getFm();
                cap += b.displayCapacity();
            }
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }
        float fraction = cap > 0 ? Mth.clamp((float) fm / cap, 0f, 1f) : 0f;
        // Liquid line in pixels, measured from this block's bottom.
        float bottom = (minY - pos.getY()) * 16f + END_INSET;
        float top = (maxY + 1 - pos.getY()) * 16f - END_INSET;
        float fill = bottom + fraction * (top - bottom);
        float time = level.getGameTime() + partialTick;

        VertexConsumer vc = buffers.getBuffer(RenderTypes.entitySolid(WHITE));
        PoseStack.Pose pose = poseStack.last();
        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (isBattery(level, pos.relative(face))) continue; // covered by another battery
            drawFace(vc, pose, level, pos, face, fill, fraction, time);
        }
    }

    private static boolean isBattery(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.STORM_BATTERY.get());
    }

    // A battery whose 'face' side is showing, so the window can run onto it.
    private static boolean exposed(Level level, BlockPos pos, Direction face) {
        return isBattery(level, pos) && !isBattery(level, pos.relative(face));
    }

    private void drawFace(VertexConsumer vc, PoseStack.Pose pose, Level level, BlockPos pos, Direction face,
                          float fill, float fraction, float time) {
        Direction right = face.getClockWise();
        Direction left = right.getOpposite();
        int light = LevelRenderer.getLightCoords(level, pos.relative(face));

        // The face is split into a 3x3 grid: the middle is always window, the edges are window only when
        // the window continues onto the neighbouring battery, and corners only when it continues three ways.
        boolean l = exposed(level, pos.relative(left), face), r = exposed(level, pos.relative(right), face);
        boolean d = exposed(level, pos.below(), face), u = exposed(level, pos.above(), face);
        boolean[][] cell = new boolean[3][3]; // [column][row], row 0 = bottom
        cell[1][1] = true;
        cell[0][1] = l;
        cell[2][1] = r;
        cell[1][0] = d;
        cell[1][2] = u;
        cell[0][0] = l && d && exposed(level, pos.relative(left).below(), face);
        cell[2][0] = r && d && exposed(level, pos.relative(right).below(), face);
        cell[0][2] = l && u && exposed(level, pos.relative(left).above(), face);
        cell[2][2] = r && u && exposed(level, pos.relative(right).above(), face);

        float[] us = {0, SIDE_INSET, 16 - SIDE_INSET, 16};
        float[] vs = {0, END_INSET, 16 - END_INSET, 16};
        // Which way the face's horizontal axis runs in the world, so the ripple lines up across blocks.
        float worldU = (right.getStepX() * pos.getX() + right.getStepZ() * pos.getZ()) * 16f;

        for (int c = 0; c < 3; c++) {
            for (int row = 0; row < 3; row++) {
                if (!cell[c][row]) continue;
                float u0 = us[c], u1 = us[c + 1], v0 = vs[row], v1 = vs[row + 1];
                // Border on each side that meets casing inside this face.
                boolean bl = c > 0 && !cell[c - 1][row], br = c < 2 && !cell[c + 1][row];
                boolean bb = row > 0 && !cell[c][row - 1], bt = row < 2 && !cell[c][row + 1];
                if (bl) rect(vc, pose, face, u0, u0 + 1, v0, v1, BORDER, light);
                if (br) rect(vc, pose, face, u1 - 1, u1, v0, v1, BORDER, light);
                if (bb) rect(vc, pose, face, u0, u1, v0, v0 + 1, BORDER, light);
                if (bt) rect(vc, pose, face, u0, u1, v1 - 1, v1, BORDER, light);
                // Close inner corners where the window wraps around a casing notch.
                if (!bl && !bb && c > 0 && row > 0 && !cell[c - 1][row - 1]) rect(vc, pose, face, u0, u0 + 1, v0, v0 + 1, BORDER, light);
                if (!br && !bb && c < 2 && row > 0 && !cell[c + 1][row - 1]) rect(vc, pose, face, u1 - 1, u1, v0, v0 + 1, BORDER, light);
                if (!bl && !bt && c > 0 && row < 2 && !cell[c - 1][row + 1]) rect(vc, pose, face, u0, u0 + 1, v1 - 1, v1, BORDER, light);
                if (!br && !bt && c < 2 && row < 2 && !cell[c + 1][row + 1]) rect(vc, pose, face, u1 - 1, u1, v1 - 1, v1, BORDER, light);

                float iu0 = bl ? u0 + 1 : u0, iu1 = br ? u1 - 1 : u1;
                float iv0 = bb ? v0 + 1 : v0, iv1 = bt ? v1 - 1 : v1;
                liquid(vc, pose, face, iu0, iu1, iv0, iv1, fill, fraction, time, worldU, light);
            }
        }
    }

    // Fill one window piece: glowing liquid up to the (gently rippling) surface, dark glass above it.
    private void liquid(VertexConsumer vc, PoseStack.Pose pose, Direction face, float u0, float u1, float v0, float v1,
                        float fill, float fraction, float time, float worldU, int light) {
        if (fraction <= 0f) {
            rect(vc, pose, face, u0, u1, v0, v1, EMPTY, light);
            return;
        }
        boolean full = fraction >= 1f;
        float pulse = 0.5f + 0.5f * Mth.sin(time * 0.15f);
        int surface = mix(SURFACE, SURFACE_BRIGHT, pulse);
        for (float x = u0; x < u1; x += 1f) {
            float x1 = Math.min(x + 1f, u1);
            float wave = full || fraction <= 0f ? 0f : 0.45f * Mth.sin(time * 0.12f + (worldU + x) * 0.7f);
            float s = fill + wave;
            float liquidTop = Mth.clamp(s, v0, v1);
            float surfaceTop = Mth.clamp(s + 1f, v0, v1);
            if (liquidTop > v0) rect(vc, pose, face, x, x1, v0, liquidTop, LIQUID, net.minecraft.util.LightCoordsUtil.FULL_BRIGHT);
            if (!full && fraction > 0f && surfaceTop > liquidTop) rect(vc, pose, face, x, x1, liquidTop, surfaceTop, surface, net.minecraft.util.LightCoordsUtil.FULL_BRIGHT);
            if (v1 > surfaceTop) rect(vc, pose, face, x, x1, surfaceTop, v1, EMPTY, light);
        }
    }

    private static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    // A flat rectangle on 'face', in pixels: u runs along the face, v runs up from the block's bottom.
    private static void rect(VertexConsumer vc, PoseStack.Pose pose, Direction face, float u0, float u1, float v0, float v1,
                             int color, int light) {
        if (u1 <= u0 || v1 <= v0) return;
        Direction right = face.getClockWise();
        float nx = face.getStepX(), nz = face.getStepZ();
        float cx = 0.5f + nx * (0.5f + LIFT), cz = 0.5f + nz * (0.5f + LIFT);
        float rx = right.getStepX(), rz = right.getStepZ();
        float a0 = (u0 - 8f) / 16f, a1 = (u1 - 8f) / 16f, y0 = v0 / 16f, y1 = v1 / 16f;
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        // Wind the corners so the quad faces outward (right x up points into the block, so go up first).
        vertex(vc, pose, cx + rx * a0, y0, cz + rz * a0, r, g, b, light, nx, nz);
        vertex(vc, pose, cx + rx * a0, y1, cz + rz * a0, r, g, b, light, nx, nz);
        vertex(vc, pose, cx + rx * a1, y1, cz + rz * a1, r, g, b, light, nx, nz);
        vertex(vc, pose, cx + rx * a1, y0, cz + rz * a1, r, g, b, light, nx, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                               int r, int g, int b, int light, float nx, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, 0f, nz);
    }
}
