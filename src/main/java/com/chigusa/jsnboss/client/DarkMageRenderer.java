package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.DarkMageEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class DarkMageRenderer extends GeoEntityRenderer<DarkMageEntity> {
    public DarkMageRenderer(EntityRendererProvider.Context context) {
        super(context, new DarkMageModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0.6F;
    }

    @Override
    protected float getDeathMaxRotation(DarkMageEntity entity) {
        return 0.0F;
    }
}
