package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.YggdrasilEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class YggdrasilRenderer extends GeoEntityRenderer<YggdrasilEntity> {
    public YggdrasilRenderer(EntityRendererProvider.Context context) {
        super(context, new YggdrasilModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 5.0F;
        withScale(YggdrasilEntity.SIZE_P1);   // 動かない木なので大きく
    }

    @Override
    public void render(YggdrasilEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float s = entity.getSizeScale();                  // 形態ごとに大きくなる
        withScale(s);
        this.shadowRadius = 3.0F * s;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    protected float getDeathMaxRotation(YggdrasilEntity entity) {
        return 0.0F;
    }
}
