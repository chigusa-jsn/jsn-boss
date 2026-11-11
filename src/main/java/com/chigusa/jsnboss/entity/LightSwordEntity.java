package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
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

/**
 * 光の剣の雨の1本：地面に金色の輪が出て（予告）、最後の 5 tick で空から光る剣が落ちて突き刺さる。
 * 描画は LightSwordRenderer。
 */
public class LightSwordEntity extends Entity {
    private static final EntityDataAccessor<Integer> WARMUP =
            SynchedEntityData.defineId(LightSwordEntity.class, EntityDataSerializers.INT);
    public static final int FALL = 5;      // 落ちてくる時間
    public static final int STUCK = 14;    // 刺さったまま光っている時間
    public static final double DROP = 12;  // 何ブロック上から落ちるか
    private static final double RADIUS = 1.1;
    private static final Vector3f GOLD = new Vector3f(1.0F, 0.86F, 0.45F);

    private AbstractBoss owner;
    private float damage = 14.0F;
    private final Set<UUID> hits = new HashSet<>();

    public LightSwordEntity(EntityType<? extends LightSwordEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, double x, double y, double z, int warmup, float damage) {
        LightSwordEntity e = new LightSwordEntity(ModEntities.LIGHT_SWORD.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.setPos(x, y, z);
        e.setYRot(owner.getRandom().nextFloat() * 360);
        e.entityData.set(WARMUP, warmup);
        owner.level().addFreshEntity(e);
    }

    public int getWarmup() { return this.entityData.get(WARMUP); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WARMUP, 16);
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount, w = getWarmup();
        if (level().isClientSide) {
            particles(t, w);
            return;
        }
        if (t == w) {
            this.playSound(SoundEvents.ANVIL_LAND, 0.8F, 1.6F);
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.8F);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(RADIUS + 1, 3, RADIUS + 1),
                    e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
                double dx = e.getX() - getX(), dz = e.getZ() - getZ();
                if (Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() / 2.0 > RADIUS) continue;
                if (e.getY() > getY() + 3 || e.getY() + e.getBbHeight() < getY()) continue;
                if (!hits.add(e.getUUID())) continue;
                if (owner != null) e.hurt(this.damageSources().indirectMagic(this, owner), damage);
                else e.hurt(this.damageSources().magic(), damage);
            }
        }
        if (t >= w + STUCK) this.discard();
    }

    private void particles(int t, int w) {
        if (t < w) {
            int n = 1 + t * 3 / Math.max(1, w);
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                level().addParticle(new DustParticleOptions(GOLD, 1.0F), getX() + Math.cos(a) * RADIUS, getY() + 0.05,
                        getZ() + Math.sin(a) * RADIUS, 0, 0, 0);
            }
        } else if (t < w + 3) {
            for (int i = 0; i < 10; i++) {
                level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(),
                        (random.nextDouble() - 0.5) * 0.4, random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.4);
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
