package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.ZombieKingEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態「不死の王」は目と宝石が緑に光るテクスチャ */
public class ZombieKingModel extends DefaultedEntityGeoModel<ZombieKingEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/zombie_king_phase2.png");

    public ZombieKingModel() {
        super(BossConstants.id("zombie_king"), false);
    }

    @Override
    public ResourceLocation getTextureResource(ZombieKingEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
