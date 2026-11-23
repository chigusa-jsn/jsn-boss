package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** 羽根の刃：翼から飛ばす光る羽根。まっすぐ飛び、ブロックに当たると消える */
public class FeatherShotEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> RED =
            SynchedEntityData.defineId(FeatherShotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Vector3f CYAN = new Vector3f(0.6F, 0.95F, 1.0F);
    private static final Vector3f MAGENTA = new Vector3f(1.0F, 0.2F, 0.45F);
    private float damage = 8.0F;

    public FeatherShotEntity(EntityType<? extends FeatherShotEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void shoot(AbstractBoss owner, Vec3 pos, Vec3 velocity, float damage) {
        FeatherShotEntity e = new FeatherShotEntity(ModEntities.FEATHER_SHOT.get(), owner.level());
        e.setOwner(owner);
        e.setPos(pos);
        e.setDeltaMovement(velocity);
        e.damage = damage;
        e.entityData.set(RED, owner.isPhase2());
        owner.level().addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RED, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (level().isClientSide) {
            Vector3f c = this.entityData.get(RED) ? MAGENTA : CYAN;
            for (int i = 0; i < 3; i++) {
                level().addParticle(new DustParticleOptions(c, 1.1F), getX() - v.x * i * 0.3, getY() - v.y * i * 0.3, getZ() - v.z * i * 0.3, 0, 0, 0);
            }
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        if (this.tickCount > 50 || !level().noCollision(this, this.getBoundingBox())) { this.discard(); return; }
        LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.3),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            e.hurt(this.damageSources().mobProjectile(this, owner), damage);
            this.discard();
            return;
        }
    }
}
