package com.chigusa.jsnboss.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * ブロックを壊さない爆発。見た目と音はバニラの爆発と同じで、ダメージは決めた値
 * （中心から半径の半分までは満額、そこから端に向かって半分まで減る）。ボスには当たらない。
 */
public final class Blasts {
    private Blasts() {}

    public static void blast(Level level, AbstractBoss owner, Vec3 c, double radius, float damage) {
        if (!(level instanceof ServerLevel sl)) return;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius + 1),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            Vec3 d = e.position().add(0, e.getBbHeight() / 2, 0).subtract(c);
            double dist = d.length();
            if (dist > radius + e.getBbWidth() / 2) continue;
            float k = dist < radius / 2 ? 1.0F : (float) (1.0 - 0.5 * (dist - radius / 2) / (radius / 2));
            boolean ok = owner != null ? e.hurt(sl.damageSources().mobAttack(owner), damage * Math.max(0.5F, k))
                    : e.hurt(sl.damageSources().generic(), damage * Math.max(0.5F, k));
            if (ok) {
                Vec3 push = d.lengthSqr() > 1e-4 ? d.multiply(1, 0, 1).normalize().scale(0.6 + radius * 0.12) : Vec3.ZERO;
                e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.35 + radius * 0.05, push.z));
                e.hurtMarked = true;
            }
        }
        sl.sendParticles(radius >= 4 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z,
                radius >= 4 ? 1 : 3, radius * 0.2, 0.2, radius * 0.2, 0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.5, c.z, (int) (radius * 8), radius * 0.35, 0.4, radius * 0.35, 0.05);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
                (float) Math.min(4.0, 1.0 + radius * 0.4), 0.9F + level.random.nextFloat() * 0.2F);
    }
}
