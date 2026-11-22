package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/** ドローンの弾：光る小さな弾。少しだけ狙った相手へ曲がる。ブロックに当たると消える */
public class DroneShotEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> CYAN =
            SynchedEntityData.defineId(DroneShotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Vector3f PURPLE = new Vector3f(0.77F, 0.36F, 1.0F);
    private static final Vector3f CYAN_C = new Vector3f(0.31F, 0.9F, 1.0F);

    private float damage = 6.0F;
    private UUID targetId;

    public DroneShotEntity(EntityType<? extends DroneShotEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void shoot(AbstractBoss owner, Vec3 pos, LivingEntity target, float damage, boolean cyan) {
        DroneShotEntity e = new DroneShotEntity(ModEntities.DRONE_SHOT.get(), owner.level());
        e.setOwner(owner);
        e.setPos(pos);
        Vec3 aim = target.getEyePosition().subtract(pos).normalize().scale(0.9);
        e.setDeltaMovement(aim);
        e.damage = damage;
        e.targetId = target.getUUID();
        e.entityData.set(CYAN, cyan);
        owner.level().addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CYAN, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        if (level().isClientSide) {
            this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
            level().addParticle(new DustParticleOptions(this.entityData.get(CYAN) ? CYAN_C : PURPLE, 1.2F), getX(), getY(), getZ(), 0, 0, 0);
            level().addParticle(this.entityData.get(CYAN) ? ParticleTypes.END_ROD : ParticleTypes.PORTAL, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        if (this.tickCount > 60) { this.discard(); return; }
        // 最初の 25 tick だけ、少し相手の方へ曲がる
        if (this.tickCount < 25 && targetId != null && level() instanceof ServerLevel sl
                && sl.getEntity(targetId) instanceof LivingEntity t && t.isAlive()) {
            Vec3 want = t.getEyePosition().subtract(position()).normalize().scale(0.9);
            v = v.add(want.subtract(v).scale(0.06)).normalize().scale(0.9);
            this.setDeltaMovement(v);
        }
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (!level().noCollision(this, this.getBoundingBox())) { this.discard(); return; }
        LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.4),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            e.hurt(this.damageSources().mobProjectile(this, owner), damage);
            this.discard();
            return;
        }
    }
}
