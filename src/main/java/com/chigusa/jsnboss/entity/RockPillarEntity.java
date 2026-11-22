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
 * 岩の柱：予告（地面のひびと砂ぼこり）のあと地面から突き出し、しばらく立ってから沈む。
 * 突き出す瞬間に上にいる相手を打ち上げる。見た目は RockPillarRenderer（石ブロックを積む）。
 */
public class RockPillarEntity extends Entity {
    public static final int RISE = 4, HOLD = 30, SINK = 8;
    public static final float HEIGHT = 3.0F;
    private static final EntityDataAccessor<Integer> WARMUP =
            SynchedEntityData.defineId(RockPillarEntity.class, EntityDataSerializers.INT);

    private AbstractBoss owner;
    private float damage = 14.0F;
    private final Set<UUID> hits = new HashSet<>();

    public RockPillarEntity(EntityType<? extends RockPillarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, double x, double z, int warmup, float damage) {
        RockPillarEntity e = new RockPillarEntity(ModEntities.ROCK_PILLAR.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.setPos(x, owner.groundY(x, owner.getY() + 3, z), z);
        e.setYRot(owner.getRandom().nextFloat() * 360F);
        e.entityData.set(WARMUP, warmup);
        owner.level().addFreshEntity(e);
    }

    public int warmup() { return this.entityData.get(WARMUP); }

    /** 地面からどれだけ出ているか（0〜1）。描画用 */
    public float progress(float partial) {
        float t = this.tickCount + partial - warmup();
        if (t <= 0) return 0;
        if (t < RISE) return t / RISE;
        if (t < RISE + HOLD) return 1;
        return Math.max(0, 1 - (t - RISE - HOLD) / SINK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WARMUP, 16);
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount, w = warmup();
        if (level().isClientSide) {
            BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
            if (t < w) {
                for (int i = 0; i < 1 + t * 4 / Math.max(1, w); i++) {
                    level().addParticle(dirt, getX() + (random.nextDouble() - 0.5) * 1.6, getY() + 0.1,
                            getZ() + (random.nextDouble() - 0.5) * 1.6, 0, 0.1, 0);
                }
            } else if (t < w + 3) {
                for (int i = 0; i < 16; i++) {
                    level().addParticle(dirt, getX() + (random.nextDouble() - 0.5) * 1.4, getY() + random.nextDouble() * HEIGHT,
                            getZ() + (random.nextDouble() - 0.5) * 1.4, (random.nextDouble() - 0.5) * 0.3, 0.3, (random.nextDouble() - 0.5) * 0.3);
                }
            }
            return;
        }
        if (t == w) this.playSound(SoundEvents.STONE_BREAK, 2.0F, 0.6F);
        if (t >= w && t < w + RISE + 1) strike();
        if (t > w + RISE + HOLD + SINK) this.discard();
    }

    private void strike() {
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.5, HEIGHT, 1.5),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            if (Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() / 2.0 > 0.9) continue;
            if (e.getY() > getY() + HEIGHT + 0.5 || e.getY() + e.getBbHeight() < getY()) continue;
            if (!hits.add(e.getUUID())) continue;
            boolean ok = owner != null ? e.hurt(this.damageSources().mobAttack(owner), damage)
                    : e.hurt(this.damageSources().generic(), damage);
            if (ok) {
                e.setDeltaMovement(e.getDeltaMovement().multiply(0.3, 0, 0.3).add(0, 0.9, 0));
                e.hurtMarked = true;
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
