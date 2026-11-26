package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.entity.StormwingEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

// Draws the Stormwing from its Blockbench files (geo/entity/stormwing.geo.json and friends).
// Each variant has its own texture and _glowmask (the glowing head streaks and wing veins).
public class StormwingRenderer extends GeoEntityRenderer<StormwingEntity, LivingEntityRenderState> {
    public StormwingRenderer(EntityRendererProvider.Context context) {
        super(context, new StormwingGeoModel<>());
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.8f;
    }

    @Override
    public void addRenderData(StormwingEntity animatable, Void relatedObject, LivingEntityRenderState state, float partialTick) {
        super.addRenderData(animatable, relatedObject, state, partialTick);
        state.addGeckolibData(StormwingGeoModel.TEXTURE, animatable.stormwingVariant().texture());
    }
}
