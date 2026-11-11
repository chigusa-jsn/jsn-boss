package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 凍てつく大地。8秒間、範囲の中にいると動きが鈍り、だんだん凍えていく。
 * 3.5秒ほど居続けると完全に凍って、凍結ダメージを受け始める（革防具を着ていれば凍らない）。
 */
public class FrostFieldEntity extends Entity {
    private static final EntityDataAccessor<Float> RADIUS =
            SynchedEntityData.defineId(FrostFieldEntity.class, EntityDataSerializers.FLOAT);

    private int life = 160;

    public FrostFieldEntity(EntityType<? extends FrostFieldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, double x, double y, double z, float radius, int life) {
        FrostFieldEntity e = new FrostFieldEntity(ModEntities.FROST_FIELD.get(), owner.level());
        e.setPos(x, y, z);
        e.life = life;
        e.entityData.set(RADIUS, radius);
        owner.level().addFreshEntity(e);
    }

    private float radius() { return this.entityData.get(RADIUS); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 5.0F);
    }

    @Override
    public void tick() {
        super.tick();
        float r = radius();
        if (level().isClientSide) {
            int n = (int) (r * 2);
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = Math.sqrt(random.nextDouble()) * r;
                level().addParticle(i % 3 == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.SNOWFLAKE,
                        getX() + Math.cos(a) * d, getY() + 0.1, getZ() + Math.sin(a) * d, 0, 0.01, 0);
            }
            if (this.tickCount % 2 == 0) {
                double a = random.nextDouble() * Math.PI * 2;
                level().addParticle(ParticleTypes.SNOWFLAKE, getX() + Math.cos(a) * r, getY() + 0.1, getZ() + Math.sin(a) * r, 0, 0.02, 0);
            }
            return;
        }
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r + 1, 2, r + 1),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            if (dx * dx + dz * dz > r * r || Math.abs(e.getY() - getY()) > 2) continue;
            IceWitchEntity.freeze(e, 4);            // 自然に溶ける分(2)より速く凍る
            if (this.tickCount % 10 == 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
        }
        if (this.tickCount >= life) this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }
}
