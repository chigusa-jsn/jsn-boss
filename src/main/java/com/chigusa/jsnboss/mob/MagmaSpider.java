package com.chigusa.jsnboss.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 溶岩グモ：溶岩のひびが光る黒いクモ。歩いた跡が 3 秒間燃え、上に乗ると炎上する（ブロックは燃やさない）。
 * 噛まれると炎上。火が効かない。
 */
public class MagmaSpider extends Spider {
    private static final int TRAIL_LIFE = 60;
    private final Deque<double[]> trail = new ArrayDeque<>();   // x, y, z, 置いた tick

    public MagmaSpider(EntityType<? extends Spider> type, Level level) { super(type, level); }

    @Override
    public void aiStep() {
        super.aiStep();
        // 歩いた跡を 0.5 秒ごとに記録（サーバーとクライアントで同じように）
        if (this.tickCount % 10 == 0 && this.onGround() && this.getDeltaMovement().horizontalDistanceSqr() > 1e-4) {
            trail.addLast(new double[]{getX(), getY(), getZ(), this.tickCount});
        }
        while (!trail.isEmpty() && this.tickCount - trail.peekFirst()[3] > TRAIL_LIFE) trail.removeFirst();
        if (level().isClientSide) {
            for (double[] p : trail) {
                if (random.nextInt(4) == 0) {
                    level().addParticle(ParticleTypes.FLAME, p[0] + (random.nextDouble() - 0.5) * 0.8, p[1] + 0.05,
                            p[2] + (random.nextDouble() - 0.5) * 0.8, 0, 0.02, 0);
                }
            }
            if (random.nextInt(6) == 0) level().addParticle(ParticleTypes.LAVA, getRandomX(0.6), getY() + 0.6, getRandomZ(0.6), 0, 0, 0);
            return;
        }
        if (this.tickCount % 5 == 0) {
            for (double[] p : trail) {
                Vec3 c = new Vec3(p[0], p[1], p[2]);
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(0.7, 0.5, 0.7),
                        e -> e.isAlive() && !(e instanceof MagmaSpider) && !e.fireImmune())) {
                    e.igniteForSeconds(3);
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) target.igniteForSeconds(4);
        return hit;
    }
}
