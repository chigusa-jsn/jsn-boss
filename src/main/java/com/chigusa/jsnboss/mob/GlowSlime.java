package com.chigusa.jsnboss.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;

/** 光るスライム：黄緑に光る。攻撃で発光。大きさ4で HP 50（分裂した小さいのは少なめ） */
public class GlowSlime extends Slime {
    public GlowSlime(EntityType<? extends Slime> type, Level level) { super(type, level); }

    @Override
    public void setSize(int size, boolean resetHealth) {
        super.setSize(size, resetHealth);
        var hp = this.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(Math.max(1, size) * 12.5);
        if (resetHealth) this.setHealth(this.getMaxHealth());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.GLOW, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
        }
    }
}
