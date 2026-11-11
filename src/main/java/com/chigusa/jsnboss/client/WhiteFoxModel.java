package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.WhiteFoxEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態は9本の尻尾が輝くテクスチャ。分身は第1形態の見た目のまま（本物を見分けるヒント） */
public class WhiteFoxModel extends DefaultedEntityGeoModel<WhiteFoxEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/white_fox_phase2.png");

    public WhiteFoxModel() {
        super(BossConstants.id("white_fox"), false);
    }

    @Override
    public ResourceLocation getTextureResource(WhiteFoxEntity entity) {
        return entity.isPhase2() && !entity.isClone() ? PHASE2 : super.getTextureResource(entity);
    }
}
