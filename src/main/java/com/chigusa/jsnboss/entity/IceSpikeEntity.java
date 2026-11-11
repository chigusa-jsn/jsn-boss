package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 地面から突き出す氷のトゲ（SPIKE）と、空から落ちてくる雹（HAIL）。
 * どちらも warmup の間に地面へ予告が出て、そのあと当たる。
 */
public class IceSpikeEntity extends Entity {
    public static final int SPIKE = 0, HAIL = 1;

    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(IceSpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WARMUP =
            SynchedEntityData.defineId(IceSpikeEntity.class, EntityDataSerializers.INT);

    private static final int ACTIVE = 8;
    private AbstractBoss owner;
    private float damage = 12.0F;
    private final Set<UUID> hits = new HashSet<>();

    public IceSpikeEntity(EntityType<? extends IceSpikeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, double x, double y, double z, int warmup, int mode, float damage) {
        IceSpikeEntity e = new IceSpikeEntity(ModEntities.ICE_SPIKE.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.setPos(x, y, z);
        e.entityData.set(MODE, mode);
        e.entityData.set(WARMUP, warmup);
        owner.level().addFreshEntity(e);
    }

    private int mode() { return this.entityData.get(MODE); }
    private int warmup() { return this.entityData.get(WARMUP); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, SPIKE);
        builder.define(WARMUP, 16);
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount, w = warmup();
        if (level().isClientSide) {
            particles(t, w);
            return;
        }
        if (t == w) {
            this.playSound(mode() == HAIL ? SoundEvents.GLASS_BREAK : SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5F, 0.8F);
        }
        if (t >= w && t < w + ACTIVE) strike();
        if (t >= w + ACTIVE) this.discard();
    }

    private void strike() {
        double radius = mode() == HAIL ? 1.6 : 1.2, height = mode() == HAIL ? 2.5 : 3.0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(radius + 1, height, radius + 1),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            if (Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() / 2.0 > radius) continue;
            if (e.getY() > getY() + height || e.getY() + e.getBbHeight() < getY()) continue;
            if (!hits.add(e.getUUID())) continue;
            boolean ok = owner != null ? e.hurt(this.damageSources().mobAttack(owner), damage)
                    : e.hurt(this.damageSources().freeze(), damage);
            if (ok) {
                IceWitchEntity.freeze(e, 80);
                if (mode() == SPIKE) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0));
                    e.hurtMarked = true;
                }
            }
        }
    }

    private void particles(int t, int w) {
        BlockParticleOption ice = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
        double radius = mode() == HAIL ? 1.6 : 1.2;
        if (t < w) {
            // 予告：地面に雪の輪。直前ほど濃くなる
            int n = 2 + t * 6 / Math.max(1, w);
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                level().addParticle(ParticleTypes.SNOWFLAKE, getX() + Math.cos(a) * radius, getY() + 0.05,
                        getZ() + Math.sin(a) * radius, 0, 0.005, 0);
            }
            if (mode() == HAIL && t > w - 8) {
                // 空から落ちてくる氷塊
                double h = (w - t) * 1.2;
                level().addParticle(ice, getX(), getY() + h, getZ(), 0, -1, 0);
                level().addParticle(ParticleTypes.END_ROD, getX(), getY() + h, getZ(), 0, -0.5, 0);
            }
        } else if (t < w + 3) {
            // 噴き出す / 砕ける
            for (int i = 0; i < 14; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * radius;
                level().addParticle(ice, getX() + Math.cos(a) * r, getY() + (mode() == SPIKE ? random.nextDouble() * 2.5 : 0.3),
                        getZ() + Math.sin(a) * r, 0, mode() == SPIKE ? 0.4 : 0.2, 0);
            }
            for (int i = 0; i < 6; i++) {
                level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY() + random.nextDouble() * 2, getZ(),
                        (random.nextDouble() - 0.5) * 0.3, 0.2, (random.nextDouble() - 0.5) * 0.3);
            }
            if (mode() == SPIKE) {
                for (int i = 0; i < 8; i++) {
                    level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 0.6,
                            getY() + i * 0.35, getZ() + (random.nextDouble() - 0.5) * 0.6, 0, 0.05, 0);
                }
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
