package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 追尾ミサイル：まず上へ打ち上がり、0.4 秒後から相手へ曲がって飛ぶ。当たるとブロックを壊さない爆発 */
public class MissileEntity extends Projectile {
    private static final double SPEED = 0.75;
    private float damage = 8.0F;
    private UUID targetId;

    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void launch(AbstractBoss owner, Vec3 pos, Vec3 vel, LivingEntity target, float damage) {
        MissileEntity e = new MissileEntity(ModEntities.MISSILE.get(), owner.level());
        e.setOwner(owner);
        e.setPos(pos);
        e.setDeltaMovement(vel);
        e.damage = damage;
        if (target != null) e.targetId = target.getUUID();
        owner.level().addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        if (level().isClientSide) {
            this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), -v.x * 0.2, -v.y * 0.2, -v.z * 0.2);
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0.02, 0);
            level().addParticle(ParticleTypes.CRIT, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        if (this.tickCount > 90) { explode(); return; }
        if (this.tickCount > 8 && targetId != null && level() instanceof ServerLevel sl
                && sl.getEntity(targetId) instanceof LivingEntity t && t.isAlive()) {
            Vec3 want = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(position()).normalize().scale(SPEED);
            v = v.add(want.subtract(v).scale(0.15));
            if (v.length() > SPEED) v = v.normalize().scale(SPEED);
        } else {
            v = v.add(0, -0.02, 0);
        }
        this.setDeltaMovement(v);
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (!level().noCollision(this, this.getBoundingBox())
                || !level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.3),
                        e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator()).isEmpty()) {
            explode();
        }
    }

    private void explode() {
        Blasts.blast(level(), this.getOwner() instanceof AbstractBoss b ? b : null, this.position(), 2.2, damage);
        this.discard();
    }
}
