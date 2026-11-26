package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.item.TempestStaffItem;

import net.minecraft.resources.Identifier;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

// Draws the Tempest Staff from its GeckoLib model (geo/item/tempest_staff.geo.json). The crystal and the
// copper coils glow (tempest_staff_glowmask.png).
public class TempestStaffRenderer extends GeoItemRenderer<TempestStaffItem> {
    public TempestStaffRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, "tempest_staff")));
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
