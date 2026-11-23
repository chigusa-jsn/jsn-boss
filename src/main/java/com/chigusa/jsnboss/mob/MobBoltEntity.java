package com.chigusa.jsnboss.mob;

import com.chigusa.jsnboss.entity.IceWitchEntity;
import com.chigusa.jsnboss.registry.ModEffects;
import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 亜種ブレイズの弾。SOUL＝蒼炎の弾（当たると蒼炎）、FROST＝氷の弾（当たると凍える＋鈍化）。
 * まっすぐ飛び、ブロックに当たると消える。見た目はパーティクルだけ。
 */
public class MobBoltEntity extends Projectile {
    public static final int SOUL = 0, FROST = 1;
    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(MobBoltEntity.class, EntityDataSerializers.INT);
    private float damage = 5.0F;

    public MobBoltEntity(EntityType<? extends MobBoltEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void shoot(LivingEntity owner, Vec3 pos, Vec3 velocity, int kind, float damage) {
        MobBoltEntity e = new MobBoltEntity(ModEntities.MOB_BOLT.get(), owner.level());
        e.setOwner(owner);
        e.setPos(pos);
        e.setDeltaMovement(velocity);
        e.damage = damage;
        e.entityData.set(KIND, kind);
        owner.level().addFreshEntity(e);
    }

    private int kind() { return this.entityData.get(KIND); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, SOUL);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(kind() == SOUL ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SNOWFLAKE,
                        getX() + (random.nextDouble() - 0.5) * 0.3, getY() + (random.nextDouble() - 0.5) * 0.3,
                        getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
            }
            if (kind() == FROST) level().addParticle(ParticleTypes.ITEM_SNOWBALL, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        if (this.tickCount > 80 || !level().noCollision(this, this.getBoundingBox())) { this.discard(); return; }
        LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2),
                e -> e.isAlive() && e != owner && !e.isSpectator())) {
            if (e.hurt(this.damageSources().mobProjectile(this, owner), damage)) {
                if (kind() == SOUL) {
                    e.addEffect(new MobEffectInstance(ModEffects.SOUL_BURN, 100, 0));
                } else {
                    IceWitchEntity.freeze(e, 100);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
            }
            this.discard();
            return;
        }
    }
}
