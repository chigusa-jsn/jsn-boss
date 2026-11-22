package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.RockGiantEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class RockGiantRenderer extends GeoEntityRenderer<RockGiantEntity> {
    public RockGiantRenderer(EntityRendererProvider.Context context) {
        super(context, new RockGiantModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 2.0F;
    }

    @Override
    protected float getDeathMaxRotation(RockGiantEntity entity) {
        return 0.0F;
    }
}
