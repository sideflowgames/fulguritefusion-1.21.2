package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sideflowgames.fulguritefusion.FulguriteFusion;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// Draws any of the Stormreach creatures from its Blockbench files:
//   geo/entity/<name>.geo.json, animations/entity/<name>.animation.json,
//   textures/entity/<name>.png (+ <name>_glowmask.png: the parts that glow in the dark).
// Babies are drawn at half size. minLight keeps glowing creatures visible in the dark.
public class StormreachMobRenderer<T extends LivingEntity & GeoAnimatable> extends GeoEntityRenderer<T> {
    private final int minLight;
    private final float scale;

    public StormreachMobRenderer(EntityRendererProvider.Context context, String name, float shadow, float scale, int minLight) {
        super(context, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, name), true));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = shadow;
        this.scale = scale;
        this.minLight = minLight;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float s = entity.isBaby() ? scale * 0.5f : scale;
        poseStack.pushPose();
        poseStack.scale(s, s, s);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return Math.max(minLight, super.getBlockLightLevel(entity, pos));
    }
}
