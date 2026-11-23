package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * ユグドラシルの黄金の実。木の周りをゆっくり回りながら浮かび、毎秒ユグドラシルの HP を回復する。
 * HP 30。叩けば壊せる。描画は GoldenFruitRenderer（光る金のリンゴ）。
 */
public class GoldenFruitEntity extends Mob {
    public static final float HEAL_PER_SECOND = 4.0F;
    private YggdrasilEntity owner;
    private double angle;

    public GoldenFruitEntity(EntityType<? extends GoldenFruitEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public static GoldenFruitEntity spawn(YggdrasilEntity owner, double angle) {
        GoldenFruitEntity e = new GoldenFruitEntity(ModEntities.GOLDEN_FRUIT.get(), owner.level());
        e.owner = owner;
        e.angle = angle;
        e.place();
        owner.level().addFreshEntity(e);
        return e;
    }

    private void place() {
        if (owner == null) return;
        this.setPos(owner.getX() + Math.cos(angle) * 7.0, owner.getY() + 6.0 + Math.sin(angle * 3 + tickCount * 0.05) * 0.4,
                owner.getZ() + Math.sin(angle) * 7.0);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.WAX_ON, getX() + (random.nextDouble() - 0.5) * 0.6, getY() + 0.4,
                        getZ() + (random.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
            }
            return;
        }
        if (owner == null || !owner.isAlive()) { this.discard(); return; }
        angle += 0.008;
        place();
        if (this.tickCount % 20 == 0) {
            owner.heal(HEAL_PER_SECOND);
            if (level() instanceof ServerLevel sl) {                // 実から木へ流れる光
                Vec3 a = this.position().add(0, 0.4, 0), b = owner.position().add(0, 3, 0);
                for (int i = 0; i <= 8; i++) {
                    Vec3 p = a.lerp(b, i / 8.0);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.WAX_OFF, getX(), getY() + 0.4, getZ(), 20, 0.3, 0.3, 0.3, 0.2);
        }
        playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5F, 1.2F);
    }

    @Override
    protected void tickDeath() {
        this.remove(RemovalReason.KILLED);
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {}

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
}
