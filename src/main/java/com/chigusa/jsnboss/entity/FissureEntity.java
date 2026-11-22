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
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 地割れ。LINE：地面を一直線に走る割れ目（ジャンプで避けられる）。第2形態は通った後に MAGMA を残す。
 * MAGMA：3秒間残るマグマ。上に乗ると 0.5 秒ごとに 2 ダメージ＋炎上。
 */
public class FissureEntity extends Entity {
    public static final int LINE = 0, MAGMA = 1;
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(FissureEntity.class, EntityDataSerializers.INT);
    private static final double SPEED = 0.7;

    private AbstractBoss owner;
    private Vec3 dir = Vec3.ZERO;
    private float damage = 12.0F;
    private boolean magma;
    private final Set<UUID> hit = new HashSet<>();
    private final Map<UUID, Integer> lastBurn = new HashMap<>();

    public FissureEntity(EntityType<? extends FissureEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, Vec3 from, Vec3 dir, float damage, boolean magma) {
        FissureEntity e = new FissureEntity(ModEntities.FISSURE.get(), owner.level());
        e.owner = owner;
        e.dir = dir.multiply(1, 0, 1).normalize();
        e.damage = damage;
        e.magma = magma;
        e.setPos(from.x, owner.groundY(from.x, from.y + 2, from.z), from.z);
        owner.level().addFreshEntity(e);
    }

    private int mode() { return this.entityData.get(MODE); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, LINE);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (mode() == LINE) {
                BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
                for (int i = 0; i < 6; i++) {
                    level().addParticle(dirt, getX() + (random.nextDouble() - 0.5), getY() + 0.1, getZ() + (random.nextDouble() - 0.5),
                            0, 0.3 + random.nextDouble() * 0.2, 0);
                }
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            } else {
                if (random.nextInt(3) == 0) {
                    level().addParticle(ParticleTypes.LAVA, getX() + (random.nextDouble() - 0.5), getY() + 0.1,
                            getZ() + (random.nextDouble() - 0.5), 0, 0, 0);
                }
                level().addParticle(ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5) * 1.2, getY() + 0.1,
                        getZ() + (random.nextDouble() - 0.5) * 1.2, 0, 0.02, 0);
            }
            return;
        }
        if (owner == null) { this.discard(); return; }
        if (mode() == MAGMA) {
            if (this.tickCount > 60) { this.discard(); return; }
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.6, 0.5, 0.6),
                    e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator() && e.onGround())) {
                Integer last = lastBurn.get(e.getUUID());
                if (last != null && this.tickCount - last < 10) continue;
                lastBurn.put(e.getUUID(), this.tickCount);
                if (e.hurt(this.damageSources().mobAttack(owner), 2.0F)) e.igniteForSeconds(3);
            }
            return;
        }
        if (this.tickCount > 26) { this.discard(); return; }
        double x = getX() + dir.x * SPEED, z = getZ() + dir.z * SPEED;
        double y = owner.groundY(x, getY() + 1.5, z);
        if (y < getY() - 3) { this.discard(); return; }
        this.setPos(x, y, z);
        if (this.tickCount % 3 == 0) playSound(SoundEvents.STONE_BREAK, 1.0F, 0.5F);
        if (magma && this.tickCount % 2 == 0) {
            FissureEntity m = new FissureEntity(ModEntities.FISSURE.get(), level());
            m.owner = owner;
            m.entityData.set(MODE, MAGMA);
            m.setPos(getX(), getY(), getZ());
            level().addFreshEntity(m);
        }
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.2, 1.5, 1.2),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            if (e.getY() > getY() + 0.8 || !hit.add(e.getUUID())) continue;      // 跳んでいれば当たらない
            if (e.hurt(this.damageSources().mobAttack(owner), damage)) {
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
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
