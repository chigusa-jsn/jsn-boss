package com.chigusa.jsnboss.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * すべてのボスに共通する部分：フェーズ、ボスバー、画面の揺れ、当たり判定、死亡演出、セーブ。
 * 各ボスは bossAiStep() に技の処理を書く。
 */
public abstract class AbstractBoss extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(AbstractBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHAKE =
            SynchedEntityData.defineId(AbstractBoss.class, EntityDataSerializers.INT);

    /** 死亡アニメの長さ（tick）。この時間が経ってから消える */
    protected static final int DEATH_TICKS = 60;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected final ServerBossEvent bossEvent;
    private final BossEvent.BossBarColor phase1Color, phase2Color;

    protected AbstractBoss(EntityType<? extends Monster> type, Level level,
                           BossEvent.BossBarColor phase1Color, BossEvent.BossBarColor phase2Color) {
        super(type, level);
        this.phase1Color = phase1Color;
        this.phase2Color = phase2Color;
        this.bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(), phase1Color,
                BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true);
        this.xpReward = 200;
        this.setPersistenceRequired();
    }

    // ============================================================ hooks
    /** サーバー側で毎 tick 呼ばれる。技の選択・実行をここに書く */
    protected abstract void bossAiStep();

    /** クライアント側で毎 tick 呼ばれる（体から出るパーティクルなど） */
    protected void clientTick() {}

    /** 死亡アニメ中のクライアント側パーティクル */
    protected void deathParticles() {}

    /** フェーズが変わったとき・ロードされたときに呼ばれる（移動速度を上げるなど） */
    protected void onPhaseChanged(int phase) {}

    /** true の間は一切ダメージを受けない（変身中など） */
    protected boolean isInvulnerableNow() { return false; }

    /** ボスバーを出すか（分身などは false） */
    protected boolean showsBossBar() { return true; }

    // ============================================================ state
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(SHAKE, 0);
    }

    public int getPhase() { return this.entityData.get(PHASE); }
    public boolean isPhase2() { return getPhase() >= 2; }
    public int getShake() { return this.entityData.get(SHAKE); }

    protected void setPhase(int phase) {
        this.entityData.set(PHASE, phase);
        this.bossEvent.setColor(phase >= 2 ? phase2Color : phase1Color);
        onPhaseChanged(phase);
    }

    /** 第1形態 / 第2形態で値を切り替える */
    protected float dmg(float phase1, float phase2) { return isPhase2() ? phase2 : phase1; }

    /** 近くのプレイヤーの画面を ticks の間揺らす */
    protected void shake(int ticks) {
        if (ticks > getShake()) this.entityData.set(SHAKE, ticks);
    }

    // ============================================================ loop
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (isAlive()) clientTick();
        } else if (getShake() > 0) {
            this.entityData.set(SHAKE, getShake() - 1);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        bossAiStep();
    }

    // ============================================================ helpers
    protected Vec3 forward() {
        return Vec3.directionFromRotation(0, this.getYRot());
    }

    /** ターゲットの方へ体を向ける（1 tick に最大 maxStep 度） */
    protected void face(LivingEntity target, float maxStep) {
        if (target == null) return;
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.approachDegrees(this.getYRot(), want, maxStep);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    /** 攻撃が当たる相手か（自分・ほかのボス・観戦者は除く） */
    protected boolean canHit(LivingEntity e) {
        return e != this && e.isAlive() && !(e instanceof AbstractBoss) && !e.isSpectator();
    }

    /** 普通の近接ダメージ＋ノックバック。当たったら true */
    protected boolean damage(LivingEntity e, float amount, double knock) {
        if (e.hurt(this.damageSources().mobAttack(this), amount)) {
            if (knock > 0) e.knockback(knock, getX() - e.getX(), getZ() - e.getZ());
            return true;
        }
        return false;
    }

    /** 前方の扇形（range ブロック、左右 halfAngle 度）にいる相手 */
    protected java.util.List<LivingEntity> inArc(double range, double halfAngle) {
        Vec3 look = forward();
        java.util.List<LivingEntity> out = new java.util.ArrayList<>();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(range, 2.0, range), this::canHit)) {
            Vec3 to = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
            if (to.length() - e.getBbWidth() / 2.0 > range) continue;
            if (to.lengthSqr() > 1.0E-4) {
                double angle = Math.toDegrees(Math.acos(Mth.clamp(to.normalize().dot(look), -1.0, 1.0)));
                if (angle > halfAngle) continue;
            }
            if (e.getY() > getY() + this.getBbHeight() + 0.5 || e.getY() + e.getBbHeight() < getY() - 1.0) continue;
            out.add(e);
        }
        return out;
    }

    /** center を中心とした円（radius ブロック、高さ height）にいる相手 */
    protected java.util.List<LivingEntity> inRadius(Vec3 center, double radius, double height) {
        java.util.List<LivingEntity> out = new java.util.ArrayList<>();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(radius + 1, height + 1, radius + 1), this::canHit)) {
            double dx = e.getX() - center.x, dz = e.getZ() - center.z;
            if (Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() / 2.0 > radius) continue;
            if (e.getY() > center.y + height || e.getY() + e.getBbHeight() < center.y - 1.0) continue;
            out.add(e);
        }
        return out;
    }

    protected void hitArc(double range, double halfAngle, float amount, double knock) {
        for (LivingEntity e : inArc(range, halfAngle)) damage(e, amount, knock);
    }

    protected void hitRadius(Vec3 center, double radius, float amount, double knock) {
        for (LivingEntity e : inRadius(center, radius, 3.0)) {
            if (damage(e, amount, knock)) launch(e, 0.4);
        }
    }

    /** 上に打ち上げる */
    protected static void launch(LivingEntity e, double up) {
        e.setDeltaMovement(e.getDeltaMovement().add(0, up, 0));
        e.hurtMarked = true;
    }

    /** 真下の地面の高さを探す（空中を狙っても地面から出るように） */
    public double groundY(double x, double y, double z) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(x, y, z).mutable();
        for (int i = 0; i < 8 && level().getBlockState(pos.below()).isAir(); i++) pos.move(0, -1, 0);
        for (int i = 0; i < 4 && !level().getBlockState(pos).isAir(); i++) pos.move(0, 1, 0);
        return pos.getY();
    }

    // ============================================================ damage / death
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableNow()) return false;
        return super.hurt(source, amount);
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
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) this.triggerAnim("main", "death");
    }

    /** 死亡アニメを最後まで見せてから消える */
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (level().isClientSide) deathParticles();
        if (this.deathTime >= DEATH_TICKS && !level().isClientSide() && !this.isRemoved()) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.5, getZ(), 1, 0, 0, 0, 0);
            }
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.8F);
            level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    // ============================================================ boss bar / save
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (showsBossBar()) this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPhase(Math.max(1, tag.getInt("Phase")));
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
