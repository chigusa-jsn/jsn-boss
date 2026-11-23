package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.FenrirEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class FenrirRenderer extends GeoEntityRenderer<FenrirEntity> {
    public FenrirRenderer(EntityRendererProvider.Context context) {
        super(context, new FenrirModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 1.5F;
    }

    @Override
    protected float getDeathMaxRotation(FenrirEntity entity) {
        return 0.0F;
    }
}
