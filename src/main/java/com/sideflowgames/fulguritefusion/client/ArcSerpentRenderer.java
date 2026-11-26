package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.entity.ArcSerpentEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

import javax.annotation.Nullable;

// Draws the Arc Serpent half see-through (the texture's alpha sets how much), with glowing lightning veins
// from arc_serpent_glowmask.png. Model: geo/entity/arc_serpent.geo.json.
public class ArcSerpentRenderer extends GeoEntityRenderer<ArcSerpentEntity, LivingEntityRenderState> {
    public ArcSerpentRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "arc_serpent")));
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.9f;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);
    }
}
