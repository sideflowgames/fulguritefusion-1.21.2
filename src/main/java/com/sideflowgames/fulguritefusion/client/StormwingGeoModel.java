package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.entity.StormwingVariant;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

// The Stormwing model (adults and hatchlings), with the texture picked by the bird's variant.
public class StormwingGeoModel<T extends Entity & GeoAnimatable & StormwingVariant.Holder> extends DefaultedEntityGeoModel<T> {
    public StormwingGeoModel() {
        super(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "stormwing"), true);
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return animatable.stormwingVariant().texture();
    }
}
