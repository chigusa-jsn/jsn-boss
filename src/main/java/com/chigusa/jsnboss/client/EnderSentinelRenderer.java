package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.EnderSentinelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class EnderSentinelRenderer extends GeoEntityRenderer<EnderSentinelEntity> {
    public EnderSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new EnderSentinelModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0.9F;
    }

    @Override
    protected float getDeathMaxRotation(EnderSentinelEntity entity) {
        return 0.0F;
    }
}
