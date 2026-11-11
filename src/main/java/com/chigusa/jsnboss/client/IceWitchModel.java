package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.IceWitchEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * 第2形態はオーロラ紫のテクスチャ。
 * 分身は周りを回る結晶（orbit）が無い ← 本物を見分けるヒント
 */
public class IceWitchModel extends DefaultedEntityGeoModel<IceWitchEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/ice_witch_phase2.png");

    public IceWitchModel() {
        super(BossConstants.id("ice_witch"), false);
    }

    @Override
    public ResourceLocation getTextureResource(IceWitchEntity entity) {
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }

    @Override
    public void setCustomAnimations(IceWitchEntity entity, long instanceId, AnimationState<IceWitchEntity> state) {
        super.setCustomAnimations(entity, instanceId, state);
        GeoBone orbit = getAnimationProcessor().getBone("orbit");
        if (orbit != null) orbit.setHidden(entity.isClone());
    }
}
