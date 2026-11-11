package com.chigusa.jsnboss.effect;

import com.chigusa.jsnboss.BossConstants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** スタン：しびれて動けない・跳べない・攻撃が弱くなる（雷のリングで2秒）。牛乳で治る。 */
public class StunEffect extends MobEffect {
    public StunEffect() {
        super(MobEffectCategory.HARMFUL, 0x7FE3FF, ParticleTypes.ELECTRIC_SPARK);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, BossConstants.id("effect.stun_speed"),
                -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, BossConstants.id("effect.stun_jump"),
                -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, BossConstants.id("effect.stun_attack"),
                -0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
