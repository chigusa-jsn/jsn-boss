package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEffects;
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
 * 影刃竜。全身に剣を生やした黒い竜。地面を駆け回り、跳びかかり・剣を飛ばす・地面から剣を生やす。
 * 第2形態（HP1000以下）：目と剣が赤く光り、速くなって残像が出る。空からの急降下を使い、剣の攻撃に「裂傷」がつく。
 */
public class BladeDragonEntity extends AbstractBoss {
    private static final String A = "animation.blade_dragon.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop(A + "run");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");
    private static final RawAnimation EMERGE = RawAnimation.begin().thenPlay(A + "emerge");

    private static final Vector3f RED = new Vector3f(1.0F, 0.15F, 0.22F);

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        POUNCE(28, 60, "pounce"),
        WING_SLASH(24, 40, "wing_slash"),
        TAIL_SPIN(32, 80, "tail_spin"),
        VOLLEY(28, 60, "sword_volley"),
        BLADE_PATH(32, 100, "blade_path"),
        AMBUSH(46, 160, "vanish"),
        DIVE(60, 200, "dive"),
        PHASE_CHANGE(60, 0, "roar");

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
    private boolean chained, landed;
    private Vec3 diveTarget;

    public BladeDragonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE, BossEvent.BossBarColor.RED);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2000.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 48.0)
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
    protected void onPhaseChanged(int phase) {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(phase >= 2 ? 0.38 : 0.32);
    }

    @Override
    protected boolean isInvulnerableNow() {
        return attack == Attack.PHASE_CHANGE;
    }

    /** 剣の攻撃：第2形態では裂傷もつける */
    private boolean slash(LivingEntity e, float amount, double knock) {
        if (!damage(e, amount, knock)) return false;
        if (isPhase2()) e.addEffect(new MobEffectInstance(ModEffects.LACERATION, 60, 0));
        return true;
    }


    // ============================================================ AI
    @Override
    protected void bossAiStep() {
        LivingEntity target = this.getTarget();
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

    private Attack chooseAttack(LivingEntity target) {
        double dist = Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ()))
                - (this.getBbWidth() + target.getBbWidth()) / 2.0;
        boolean sight = this.hasLineOfSight(target);
        List<Attack> pool = new ArrayList<>();
        if (dist < 3.5) {
            add(pool, Attack.WING_SLASH, 4);
            add(pool, Attack.TAIL_SPIN, 3);
        } else if (dist < 12) {
            add(pool, Attack.POUNCE, 4);
            add(pool, Attack.BLADE_PATH, 3);
            if (sight) add(pool, Attack.VOLLEY, 2);
            add(pool, Attack.AMBUSH, 2);
        } else {
            if (sight) add(pool, Attack.VOLLEY, 3);
            add(pool, Attack.BLADE_PATH, 3);
            add(pool, Attack.POUNCE, 2);
            add(pool, Attack.AMBUSH, 2);
        }
        if (isPhase2() && dist > 5) add(pool, Attack.DIVE, 3);
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
        this.landed = false;
        this.hitThisAttack.clear();
        this.getNavigation().stop();
        this.triggerAnim("main", a.name());
    }

    private void endAttack() {
        Attack done = this.attack;
        this.attack = Attack.NONE;
        this.lastAttack = done;
        this.setNoGravity(false);
        this.setInvisible(false);
        this.cooldowns.put(done, done.cooldown);
        this.globalCooldown = done == Attack.PHASE_CHANGE ? 10 : (isPhase2() ? 10 : 18);
        // 第2形態の飛びかかりは2連続
        if (done == Attack.POUNCE && isPhase2() && !chained) {
            chained = true;
            startAttack(Attack.POUNCE);
        } else if (done == Attack.POUNCE) {
            chained = false;
        }
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case POUNCE -> {                        // 飛びかかり  0.5 秒で跳び、0.9 秒に着地
                if (t < 10) face(target, 30);
                if (t == 10) {
                    leapAt(target, 1.8);
                    playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 1.4F);
                }
                if (t > 10 && t <= 20) contactHit(dmg(16, 20), 1.4);
                if (t == 18) {
                    for (LivingEntity e : inArc(4.0, 90)) {
                        if (hitThisAttack.add(e.getUUID())) slash(e, dmg(16, 20), 1.4);
                    }
                    swordSound();
                }
            }
            case WING_SLASH -> {                    // 翼斬り  0.6 秒
                if (t < 8) face(target, 30);
                if (t == 12) {
                    for (LivingEntity e : inArc(5.0, 75)) slash(e, dmg(14, 18), 1.2);
                    swordSound();
                    sweepParticles();
                }
            }
            case TAIL_SPIN -> {                     // 回転斬り  0.85 秒に周り全部
                if (t == 17) {
                    for (LivingEntity e : inRadius(this.position(), 6.0, 3.0)) {
                        if (slash(e, dmg(15, 19), 1.6)) launch(e, 0.3);
                    }
                    swordSound();
                    if (level() instanceof ServerLevel sl) {
                        for (int i = 0; i < 36; i++) {
                            double a = Math.PI * 2 * i / 36;
                            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(a) * 4, getY() + 1, getZ() + Math.sin(a) * 4, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }
            case VOLLEY -> {                        // 剣を飛ばす  0.7 秒に扇状に
                if (t < 12) face(target, 25);
                if (t == 14 && target != null) {
                    int n = isPhase2() ? 7 : 5;
                    Vec3 start = this.position().add(forward().scale(2.0)).add(0, 1.8, 0);
                    Vec3 aim = target.getEyePosition().subtract(start).normalize();
                    for (int i = 0; i < n; i++) {
                        float spread = (i - (n - 1) / 2.0F) * (80.0F / (n - 1)) * Mth.DEG_TO_RAD;
                        FlyingSwordEntity.shoot(this, start, aim.yRot(spread).scale(1.2), dmg(8, 10), isPhase2());
                    }
                    playSound(SoundEvents.PLAYER_ATTACK_STRONG, 2.0F, 0.6F);
                    swordSound();
                }
            }
            case BLADE_PATH -> {                    // 刃の道  0.8 秒に前脚を叩きつけ、剣が一直線に生える
                if (t < 14) face(target, 25);
                if (t == 16 && target != null) {
                    Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                    float[] angles = isPhase2() ? new float[]{-20, 0, 20} : new float[]{0};
                    for (float ang : angles) {
                        Vec3 d = dir.yRot(ang * Mth.DEG_TO_RAD);
                        for (int i = 1; i <= 12; i++) {
                            double x = getX() + d.x * (1.5 + i * 1.3), z = getZ() + d.z * (1.5 + i * 1.3);
                            BladeSpikeEntity.spawn(this, x, groundY(x, getY() + 2, z), z, 12 + (int) (i * 1.5), dmg(14, 18));
                        }
                    }
                    groundImpact(this.position().add(forward().scale(2.5)), 2.0F);
                    shake(10);
                }
            }
            case AMBUSH -> {                        // 影の奇襲：影に溶けて、横か後ろから現れて飛びかかる
                if (t == 10) {
                    this.setInvisible(true);
                    smoke(this.position());
                    Vec3 spot = flank(target);
                    if (spot != null) this.teleportTo(spot.x, spot.y, spot.z);
                }
                if (t == 26) {
                    this.setInvisible(false);
                    this.triggerAnim("main", "emerge");
                    smoke(this.position());
                    playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 1.6F);
                }
                if (t > 26 && t < 34) face(target, 40);
                if (t == 34) leapAt(target, 1.6);
                if (t > 34) contactHit(dmg(18, 22), 1.5);
            }
            case DIVE -> {                          // 急降下：飛び立って、上空から突っ込む
                if (t == 12) {
                    this.setNoGravity(true);
                    playSound(SoundEvents.ENDER_DRAGON_FLAP, 2.0F, 0.8F);
                }
                if (t >= 12 && t < 44) {
                    Vec3 above = (target != null ? target.position() : position()).add(0, 9, 0);
                    Vec3 d = above.subtract(position());
                    this.setDeltaMovement(d.scale(0.12).add(0, 0, 0));
                    face(target, 20);
                    if (t % 6 == 0) playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.5F, 1.0F);
                }
                if (t == 44) {
                    diveTarget = target != null ? target.position() : position().add(forward().scale(6)).add(0, -9, 0);
                    playSound(SoundEvents.PHANTOM_SWOOP, 3.0F, 0.5F);
                }
                if (t > 44 && !landed) {
                    Vec3 d = diveTarget.subtract(position());
                    this.setDeltaMovement(d.normalize().scale(1.6));
                    contactHit(dmg(24, 24), 1.8);
                    if (this.onGround() || d.length() < 1.5 || t >= 56) {
                        landed = true;
                        this.setNoGravity(false);
                        this.setDeltaMovement(Vec3.ZERO);
                        for (LivingEntity e : inRadius(this.position(), 5.0, 3.0)) {
                            if (hitThisAttack.add(e.getUUID()) && slash(e, 24, 2.0)) launch(e, 0.6);
                        }
                        groundImpact(this.position(), 5.0F);
                        shake(25);
                    }
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.4 秒に目と剣が赤く
                if (t == 28) {
                    setPhase(2);
                    shake(20);
                    playSound(SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 0.8F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(new DustParticleOptions(RED, 2.5F), getX(), getY() + 1.5, getZ(), 150, 2.5, 1.5, 2.5, 0);
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1.5, getZ(), 20, 3, 1, 3, 0);
                    }
                    for (LivingEntity e : inRadius(this.position(), 7.0, 4.0)) {
                        e.knockback(2.0, getX() - e.getX(), getZ() - e.getZ());
                        launch(e, 0.5);
                    }
                }
            }
            default -> {}
        }
        if (attackTick >= attack.length) endAttack();
    }

    /** ターゲットの少し手前へ跳ぶ */
    private void leapAt(LivingEntity target, double maxSpeed) {
        Vec3 to = target != null ? target.position().subtract(position()) : forward().scale(6);
        Vec3 flat = new Vec3(to.x, 0, to.z);
        double dist = Math.max(0, flat.length() - 1.5);
        Vec3 dir = flat.lengthSqr() > 1e-4 ? flat.normalize() : forward();
        double speed = Math.min(maxSpeed, dist / 7.0 + 0.3);
        this.setDeltaMovement(dir.x * speed, 0.5, dir.z * speed);
        this.hasImpulse = true;
    }

    /** 体当たり（同じ技で同じ相手には1回だけ） */
    private void contactHit(float amount, double knock) {
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.6), this::canHit)) {
            if (hitThisAttack.add(e.getUUID())) slash(e, amount, knock);
        }
    }

    /** ターゲットの横か後ろ、5 ブロック離れた場所 */
    private Vec3 flank(LivingEntity target) {
        if (target == null) return null;
        Vec3 look = Vec3.directionFromRotation(0, target.getYRot());
        for (float ang : new float[]{180, 90, -90, 135, -135}) {
            Vec3 p = target.position().add(look.yRot(ang * Mth.DEG_TO_RAD).scale(5));
            double y = groundY(p.x, target.getY() + 2, p.z);
            if (level().noCollision(this, this.getBoundingBox().move(p.x - getX(), y - getY(), p.z - getZ()))) {
                return new Vec3(p.x, y, p.z);
            }
        }
        return null;
    }

    private void swordSound() {
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.6F);
        playSound(SoundEvents.ANVIL_PLACE, 0.6F, 1.8F);
    }

    private void sweepParticles() {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 f = forward();
        for (int i = -3; i <= 3; i++) {
            Vec3 p = this.position().add(f.yRot(i * 0.35F).scale(3.5)).add(0, 1.2, 0);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private void smoke(Vec3 at) {
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1, at.z, 40, 1.5, 0.6, 1.5, 0.02);
        sl.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 0.5, at.z, 30, 1.5, 0.3, 1.5, 0.05);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.5F);
    }

    private void groundImpact(Vec3 at, float radius) {
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.8F);
        if (!(level() instanceof ServerLevel sl)) return;
        BlockState ground = level().getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z,
                    (int) (radius * 25), radius * 0.5, 0.2, radius * 0.5, 0.2);
        }
        sl.sendParticles(ParticleTypes.CRIT, at.x, at.y + 0.5, at.z, (int) (radius * 10), radius * 0.4, 0.3, radius * 0.4, 0.4);
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        // 第2形態：動くと赤い残像が残る
        if (isPhase2() && this.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
            double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
            for (int i = 0; i < 4; i++) {
                double along = (random.nextDouble() - 0.5) * 5.0;
                level().addParticle(new DustParticleOptions(RED, 1.6F),
                        getX() + fx * along + (random.nextDouble() - 0.5) * 1.5, getY() + 0.5 + random.nextDouble() * 1.6,
                        getZ() + fz * along + (random.nextDouble() - 0.5) * 1.5, 0, 0, 0);
            }
        }
        if (isPhase2() && random.nextInt(4) == 0) {
            float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
            level().addParticle(new DustParticleOptions(RED, 0.8F), getX() - Mth.sin(yaw) * 2.3, getY() + 2.0,
                    getZ() + Mth.cos(yaw) * 2.3, 0, 0.02, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2,
                getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.03, 0);
        if (random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.CRIT, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2,
                    getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.1, 0);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        this.setNoGravity(false);
        this.setInvisible(false);
        if (!level().isClientSide) playSound(SoundEvents.ENDER_DRAGON_DEATH, 1.5F, 1.4F);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.RAVAGER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 0.8F, 1.2F);
    }

    @Override
    public float getVoicePitch() { return 0.8F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<BladeDragonEntity> main = new AnimationController<>(this, "main", 3, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DEATH);
            if (!state.isMoving()) return state.setAndContinue(IDLE);
            return state.setAndContinue(this.getDeltaMovement().horizontalDistanceSqr() > 0.012 ? RUN : WALK);
        });
        for (Attack a : Attack.values()) {
            if (a.anim != null) main.triggerableAnim(a.name(), RawAnimation.begin().thenPlay(A + a.anim));
        }
        main.triggerableAnim("emerge", EMERGE);
        main.triggerableAnim("death", DEATH);
        controllers.add(main);
    }
}
