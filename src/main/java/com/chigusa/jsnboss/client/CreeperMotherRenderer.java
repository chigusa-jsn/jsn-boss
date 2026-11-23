package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.CreeperMotherEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class CreeperMotherRenderer extends GeoEntityRenderer<CreeperMotherEntity> {
    public CreeperMotherRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperMotherModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 1.4F;
    }

    @Override
    protected float getDeathMaxRotation(CreeperMotherEntity entity) {
        return 0.0F;
    }
}
