package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.entity.LumenSlugEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

// Draws the Lumen Slug from its Blockbench files:
//   geo/entity/lumen_slug.geo.json, animations/entity/lumen_slug.animation.json,
//   textures/entity/lumen_slug.png (+ lumen_slug_glowmask.png: the parts that glow in the dark).
public class LumenSlugRenderer extends GeoEntityRenderer<LumenSlugEntity, LivingEntityRenderState> {
    public LumenSlugRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(
                Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "lumen_slug")));
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.3f;
    }

    // The slug always looks softly lit, even in a dark cave.
    @Override
    protected int getBlockLightLevel(LumenSlugEntity entity, BlockPos pos) {
        return Math.max(10, super.getBlockLightLevel(entity, pos));
    }
}
