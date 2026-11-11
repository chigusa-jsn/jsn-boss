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
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * 落雷の予告。地面に青い輪が出て、DELAY tick 後に本物の雷（見た目だけ）が落ちる。
 * ダメージはこのエンティティが与える（火事にならない）。
 */
public class LightningMarkEntity extends Entity {
    private static final EntityDataAccessor<Integer> DELAY =
            SynchedEntityData.defineId(LightningMarkEntity.class, EntityDataSerializers.INT);
    private static final Vector3f BLUE = new Vector3f(0.5F, 0.9F, 1.0F);
    private static final double RADIUS = 1.8;

    private AbstractBoss owner;
    private float damage = 12.0F;

    public LightningMarkEntity(EntityType<? extends LightningMarkEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, double x, double z, int delay, float damage) {
        LightningMarkEntity e = new LightningMarkEntity(ModEntities.LIGHTNING_MARK.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.setPos(x, owner.groundY(x, owner.getY() + 3, z), z);
        e.entityData.set(DELAY, delay);
        owner.level().addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DELAY, 20);
    }

    @Override
    public void tick() {
        super.tick();
        int delay = this.entityData.get(DELAY);
        if (level().isClientSide) {
            if (this.tickCount <= delay) {
                // 予告の輪：時間がたつほど濃くなる
                int n = 10 + this.tickCount;
                for (int i = 0; i < 6; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    level().addParticle(new DustParticleOptions(BLUE, 1.2F),
                            getX() + Math.cos(a) * RADIUS, getY() + 0.1, getZ() + Math.sin(a) * RADIUS, 0, 0, 0);
                }
                if (random.nextInt(Math.max(1, 30 - n)) == 0) {
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5) * 2,
                            getY() + 0.2, getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.2, 0);
                }
            }
            return;
        }
        if (this.tickCount == delay) strike();
        if (this.tickCount > delay + 2) this.discard();
    }

    private void strike() {
        if (!(level() instanceof ServerLevel sl)) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(getX(), getY(), getZ());
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.5, getZ(), 40, 1.0, 0.6, 1.0, 0.4);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(RADIUS + 1, 3, RADIUS + 1),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            if (Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() / 2.0 > RADIUS || e.getY() > getY() + 4) continue;
            boolean ok = owner != null ? e.hurt(this.damageSources().mobAttack(owner), damage)
                    : e.hurt(this.damageSources().lightningBolt(), damage);
            if (ok) {
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.3, 0));
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
