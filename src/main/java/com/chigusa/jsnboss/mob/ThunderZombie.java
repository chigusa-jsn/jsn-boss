package com.chigusa.jsnboss.mob;

import com.chigusa.jsnboss.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** 雷のゾンビ：体に電気が走るゾンビ。殴ると感電して 1 秒スタン。雷に打たれると回復する。 */
public class ThunderZombie extends Zombie {
    public ThunderZombie(EntityType<? extends Zombie> type, Level level) { super(type, level); }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(ModEffects.STUN, 20, 0));
            playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 2.0F);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, le.getX(), le.getY() + 1, le.getZ(), 20, 0.3, 0.5, 0.3, 0.2);
            }
        }
        return hit;
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        this.heal(20.0F);
    }

    @Override
    protected boolean convertsInWater() { return false; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0, 0);
        }
    }
}
