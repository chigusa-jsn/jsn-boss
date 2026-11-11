package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.BladeDragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class BladeDragonRenderer extends GeoEntityRenderer<BladeDragonEntity> {
    public BladeDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new BladeDragonModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 2.0F;
    }

    @Override
    protected float getDeathMaxRotation(BladeDragonEntity entity) {
        return 0.0F;
    }
}
