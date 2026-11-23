package com.chigusa.jsnboss.mob;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** 王墓のゾンビ：包帯を巻いたミイラ。殴ると「呪いの砂」で空腹と一瞬の盲目。日光で燃えない。 */
public class TombZombie extends Husk {
    public TombZombie(EntityType<? extends Husk> type, Level level) { super(type, level); }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, 1));
            le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                        le.getX(), le.getY() + 1.4, le.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
            }
        }
        return hit;
    }

    @Override
    protected boolean convertsInWater() { return false; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(8) == 0) {
            level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                    getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0, 0);
        }
    }
}
