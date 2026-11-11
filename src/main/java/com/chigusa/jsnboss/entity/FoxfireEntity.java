package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * 狐火。光の玉がゆっくり飛びながらターゲットを追いかける。
 * 剣で叩いたり矢を当てたりすると消せる。
 */
public class FoxfireEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> CRIMSON =
            SynchedEntityData.defineId(FoxfireEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Vector3f VIOLET = new Vector3f(1.0F, 0.86F, 0.45F);
    private static final Vector3f RED = new Vector3f(1.0F, 0.95F, 0.8F);
    private static final double SPEED = 0.45, TURN = 0.08;

    private UUID targetId;
    private float damage = 8.0F;

    public FoxfireEntity(EntityType<? extends FoxfireEntity> type, Level level) {
        super(type, level);
    }

    public static void shoot(LivingEntity owner, LivingEntity target, Vec3 pos, Vec3 velocity, float damage) {
        shoot(owner, target, pos, velocity, damage, false);
    }

    public static void shoot(LivingEntity owner, LivingEntity target, Vec3 pos, Vec3 velocity, float damage, boolean crimson) {
        FoxfireEntity orb = new FoxfireEntity(ModEntities.FOXFIRE.get(), owner.level());
        orb.setOwner(owner);
        orb.setPos(pos);
        orb.setDeltaMovement(velocity);
        orb.damage = damage;
        orb.targetId = target.getUUID();
        orb.entityData.set(CRIMSON, crimson);
        owner.level().addFreshEntity(orb);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CRIMSON, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        if (!level().isClientSide) {
            // ターゲットの方へ少しずつ曲がる
            if (targetId != null && level() instanceof ServerLevel sl && sl.getEntity(targetId) instanceof LivingEntity t && t.isAlive()) {
                Vec3 want = t.position().add(0, t.getBbHeight() * 0.6, 0).subtract(this.position()).normalize().scale(SPEED);
                v = v.add(want.subtract(v).scale(TURN)).normalize().scale(SPEED);
                this.setDeltaMovement(v);
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                this.onHit(hit);
                return;
            }
            if (this.tickCount > 120) { pop(); return; }
        } else {
            DustParticleOptions dust = new DustParticleOptions(this.entityData.get(CRIMSON) ? RED : VIOLET, 1.6F);
            for (int i = 0; i < 3; i++) {
                level().addParticle(dust, getX() + (random.nextDouble() - 0.5) * 0.4, getY() + 0.3 + (random.nextDouble() - 0.5) * 0.4,
                        getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0, 0);
            }
            level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 0, 0, 0);
        }
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        ProjectileUtil.rotateTowardsMovement(this, 0.2F);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof AbstractBoss);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity target)) return;
        LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
        target.hurt(this.damageSources().mobProjectile(this, owner), damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) pop();
    }

    /** 叩けば消せる */
    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        if (!level().isClientSide && source.getEntity() != this.getOwner()) pop();
        return true;
    }

    private void pop() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new DustParticleOptions(this.entityData.get(CRIMSON) ? RED : VIOLET, 2.0F),
                    getX(), getY() + 0.3, getZ(), 20, 0.3, 0.3, 0.3, 0);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 8, 0.2, 0.2, 0.2, 0.05);
        }
        this.playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 0.8F, 1.6F);
        this.discard();
    }
}
