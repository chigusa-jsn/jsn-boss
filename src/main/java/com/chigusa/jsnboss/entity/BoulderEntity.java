package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 岩のかたまり。3つの使い方がある：
 *  THROWN：大岩投げ。放物線を描いて飛び、当たった所で砕ける（第2形態は破片が飛び散る）
 *  FALL  ：落石。真上から落ちてくる。落ちる場所の地面に影（予告）が出る
 *  SHARD ：砕けた大岩の破片（小さい）
 * 見た目は VARIANT で変わる（ROCK：苔の丸石、END：エンドストーン＝虚空の監視者の空間の裂け目）。
 * HOVER の間は空中で止まっている（裂け目が開く予告）。
 */
public class BoulderEntity extends Entity {
    public static final int THROWN = 0, FALL = 1, SHARD = 2;
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(BoulderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(BoulderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GROUND =
            SynchedEntityData.defineId(BoulderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(BoulderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HOVER =
            SynchedEntityData.defineId(BoulderEntity.class, EntityDataSerializers.INT);
    public static final int ROCK = 0, END = 1, TNT = 2;

    private AbstractBoss owner;
    private float damage = 16.0F;
    private boolean shatter;

    public BoulderEntity(EntityType<? extends BoulderEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    private static BoulderEntity make(AbstractBoss owner, Vec3 pos, Vec3 vel, int mode, float size, float damage) {
        BoulderEntity e = new BoulderEntity(ModEntities.BOULDER.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.setPos(pos);
        e.setDeltaMovement(vel);
        e.entityData.set(MODE, mode);
        e.entityData.set(SIZE, size);
        return e;
    }

    /** target に向けて放物線で投げる（重力 0.05、水平方向の速さ speed） */
    public static void throwAt(AbstractBoss owner, Vec3 from, Vec3 target, float damage, boolean shatter) {
        Vec3 d = target.subtract(from);
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        double speed = 1.0, time = Math.max(8, horiz / speed);
        Vec3 vel = new Vec3(d.x / time, (d.y + 0.5 * 0.05 * time * time) / time, d.z / time);
        BoulderEntity e = make(owner, from, vel, THROWN, 1.6F, damage);
        e.shatter = shatter;
        owner.level().addFreshEntity(e);
    }

    /** 火のついた TNT を target に向けて放物線で投げる（着地でブロックを壊さない爆発） */
    public static void throwTnt(AbstractBoss owner, Vec3 from, Vec3 target, float damage) {
        Vec3 d = target.subtract(from);
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        double time = Math.max(10, horiz / 0.7);
        Vec3 vel = new Vec3(d.x / time, (d.y + 0.5 * 0.05 * time * time) / time, d.z / time);
        BoulderEntity e = make(owner, from, vel, THROWN, 1.0F, damage);
        e.entityData.set(VARIANT, TNT);
        owner.level().addFreshEntity(e);
    }

    /** (x, z) の真上 height ブロックから落とす */
    public static void drop(AbstractBoss owner, double x, double z, double height, float damage) {
        drop(owner, x, z, height, damage, ROCK, 0);
    }

    /** variant の見た目で、hover tick 空中に止まってから落とす */
    public static void drop(AbstractBoss owner, double x, double z, double height, float damage, int variant, int hover) {
        double ground = owner.groundY(x, owner.getY() + 4, z);
        BoulderEntity e = make(owner, new Vec3(x, ground + height, z), Vec3.ZERO, FALL, 1.3F, damage);
        e.entityData.set(GROUND, (float) ground);
        e.entityData.set(VARIANT, variant);
        e.entityData.set(HOVER, hover);
        owner.level().addFreshEntity(e);
    }

    public int variant() { return this.entityData.get(VARIANT); }

    public int mode() { return this.entityData.get(MODE); }
    public float size() { return this.entityData.get(SIZE); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, THROWN);
        builder.define(SIZE, 1.6F);
        builder.define(GROUND, 0F);
        builder.define(VARIANT, ROCK);
        builder.define(HOVER, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount < this.entityData.get(HOVER)) {
            if (level().isClientSide) {
                // 空間の裂け目：紫の渦
                for (int i = 0; i < 6; i++) {
                    double a = random.nextDouble() * Math.PI * 2, r = 0.8 + random.nextDouble() * 0.8;
                    level().addParticle(ParticleTypes.PORTAL, getX() + Math.cos(a) * r, getY() + 0.6, getZ() + Math.sin(a) * r,
                            -Math.cos(a) * 0.5, 0, -Math.sin(a) * 0.5);
                }
                float g = this.entityData.get(GROUND);
                double a = random.nextDouble() * Math.PI * 2;
                level().addParticle(ParticleTypes.REVERSE_PORTAL, getX() + Math.cos(a) * 1.4, g + 0.1, getZ() + Math.sin(a) * 1.4, 0, 0.02, 0);
            }
            return;
        }
        Vec3 v = this.getDeltaMovement().add(0, mode() == FALL ? -0.08 : -0.05, 0);
        this.setDeltaMovement(v);
        if (level().isClientSide) {
            this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
            if (mode() == FALL) {
                // 落ちる場所に影の輪（落ちてくるほど小さく濃く）
                float g = this.entityData.get(GROUND);
                double r = 1.3 + Math.max(0, getY() - g) * 0.08;
                for (int i = 0; i < 5; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    level().addParticle(ParticleTypes.SMOKE, getX() + Math.cos(a) * r, g + 0.1, getZ() + Math.sin(a) * r, 0, 0, 0);
                }
            }
            if (variant() == TNT) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + size() + 0.2, getZ(), 0, 0.05, 0);
                if (this.tickCount % 2 == 0) level().addParticle(ParticleTypes.FLAME, getX(), getY() + size() + 0.2, getZ(), 0, 0.02, 0);
            } else if (this.tickCount % 2 == 0) {
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                        getX(), getY() + size() / 2, getZ(), 0, 0, 0);
            }
            return;
        }
        if (this.tickCount > 120 + this.entityData.get(HOVER)) { this.discard(); return; }
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        boolean hitBlock = !level().noCollision(this, this.getBoundingBox().deflate(0.1));
        boolean hitMob = !level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator()).isEmpty();
        if (hitBlock || hitMob) impact();
    }

    private void impact() {
        if (variant() == TNT) {
            Blasts.blast(level(), owner, this.position(), 3.0, damage);
            this.discard();
            return;
        }
        double radius = mode() == SHARD ? 1.2 : 2.5;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius + 1),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            if (e.distanceToSqr(this.position()) > (radius + 1) * (radius + 1)) continue;
            boolean ok = owner != null ? e.hurt(this.damageSources().mobAttack(owner), damage)
                    : e.hurt(this.damageSources().generic(), damage);
            if (ok) {
                Vec3 push = e.position().subtract(this.position()).multiply(1, 0, 1);
                if (push.lengthSqr() > 1e-4) push = push.normalize().scale(0.6);
                e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.4, push.z));
                e.hurtMarked = true;
            }
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                            (variant() == END ? Blocks.END_STONE : Blocks.COBBLESTONE).defaultBlockState()),
                    getX(), getY() + 0.5, getZ(), mode() == SHARD ? 10 : 50, radius * 0.4, 0.5, radius * 0.4, 0.2);
            if (mode() != SHARD) sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
        }
        playSound(mode() == SHARD ? SoundEvents.STONE_BREAK : SoundEvents.GENERIC_EXPLODE.value(), mode() == SHARD ? 1.0F : 1.5F, 0.7F);
        if (shatter && owner != null) {
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + random.nextDouble() * 0.5;
                Vec3 vel = new Vec3(Math.cos(a) * 0.35, 0.45, Math.sin(a) * 0.35);
                owner.level().addFreshEntity(make(owner, this.position().add(0, 1.0, 0), vel, SHARD, 0.6F, 6.0F));
            }
        }
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }
}
