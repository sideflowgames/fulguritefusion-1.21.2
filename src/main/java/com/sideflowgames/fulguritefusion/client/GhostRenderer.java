package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.BiFunction;

// A block entity renderer that only draws ghost build outlines (see GhostBuild), while the block is powered.
// Used for the Reactor Control Console and the Storm / Mini Collider.
public class GhostRenderer<T extends BlockEntity> extends LegacyBlockEntityRenderer<T> {
    private final BiFunction<Level, BlockPos, List<GhostBuild.Ghost>> build;
    private final int reach;

    public GhostRenderer(BiFunction<Level, BlockPos, List<GhostBuild.Ghost>> build, int reach) {
        this.build = build;
        this.reach = reach;
    }

    @Override
    protected void render(T be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        if (level == null || !GhostBuild.powered(level, pos)) return;
        GhostBuild.render(GhostBuild.get(level, pos, l -> build.apply(l, pos)), pos, level, partialTick, poseStack, buffers);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(T be) {
        return new AABB(be.getBlockPos()).inflate(reach);
    }
}
