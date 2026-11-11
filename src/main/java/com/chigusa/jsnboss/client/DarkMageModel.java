package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.DarkMageEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態「深淵」は紫の光が深紅になるテクスチャ */
public class DarkMageModel extends DefaultedEntityGeoModel<DarkMageEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/dark_mage_phase2.png");

    public DarkMageModel() {
        super(BossConstants.id("dark_mage"), false);
    }

    @Override
    public ResourceLocation getTextureResource(DarkMageEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
