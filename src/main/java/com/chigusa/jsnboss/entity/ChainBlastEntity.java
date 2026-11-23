package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 連鎖爆発：地面の上を、爆発が一直線に 0.15 秒ごとに進んでくる（1つ先に煙の予告） */
public class ChainBlastEntity extends Entity {
    private static final double STEP = 1.8;
    private static final int EVERY = 3, STEPS = 11;

    private AbstractBoss owner;
    private Vec3 dir = Vec3.ZERO;
    private float damage = 16.0F;

    public ChainBlastEntity(EntityType<? extends ChainBlastEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, Vec3 from, Vec3 dir, float damage) {
        ChainBlastEntity e = new ChainBlastEntity(ModEntities.CHAIN_BLAST.get(), owner.level());
        e.owner = owner;
        e.dir = dir.multiply(1, 0, 1).normalize();
        e.damage = damage;
        e.setPos(from.x, owner.groundY(from.x, from.y + 2, from.z), from.z);
        owner.level().addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (owner == null || this.tickCount > EVERY * STEPS + 1) { this.discard(); return; }
        if (this.tickCount % EVERY != 0) return;
        double x = getX() + dir.x * STEP, z = getZ() + dir.z * STEP;
        double y = owner.groundY(x, getY() + 2, z);
        this.setPos(x, y, z);
        Blasts.blast(level(), owner, this.position(), 2.0, damage);
        if (level() instanceof ServerLevel sl) {                 // 次に爆発する場所に煙
            Vec3 n = this.position().add(dir.scale(STEP));
            sl.sendParticles(ParticleTypes.SMOKE, n.x, y + 0.2, n.z, 8, 0.4, 0.1, 0.4, 0.02);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }
}
