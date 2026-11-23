package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.IronSpiderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class IronSpiderRenderer extends GeoEntityRenderer<IronSpiderEntity> {
    public IronSpiderRenderer(EntityRendererProvider.Context context) {
        super(context, new IronSpiderModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 2.0F;
    }

    @Override
    protected float getDeathMaxRotation(IronSpiderEntity entity) {
        return 0.0F;
    }
}
