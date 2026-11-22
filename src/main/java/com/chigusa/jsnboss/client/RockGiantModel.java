package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.RockGiantEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態は割れ目と結晶がマグマのオレンジに光るテクスチャ */
public class RockGiantModel extends DefaultedEntityGeoModel<RockGiantEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/rock_giant_phase2.png");

    public RockGiantModel() {
        super(BossConstants.id("rock_giant"), false);
    }

    @Override
    public ResourceLocation getTextureResource(RockGiantEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
