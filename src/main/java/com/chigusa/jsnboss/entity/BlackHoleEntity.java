package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 虚空の崩壊：3秒間まわり（半径14）を吸い寄せ、最後に半径6で大爆発する黒い穴。
 * 吸い寄せる力は走れば振り切れる強さ。
 */
public class BlackHoleEntity extends Entity {
    public static final int LIFE = 60;
    private static final double PULL_RADIUS = 14.0, BLAST_RADIUS = 6.0;
    private static final Vector3f BLACK = new Vector3f(0.05F, 0.02F, 0.1F);
    private static final Vector3f CYAN = new Vector3f(0.31F, 0.9F, 1.0F);

    private AbstractBoss owner;
    private float damage = 22.0F;

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, Vec3 pos, float damage) {
        BlackHoleEntity e = new BlackHoleEntity(ModEntities.BLACK_HOLE.get(), owner.level());
        e.owner = owner;
        e.damage = damage;
        e.setPos(pos);
        owner.level().addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            double grow = Math.min(1.0, this.tickCount / 20.0);
            for (int i = 0; i < 4; i++) {                               // 黒い核
                level().addParticle(new DustParticleOptions(BLACK, 3.0F), getX() + (random.nextDouble() - 0.5) * 1.5 * grow,
                        getY() + 1.5 + (random.nextDouble() - 0.5) * 1.5 * grow, getZ() + (random.nextDouble() - 0.5) * 1.5 * grow, 0, 0, 0);
            }
            for (int i = 0; i < 8; i++) {                               // 吸い込まれていく光
                double a = random.nextDouble() * Math.PI * 2, r = 2 + random.nextDouble() * 8;
                double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r, y = getY() + 0.3 + random.nextDouble() * 3;
                level().addParticle(ParticleTypes.PORTAL, x, y, z, (getX() - x) * 0.25, (getY() + 1.5 - y) * 0.25, (getZ() - z) * 0.25);
            }
            double a = this.tickCount * 0.3;
            level().addParticle(new DustParticleOptions(CYAN, 1.5F), getX() + Math.cos(a) * 2.2, getY() + 1.5, getZ() + Math.sin(a) * 2.2, 0, 0, 0);
            level().addParticle(new DustParticleOptions(CYAN, 1.5F), getX() - Math.cos(a) * 2.2, getY() + 1.5, getZ() - Math.sin(a) * 2.2, 0, 0, 0);
            return;
        }
        if (owner == null) { this.discard(); return; }
        if (this.tickCount % 20 == 0) playSound(SoundEvents.PORTAL_AMBIENT, 2.0F, 0.5F + this.tickCount / 60.0F);
        if (this.tickCount < LIFE) {
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(PULL_RADIUS),
                    e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator()
                            && !(e instanceof Player p && p.isCreative()))) {
                Vec3 d = this.position().add(0, 1, 0).subtract(e.position());
                double dist = d.length();
                if (dist > PULL_RADIUS || dist < 0.5) continue;
                e.setDeltaMovement(e.getDeltaMovement().add(d.normalize().scale(0.065)));
                e.hurtMarked = true;
            }
            return;
        }
        // 大爆発
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(BLAST_RADIUS + 1),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            Vec3 d = e.position().subtract(this.position());
            if (d.length() > BLAST_RADIUS) continue;
            if (e.hurt(this.damageSources().mobAttack(owner), damage)) {
                Vec3 push = d.lengthSqr() > 1e-4 ? d.normalize().scale(1.4) : Vec3.ZERO;
                e.setDeltaMovement(push.x, 0.8, push.z);
                e.hurtMarked = true;
            }
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 200, 2, 2, 2, 0.6);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 80, 1, 1, 1, 0.5);
        }
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 0.6F);
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }
}
