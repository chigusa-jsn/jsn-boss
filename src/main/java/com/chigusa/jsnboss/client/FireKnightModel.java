package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.FireKnightEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * geo/entity/fire_knight.geo.json, animations/entity/fire_knight.animation.json,
 * textures/entity/fire_knight.png を自動で読み込む。
 * 第2形態では青い炎のテクスチャに切り替える（発光マスクも _glowmask 付きのものに自動で切り替わる）。
 */
public class FireKnightModel extends DefaultedEntityGeoModel<FireKnightEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/fire_knight_phase2.png");

    public FireKnightModel() {
        super(BossConstants.id("fire_knight"), false);
    }

    @Override
    public ResourceLocation getTextureResource(FireKnightEntity entity) {
        return entity.getPhase() >= 2 ? PHASE2 : super.getTextureResource(entity);
    }
}
