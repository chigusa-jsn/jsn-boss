package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.FallenValkyrieEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態は光が赤紫に、翼がボロボロになるテクスチャ */
public class FallenValkyrieModel extends DefaultedEntityGeoModel<FallenValkyrieEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/fallen_valkyrie_phase2.png");

    public FallenValkyrieModel() {
        super(BossConstants.id("fallen_valkyrie"), false);
    }

    @Override
    public ResourceLocation getTextureResource(FallenValkyrieEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
