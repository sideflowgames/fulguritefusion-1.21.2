package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.entity.StormwingEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// Draws the Stormwing from its Blockbench files (geo/entity/stormwing.geo.json and friends).
// Each variant has its own texture and _glowmask (the glowing head streaks and wing veins).
public class StormwingRenderer extends GeoEntityRenderer<StormwingEntity> {
    public StormwingRenderer(EntityRendererProvider.Context context) {
        super(context, new StormwingGeoModel<>());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.8f;
    }
}
