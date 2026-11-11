package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModDamageTypes;
import com.chigusa.jsnboss.registry.ModEffects;
import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 火柱（と、青い火の玉のあとに残る燃える地面）。
 * 火柱：1秒間地面が光って予告 → 0.6秒間噴き上がる。
 *   赤：10ダメージ＋炎上
 *   青：防具・エンチャント・難易度を無視して必ず15ダメージ（ModDamageTypes.BLUE_FIRE）
 * 燃える地面：3秒間、上に乗ると1秒ごとに2ダメージ＋蒼炎
 */
public class FirePillarEntity extends Entity {
    private static final EntityDataAccessor<Boolean> BLUE =
            SynchedEntityData.defineId(FirePillarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PATCH =
            SynchedEntityData.defineId(FirePillarEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int WARMUP = 20;        // 予告 1.0秒
    public static final int ERUPT = 12;         // 噴き上がり 0.6秒
    public static final int PATCH_LIFE = 60;    // 燃える地面 3秒
    private static final double RADIUS = 1.3;
    private static final double HEIGHT = 4.5;

    private FireKnightEntity owner;
    private final Set<UUID> localHits = new HashSet<>();

    public FirePillarEntity(EntityType<? extends FirePillarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(FireKnightEntity owner, double x, double y, double z, boolean blue, boolean patch) {
        FirePillarEntity e = new FirePillarEntity(ModEntities.FIRE_PILLAR.get(), owner.level());
        e.owner = owner;
        e.setPos(x, y, z);
        e.entityData.set(BLUE, blue);
        e.entityData.set(PATCH, patch);
        owner.level().addFreshEntity(e);
    }

    public boolean isBlue() { return this.entityData.get(BLUE); }
    public boolean isPatch() { return this.entityData.get(PATCH); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLUE, false);
        builder.define(PATCH, false);
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount;
        if (level().isClientSide) {
            particles(t);
            return;
        }
        if (isPatch()) {
            if (t % 20 == 0) burnGround();
            if (t >= PATCH_LIFE) this.discard();
            return;
        }
        if (t == WARMUP) {
            this.playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.5F);
            this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.4F);
        }
        if (t >= WARMUP && t < WARMUP + ERUPT) erupt();
        if (t >= WARMUP + ERUPT) this.discard();
    }

    private void erupt() {
        Set<UUID> hits = owner != null ? owner.pillarHits : localHits;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(RADIUS + 1, HEIGHT, RADIUS + 1),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            if (Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() / 2.0 > RADIUS) continue;
            if (e.getY() > getY() + HEIGHT || e.getY() + e.getBbHeight() < getY()) continue;
            if (!hits.add(e.getUUID())) continue;     // 同じ技の火柱は1本しか当たらない

            boolean ok = isBlue()
                    ? e.hurt(ModDamageTypes.blueFire(level(), owner), 15.0F)
                    : e.hurt(owner != null ? this.damageSources().mobAttack(owner) : this.damageSources().inFire(), 10.0F);
            if (ok) {
                if (!isBlue()) e.igniteForSeconds(4.0F);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.9, 0));   // 打ち上げる
                e.hurtMarked = true;
            }
        }
    }

    private void burnGround() {
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.2, 0.5, 1.2),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            if (e.hurt(this.damageSources().magic(), 2.0F)) {
                e.addEffect(new MobEffectInstance(ModEffects.SOUL_BURN, 40, 0));
            }
        }
    }

    private void particles(int t) {
        ParticleOptions fire = isBlue() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        if (isPatch()) {
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * 1.1;
                level().addParticle(fire, getX() + Math.cos(a) * r, getY() + 0.1, getZ() + Math.sin(a) * r, 0, 0.03, 0);
            }
            return;
        }
        if (t < WARMUP) {
            // 予告：地面に炎の輪。噴き出す直前ほど激しくなる
            int n = 4 + t / 3;
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                level().addParticle(fire, getX() + Math.cos(a) * RADIUS, getY() + 0.05, getZ() + Math.sin(a) * RADIUS, 0, 0.01, 0);
            }
            if (t % 2 == 0) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            if (t > WARMUP - 6) {
                level().addParticle(isBlue() ? ParticleTypes.SOUL : ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 0, 0, 0);
            }
        } else if (t < WARMUP + ERUPT) {
            // 噴き上がる炎の柱
            for (int i = 0; i < 18; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * RADIUS * 0.8;
                level().addParticle(fire, getX() + Math.cos(a) * r, getY() + random.nextDouble() * HEIGHT, getZ() + Math.sin(a) * r,
                        0, 0.25 + random.nextDouble() * 0.25, 0);
            }
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + HEIGHT, getZ(), 0, 0.1, 0);
            if (random.nextInt(2) == 0) {
                level().addParticle(isBlue() ? ParticleTypes.SOUL : ParticleTypes.LAVA, getX(), getY() + 1, getZ(), 0, 0, 0);
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
