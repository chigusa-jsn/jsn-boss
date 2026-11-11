package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** かまいたち：ブロックを貫通してまっすぐ飛ぶ、横向きの三日月形の風の刃（体の高さ）。 */
public class WindBladeEntity extends Projectile {
    private static final double HALF_WIDTH = 1.8;
    private static final Vector3f MINT = new Vector3f(0.55F, 1.0F, 0.86F);

    private float damage = 9.0F;
    private final Set<UUID> hit = new HashSet<>();

    public WindBladeEntity(EntityType<? extends WindBladeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void shoot(LivingEntity owner, Vec3 pos, Vec3 velocity, float damage) {
        WindBladeEntity e = new WindBladeEntity(ModEntities.WIND_BLADE.get(), owner.level());
        e.setOwner(owner);
        e.setPos(pos);
        e.setDeltaMovement(velocity);
        e.damage = damage;
        owner.level().addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        Vec3 dir = new Vec3(v.x, 0, v.z);
        if (dir.lengthSqr() < 1e-6) { this.discard(); return; }
        dir = dir.normalize();
        Vec3 right = new Vec3(-dir.z, 0, dir.x);

        if (level().isClientSide) {
            for (int k = -8; k <= 8; k++) {
                double c = k / 8.0;
                Vec3 p = this.position().add(right.scale(c * HALF_WIDTH)).add(dir.scale(-0.6 * c * c));
                level().addParticle(k % 2 == 0 ? ParticleTypes.CLOUD : new DustParticleOptions(MINT, 1.0F),
                        p.x, getY() + 1.0, p.z, dir.x * 0.05, 0, dir.z * 0.05);
            }
            if (this.tickCount % 3 == 0) level().addParticle(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1.0, getZ(), 0, 0, 0);
            return;
        }
        if (this.tickCount > 30) { this.discard(); return; }
        LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0, 3.0, 3.0),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            Vec3 rel = e.position().subtract(this.position());
            double f = rel.dot(dir), l = rel.dot(right), w = e.getBbWidth() / 2.0;
            double bottom = e.getY() - getY(), top = bottom + e.getBbHeight();
            if (Math.abs(f) < 0.9 + w && Math.abs(l) < HALF_WIDTH + w && top > 0.3 && bottom < 1.7 && hit.add(e.getUUID())) {
                if (e.hurt(this.damageSources().mobProjectile(this, owner), damage)) e.knockback(0.6, -dir.x, -dir.z);
            }
        }
    }
}
