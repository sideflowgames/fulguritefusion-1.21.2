package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.item.TempestStaffItem;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// Draws the Tempest Staff from its GeckoLib model (geo/item/tempest_staff.geo.json). The crystal and the
// copper coils glow (tempest_staff_glowmask.png).
public class TempestStaffRenderer extends GeoItemRenderer<TempestStaffItem> {
    public TempestStaffRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(FulguriteFusion.MODID, "tempest_staff")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    // Hooks the renderer up to the item (registered in FulguriteFusionClient).
    public static IClientItemExtensions extensions() {
        return new IClientItemExtensions() {
            private TempestStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new TempestStaffRenderer();
                return renderer;
            }
        };
    }
}
