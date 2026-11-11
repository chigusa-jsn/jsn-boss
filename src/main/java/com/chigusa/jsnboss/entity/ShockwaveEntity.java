package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 地面を走る衝撃波の輪。中心から外へ広がり、輪が通るときに地面に立っている相手に当たる。
 * 輪が来る瞬間にジャンプしていれば当たらない。
 */
public class ShockwaveEntity extends Entity {
    private static final EntityDataAccessor<Float> MAX_RADIUS =
            SynchedEntityData.defineId(ShockwaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED =
            SynchedEntityData.defineId(ShockwaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ELECTRIC =
            SynchedEntityData.defineId(ShockwaveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final org.joml.Vector3f BLUE = new org.joml.Vector3f(0.5F, 0.9F, 1.0F);

    private static final double THICKNESS = 0.9;   // 輪の太さ
    private static final double JUMP_CLEAR = 0.7;  // 足元がこれより高ければ当たらない

    private AbstractBoss owner;
    private float damage = 12.0F;
    private int stunTicks;
    private final Set<UUID> hits = new HashSet<>();

    public ShockwaveEntity(EntityType<? extends ShockwaveEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, double x, double y, double z, float maxRadius, float speed, float damage) {
        ShockwaveEntity e = new ShockwaveEntity(ModEntities.SHOCKWAVE.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.setPos(x, y, z);
        e.entityData.set(MAX_RADIUS, maxRadius);
        e.entityData.set(SPEED, speed);
        owner.level().addFreshEntity(e);
    }

    /** 雷のリング：電気の輪。当たるとスタン（stunTicks）。ジャンプで避けられるのは同じ */
    public static void spawnElectric(AbstractBoss owner, double x, double y, double z, float maxRadius, float speed,
                                     float damage, int stunTicks) {
        ShockwaveEntity e = new ShockwaveEntity(ModEntities.SHOCKWAVE.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.stunTicks = stunTicks;
        e.setPos(x, y, z);
        e.entityData.set(MAX_RADIUS, maxRadius);
        e.entityData.set(SPEED, speed);
        e.entityData.set(ELECTRIC, true);
        owner.level().addFreshEntity(e);
    }

    private double radius() { return 1.5 + this.tickCount * this.entityData.get(SPEED); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAX_RADIUS, 12.0F);
        builder.define(SPEED, 0.45F);
        builder.define(ELECTRIC, false);
    }

    @Override
    public void tick() {
        super.tick();
        double r = radius();
        if (r > this.entityData.get(MAX_RADIUS)) {
            if (!level().isClientSide) this.discard();
            return;
        }
        if (level().isClientSide) {
            particles(r);
            return;
        }
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r + 1, 2, r + 1),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !ZombieKingEntity.isMinion(e) && !e.isSpectator())) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (Math.abs(d - r) > THICKNESS + e.getBbWidth() / 2.0) continue;
            if (e.getY() - getY() > JUMP_CLEAR || e.getY() < getY() - 1.5) continue;
            if (!hits.add(e.getUUID())) continue;
            boolean ok = owner != null ? e.hurt(this.damageSources().mobAttack(owner), damage)
                    : e.hurt(this.damageSources().generic(), damage);
            if (ok && stunTicks > 0) {
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.chigusa.jsnboss.registry.ModEffects.STUN, stunTicks, 0));
            }
            if (ok && d > 0.01) {
                e.knockback(1.2, -dx / d, -dz / d);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.45, 0));
                e.hurtMarked = true;
            }
        }
    }

    private void particles(double r) {
        if (this.entityData.get(ELECTRIC)) {
            int n = (int) (r * 6);
            double offset = random.nextDouble();
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * (i + offset) / n;
                double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
                level().addParticle(new net.minecraft.core.particles.DustParticleOptions(BLUE, 1.4F), x, getY() + 0.15, z, 0, 0, 0);
                if (i % 2 == 0) {
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, x, getY() + 0.2 + random.nextDouble() * 0.5, z,
                            (random.nextDouble() - 0.5) * 0.2, 0.15, (random.nextDouble() - 0.5) * 0.2);
                }
            }
            return;
        }
        int n = (int) (r * 5);
        double offset = random.nextDouble();
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * (i + offset) / n;
            double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
            BlockState ground = level().getBlockState(BlockPos.containing(x, getY() - 0.5, z));
            if (!ground.isAir() && i % 2 == 0) {
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, getY() + 0.1, z, 0, 0.3, 0);
            }
            if (i % 3 == 0) {
                level().addParticle(ParticleTypes.CLOUD, x, getY() + 0.15, z, Math.cos(a) * 0.05, 0.02, Math.sin(a) * 0.05);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }
}
