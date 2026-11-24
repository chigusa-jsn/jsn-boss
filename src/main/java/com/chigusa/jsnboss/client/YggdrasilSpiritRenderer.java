package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.YggdrasilSpiritEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** 光の翼と結晶の剣は半透明なので、半透明で描く */
public class YggdrasilSpiritRenderer extends GeoEntityRenderer<YggdrasilSpiritEntity> {
    public YggdrasilSpiritRenderer(EntityRendererProvider.Context context) {
        super(context, new YggdrasilSpiritModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 1.2F;
    }

    @Override
    public RenderType getRenderType(YggdrasilSpiritEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    protected float getDeathMaxRotation(YggdrasilSpiritEntity entity) {
        return 0.0F;
    }
}
