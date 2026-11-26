package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.entity.StormwingVariant;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedEntityGeoModel;

// The Stormwing model (adults and hatchlings), with the texture picked by the bird's variant.
public class StormwingGeoModel<T extends Entity & GeoAnimatable & StormwingVariant.Holder> extends DefaultedEntityGeoModel<T> {
    public StormwingGeoModel() {
        super(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "stormwing"));
    }

    // 26.1 / GeckoLib 5: the model only sees the render state, so the renderer stores the variant's texture in it.
    public static final com.geckolib.constant.dataticket.DataTicket<Identifier> TEXTURE =
            com.geckolib.constant.dataticket.DataTicket.create("fulguritefusion_stormwing_texture", Identifier.class);

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState state) {
        Identifier tex = state.getGeckolibData(TEXTURE);
        return tex != null ? tex : super.getTextureResource(state);
    }
}
