package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.YggdrasilEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第2形態は葉が紅葉し、光が赤橙に。第3形態「神樹」は白金と桃色の花が咲き、冠の花（blossoms）が見えるようになる */
public class YggdrasilModel extends DefaultedEntityGeoModel<YggdrasilEntity> {
    private static final ResourceLocation PHASE2 = BossConstants.id("textures/entity/yggdrasil_phase2.png");
    private static final ResourceLocation PHASE3 = BossConstants.id("textures/entity/yggdrasil_phase3.png");

    public YggdrasilModel() {
        super(BossConstants.id("yggdrasil"), false);
    }

    @Override
    public ResourceLocation getTextureResource(YggdrasilEntity entity) {
        if (entity.isPhase3()) return PHASE3;
        return entity.isPhase2() ? PHASE2 : super.getTextureResource(entity);
    }

    /** 冠の花は第3形態でだけ見せる */
    @Override
    public void setCustomAnimations(YggdrasilEntity animatable, long instanceId,
                                    software.bernie.geckolib.animation.AnimationState<YggdrasilEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        var bone = getAnimationProcessor().getBone("blossoms");
        if (bone != null) bone.setHidden(!animatable.isPhase3());
    }
}
