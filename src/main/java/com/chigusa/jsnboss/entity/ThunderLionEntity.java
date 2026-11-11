package com.chigusa.jsnboss.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 雷の獅子。青い電気をまとった大きなライオン。突進・落雷・咆哮・地を走る雷・雷のリングで戦う。
 * 第2形態（HP900以下）：全身に稲妻が走り、体に触れるだけでしびれる（帯電）。雷雲を呼ぶ。
 */
public class ThunderLionEntity extends AbstractBoss {
    private static final String A = "animation.thunder_lion.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop(A + "run");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");

    private static final Vector3f BLUE = new Vector3f(0.5F, 0.9F, 1.0F);

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        RUSH(36, 80, "rush"),
        LIGHTNING(40, 100, "lightning_call"),
        HOWL(36, 120, "howl"),
        GROUND_BOLT(28, 70, "ground_bolt"),
        RING(36, 140, "ring"),
        STORM(44, 400, "storm"),
        PHASE_CHANGE(60, 0, "phase_change");

        public final int length, cooldown;
        public final String anim;

        Attack(int length, int cooldown, String anim) {
            this.length = length;
            this.cooldown = cooldown;
            this.anim = anim;
        }
    }

    private Attack attack = Attack.NONE;
    private Attack lastAttack = Attack.NONE;
    private int attackTick;
    private int globalCooldown = 40;
    private final Map<Attack, Integer> cooldowns = new EnumMap<>(Attack.class);
    private final Set<UUID> hitThisAttack = new HashSet<>();
    private Vec3 rushDir = Vec3.ZERO;
    private int stormTicks;

    public ThunderLionEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarColor.BLUE);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1800.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
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
    protected boolean isInvulnerableNow() {
        return attack == Attack.PHASE_CHANGE;
    }

    // ============================================================ AI
    @Override
    protected void bossAiStep() {
        LivingEntity target = this.getTarget();

        if (isPhase2()) {
            charged();
            if (stormTicks > 0) storm();
        }

        if (attack != Attack.NONE) {
            tickAttack(target);
            return;
        }
        if (!isPhase2() && getHealth() <= getMaxHealth() * 0.5F) {
            startAttack(Attack.PHASE_CHANGE);
            return;
        }
        if (globalCooldown > 0) globalCooldown--;
        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
        if (target == null || !target.isAlive()) return;

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.tickCount % 8 == 0 || this.getNavigation().isDone()) {
            this.getNavigation().moveTo(target, 1.0);
        }
        if (globalCooldown <= 0) {
            Attack next = chooseAttack(target);
            if (next != null) startAttack(next);
        }
    }

    /** 帯電：第2形態の体に触れると 4 ダメージ＋鈍化（同じ相手には0.5秒に1回） */
    private void charged() {
        if (this.tickCount % 10 != 0) return;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.4), this::canHit)) {
            if (e.hurt(this.damageSources().mobAttack(this), 4.0F)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1, e.getZ(), 12, 0.3, 0.5, 0.3, 0.2);
                }
            }
        }
    }

    /** 雷雲：しばらくのあいだ、近くのプレイヤーの足元に雷が落ち続ける */
    private void storm() {
        stormTicks--;
        if (stormTicks % 12 != 0) return;
        List<Player> players = level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24, 10, 24),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
        for (Player p : players) {
            Vec3 v = p.getDeltaMovement();
            LightningMarkEntity.spawn(this, p.getX() + v.x * 8 + (random.nextDouble() - 0.5) * 3,
                    p.getZ() + v.z * 8 + (random.nextDouble() - 0.5) * 3, 18, 10.0F);
        }
        if (players.isEmpty() && getTarget() != null) {
            LightningMarkEntity.spawn(this, getTarget().getX(), getTarget().getZ(), 18, 10.0F);
        }
    }

    private Attack chooseAttack(LivingEntity target) {
        double dist = Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ()))
                - (this.getBbWidth() + target.getBbWidth()) / 2.0;
        List<Attack> pool = new ArrayList<>();
        if (isPhase2() && stormTicks <= 0) add(pool, Attack.STORM, 3);
        if (dist < 4) {
            add(pool, Attack.HOWL, 3);
            add(pool, Attack.RING, 3);
            add(pool, Attack.GROUND_BOLT, 1);
        } else if (dist < 14) {
            add(pool, Attack.RUSH, 3);
            add(pool, Attack.GROUND_BOLT, 3);
            add(pool, Attack.LIGHTNING, 2);
            add(pool, Attack.RING, 2);
        } else {
            add(pool, Attack.LIGHTNING, 3);
            add(pool, Attack.RUSH, 3);
            add(pool, Attack.GROUND_BOLT, 2);
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
        this.hitThisAttack.clear();
        this.getNavigation().stop();
        this.triggerAnim("main", a.name());
    }

    private void endAttack() {
        Attack done = this.attack;
        this.attack = Attack.NONE;
        this.lastAttack = done;
        this.cooldowns.put(done, done.cooldown);
        this.globalCooldown = done == Attack.PHASE_CHANGE ? 10 : (isPhase2() ? 12 : 20);
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case RUSH -> {                          // 突進  0.6 秒から一直線に突っ込む
                if (t < 12) face(target, 25);
                if (t == 12) {
                    rushDir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 1.2F);
                }
                if (t >= 12 && t <= 28) {
                    this.setDeltaMovement(rushDir.x * 1.3, this.getDeltaMovement().y, rushDir.z * 1.3);
                    this.hasImpulse = true;
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8), this::canHit)) {
                        if (hitThisAttack.add(e.getUUID()) && damage(e, dmg(16, 20), 2.5)) launch(e, 0.5);
                    }
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.2, getZ(), 6, 0.8, 0.6, 0.8, 0.2);
                    }
                }
                if (t == 29) this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1, 0.2));
            }
            case LIGHTNING -> {                     // 落雷  0.9 秒に前脚を叩きつけ、予告の1秒後に雷が落ちる
                if (t < 16) face(target, 20);
                if (target != null) {
                    if (!isPhase2() && (t == 18 || t == 26 || t == 34)) {
                        LightningMarkEntity.spawn(this, target.getX(), target.getZ(), 20, 12.0F);
                    }
                    if (isPhase2() && (t == 18 || t == 30)) {
                        for (int i = 0; i < 3; i++) {
                            double a = random.nextDouble() * Math.PI * 2, r = i == 0 ? 0 : 2.5 + random.nextDouble() * 2;
                            LightningMarkEntity.spawn(this, target.getX() + Math.cos(a) * r, target.getZ() + Math.sin(a) * r, 20, 14.0F);
                        }
                    }
                }
                if (t == 18) {
                    shake(6);
                    playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5F, 1.2F);
                }
            }
            case HOWL -> {                          // 咆哮  0.7 秒に吠える（半径5）
                if (t < 12) face(target, 25);
                if (t == 14) {
                    for (LivingEntity e : inRadius(this.position(), 5.0, 3.0)) {
                        if (damage(e, dmg(10, 13), 3.0)) launch(e, 0.5);
                    }
                    playSound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.6F);
                    shake(10);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.5, getZ(), 80, 2.5, 1.0, 2.5, 0.5);
                        sl.sendParticles(ParticleTypes.SONIC_BOOM, getX() + forward().x * 2, getY() + 2, getZ() + forward().z * 2, 1, 0, 0, 0, 0);
                    }
                }
            }
            case GROUND_BOLT -> {                   // 地を走る雷  0.6 秒に地面を引っかく（第1形態 3方向、第2形態 5方向）
                if (t < 10) face(target, 30);
                if (t == 12) {
                    Vec3 dir = target != null
                            ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    int n = isPhase2() ? 5 : 3;
                    for (int i = 0; i < n; i++) {
                        float ang = (i - (n - 1) / 2.0F) * 22.0F * Mth.DEG_TO_RAD;
                        Vec3 d = dir.yRot(ang);
                        GroundBoltEntity.spawn(this, this.position().add(d.scale(2.0)), d, dmg(11, 13));
                    }
                    playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5F, 1.4F);
                }
            }
            case RING -> {                          // 雷のリング  0.9 秒に着地して雷の輪（第2形態は2重）
                if (t == 18) {
                    ShockwaveEntity.spawnElectric(this, getX(), getY(), getZ(), 14.0F, 0.45F, dmg(8, 10), 40);
                    landImpact();
                    shake(12);
                }
                if (t == 30 && isPhase2()) {
                    ShockwaveEntity.spawnElectric(this, getX(), getY(), getZ(), 14.0F, 0.45F, 10.0F, 40);
                    playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 2.0F, 0.8F);
                }
            }
            case STORM -> {                         // 雷雲  空を見上げて吠え、6秒ほど雷が落ち続ける
                if (t == 16) {
                    stormTicks = 120;
                    playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3.0F, 0.8F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 4, getZ(), 100, 2, 2, 2, 0.6);
                    }
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.4 秒で全身に雷が走る
                if (t == 28) {
                    setPhase(2);
                    shake(20);
                    playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 4.0F, 0.7F);
                    for (int i = 0; i < 6; i++) {
                        double a = Math.PI * 2 * i / 6;
                        LightningMarkEntity.spawn(this, getX() + Math.cos(a) * 6, getZ() + Math.sin(a) * 6, 2, 10.0F);
                    }
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.5, getZ(), 200, 2.5, 1.5, 2.5, 0.8);
                        sl.sendParticles(new DustParticleOptions(BLUE, 2.0F), getX(), getY() + 1.5, getZ(), 100, 2.5, 1.5, 2.5, 0);
                    }
                    for (LivingEntity e : inRadius(this.position(), 7.0, 4.0)) {
                        e.knockback(2.5, getX() - e.getX(), getZ() - e.getZ());
                        launch(e, 0.6);
                    }
                }
            }
            default -> {}
        }
        if (attackTick >= attack.length) endAttack();
    }

    private void landImpact() {
        playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 2.0F, 0.6F);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.2F);
        if (!(level() instanceof ServerLevel sl)) return;
        BlockState ground = level().getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
        if (!ground.isAir()) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.2, getZ(), 60, 2, 0.2, 2, 0.2);
        }
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.5, getZ(), 60, 2, 0.3, 2, 0.5);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        this.stormTicks = 0;
        if (!level().isClientSide) playSound(SoundEvents.RAVAGER_DEATH, 2.0F, 0.6F);
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        int n = isPhase2() ? 3 : 1;
        for (int i = 0; i < n; i++) {
            if (random.nextInt(isPhase2() ? 1 : 2) != 0) continue;
            level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    getX() + (random.nextDouble() - 0.5) * 3.0, getY() + 0.5 + random.nextDouble() * 2.2,
                    getZ() + (random.nextDouble() - 0.5) * 3.0,
                    (random.nextDouble() - 0.5) * 0.3, 0.1, (random.nextDouble() - 0.5) * 0.3);
        }
        if (isPhase2() && random.nextInt(4) == 0) {
            level().addParticle(new DustParticleOptions(BLUE, 1.0F), getX() + (random.nextDouble() - 0.5) * 2.6,
                    getY() + random.nextDouble() * 2.4, getZ() + (random.nextDouble() - 0.5) * 2.6, 0, 0.02, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5) * 2.5, getY() + random.nextDouble() * 2,
                getZ() + (random.nextDouble() - 0.5) * 2.5, 0, 0.2, 0);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.RAVAGER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 0.8F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<ThunderLionEntity> main = new AnimationController<>(this, "main", 3, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DEATH);
            if (!state.isMoving()) return state.setAndContinue(IDLE);
            return state.setAndContinue(this.getDeltaMovement().horizontalDistanceSqr() > 0.012 ? RUN : WALK);
        });
        for (Attack a : Attack.values()) {
            if (a.anim != null) main.triggerableAnim(a.name(), RawAnimation.begin().thenPlay(A + a.anim));
        }
        main.triggerableAnim("death", DEATH);
        controllers.add(main);
    }
}
