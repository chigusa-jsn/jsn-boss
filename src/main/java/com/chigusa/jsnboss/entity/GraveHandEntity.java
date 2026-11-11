package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 死者の手。足元の地面が割れて（予告）、そのあと地面から手が出て足をつかむ。
 * つかまれるとダメージ＋1.5秒ほとんど動けない。
 */
public class GraveHandEntity extends Entity {
    private static final int ACTIVE = 10;
    private static final double RADIUS = 1.3;

    private int warmup = 18;
    private AbstractBoss owner;
    private float damage = 10.0F;
    private final Set<UUID> hits = new HashSet<>();

    public GraveHandEntity(EntityType<? extends GraveHandEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, double x, double y, double z, int warmup, float damage) {
        GraveHandEntity e = new GraveHandEntity(ModEntities.GRAVE_HAND.get(), owner.level());
        e.owner = owner;
        e.warmup = warmup;
        e.damage = damage;
        e.setPos(x, y, z);
        owner.level().addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount;
        if (level().isClientSide) {
            particles(t);
            return;
        }
        if (t == warmup) this.playSound(SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.6F);
        if (t >= warmup && t < warmup + ACTIVE) grab();
        if (t >= warmup + ACTIVE) this.discard();
    }

    private void grab() {
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(RADIUS + 1, 2, RADIUS + 1),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !ZombieKingEntity.isMinion(e) && !e.isSpectator())) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            if (Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() / 2.0 > RADIUS) continue;
            if (e.getY() - getY() > 1.2 || e.getY() < getY() - 1.5) continue;
            if (!hits.add(e.getUUID())) continue;
            boolean ok = owner != null ? e.hurt(this.damageSources().mobAttack(owner), damage)
                    : e.hurt(this.damageSources().generic(), damage);
            if (ok) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
                e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
                e.hurtMarked = true;
            }
        }
    }

    private void particles(int t) {
        BlockState ground = level().getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
        if (ground.isAir()) ground = Blocks.DIRT.defaultBlockState();
        BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, ground);
        if (t < warmup) {
            // 予告：地面が割れて土ぼこりと魂が漏れる
            int n = 1 + t * 4 / Math.max(1, warmup);
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * RADIUS;
                level().addParticle(dirt, getX() + Math.cos(a) * r, getY() + 0.1, getZ() + Math.sin(a) * r, 0, 0.1, 0);
            }
            if (t % 4 == 0) level().addParticle(ParticleTypes.SOUL, getX(), getY() + 0.2, getZ(), 0, 0.03, 0);
        } else if (t < warmup + 4) {
            // 手が突き出す：骨と土が噴き上がる
            BlockParticleOption bone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState());
            for (int i = 0; i < 12; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * RADIUS * 0.7;
                level().addParticle(i % 2 == 0 ? bone : dirt, getX() + Math.cos(a) * r, getY() + random.nextDouble() * 1.4,
                        getZ() + Math.sin(a) * r, 0, 0.25, 0);
            }
            level().addParticle(ParticleTypes.SOUL, getX(), getY() + 0.8, getZ(), 0, 0.08, 0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }
}
