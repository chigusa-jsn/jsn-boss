package com.chigusa.jsnboss.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 炎の騎士。
 * 技はすべて「状態機械」で動く：startAttack() で技を始め、tickAttack() で
 * アニメーションの決まったタイミング（tick）に当たり判定や弾の発射を行う。
 * 1 tick = 0.05 秒。アニメーションの秒数 × 20 = tick。
 */
public class FireKnightEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(FireKnightEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHAKE =
            SynchedEntityData.defineId(FireKnightEntity.class, EntityDataSerializers.INT);

    // ------------------------------------------------------------ animations
    private static final String A = "animation.fire_knight.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");

    /** 技の一覧。length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまでの tick */
    public enum Attack {
        NONE(0, 0, null),
        SWEEP(30, 40, "attack_sweep"),
        COMBO(48, 80, "attack_combo"),
        DASH(32, 70, "attack_dash"),
        LEAP(44, 120, "attack_leap"),
        PILLAR(36, 100, "attack_pillar"),
        FIREBALL(28, 60, "attack_fireball"),
        WAVE_V(26, 80, "attack_wave_v"),
        WAVE_H(24, 60, "attack_wave_h"),
        PHASE_CHANGE(50, 0, "phase_change");

        public final int length, cooldown;
        public final String anim;

        Attack(int length, int cooldown, String anim) {
            this.length = length;
            this.cooldown = cooldown;
            this.anim = anim;
        }
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10)
            .setDarkenScreen(true);

    private Attack attack = Attack.NONE;
    private Attack lastAttack = Attack.NONE;
    private int attackTick;
    private int globalCooldown = 40;
    private final Map<Attack, Integer> cooldowns = new EnumMap<>(Attack.class);

    private Vec3 dashDir = Vec3.ZERO;
    private boolean leapLanded;
    private final Set<UUID> hitThisAttack = new HashSet<>();
    /** 1回の火柱の技で、同じ相手には1本しか当たらないようにする */
    public final Set<UUID> pillarHits = new HashSet<>();

    public FireKnightEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 200;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1500.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(SHAKE, 0);
    }

    public int getPhase() { return this.entityData.get(PHASE); }
    public int getShake() { return this.entityData.get(SHAKE); }
    private boolean blue() { return getPhase() >= 2; }

    /** 第1形態 / 第2形態のダメージを切り替える */
    private float dmg(float phase1, float phase2) { return blue() ? phase2 : phase1; }

    private void shake(int ticks) {
        if (ticks > getShake()) this.entityData.set(SHAKE, ticks);
    }

    // ============================================================ main loop
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (isAlive()) ambientParticles();
        } else if (getShake() > 0) {
            this.entityData.set(SHAKE, getShake() - 1);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        LivingEntity target = this.getTarget();
        if (attack != Attack.NONE) {
            tickAttack(target);
            return;
        }
        if (getPhase() == 1 && getHealth() <= getMaxHealth() * 0.5F) {
            startAttack(Attack.PHASE_CHANGE);
            return;
        }

        if (globalCooldown > 0) globalCooldown--;
        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
        if (target == null || !target.isAlive()) return;

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
            this.getNavigation().moveTo(target, 1.0);
        }
        if (globalCooldown <= 0) {
            Attack next = chooseAttack(target);
            if (next != null) startAttack(next);
        }
    }

    /** 距離で技を選ぶ。近いと剣、離れると火の玉や斬撃。 */
    private Attack chooseAttack(LivingEntity target) {
        double dist = Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ()))
                - (this.getBbWidth() + target.getBbWidth()) / 2.0;
        boolean sight = this.hasLineOfSight(target);
        List<Attack> pool = new ArrayList<>();
        if (!blue()) {
            if (dist < 3.5) { add(pool, Attack.SWEEP, 5); add(pool, Attack.PILLAR, 1); }
            else if (dist < 11) { add(pool, Attack.DASH, 4); add(pool, Attack.PILLAR, 3); if (sight) add(pool, Attack.WAVE_H, 3); }
            else if (sight) { add(pool, Attack.FIREBALL, 4); add(pool, Attack.WAVE_H, 2); add(pool, Attack.PILLAR, 2); }
        } else {
            if (dist < 3.5) { add(pool, Attack.COMBO, 5); add(pool, Attack.SWEEP, 2); add(pool, Attack.LEAP, 1); }
            else if (dist < 13) { add(pool, Attack.DASH, 3); add(pool, Attack.LEAP, 3); add(pool, Attack.PILLAR, 3); if (sight) add(pool, Attack.WAVE_V, 3); }
            else if (sight) { add(pool, Attack.FIREBALL, 4); add(pool, Attack.LEAP, 2); add(pool, Attack.WAVE_V, 2); add(pool, Attack.PILLAR, 2); }
        }
        if (pool.isEmpty()) return null;
        return pool.get(this.random.nextInt(pool.size()));
    }

    private void add(List<Attack> pool, Attack a, int weight) {
        if (cooldowns.getOrDefault(a, 0) > 0 || a == lastAttack) return;
        for (int i = 0; i < weight; i++) pool.add(a);
    }

    private void startAttack(Attack a) {
        this.attack = a;
        this.attackTick = 0;
        this.leapLanded = false;
        this.hitThisAttack.clear();
        if (a == Attack.PILLAR) this.pillarHits.clear();
        this.getNavigation().stop();
        this.triggerAnim("main", a.name());
    }

    private void endAttack() {
        Attack done = this.attack;
        this.attack = Attack.NONE;
        this.lastAttack = done;
        if (done == Attack.PHASE_CHANGE) {
            this.globalCooldown = 10;
            return;
        }
        this.cooldowns.put(done, done.cooldown);
        this.globalCooldown = blue() ? 14 : 24;
        // 第2形態の縦斬撃は、そのまま横斬撃につなげて2連発にする
        if (done == Attack.WAVE_V && blue()) startAttack(Attack.WAVE_H);
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case SWEEP -> {                         // 横なぎ  当たり 0.75秒
                if (t < 13) face(target, 25);
                if (t == 15) { swingSound(); hitArc(5.0, 75, dmg(12, 16), 1.0); }
            }
            case COMBO -> {                         // 3連撃  0.5 / 1.0 / 1.7秒
                if (t < 9 || (t > 11 && t < 18) || (t > 21 && t < 30)) face(target, 25);
                if (t == 10) { swingSound(); hitArc(5.0, 75, 12, 0.6); }
                if (t == 20) { swingSound(); hitArc(5.0, 75, 12, 0.6); }
                if (t == 34) {
                    hitArc(5.5, 60, 18, 1.4);
                    impact(this.position().add(forward().scale(3.0)), 3.0F, 12);
                }
            }
            case DASH -> {                          // 突進斬り  0.6〜0.9秒で突進、0.9秒で斬る
                if (t < 12) face(target, 25);
                if (t == 12) {
                    dashDir = target != null
                            ? new Vec3(target.getX() - getX(), 0, target.getZ() - getZ()).normalize()
                            : forward();
                    playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 1.3F);
                }
                if (t >= 12 && t <= 18) {
                    this.setDeltaMovement(dashDir.x * 1.3, this.getDeltaMovement().y, dashDir.z * 1.3);
                    this.hasImpulse = true;
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                            this.getBoundingBox().inflate(0.8), this::canHit)) {
                        if (hitThisAttack.add(e.getUUID())) damage(e, dmg(15, 20), 1.6);
                    }
                }
                if (t == 18) {
                    swingSound();
                    hitArc(4.5, 60, dmg(15, 20), 1.6);
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1, 0.2));
                }
            }
            case LEAP -> {                          // 跳躍叩きつけ  0.4秒で跳ぶ、1.2秒ごろ着地
                if (t < 8) face(target, 30);
                if (t == 8) {
                    Vec3 to = target != null ? target.position().subtract(position()) : forward().scale(6);
                    Vec3 flat = new Vec3(to.x, 0, to.z);
                    double dist = Math.max(0, flat.length() - 2.0);
                    double speed = Math.min(1.6, dist / 8.4);
                    Vec3 dir = flat.lengthSqr() > 1e-4 ? flat.normalize() : forward();
                    this.setDeltaMovement(dir.x * speed, 0.65, dir.z * speed);
                    this.hasImpulse = true;
                    playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 0.8F);
                }
                if (t > 12 && !leapLanded && (this.onGround() || t >= 30)) {
                    leapLanded = true;
                    hitRadius(this.position(), 5.0, dmg(18, 22), 1.8);
                    impact(this.position(), 5.0F, 20);
                }
            }
            case PILLAR -> {                        // 火柱  0.9秒で剣を突き刺す、その1秒後に噴き出す
                if (t < 16) face(target, 25);
                if (t == 18 && target != null) {
                    impact(this.position().add(forward().scale(2.0)), 1.5F, 6);
                    spawnPillar(target.getX(), target.getY(), target.getZ());
                    if (blue()) {
                        Vec3 lead = target.getDeltaMovement().multiply(12, 0, 12);
                        spawnPillar(target.getX() + lead.x, target.getY(), target.getZ() + lead.z);
                        double a = this.random.nextDouble() * Math.PI * 2;
                        double r = 2.5 + this.random.nextDouble();
                        spawnPillar(target.getX() + Math.cos(a) * r, target.getY(), target.getZ() + Math.sin(a) * r);
                    }
                }
            }
            case FIREBALL -> {                      // 火の玉  0.8秒で発射
                if (t <= 16) face(target, 25);
                if (t == 16 && target != null) {
                    Vec3 start = this.position().add(forward().scale(1.6)).add(0, 2.6, 0);
                    Vec3 aim = target.getEyePosition().subtract(start).normalize();
                    int count = blue() ? 3 : 1;
                    for (int i = 0; i < count; i++) {
                        float spread = (i - (count - 1) / 2.0F) * 15.0F * Mth.DEG_TO_RAD;
                        KnightFireballEntity.shoot(this, start, aim.yRot(spread).scale(0.9), blue(), dmg(9, 12));
                    }
                    playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.6F);
                }
            }
            case WAVE_V, WAVE_H -> {                // 飛ぶ斬撃  縦0.65秒 / 横0.55秒で発射
                boolean vertical = attack == Attack.WAVE_V;
                int release = vertical ? 13 : 11;
                if (t < release - 1) face(target, 25);
                if (t == release) {
                    swingSound();
                    Vec3 dir = forward();
                    Vec3 start = this.position().add(dir.scale(1.5));
                    FlameSlashEntity.shoot(this, start, dir.scale(1.0), vertical, blue(), dmg(10, 14));
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.0秒で炎が青く爆発
                if (t == 20) {
                    this.entityData.set(PHASE, 2);
                    this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
                    AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (speed != null) speed.setBaseValue(0.33);
                    playSound(SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.6F);
                    playSound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.5F);
                    shake(30);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 2, getZ(), 250, 1.5, 1.5, 1.5, 0.2);
                        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.5, getZ(), 1, 0, 0, 0, 0);
                    }
                    // 周りのプレイヤーを吹き飛ばす（ダメージはなし）
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                            this.getBoundingBox().inflate(6.0), this::canHit)) {
                        e.knockback(2.0, getX() - e.getX(), getZ() - e.getZ());
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.5, 0));
                        e.hurtMarked = true;
                    }
                }
            }
            default -> {}
        }
        if (t >= attack.length) endAttack();
    }

    // ------------------------------------------------------------ helpers
    private Vec3 forward() {
        return Vec3.directionFromRotation(0, this.getYRot());
    }

    /** ターゲットの方へ体を向ける（1 tick に最大 maxStep 度） */
    private void face(LivingEntity target, float maxStep) {
        if (target == null) return;
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.approachDegrees(this.getYRot(), want, maxStep);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private boolean canHit(LivingEntity e) {
        return e != this && e.isAlive() && !(e instanceof FireKnightEntity) && !e.isSpectator();
    }

    private void damage(LivingEntity e, float amount, double knock) {
        if (e.hurt(this.damageSources().mobAttack(this), amount)) {
            e.knockback(knock, getX() - e.getX(), getZ() - e.getZ());
        }
    }

    /** 前方の扇形（range ブロック、左右 halfAngle 度）に当てる */
    private void hitArc(double range, double halfAngle, float amount, double knock) {
        Vec3 look = forward();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(range, 2.0, range), this::canHit)) {
            Vec3 to = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
            if (to.length() - e.getBbWidth() / 2.0 > range) continue;
            if (to.lengthSqr() > 1.0E-4) {
                double angle = Math.toDegrees(Math.acos(Mth.clamp(to.normalize().dot(look), -1.0, 1.0)));
                if (angle > halfAngle) continue;
            }
            if (e.getY() > getY() + 4.0 || e.getY() + e.getBbHeight() < getY() - 1.0) continue;
            damage(e, amount, knock);
        }
    }

    /** 円の範囲に当てる（着地の衝撃など） */
    private void hitRadius(Vec3 center, double radius, float amount, double knock) {
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(radius, 2.0, radius), this::canHit)) {
            double dx = e.getX() - center.x, dz = e.getZ() - center.z;
            if (Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() / 2.0 > radius) continue;
            if (e.getY() > center.y + 3.0 || e.getY() + e.getBbHeight() < center.y - 1.0) continue;
            damage(e, amount, knock);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.4, 0));
            e.hurtMarked = true;
        }
    }

    /** 地面を叩いた演出：揺れ、音、地面の破片、炎の輪 */
    private void impact(Vec3 at, float radius, int shakeTicks) {
        shake(shakeTicks);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.7F);
        playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
        if (!(level() instanceof ServerLevel sl)) return;
        ParticleOptions fire = blue() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        BlockState ground = level().getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        int ring = (int) (radius * 12);
        for (int i = 0; i < ring; i++) {
            double a = Math.PI * 2 * i / ring;
            double x = at.x + Math.cos(a) * radius, z = at.z + Math.sin(a) * radius;
            sl.sendParticles(fire, x, at.y + 0.1, z, 2, 0.1, 0.05, 0.1, 0.03);
            if (!ground.isAir()) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, at.y + 0.1, z, 3, 0.2, 0.1, 0.2, 0.15);
            }
        }
        sl.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 3, radius * 0.4, 0.2, radius * 0.4, 0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.3, at.z, 20, radius * 0.5, 0.2, radius * 0.5, 0.02);
    }

    private void spawnPillar(double x, double y, double z) {
        FirePillarEntity.spawn(this, x, groundY(x, y, z), z, blue(), false);
    }

    /** 真下の地面の高さを探す（空中のプレイヤーを狙っても地面から出るように） */
    private double groundY(double x, double y, double z) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(x, y, z).mutable();
        for (int i = 0; i < 8 && level().getBlockState(pos.below()).isAir(); i++) pos.move(0, -1, 0);
        for (int i = 0; i < 4 && !level().getBlockState(pos).isAir(); i++) pos.move(0, 1, 0);
        return pos.getY();
    }

    private void swingSound() {
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.6F);
        playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
    }

    /** 兜と肩から立ちのぼる炎、ひびから漏れる煙 */
    private void ambientParticles() {
        ParticleOptions fire = blue() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
        if (this.random.nextInt(2) == 0) {
            level().addParticle(fire, getX() + (random.nextDouble() - 0.5) * 0.4, getY() + 3.35,
                    getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0.05 + random.nextDouble() * 0.03, 0);
        }
        for (int side = -1; side <= 1; side += 2) {
            if (this.random.nextInt(3) == 0) {
                level().addParticle(fire, getX() + rx * side * 0.95 + (random.nextDouble() - 0.5) * 0.3, getY() + 2.85,
                        getZ() + rz * side * 0.95 + (random.nextDouble() - 0.5) * 0.3, 0, 0.04, 0);
            }
        }
        if (this.random.nextInt(5) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5), getY() + 1.0 + random.nextDouble() * 1.6,
                    getZ() + (random.nextDouble() - 0.5), 0, 0.02, 0);
        }
        if (this.random.nextInt(blue() ? 4 : 8) == 0) {
            level().addParticle(blue() ? ParticleTypes.SOUL : ParticleTypes.LAVA, getX(), getY() + 2.0, getZ(), 0, 0, 0);
        }
    }

    // ============================================================ damage / death
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (attack == Attack.PHASE_CHANGE) return false;     // 変身中は無敵
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        if (!level().isClientSide) {
            this.triggerAnim("main", "death");
            playSound(SoundEvents.WITHER_DEATH, 2.0F, 0.7F);
        }
    }

    /** 死亡アニメ（3秒）を最後まで見せてから消える */
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (level().isClientSide) {
            ParticleOptions fire = blue() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
            int n = Math.max(0, 6 - this.deathTime / 10);
            for (int i = 0; i < n; i++) {
                level().addParticle(fire, getX() + (random.nextDouble() - 0.5) * 1.5, getY() + random.nextDouble() * 3,
                        getZ() + (random.nextDouble() - 0.5) * 1.5, 0, 0.05, 0);
            }
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble() * 2.5,
                    getZ() + (random.nextDouble() - 0.5), 0, 0.03, 0);
        }
        if (this.deathTime >= 60 && !level().isClientSide() && !this.isRemoved()) {
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
        this.bossEvent.addPlayer(player);
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
        int phase = Math.max(1, tag.getInt("Phase"));
        this.entityData.set(PHASE, phase);
        if (phase >= 2) {
            this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
            AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.33);
        }
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ============================================================ sounds
    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 0.7F);
    }

    @Override
    public float getVoicePitch() { return 0.6F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<FireKnightEntity> main = new AnimationController<>(this, "main", 4, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DEATH);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        });
        for (Attack a : Attack.values()) {
            if (a.anim != null) main.triggerableAnim(a.name(), RawAnimation.begin().thenPlay(A + a.anim));
        }
        main.triggerableAnim("death", DEATH);
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
