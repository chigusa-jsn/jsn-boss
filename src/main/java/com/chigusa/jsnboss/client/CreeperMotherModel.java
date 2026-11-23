package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.CreeperMotherEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態「臨界」は白く光るひびが走るテクスチャ */
public class CreeperMotherModel extends DefaultedEntityGeoModel<CreeperMotherEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/creeper_mother_phase2.png");

    public CreeperMotherModel() {
        super(BossConstants.id("creeper_mother"), false);
    }

    @Override
    public ResourceLocation getTextureResource(CreeperMotherEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
