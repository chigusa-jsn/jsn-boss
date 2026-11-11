package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.WindCatEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態は毛が半透明で、縞とリボンが光るテクスチャ */
public class WindCatModel extends DefaultedEntityGeoModel<WindCatEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/wind_cat_phase2.png");

    public WindCatModel() {
        super(BossConstants.id("wind_cat"), false);
    }

    @Override
    public ResourceLocation getTextureResource(WindCatEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
