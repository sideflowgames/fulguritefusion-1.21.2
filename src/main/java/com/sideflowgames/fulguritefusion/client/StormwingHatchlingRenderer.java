package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.entity.StormwingHatchlingEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

// A hatchling is the Stormwing model at about a third of the size, in its variant's colors.
public class StormwingHatchlingRenderer extends GeoEntityRenderer<StormwingHatchlingEntity, LivingEntityRenderState> {
    public StormwingHatchlingRenderer(EntityRendererProvider.Context context) {
        super(context, new StormwingGeoModel<>());
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
        withScale(0.32f);
        shadowRadius = 0.3f;
    }

    @Override
    public void addRenderData(StormwingHatchlingEntity animatable, Void relatedObject, LivingEntityRenderState state, float partialTick) {
        super.addRenderData(animatable, relatedObject, state, partialTick);
        state.addGeckolibData(StormwingGeoModel.TEXTURE, animatable.stormwingVariant().texture());
    }
}
