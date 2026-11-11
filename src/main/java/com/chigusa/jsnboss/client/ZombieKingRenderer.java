package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.ZombieKingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ZombieKingRenderer extends GeoEntityRenderer<ZombieKingEntity> {
    public ZombieKingRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieKingModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 1.5F;
    }

    @Override
    protected float getDeathMaxRotation(ZombieKingEntity entity) {
        return 0.0F;
    }
}
