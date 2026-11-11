package com.chigusa.jsnboss.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** 裂傷：0.5 秒ごとに1ダメージ（防具無視）。牛乳で治る。 */
public class LacerationEffect extends MobEffect {
    public LacerationEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0F1A, ParticleTypes.DAMAGE_INDICATOR);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(entity.damageSources().magic(), 1.0F + amplifier);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }
}
