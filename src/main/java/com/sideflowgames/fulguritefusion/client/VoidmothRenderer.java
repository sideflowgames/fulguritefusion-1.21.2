package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.entity.VoidmothEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

// Draws the Voidmoth from its Blockbench files (geo/entity/voidmoth.geo.json and friends).
// voidmoth_glowmask.png marks the wing speckles and eyes that shimmer in the dark.
public class VoidmothRenderer extends GeoEntityRenderer<VoidmothEntity, LivingEntityRenderState> {
    public VoidmothRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "voidmoth")));
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.2f;
    }
}
