package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.entity.ArcSerpentEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import javax.annotation.Nullable;

// Draws the Arc Serpent half see-through (the texture's alpha sets how much), with glowing lightning veins
// from arc_serpent_glowmask.png. Model: geo/entity/arc_serpent.geo.json.
public class ArcSerpentRenderer extends GeoEntityRenderer<ArcSerpentEntity> {
    public ArcSerpentRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "arc_serpent"), true));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.9f;
    }

    @Override
    public RenderType getRenderType(ArcSerpentEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
