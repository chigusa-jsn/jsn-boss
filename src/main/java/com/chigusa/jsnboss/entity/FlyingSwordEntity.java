package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEffects;
import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** 影刃竜が飛ばす剣。本物の剣（鉄・金・ダイヤ・ネザライトなど）の見た目で、まっすぐ飛ぶ。 */
public class FlyingSwordEntity extends Projectile {
    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RED =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.BOOLEAN);

    private static final ItemStack[] SWORDS = {
            new ItemStack(Items.IRON_SWORD), new ItemStack(Items.GOLDEN_SWORD), new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Items.STONE_SWORD)};
    private static final Vector3f RED_DUST = new Vector3f(1.0F, 0.15F, 0.22F);

    private float damage = 8.0F;

    public FlyingSwordEntity(EntityType<? extends FlyingSwordEntity> type, Level level) {
        super(type, level);
    }

    public static void shoot(BladeDragonEntity owner, Vec3 pos, Vec3 velocity, float damage, boolean red) {
        FlyingSwordEntity sword = new FlyingSwordEntity(ModEntities.FLYING_SWORD.get(), owner.level());
        sword.setOwner(owner);
        sword.setPos(pos);
        sword.setDeltaMovement(velocity);
        sword.damage = damage;
        sword.entityData.set(KIND, owner.getRandom().nextInt(SWORDS.length));
        sword.entityData.set(RED, red);
        ProjectileUtil.rotateTowardsMovement(sword, 1.0F);
        owner.level().addFreshEntity(sword);
    }

    /** 見た目に使う剣のアイテム */
    public ItemStack getSwordStack() {
        return SWORDS[Math.floorMod(this.entityData.get(KIND), SWORDS.length)];
    }

    public boolean isRed() { return this.entityData.get(RED); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0);
        builder.define(RED, false);
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
            if (this.tickCount > 60) this.discard();
        } else if (isRed()) {
            level().addParticle(new DustParticleOptions(RED_DUST, 1.0F), getX(), getY() + 0.25, getZ(), 0, 0, 0);
        } else if (this.tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.CRIT, getX(), getY() + 0.25, getZ(), 0, 0, 0);
        }
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        ProjectileUtil.rotateTowardsMovement(this, 0.5F);
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
        if (target.hurt(this.damageSources().mobProjectile(this, owner), damage) && isRed()) {
            target.addEffect(new MobEffectInstance(ModEffects.LACERATION, 60, 0));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.25, getZ(), 10, 0.2, 0.2, 0.2, 0.2);
        }
        this.playSound(SoundEvents.ANVIL_LAND, 0.4F, 1.8F);
        this.discard();
    }
}
