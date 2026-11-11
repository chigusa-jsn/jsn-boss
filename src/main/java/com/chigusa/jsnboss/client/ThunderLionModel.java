package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.ThunderLionEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態は全身に稲妻が走り、たてがみ全体が帯電するテクスチャ */
public class ThunderLionModel extends DefaultedEntityGeoModel<ThunderLionEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/thunder_lion_phase2.png");

    public ThunderLionModel() {
        super(BossConstants.id("thunder_lion"), false);
    }

    @Override
    public ResourceLocation getTextureResource(ThunderLionEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
