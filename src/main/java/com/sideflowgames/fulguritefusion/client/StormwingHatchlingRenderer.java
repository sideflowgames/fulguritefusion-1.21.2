package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.entity.StormwingHatchlingEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// A hatchling is the Stormwing model at about a third of the size, in its variant's colors.
public class StormwingHatchlingRenderer extends GeoEntityRenderer<StormwingHatchlingEntity> {
    public StormwingHatchlingRenderer(EntityRendererProvider.Context context) {
        super(context, new StormwingGeoModel<>());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        withScale(0.32f);
        shadowRadius = 0.3f;
    }
}
