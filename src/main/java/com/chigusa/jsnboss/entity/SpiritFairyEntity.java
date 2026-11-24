package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.mob.MobBoltEntity;
import com.chigusa.jsnboss.registry.ModEntities;
import com.chigusa.jsnboss.vfx.Element;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * 大詠唱で呼ばれる妖精。精霊のまわりを飛び回り、ときどき小さな魔法を撃つ。
 * 4体とも倒すと精霊の結界が割れる。属性ごとに色が違う。
 */
public class SpiritFairyEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> ELEMENT =
            SynchedEntityData.defineId(SpiritFairyEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.spirit_fairy.idle");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("animation.spirit_fairy.cast");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerId;
    private double angle;
    private int shootTimer = 40;

    public SpiritFairyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.xpReward = 10;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    public static SpiritFairyEntity summon(YggdrasilSpiritEntity owner, Vec3 pos, Element el, double angle) {
        SpiritFairyEntity f = new SpiritFairyEntity(ModEntities.SPIRIT_FAIRY.get(), owner.level());
        f.moveTo(pos.x, pos.y, pos.z, 0, 0);
        f.ownerId = owner.getUUID();
        f.angle = angle;
        f.shootTimer = 30 + owner.getRandom().nextInt(40);
        f.entityData.set(ELEMENT, el.ordinal());
        owner.level().addFreshEntity(f);
        return f;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ELEMENT, 0);
    }

    public Element element() {
        return Element.values()[Mth.clamp(this.entityData.get(ELEMENT), 0, 7)];
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        this.setNoGravity(true);
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) {
                level().addParticle(new DustParticleOptions(element().color(), 0.8F), getX() + (random.nextDouble() - 0.5) * 0.8,
                        getY() + 0.4 + random.nextDouble() * 0.5, getZ() + (random.nextDouble() - 0.5) * 0.8, 0, -0.02, 0);
            }
            return;
        }
        Entity owner = ownerId == null ? null : ((ServerLevel) level()).getEntity(ownerId);
        if (!(owner instanceof YggdrasilSpiritEntity spirit) || !spirit.isAlive()) {
            if (tickCount > 20) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                discard();
            }
            return;
        }
        // 精霊のまわりを回る（少し上下に揺れる）
        angle += 0.018;
        double r = 9 + Math.sin(tickCount * 0.05 + angle) * 1.5;
        Vec3 want = new Vec3(spirit.getX() + Math.cos(angle) * r, spirit.getY() - 1.5 + Math.sin(tickCount * 0.08 + angle * 3) * 1.2,
                spirit.getZ() + Math.sin(angle) * r);
        Vec3 d = want.subtract(position());
        this.setDeltaMovement(getDeltaMovement().scale(0.7).add(d.scale(0.08)));
        // 小さな魔法を撃つ
        if (--shootTimer <= 0) {
            shootTimer = 50 + random.nextInt(30);
            Player p = level().getNearestPlayer(this, 32);
            if (p != null && !p.isCreative() && !p.isSpectator()) {
                Vec3 from = position().add(0, 0.6, 0);
                Vec3 v = p.position().add(0, 1.0, 0).subtract(from).normalize().scale(0.7);
                int kind = element() == Element.ICE || element() == Element.WIND ? 1 : 0;
                MobBoltEntity.shoot(this, from, v, kind, 5.0F);
                this.triggerAnim("main", "cast");
                playSound(SoundEvents.ALLAY_THROW, 1.2F, 1.4F);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new DustParticleOptions(element().color(), 1.6F), getX(), getY() + 0.5, getZ(), 30, 0.4, 0.4, 0.4, 0);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 15, 0.3, 0.3, 0.3, 0.15);
        }
    }

    @Override
    protected void tickDeath() {
        if (++this.deathTime >= 5 && !level().isClientSide) this.remove(RemovalReason.KILLED);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putInt("Element", this.entityData.get(ELEMENT));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        this.entityData.set(ELEMENT, tag.getInt("Element"));
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ALLAY_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ALLAY_DEATH; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<SpiritFairyEntity> main = new AnimationController<>(this, "main", 3, state -> state.setAndContinue(IDLE));
        main.triggerableAnim("cast", CAST);
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
