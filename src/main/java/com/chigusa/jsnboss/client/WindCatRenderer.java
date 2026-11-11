package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.WindCatEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class WindCatRenderer extends GeoEntityRenderer<WindCatEntity> {
    public WindCatRenderer(EntityRendererProvider.Context context) {
        super(context, new WindCatModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 1.2F;
    }

    /** 第2形態は半透明で描く（体が半分風になる） */
    @Override
    public RenderType getRenderType(WindCatEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return animatable.isPhase2() ? RenderType.entityTranslucent(texture) : super.getRenderType(animatable, texture, bufferSource, partialTick);
    }

    @Override
    protected float getDeathMaxRotation(WindCatEntity entity) {
        return 0.0F;
    }
}
