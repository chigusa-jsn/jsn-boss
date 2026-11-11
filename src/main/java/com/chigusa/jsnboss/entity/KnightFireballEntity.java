package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEffects;
import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 火の玉。第2形態では青くなり、当たると蒼炎＋地面に青い炎が少し残る。 */
public class KnightFireballEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> BLUE =
            SynchedEntityData.defineId(KnightFireballEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage = 9.0F;

    public KnightFireballEntity(EntityType<? extends KnightFireballEntity> type, Level level) {
        super(type, level);
    }

    public static void shoot(FireKnightEntity owner, Vec3 pos, Vec3 velocity, boolean blue, float damage) {
        KnightFireballEntity ball = new KnightFireballEntity(ModEntities.KNIGHT_FIREBALL.get(), owner.level());
        ball.setOwner(owner);
        ball.setPos(pos);
        ball.setDeltaMovement(velocity);
        ball.entityData.set(BLUE, blue);
        ball.damage = damage;
        owner.level().addFreshEntity(ball);
    }

    public boolean isBlue() { return this.entityData.get(BLUE); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLUE, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        if (!level().isClientSide) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                this.onHit(hit);
                return;
            }
            if (this.tickCount > 100) this.discard();
        } else {
            trail();
        }
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        ProjectileUtil.rotateTowardsMovement(this, 0.2F);
    }

    private void trail() {
        ParticleOptions fire = isBlue() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        for (int i = 0; i < 5; i++) {
            level().addParticle(fire, getX() + (random.nextDouble() - 0.5) * 0.5, getY() + 0.4 + (random.nextDouble() - 0.5) * 0.5,
                    getZ() + (random.nextDouble() - 0.5) * 0.5, 0, 0, 0);
        }
        level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4, getZ(), 0, 0.01, 0);
        if (random.nextInt(3) == 0) {
            level().addParticle(isBlue() ? ParticleTypes.SOUL : ParticleTypes.LAVA, getX(), getY() + 0.4, getZ(), 0, 0, 0);
        }
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
        if (target.hurt(this.damageSources().mobProjectile(this, owner), damage)) {
            if (isBlue()) {
                target.addEffect(new MobEffectInstance(ModEffects.SOUL_BURN, 60, 0));
            } else {
                target.igniteForSeconds(4.0F);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        if (level() instanceof ServerLevel sl) {
            ParticleOptions fire = isBlue() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
            sl.sendParticles(fire, getX(), getY() + 0.4, getZ(), 40, 0.4, 0.4, 0.4, 0.12);
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.4, getZ(), 1, 0, 0, 0, 0);
        }
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.2F);
        // 第2形態：当たった場所に青い炎が少し残る
        if (isBlue() && this.getOwner() instanceof FireKnightEntity knight) {
            FirePillarEntity.spawn(knight, getX(), Math.floor(getY() + 0.2), getZ(), true, true);
        }
        this.discard();
    }
}
