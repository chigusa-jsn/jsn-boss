package com.chigusa.jsnboss.mob;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;

/** 蒼炎のブレイズ：青い炎の弾を撃つ。当たると蒼炎（水でも消えない）。 */
public class SoulBlaze extends VariantBlaze {
    public SoulBlaze(EntityType<? extends Blaze> type, Level level) { super(type, level); }

    @Override protected int boltKind() { return MobBoltEntity.SOUL; }
    @Override protected float boltDamage() { return 6.0F; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.SOUL_FIRE_FLAME; }
}
