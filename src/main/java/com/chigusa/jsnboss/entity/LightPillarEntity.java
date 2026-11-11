package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 光の柱：地面に金色の輪が浮かび（予告1秒）、そのあと天まで光が立ちのぼる。 */
public class LightPillarEntity extends Entity {
    private static final int WARMUP = 20, ERUPT = 10;
    private static final double RADIUS = 1.4, HEIGHT = 6.0;
    private static final Vector3f GOLD = new Vector3f(1.0F, 0.86F, 0.45F);

    private AbstractBoss owner;
    private float damage = 14.0F;
    private final Set<UUID> hits = new HashSet<>();

    public LightPillarEntity(EntityType<? extends LightPillarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, double x, double y, double z, float damage) {
        LightPillarEntity e = new LightPillarEntity(ModEntities.LIGHT_PILLAR.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.setPos(x, y, z);
        owner.level().addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount;
        if (level().isClientSide) {
            particles(t);
            return;
        }
        if (t == WARMUP) this.playSound(SoundEvents.BEACON_ACTIVATE, 2.0F, 1.8F);
        if (t >= WARMUP && t < WARMUP + ERUPT) {
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(RADIUS + 1, HEIGHT, RADIUS + 1),
                    e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
                double dx = e.getX() - getX(), dz = e.getZ() - getZ();
                if (Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() / 2.0 > RADIUS) continue;
                if (e.getY() > getY() + HEIGHT || e.getY() + e.getBbHeight() < getY()) continue;
                if (!hits.add(e.getUUID())) continue;
                boolean ok = owner != null ? e.hurt(this.damageSources().indirectMagic(this, owner), damage)
                        : e.hurt(this.damageSources().magic(), damage);
                if (ok) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0));
                    e.hurtMarked = true;
                }
            }
        }
        if (t >= WARMUP + ERUPT) this.discard();
    }

    private void particles(int t) {
        DustParticleOptions gold = new DustParticleOptions(GOLD, 1.2F);
        if (t < WARMUP) {
            int n = 3 + t / 3;
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                level().addParticle(gold, getX() + Math.cos(a) * RADIUS, getY() + 0.05, getZ() + Math.sin(a) * RADIUS, 0, 0.01, 0);
            }
            if (t > WARMUP - 6) level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 0, 0.15, 0);
        } else if (t < WARMUP + ERUPT) {
            for (int i = 0; i < 16; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * RADIUS * 0.8;
                level().addParticle(ParticleTypes.END_ROD, getX() + Math.cos(a) * r, getY() + random.nextDouble() * HEIGHT,
                        getZ() + Math.sin(a) * r, 0, 0.3, 0);
            }
            level().addParticle(gold, getX(), getY() + random.nextDouble() * HEIGHT, getZ(), 0, 0.2, 0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }
}
