package com.chigusa.jsnboss.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/**
 * 氷のクリーパー：雪の積もった氷のクリーパー。爆発はブロックを壊さないかわりに、巻き込んだ相手を凍らせて鈍くする
 * （BossEvents.onExplosion）。粉雪で凍らない。
 */
public class FrostCreeper extends Creeper {
    public FrostCreeper(EntityType<? extends Creeper> type, Level level) { super(type, level); }

    @Override
    public boolean canFreeze() { return false; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, -0.02, 0);
        }
    }
}
