package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 飛ぶ斬撃。ブロックを貫通してまっすぐ進む三日月形の炎の刃。
 * 横向き：足元の高さ（ジャンプで避ける）
 * 縦向き：高さ3.5ブロック・幅が細い（横に動いて避ける）
 */
public class FlameSlashEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> BLUE =
            SynchedEntityData.defineId(FlameSlashEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> VERTICAL =
            SynchedEntityData.defineId(FlameSlashEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double HALF_WIDTH = 2.3;   // 横斬撃の左右の幅（片側）
    private static final double LOW_TOP = 0.8;      // 横斬撃の高さ（これより上ならジャンプで避けられる）
    private static final double TALL = 3.6;         // 縦斬撃の高さ

    private float damage = 10.0F;
    private final Set<UUID> hit = new HashSet<>();

    public FlameSlashEntity(EntityType<? extends FlameSlashEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void shoot(FireKnightEntity owner, Vec3 pos, Vec3 velocity, boolean vertical, boolean blue, float damage) {
        FlameSlashEntity slash = new FlameSlashEntity(ModEntities.FLAME_SLASH.get(), owner.level());
        slash.setOwner(owner);
        slash.setPos(pos);
        slash.setDeltaMovement(velocity);
        slash.entityData.set(VERTICAL, vertical);
        slash.entityData.set(BLUE, blue);
        slash.damage = damage;
        owner.level().addFreshEntity(slash);
    }

    public boolean isBlue() { return this.entityData.get(BLUE); }
    public boolean isVertical() { return this.entityData.get(VERTICAL); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLUE, false);
        builder.define(VERTICAL, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);

        Vec3 dir = new Vec3(v.x, 0, v.z);
        if (dir.lengthSqr() < 1.0E-6) { this.discard(); return; }
        dir = dir.normalize();
        Vec3 right = new Vec3(-dir.z, 0, dir.x);

        if (level().isClientSide) {
            particles(dir, right);
            return;
        }
        if (this.tickCount > 40) { this.discard(); return; }

        LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
        boolean vertical = isVertical();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0, 4.0, 3.0),
                e -> e.isAlive() && !(e instanceof FireKnightEntity) && !e.isSpectator())) {
            Vec3 rel = e.position().subtract(this.position());
            double f = rel.dot(dir), l = rel.dot(right), w = e.getBbWidth() / 2.0;
            double bottom = e.getY() - getY(), top = bottom + e.getBbHeight();
            boolean inside;
            if (vertical) {
                inside = Math.abs(f) < 0.9 + w && Math.abs(l) < 0.6 + w && top > 0 && bottom < TALL;
            } else {
                inside = Math.abs(f) < 0.9 + w && Math.abs(l) < HALF_WIDTH + w && top > 0.15 && bottom < LOW_TOP;
            }
            if (inside && hit.add(e.getUUID())) {
                if (e.hurt(this.damageSources().mobProjectile(this, owner), damage)) {
                    e.knockback(0.8, -dir.x, -dir.z);
                    if (!isBlue()) e.igniteForSeconds(3.0F);
                }
            }
        }
    }

    /** 三日月形に炎を並べる（両端が少し後ろに反った形） */
    private void particles(Vec3 dir, Vec3 right) {
        ParticleOptions fire = isBlue() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        if (isVertical()) {
            for (int k = 0; k <= 14; k++) {
                double h = k / 14.0 * 3.4, c = (h - 1.7) / 1.7;
                Vec3 p = this.position().add(dir.scale(-0.7 * c * c));
                level().addParticle(fire, p.x, getY() + h, p.z, dir.x * 0.05, 0, dir.z * 0.05);
            }
        } else {
            for (int k = -10; k <= 10; k++) {
                double c = k / 10.0;
                Vec3 p = this.position().add(right.scale(c * HALF_WIDTH)).add(dir.scale(-0.7 * c * c));
                level().addParticle(fire, p.x, getY() + 0.45, p.z, dir.x * 0.05, 0.005, dir.z * 0.05);
            }
        }
        if (this.tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.SWEEP_ATTACK, getX(), getY() + (isVertical() ? 1.7 : 0.5), getZ(), 0, 0, 0);
        }
    }
}
