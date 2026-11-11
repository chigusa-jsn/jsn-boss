package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.IceWitchEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class IceWitchRenderer extends GeoEntityRenderer<IceWitchEntity> {
    public IceWitchRenderer(EntityRendererProvider.Context context) {
        super(context, new IceWitchModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0.8F;
    }

    @Override
    protected float getDeathMaxRotation(IceWitchEntity entity) {
        return 0.0F;
    }
}
