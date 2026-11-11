package com.chigusa.jsnboss.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 闇の魔導士。素早く動き回り、追尾弾・背後への瞬間移動・命を吸う鎖で追い詰めてくる。
 * 第2形態「深淵」（HP600以下）：宙に浮かび、闇の光線をなぎ払う。
 * 大技「深淵の詠唱」は、6秒の溜めの間に120ダメージ与えれば止められる（止めると3秒ふらつく）。
 */
public class DarkMageEntity extends AbstractBoss {
    private static final String A = "animation.dark_mage.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");

    private static final Vector3f VIOLET = new Vector3f(0.69F, 0.42F, 1.0F);
    private static final Vector3f CRIMSON = new Vector3f(0.9F, 0.1F, 0.18F);
    private static final float ABYSS_BREAK = 120.0F;     // 詠唱を止めるのに必要なダメージ
    private static final double HOVER_HEIGHT = 3.0;     // 第2形態で浮かぶ高さ

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        ORBS(28, 40, "cast_orbs"),
        BLINK(24, 80, "blink"),
        DRAIN(60, 160, "drain"),
        BURST(20, 60, "burst"),
        BEAM(68, 140, "beam"),
        ABYSS(120, 700, "abyss"),
        STAGGER(60, 0, "stagger"),
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
    private Vec3 blinkTo;
    private boolean blinkChained;
    private float beamBaseYaw, beamPitch;
    private float abyssDamage;
    private double orbitAngle;

    public DarkMageEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1200.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    private Vector3f color() { return isPhase2() ? CRIMSON : VIOLET; }

    @Override
    protected void onPhaseChanged(int phase) {
        this.setNoGravity(phase >= 2);           // 第2形態は宙に浮く
        if (phase >= 2) this.cooldowns.put(Attack.ABYSS, 200);
    }

    @Override
    protected boolean isInvulnerableNow() {
        return attack == Attack.PHASE_CHANGE;
    }

    // ============================================================ AI
    @Override
    protected void bossAiStep() {
        LivingEntity target = this.getTarget();
        if (attack != Attack.NONE) {
            if (isPhase2()) hover(null, 0.0);
            tickAttack(target);
            return;
        }
        if (!isPhase2() && getHealth() <= getMaxHealth() * 0.5F) {
            startAttack(Attack.PHASE_CHANGE);
            return;
        }
        if (globalCooldown > 0) globalCooldown--;
        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
        if (target == null || !target.isAlive()) {
            if (isPhase2()) hover(null, 0.0);
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double dist = Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ()));

        if (isPhase2()) {
            // ターゲットの周りをゆっくり回りながら浮かぶ
            orbitAngle += 0.02;
            Vec3 p = target.position().add(Math.cos(orbitAngle) * 8, 0, Math.sin(orbitAngle) * 8);
            hover(p, 0.32);
            face(target, 20);
        } else if (this.tickCount % 10 == 0) {
            if (dist < 5) {
                Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(7);
                this.getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.2);
            } else if (dist > 11) {
                this.getNavigation().moveTo(target, 1.0);
            } else {
                this.getNavigation().stop();
            }
        }
        if (globalCooldown <= 0) {
            Attack next = chooseAttack(target, dist);
            if (next != null) startAttack(next);
        }
    }

    /** 第2形態：地面から HOVER_HEIGHT の高さに浮かびながら to へ近づく */
    private void hover(Vec3 to, double maxSpeed) {
        this.getNavigation().stop();
        Vec3 v = this.getDeltaMovement().scale(0.6);
        if (to != null) {
            Vec3 d = to.subtract(this.position()).multiply(1, 0, 1);
            if (d.length() > 0.5) v = v.add(d.normalize().scale(Math.min(maxSpeed, d.length() * 0.08)));
        }
        double floor = groundY(getX(), getY() + 0.5, getZ()) + HOVER_HEIGHT;
        double vy = Mth.clamp((floor - getY()) * 0.1, -0.15, 0.15) + Math.sin(this.tickCount * 0.1) * 0.01;
        this.setDeltaMovement(v.x, vy, v.z);
    }

    private Attack chooseAttack(LivingEntity target, double dist) {
        boolean sight = this.hasLineOfSight(target);
        List<Attack> pool = new ArrayList<>();
        if (dist < 3.5) add(pool, Attack.BURST, 4);
        if (sight) add(pool, Attack.ORBS, 4);
        add(pool, Attack.BLINK, 3);
        if (sight && dist < 12) add(pool, Attack.DRAIN, 2);
        if (isPhase2()) {
            if (sight) add(pool, Attack.BEAM, 3);
            add(pool, Attack.ABYSS, 4);
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
        this.getNavigation().stop();
        this.triggerAnim("main", a.name());
    }

    private void endAttack() {
        Attack done = this.attack;
        this.attack = Attack.NONE;
        if (done == Attack.ABYSS || done == Attack.STAGGER) this.bossEvent.setName(this.getDisplayName());
        if (done != Attack.STAGGER) this.lastAttack = done;
        this.cooldowns.put(done, done.cooldown);
        this.globalCooldown = (done == Attack.PHASE_CHANGE || done == Attack.STAGGER) ? 10 : (isPhase2() ? 12 : 20);
        // 第2形態の背後斬りは2回続けて
        if (done == Attack.BLINK && isPhase2() && !blinkChained) {
            blinkChained = true;
            startAttack(Attack.BLINK);
        } else if (done == Attack.BLINK) {
            blinkChained = false;
        }
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case ORBS -> {                          // 追いかける闇の弾  0.6 / 0.8 / 1.0 秒
                if (t < 20) face(target, 25);
                if ((t == 12 || t == 16 || t == 20) && target != null) {
                    Vec3 start = this.position().add(forward().scale(0.8)).add(0, 2.0, 0);
                    int count = isPhase2() ? 3 : 1;
                    for (int i = 0; i < count; i++) {
                        float spread = (i - (count - 1) / 2.0F) * 25.0F * Mth.DEG_TO_RAD;
                        Vec3 v = target.getEyePosition().subtract(start).normalize().yRot(spread).scale(0.45);
                        DarkOrbEntity.shoot(this, target, start, v, dmg(8, 10), isPhase2());
                    }
                    playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 1.3F);
                }
            }
            case BLINK -> {                         // 背後に瞬間移動して斬る  0.35 秒に移動、0.7 秒に斬る
                if (t == 1) {
                    blinkTo = behind(target);
                    playSound(SoundEvents.ENDERMAN_TELEPORT, 1.2F, 0.6F);
                    smoke(this.position(), 20);
                }
                if (t == 7 && blinkTo != null) {
                    this.teleportTo(blinkTo.x, blinkTo.y, blinkTo.z);
                    smoke(blinkTo, 20);
                }
                if (t > 7 && t < 13) face(target, 60);
                if (t == 14) {
                    hitArc(4.0, 75, dmg(12, 16), 0.8);
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.6F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + forward().x * 1.5, getY() + 1.4, getZ() + forward().z * 1.5,
                                2, 0.5, 0.2, 0.5, 0);
                    }
                }
            }
            case DRAIN -> {                         // 命を吸う鎖  0.4〜2.6 秒（離れるか物陰に隠れると切れる）
                if (t < 8) face(target, 25);
                if (t >= 8 && t <= 52) {
                    List<LivingEntity> victims = new ArrayList<>();
                    if (isPhase2()) {
                        for (LivingEntity e : inRadius(this.position(), 12.0, 8.0)) if (this.hasLineOfSight(e)) victims.add(e);
                    } else if (target != null && target.distanceTo(this) < 14 && this.hasLineOfSight(target)) {
                        victims.add(target);
                    }
                    if (victims.isEmpty() && t > 12) { this.attackTick = attack.length; break; }
                    if (target != null) face(target, 10);
                    for (LivingEntity v : victims) {
                        if (t % 2 == 0) tether(v);
                        if (t % 10 == 0 && v.hurt(this.damageSources().indirectMagic(this, this), 3.0F)) {
                            this.heal(6.0F);
                        }
                    }
                    if (t % 10 == 0) playSound(SoundEvents.EVOKER_CAST_SPELL, 0.8F, 0.5F);
                }
            }
            case BURST -> {                         // 闇の爆発（近づかれたとき）  0.5 秒
                if (t == 10) {
                    for (LivingEntity e : inRadius(this.position(), 4.0, 3.5)) {
                        if (damage(e, dmg(10, 13), 1.8)) launch(e, 0.4);
                    }
                    smoke(this.position(), 60);
                    playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.5F, 0.7F);
                    shake(6);
                }
            }
            case BEAM -> {                          // 闇の光線  1.0 秒溜めて、2.6 秒までなぎ払う
                if (t < 20) face(target, 25);
                if (t == 20) {
                    beamBaseYaw = this.getYRot();
                    beamPitch = target != null ? pitchTo(target) : 15;
                    playSound(SoundEvents.BEACON_ACTIVATE, 2.0F, 0.6F);
                }
                if (t < 20 && t % 4 == 0 && target != null && level() instanceof ServerLevel sl) {
                    // 溜め：杖の先に光が集まる
                    Vec3 tip = this.position().add(forward().scale(1.2)).add(0, 1.9, 0);
                    sl.sendParticles(new DustParticleOptions(color(), 1.5F), tip.x, tip.y, tip.z, 6, 0.3, 0.3, 0.3, 0);
                }
                if (t >= 20 && t <= 52) {
                    float progress = (t - 20) / 32.0F;
                    float yaw = beamBaseYaw + Mth.lerp(progress, 60.0F, -60.0F);
                    this.setYRot(beamBaseYaw);
                    this.yBodyRot = beamBaseYaw;
                    beam(yaw, beamPitch, t);
                }
            }
            case ABYSS -> {                         // 深淵の詠唱  6 秒溜めて爆発（120 ダメージで止められる）
                if (t == 1) {
                    abyssDamage = 0;
                    playSound(SoundEvents.WITHER_SPAWN, 2.0F, 0.7F);
                }
                if (t % 5 == 0) {
                    this.bossEvent.setName(Component.translatable("boss.jsnboss.dark_mage.channel",
                            this.getDisplayName(), (int) Math.min(abyssDamage, ABYSS_BREAK), (int) ABYSS_BREAK));
                }
                if (t < 112 && t % 4 == 0 && level() instanceof ServerLevel sl) {
                    // 届く範囲（半径24）を輪で見せ、中心へ闇が集まっていく
                    for (int i = 0; i < 64; i++) {
                        double a = Math.PI * 2 * i / 64;
                        sl.sendParticles(new DustParticleOptions(color(), 2.0F), getX() + Math.cos(a) * 24, getY() - HOVER_HEIGHT + 0.3,
                                getZ() + Math.sin(a) * 24, 1, 0, 0.1, 0, 0);
                    }
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 30, 3, 3, 3, 0.5);
                }
                if (t == 112) {
                    for (LivingEntity e : inRadius(this.position().add(0, -HOVER_HEIGHT, 0), 24.0, 16.0)) {
                        if (damage(e, 30, 2.0)) {
                            e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                            launch(e, 0.5);
                        }
                    }
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 3, 3, 1, 3, 0);
                        sl.sendParticles(new DustParticleOptions(color(), 3.0F), getX(), getY(), getZ(), 300, 8, 3, 8, 0);
                    }
                    shake(30);
                    playSound(SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.5F);
                    playSound(SoundEvents.WITHER_SHOOT, 3.0F, 0.5F);
                }
            }
            case STAGGER -> {                       // 詠唱を止められてふらつく  3 秒（この間は受けるダメージ1.5倍）
                if (t == 1 && level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2, getZ(), 40, 0.8, 0.8, 0.8, 0.3);
                }
                if (t % 6 == 0 && level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 2.6, getZ(), 4, 0.3, 0.1, 0.3, 0.02);
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.4 秒で光が深紅に
                if (t == 28) {
                    setPhase(2);
                    shake(20);
                    playSound(SoundEvents.WITHER_SPAWN, 2.0F, 1.0F);
                    smoke(this.position(), 120);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(new DustParticleOptions(CRIMSON, 2.5F), getX(), getY() + 1.5, getZ(), 120, 2, 2, 2, 0);
                    }
                    for (LivingEntity e : inRadius(this.position(), 6.0, 4.0)) {
                        e.knockback(2.0, getX() - e.getX(), getZ() - e.getZ());
                        launch(e, 0.5);
                    }
                }
            }
            default -> {}
        }
        if (attackTick >= attack.length) endAttack();
    }

    /** ターゲットの背中側 2.2 ブロックの、立てる場所 */
    private Vec3 behind(LivingEntity target) {
        if (target == null) return null;
        Vec3 look = Vec3.directionFromRotation(0, target.getYRot());
        for (double d : new double[]{2.2, 1.6, 3.0}) {
            Vec3 p = target.position().subtract(look.scale(d));
            double y = groundY(p.x, target.getY() + 1, p.z) + (isPhase2() ? 1.0 : 0.0);
            if (level().noCollision(this, this.getBoundingBox().move(p.x - getX(), y - getY(), p.z - getZ()))) {
                return new Vec3(p.x, y, p.z);
            }
        }
        return null;
    }

    private float pitchTo(LivingEntity target) {
        Vec3 from = this.position().add(0, 1.9, 0);
        Vec3 d = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from);
        return (float) (-Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
    }

    /** 光線：長さ20ブロック。0.2 秒ごとに、線の近くにいる相手に8ダメージ */
    private void beam(float yaw, float pitch, int t) {
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        Vec3 from = this.position().add(0, 1.9, 0).add(Vec3.directionFromRotation(0, yaw).scale(1.0));
        double len = 20.0;
        if (level() instanceof ServerLevel sl && t % 2 == 0) {
            for (int i = 0; i < 40; i++) {
                Vec3 p = from.add(dir.scale(len * i / 40));
                sl.sendParticles(new DustParticleOptions(color(), 1.8F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
                if (i % 5 == 0) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0);
            }
        }
        if (t % 4 != 0) return;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(from, from.add(dir.scale(len))).inflate(1.5), this::canHit)) {
            Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
            double along = Mth.clamp(c.subtract(from).dot(dir), 0, len);
            double dist = c.distanceTo(from.add(dir.scale(along)));
            if (dist < 0.9 + e.getBbWidth() / 2.0) damage(e, 8, 0.3);
        }
    }

    /** 命を吸う鎖の見た目 */
    private void tether(LivingEntity v) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = this.position().add(0, 1.8, 0);
        Vec3 to = v.position().add(0, v.getBbHeight() * 0.6, 0);
        for (int i = 0; i <= 16; i++) {
            Vec3 p = from.lerp(to, i / 16.0).add(0, Math.sin(i / 16.0 * Math.PI + this.tickCount * 0.5) * 0.15, 0);
            sl.sendParticles(new DustParticleOptions(color(), 1.0F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, to.x, to.y, to.z, 1, 0.2, 0.2, 0.2, 0);
    }

    private void smoke(Vec3 at, int count) {
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.2, at.z, count / 2, 0.5, 0.8, 0.5, 0.03);
        sl.sendParticles(new DustParticleOptions(color(), 1.5F), at.x, at.y + 1.2, at.z, count, 0.6, 0.9, 0.6, 0);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.2, at.z, count / 2, 0.5, 0.8, 0.5, 0.1);
    }

    // ============================================================ damage
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (attack == Attack.STAGGER) amount *= 1.5F;           // ふらつき中は大チャンス
        boolean hurt = super.hurt(source, amount);
        if (hurt && attack == Attack.ABYSS) {
            abyssDamage += amount;
            if (abyssDamage >= ABYSS_BREAK) {
                // 詠唱を止めた！
                this.attack = Attack.NONE;
                this.bossEvent.setName(this.getDisplayName());
                playSound(SoundEvents.SHIELD_BREAK, 2.0F, 0.6F);
                playSound(SoundEvents.WITHER_HURT, 2.0F, 1.4F);
                this.cooldowns.put(Attack.ABYSS, Attack.ABYSS.cooldown);
                this.lastAttack = Attack.ABYSS;
                startAttack(Attack.STAGGER);
            }
        }
        return hurt;
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        if (random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (random.nextDouble() - 0.5) * 1.2, getY() + random.nextDouble() * 0.8,
                    getZ() + (random.nextDouble() - 0.5) * 1.2, 0, -0.02, 0);
        }
        if (random.nextInt(5) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5), getY() + 0.3, getZ() + (random.nextDouble() - 0.5), 0, -0.01, 0);
        }
        if (random.nextInt(6) == 0) {
            level().addParticle(new DustParticleOptions(color(), 0.8F), getX() + (random.nextDouble() - 0.5) * 2, getY() + 1 + random.nextDouble() * 1.5,
                    getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.02, 0);
        }
    }

    @Override
    protected void deathParticles() {
        for (int i = 0; i < 3; i++) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble() * 2.2,
                    getZ() + (random.nextDouble() - 0.5), 0, 0.04, 0);
            level().addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble() * 2.2,
                    getZ() + (random.nextDouble() - 0.5), 0, 0.1, 0);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        this.setNoGravity(false);
        this.bossEvent.setName(this.getDisplayName());
        if (!level().isClientSide) playSound(SoundEvents.WITHER_DEATH, 1.5F, 1.3F);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.EVOKER_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.EVOKER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 0.6F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<DarkMageEntity> main = new AnimationController<>(this, "main", 4, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DEATH);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        });
        for (Attack a : Attack.values()) {
            if (a.anim != null) main.triggerableAnim(a.name(), RawAnimation.begin().thenPlay(A + a.anim));
        }
        main.triggerableAnim("death", DEATH);
        controllers.add(main);
    }
}
