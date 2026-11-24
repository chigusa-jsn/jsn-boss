package com.chigusa.jsnboss.client;

import com.chigusa.jsnboss.BossConstants;
import com.chigusa.jsnboss.entity.YggdrasilSpiritEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** 第4形態は杖、第5形態は光の翼と結晶の剣を見せる */
public class YggdrasilSpiritModel extends DefaultedEntityGeoModel<YggdrasilSpiritEntity> {
    private static final String[] STAFF = {"staff", "staff_crystal"};
    private static final String[] SWORD = {"wings", "wing_right", "wing_left", "sword"};

    public YggdrasilSpiritModel() {
        super(BossConstants.id("yggdrasil_spirit"), false);
    }

    /** 第5形態は、剣と光の翼が今の属性の色になる */
    @Override
    public net.minecraft.resources.ResourceLocation getTextureResource(YggdrasilSpiritEntity e) {
        if (e.isPhase2()) return BossConstants.id("textures/entity/yggdrasil_spirit_" + e.element().id + ".png");
        return super.getTextureResource(e);
    }

    @Override
    public void setCustomAnimations(YggdrasilSpiritEntity animatable, long instanceId, AnimationState<YggdrasilSpiritEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        boolean p5 = animatable.isPhase2();
        for (String b : STAFF) hide(b, p5);
        for (String b : SWORD) hide(b, !p5);
    }

    private void hide(String name, boolean hidden) {
        var bone = getAnimationProcessor().getBone(name);
        if (bone != null) {
            bone.setHidden(hidden);
            bone.setChildrenHidden(hidden);
        }
    }
}
