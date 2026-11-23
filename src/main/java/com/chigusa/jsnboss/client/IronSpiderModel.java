package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.IronSpiderEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態「暴走」は光が赤くなり、装甲に焦げ跡がつくテクスチャ */
public class IronSpiderModel extends DefaultedEntityGeoModel<IronSpiderEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/iron_spider_phase2.png");

    public IronSpiderModel() {
        super(BossConstants.id("iron_spider"), false);
    }

    @Override
    public ResourceLocation getTextureResource(IronSpiderEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
