package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.YggdrasilEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態は葉が紅葉し、光が赤橙になるテクスチャ */
public class YggdrasilModel extends DefaultedEntityGeoModel<YggdrasilEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/yggdrasil_phase2.png");

    public YggdrasilModel() {
        super(BossConstants.id("yggdrasil"), false);
    }

    @Override
    public ResourceLocation getTextureResource(YggdrasilEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
