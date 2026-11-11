package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** 氷の槍。まっすぐ飛んで、当たると凍える＋鈍化。 */
public class IceSpearEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> AURORA =
            SynchedEntityData.defineId(IceSpearEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Vector3f PURPLE = new Vector3f(0.75F, 0.5F, 1.0F);

    private float damage = 9.0F;
    private int freezeTicks = 70;

    public IceSpearEntity(EntityType<? extends IceSpearEntity> type, Level level) {
        super(type, level);
    }

    public static void shoot(LivingEntity owner, Vec3 pos, Vec3 velocity, float damage, int freezeTicks, boolean aurora) {
        IceSpearEntity spear = new IceSpearEntity(ModEntities.ICE_SPEAR.get(), owner.level());
        spear.setOwner(owner);
        spear.setPos(pos);
        spear.setDeltaMovement(velocity);
        spear.damage = damage;
        spear.freezeTicks = freezeTicks;
        spear.entityData.set(AURORA, aurora);
        owner.level().addFreshEntity(spear);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(AURORA, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        if (!level().isClientSide) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                this.onHit(hit);
                return;
            }
            if (this.tickCount > 80) this.discard();
        } else {
            trail(v);
        }
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        ProjectileUtil.rotateTowardsMovement(this, 0.2F);
    }

    /** 槍の形：進む向きに白い光の線、まわりに雪 */
    private void trail(Vec3 v) {
        Vec3 dir = v.normalize();
        for (int i = 0; i < 4; i++) {
            Vec3 p = this.position().subtract(dir.scale(i * 0.35)).add(0, 0.25, 0);
            level().addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0, 0);
        }
        level().addParticle(this.entityData.get(AURORA) ? new DustParticleOptions(PURPLE, 1.2F) : ParticleTypes.SNOWFLAKE,
                getX() + (random.nextDouble() - 0.5) * 0.4, getY() + 0.25, getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0, 0);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof AbstractBoss);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity target)) return;
        LivingEntity owner = this.getOwner() instanceof LivingEntity le ? le : null;
        if (target.hurt(this.damageSources().mobProjectile(this, owner), damage)) {
            IceWitchEntity.freeze(target, freezeTicks);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                    getX(), getY() + 0.25, getZ(), 20, 0.2, 0.2, 0.2, 0.15);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.25, getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        }
        this.playSound(SoundEvents.GLASS_BREAK, 0.8F, 1.4F);
        this.discard();
    }
}
