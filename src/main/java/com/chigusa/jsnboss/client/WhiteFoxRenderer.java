package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.WhiteFoxEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class WhiteFoxRenderer extends GeoEntityRenderer<WhiteFoxEntity> {
    public WhiteFoxRenderer(EntityRendererProvider.Context context) {
        super(context, new WhiteFoxModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 1.3F;
    }

    @Override
    protected float getDeathMaxRotation(WhiteFoxEntity entity) {
        return 0.0F;
    }
}
