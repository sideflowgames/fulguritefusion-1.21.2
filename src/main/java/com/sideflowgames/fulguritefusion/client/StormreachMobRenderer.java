package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

// Draws any of the Stormreach creatures from its Blockbench files:
//   geo/entity/<name>.geo.json, animations/entity/<name>.animation.json,
//   textures/entity/<name>.png (+ <name>_glowmask.png: the parts that glow in the dark).
// Babies are drawn at half size. minLight keeps glowing creatures visible in the dark.
public class StormreachMobRenderer<T extends LivingEntity & GeoAnimatable> extends GeoEntityRenderer<T, LivingEntityRenderState> {
    private final int minLight;
    private final float scale;

    public StormreachMobRenderer(EntityRendererProvider.Context context, String name, float shadow, float scale, int minLight) {
        super(context, new DefaultedEntityGeoModel<>(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, name)));
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = shadow;
        this.scale = scale;
        this.minLight = minLight;
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
        float s = info.renderState().isBaby ? scale * 0.5f : scale;
        super.scaleModelForRender(info, widthScale * s, heightScale * s);
    }

    // Boss models are bigger than their hitboxes (hanging tendrils, orbiting shards), so don't cull them early.
    @Override
    protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(T entity) {
        if (entity instanceof com.sideflowgames.fulguritefusion.entity.boss.StormBossEntity boss) return boss.cullingBox();
        return super.getBoundingBoxForCulling(entity);
    }

    @Override
    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return Math.max(minLight, super.getBlockLightLevel(entity, pos));
    }
}
