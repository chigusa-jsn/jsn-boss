package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 竜巻。3つの動き方がある：
 *  SEEK  ：ゆっくりターゲットを追いかけ、触れた相手を高く巻き上げる（7秒）
 *  ORBIT ：第2形態の風の猫の周りを回り続ける。触れると押し出される
 *  TRAIL ：疾風の突進の通り道に残る小さな竜巻（3秒）
 */
public class TornadoEntity extends Entity {
    public static final int SEEK = 0, ORBIT = 1, TRAIL = 2;
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);
    private static final Vector3f MINT = new Vector3f(0.55F, 1.0F, 0.86F);

    private WindCatEntity owner;
    private UUID targetId;
    private float damage = 10.0F;
    private double angle;
    private final Map<UUID, Integer> lastHit = new HashMap<>();

    public TornadoEntity(EntityType<? extends TornadoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    private static TornadoEntity make(WindCatEntity owner, double x, double y, double z, int mode, float damage) {
        TornadoEntity e = new TornadoEntity(ModEntities.TORNADO.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.setPos(x, y, z);
        e.entityData.set(MODE, mode);
        return e;
    }

    public static void spawnSeeker(WindCatEntity owner, double x, double y, double z, LivingEntity target, float damage) {
        TornadoEntity e = make(owner, x, y, z, SEEK, damage);
        if (target != null) e.targetId = target.getUUID();
        owner.level().addFreshEntity(e);
    }

    public static void spawnOrbit(WindCatEntity owner, double startAngle) {
        TornadoEntity e = make(owner, owner.getX(), owner.getY(), owner.getZ(), ORBIT, 5.0F);
        e.angle = startAngle;
        owner.level().addFreshEntity(e);
    }

    public static void spawnTrail(WindCatEntity owner, double x, double y, double z, float damage) {
        owner.level().addFreshEntity(make(owner, x, y, z, TRAIL, damage));
    }

    public static int countOrbiting(WindCatEntity owner) {
        return owner.level().getEntitiesOfClass(TornadoEntity.class, owner.getBoundingBox().inflate(10),
                t -> t.mode() == ORBIT && t.owner == owner).size();
    }

    private int mode() { return this.entityData.get(MODE); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, SEEK);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            particles();
            return;
        }
        int m = mode();
        if (m == SEEK) {
            if (this.tickCount > 140) { this.discard(); return; }
            if (targetId != null && level() instanceof ServerLevel sl && sl.getEntity(targetId) instanceof LivingEntity t && t.isAlive()) {
                Vec3 d = t.position().subtract(position()).multiply(1, 0, 1);
                if (d.length() > 0.3) {
                    Vec3 step = d.normalize().scale(0.18);
                    double y = owner != null ? owner.groundY(getX() + step.x, getY() + 1.5, getZ() + step.z) : getY();
                    this.setPos(getX() + step.x, y, getZ() + step.z);
                }
            }
        } else if (m == ORBIT) {
            if (owner == null || !owner.isAlive() || !owner.isPhase2()) { this.discard(); return; }
            angle += 0.12;
            double x = owner.getX() + Math.cos(angle) * 4.5, z = owner.getZ() + Math.sin(angle) * 4.5;
            this.setPos(x, owner.groundY(x, owner.getY() + 1.5, z), z);
        } else if (this.tickCount > 60) {
            this.discard();
            return;
        }
        hitNearby(m);
    }

    private void hitNearby(int m) {
        double radius = m == SEEK ? 1.5 : 1.2;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius + 1, 3, radius + 1),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d - e.getBbWidth() / 2.0 > radius || e.getY() > getY() + 3.2 || e.getY() + e.getBbHeight() < getY()) continue;
            Integer last = lastHit.get(e.getUUID());
            if (last != null && this.tickCount - last < 20) continue;      // 同じ相手には1秒に1回
            lastHit.put(e.getUUID(), this.tickCount);
            boolean ok = owner != null ? e.hurt(this.damageSources().mobAttack(owner), damage)
                    : e.hurt(this.damageSources().generic(), damage);
            if (!ok) continue;
            if (m == SEEK) {
                e.setDeltaMovement(e.getDeltaMovement().multiply(0.3, 0, 0.3).add(0, 1.1, 0));    // 高く巻き上げる
            } else if (d > 0.01) {
                e.setDeltaMovement(e.getDeltaMovement().add(dx / d * 0.8, 0.3, dz / d * 0.8));    // 押し出す
            }
            e.hurtMarked = true;
        }
    }

    /** らせん状に雲を巻き上げる */
    private void particles() {
        double h = mode() == TRAIL ? 1.8 : 3.2;
        for (int i = 0; i < (mode() == TRAIL ? 3 : 6); i++) {
            double y = (this.tickCount * 0.25 + i * 0.6) % h;
            double a = this.tickCount * 0.6 + i * 1.7 + y * 2;
            double r = 0.3 + y * 0.35;
            double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
            level().addParticle(ParticleTypes.CLOUD, x, getY() + y, z, -Math.sin(a) * 0.08, 0.04, Math.cos(a) * 0.08);
            if (i % 2 == 0) level().addParticle(new DustParticleOptions(MINT, 1.0F), x, getY() + y, z, 0, 0, 0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }
}
