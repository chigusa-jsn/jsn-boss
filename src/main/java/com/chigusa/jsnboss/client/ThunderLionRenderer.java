package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.ThunderLionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ThunderLionRenderer extends GeoEntityRenderer<ThunderLionEntity> {
    public ThunderLionRenderer(EntityRendererProvider.Context context) {
        super(context, new ThunderLionModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 1.5F;
    }

    @Override
    protected float getDeathMaxRotation(ThunderLionEntity entity) {
        return 0.0F;
    }
}
