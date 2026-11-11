package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.entity.FireKnightEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class FireKnightRenderer extends GeoEntityRenderer<FireKnightEntity> {
    public FireKnightRenderer(EntityRendererProvider.Context context) {
        super(context, new FireKnightModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));   // 炎・ひび・目を暗闇でも光らせる
        this.shadowRadius = 1.2F;
    }

    /** バニラの「死ぬと横に倒れる」回転を止めて、死亡アニメだけを見せる */
    @Override
    protected float getDeathMaxRotation(FireKnightEntity entity) {
        return 0.0F;
    }
}
