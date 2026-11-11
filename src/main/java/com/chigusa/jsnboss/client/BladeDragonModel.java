package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.BladeDragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態は目と全身の剣の刃先が赤く光るテクスチャ */
public class BladeDragonModel extends DefaultedEntityGeoModel<BladeDragonEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/blade_dragon_phase2.png");

    public BladeDragonModel() {
        super(BossConstants.id("blade_dragon"), false);
    }

    @Override
    public ResourceLocation getTextureResource(BladeDragonEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
