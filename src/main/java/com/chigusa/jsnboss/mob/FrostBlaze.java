package com.chigusa.jsnboss.mob;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;

/** 氷結のブレイズ：氷の弾を撃つ。当たると凍えて鈍くなる。水も雪も平気。 */
public class FrostBlaze extends VariantBlaze {
    public FrostBlaze(EntityType<? extends Blaze> type, Level level) { super(type, level); }

    @Override protected int boltKind() { return MobBoltEntity.FROST; }
    @Override protected float boltDamage() { return 5.0F; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.SNOWFLAKE; }

    @Override
    public boolean isSensitiveToWater() { return false; }

    @Override
    public boolean canFreeze() { return false; }

    @Override
    public boolean isOnFire() { return false; }
}
