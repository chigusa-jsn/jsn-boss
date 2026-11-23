package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.FallenValkyrieEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class FallenValkyrieRenderer extends GeoEntityRenderer<FallenValkyrieEntity> {
    public FallenValkyrieRenderer(EntityRendererProvider.Context context) {
        super(context, new FallenValkyrieModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 1.0F;
    }

    @Override
    protected float getDeathMaxRotation(FallenValkyrieEntity entity) {
        return 0.0F;
    }
}
