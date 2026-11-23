package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.FenrirEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態は金色の毛に赤い目とルーンのテクスチャ */
public class FenrirModel extends DefaultedEntityGeoModel<FenrirEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/fenrir_phase2.png");

    public FenrirModel() {
        super(BossConstants.id("fenrir"), false);
    }

    @Override
    public ResourceLocation getTextureResource(FenrirEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
