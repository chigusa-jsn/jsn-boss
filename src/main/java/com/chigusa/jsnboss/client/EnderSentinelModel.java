package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.EnderSentinelEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態は紫の光がすべてシアンに変わるテクスチャ */
public class EnderSentinelModel extends DefaultedEntityGeoModel<EnderSentinelEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/ender_sentinel_phase2.png");

    public EnderSentinelModel() {
        super(BossConstants.id("ender_sentinel"), false);
    }

    @Override
    public ResourceLocation getTextureResource(EnderSentinelEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }
}
