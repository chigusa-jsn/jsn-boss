package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 地を走る雷：地面をはうようにまっすぐ走る電撃。当たると少しのあいだ鈍くなる。 */
public class GroundBoltEntity extends Entity {
    private static final Vector3f BLUE = new Vector3f(0.5F, 0.9F, 1.0F);
    private static final double SPEED = 0.75;

    private AbstractBoss owner;
    private Vec3 dir = Vec3.ZERO;
    private float damage = 11.0F;
    private final Set<UUID> hit = new HashSet<>();

    public GroundBoltEntity(EntityType<? extends GroundBoltEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(AbstractBoss owner, Vec3 from, Vec3 dir, float damage) {
        GroundBoltEntity e = new GroundBoltEntity(ModEntities.GROUND_BOLT.get(), owner.level());
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
        if (level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5) * 0.8,
                        getY() + 0.1 + random.nextDouble() * 0.8, getZ() + (random.nextDouble() - 0.5) * 0.8,
                        (random.nextDouble() - 0.5) * 0.3, 0.15, (random.nextDouble() - 0.5) * 0.3);
                level().addParticle(new DustParticleOptions(BLUE, 1.5F), getX() + (random.nextDouble() - 0.5) * 0.6,
                        getY() + 0.1 + random.nextDouble() * 0.4, getZ() + (random.nextDouble() - 0.5) * 0.6, 0, 0, 0);
            }
            return;
        }
        if (this.tickCount > 24 || owner == null) { this.discard(); return; }
        double x = getX() + dir.x * SPEED, z = getZ() + dir.z * SPEED;
        double y = owner.groundY(x, getY() + 1.5, z);
        if (y < getY() - 3) { this.discard(); return; }               // 崖で消える
        this.setPos(x, y, z);
        if (this.tickCount % 4 == 0) playSound(net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.6F, 1.6F);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.2, 1.5, 1.2),
                e -> e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator())) {
            if (e.getY() > getY() + 0.8 || !hit.add(e.getUUID())) continue;   // 高く跳んでいれば当たらない
            if (e.hurt(this.damageSources().mobAttack(owner), damage)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.35, 0));
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
