package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.entity.VoidmothEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// Draws the Voidmoth from its Blockbench files (geo/entity/voidmoth.geo.json and friends).
// voidmoth_glowmask.png marks the wing speckles and eyes that shimmer in the dark.
public class VoidmothRenderer extends GeoEntityRenderer<VoidmothEntity> {
    public VoidmothRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "voidmoth")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.2f;
    }
}
