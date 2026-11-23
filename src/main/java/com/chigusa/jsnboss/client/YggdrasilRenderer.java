package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.YggdrasilEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class YggdrasilRenderer extends GeoEntityRenderer<YggdrasilEntity> {
    public YggdrasilRenderer(EntityRendererProvider.Context context) {
        super(context, new YggdrasilModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 3.0F;
    }

    @Override
    protected float getDeathMaxRotation(YggdrasilEntity entity) {
        return 0.0F;
    }
}
