package com.chigusa.jsnboss.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** 蒼炎：1秒ごとに1ダメージ。エフェクトなので水に入っても消えない。 */
public class SoulBurnEffect extends MobEffect {
    public SoulBurnEffect() {
        super(MobEffectCategory.HARMFUL, 0x3FA9FF, ParticleTypes.SOUL_FIRE_FLAME);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(entity.damageSources().magic(), 1.0F + amplifier);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
