package com.chigusa.jsnboss.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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
 * 虚空の監視者（エンダーマンのボス、未来風）。テレポートを多用し、レーザー・ドローン・伸びる腕で戦う。
 * 飛び道具は半分の確率でテレポートしてかわす。
 * 第2形態（HP800以下）：光が紫からシアンに変わる。重力異常と虚空の崩壊（ブラックホール）が増える。
 */
public class EnderSentinelEntity extends AbstractBoss {
    private static final String A = "animation.ender_sentinel.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation AMBIENT = RawAnimation.begin().thenLoop(A + "ambient");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");

    private static final Vector3f PURPLE = new Vector3f(0.77F, 0.36F, 1.0F);
    private static final Vector3f CYAN = new Vector3f(0.31F, 0.9F, 1.0F);
    private static final double LASER_LENGTH = 22.0, REACH = 11.0;

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        SLASH(20, 60, "slash"),
        SLASH2(32, 80, "slash"),
        LASER(60, 160, "void_laser"),
        DRONES(40, 100, "drones"),
        RIFT(30, 120, "rift"),
        REACH(34, 90, "reach"),
        REACH_SWEEP(36, 90, "reach_sweep"),
        GRAVITY(30, 240, "gravity"),
        COLLAPSE(70, 360, "collapse"),
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
    private float laserYaw;
    private double laserDy;

    public EnderSentinelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarColor.BLUE);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1600.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
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

    /** 飛び道具は半分の確率でテレポートしてかわす（技の途中はかわさない） */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && attack == Attack.NONE && source.is(DamageTypeTags.IS_PROJECTILE)
                && random.nextFloat() < 0.5F && !isDeadOrDying()) {
            for (int i = 0; i < 8; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = 4 + random.nextDouble() * 6;
                if (teleport(getX() + Math.cos(a) * r, getZ() + Math.sin(a) * r)) return false;
            }
        }
        return super.hurt(source, amount);
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
        double dist = Math.sqrt(this.distanceToSqr(target));
        if (dist > 20 && this.tickCount % 40 == 0) {                 // 遠すぎたら近くへテレポート
            Vec3 d = position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(8);
            teleport(target.getX() + d.x, target.getZ() + d.z);
        }
        if (this.tickCount % 8 == 0 || this.getNavigation().isDone()) {
            this.getNavigation().moveTo(target, 1.0);
        }
        if (globalCooldown <= 0) {
            Attack next = chooseAttack(target);
            if (next != null) startAttack(next);
        }
    }

    private Attack chooseAttack(LivingEntity target) {
        double dist = Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ()));
        boolean p2 = isPhase2();
        List<Attack> pool = new ArrayList<>();
        if (p2) {
            add(pool, Attack.COLLAPSE, 2);
            add(pool, Attack.GRAVITY, 2);
        }
        add(pool, p2 ? Attack.SLASH2 : Attack.SLASH, 3);
        if (dist < 5) {
            add(pool, p2 ? Attack.REACH_SWEEP : Attack.REACH, 2);
        } else if (dist < REACH) {
            add(pool, p2 ? Attack.REACH_SWEEP : Attack.REACH, 4);
            add(pool, Attack.LASER, 2);
            add(pool, Attack.DRONES, 2);
            add(pool, Attack.RIFT, 2);
        } else {
            add(pool, Attack.LASER, 3);
            add(pool, Attack.DRONES, 3);
            add(pool, Attack.RIFT, 3);
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
        if (done == Attack.SLASH) this.cooldowns.put(Attack.SLASH2, done.cooldown);
        if (done == Attack.REACH) this.cooldowns.put(Attack.REACH_SWEEP, done.cooldown);
        this.globalCooldown = done == Attack.PHASE_CHANGE ? 10 : (isPhase2() ? 24 : 40);
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case SLASH, SLASH2 -> {                 // テレポート斬り  相手の後ろ（2回目は横）に出て爪で斬る
                if ((t == 6 || (attack == Attack.SLASH2 && t == 18)) && target != null) {
                    Vec3 look = target.getLookAngle().multiply(1, 0, 1);
                    if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, 1);
                    look = look.normalize();
                    Vec3 off = t == 6 ? look.scale(-2.5) : new Vec3(-look.z, 0, look.x).scale(random.nextBoolean() ? 2.5 : -2.5);
                    teleport(target.getX() + off.x, target.getZ() + off.z);
                    face(target, 360);
                }
                if (t == 12 || (attack == Attack.SLASH2 && t == 24)) {
                    hitArc(4.0, 75, dmg(12, 15), 1.0);
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 1.4F);
                }
            }
            case LASER -> {                         // 虚空レーザー  1 秒ためて、体ごと回してなぎ払う
                if (t < 20) {
                    face(target, 20);
                    if (t % 4 == 0) playSound(SoundEvents.BEACON_AMBIENT, 2.0F, 1.5F + t * 0.02F);
                }
                if (t == 20) {
                    laserYaw = this.getYRot() - 50.0F;
                    Vec3 o = laserOrigin();
                    laserDy = target != null
                            ? Mth.clamp((target.getEyeY() - 0.4 - o.y) / Math.max(1, Math.sqrt(distanceToSqr(target))), -0.6, 0.3) : -0.1;
                    playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 1.8F);
                }
                if (t >= 20 && t <= 50) {
                    float yaw = laserYaw + (t - 20) * 100.0F / 30.0F;
                    this.setYRot(yaw);
                    this.yBodyRot = yaw;
                    this.yHeadRot = yaw;
                    fireLaser(yaw, t % 4 == 0);
                }
            }
            case DRONES -> {                        // ドローン射撃
                face(target, 20);
                int every = isPhase2() ? 3 : 5;
                if (t >= 10 && t <= 39 && (t - 10) % every == 0 && target != null) {
                    int shot = (t - 10) / every;
                    Vec3 f = forward();
                    Vec3 side = new Vec3(-f.z, 0, f.x).scale(shot % 2 == 0 ? 1.2 : -1.2);
                    Vec3 pos = this.position().add(side).add(0, 4.0, 0).add(f.scale(0.3));
                    DroneShotEntity.shoot(this, pos, target, dmg(6, 7), isPhase2());
                    playSound(SoundEvents.SHULKER_SHOOT, 1.0F, 1.6F);
                }
            }
            case RIFT -> {                          // 空間の裂け目  頭上に裂け目が開き、エンドストーンが降る
                if (t == 14) {
                    playSound(SoundEvents.END_PORTAL_FRAME_FILL, 2.0F, 0.6F);
                    List<LivingEntity> victims = new ArrayList<>(level().getEntitiesOfClass(Player.class,
                            this.getBoundingBox().inflate(24, 12, 24), p -> p.isAlive() && !p.isSpectator() && !p.isCreative()));
                    if (victims.isEmpty() && target != null) victims.add(target);
                    for (LivingEntity v : victims) {
                        BoulderEntity.drop(this, v.getX(), v.getZ(), 9, dmg(14, 16), BoulderEntity.END, 18);
                        if (isPhase2()) {
                            for (int i = 0; i < 2; i++) {
                                double a = random.nextDouble() * Math.PI * 2, r = 2.5 + random.nextDouble() * 2;
                                BoulderEntity.drop(this, v.getX() + Math.cos(a) * r, v.getZ() + Math.sin(a) * r, 9, 16.0F, BoulderEntity.END, 24 + i * 6);
                            }
                        }
                    }
                }
            }
            case REACH -> {                         // 伸びる腕  0.6 秒に前腕が伸びて突き刺し、手元へ引き寄せる
                if (t < 10) face(target, 30);
                if (t == 9) playSound(SoundEvents.ENDERMAN_STARE, 1.5F, 1.6F);
                if (t == 12) {
                    Vec3 o = this.position().add(0, 3.2, 0);
                    Vec3 end = o.add(forward().scale(REACH));
                    LivingEntity hit = null;
                    double best = Double.MAX_VALUE;
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(o, end).inflate(1.5), this::canHit)) {
                        var clip = e.getBoundingBox().inflate(0.6).clip(o, end);
                        if (clip.isPresent() && clip.get().distanceTo(o) < best) {
                            best = clip.get().distanceTo(o);
                            hit = e;
                        }
                    }
                    playSound(SoundEvents.ENDER_EYE_LAUNCH, 1.5F, 0.6F);
                    if (hit != null && damage(hit, 12.0F, 0)) {
                        Vec3 to = this.position().add(forward().scale(2.2)).subtract(hit.position());
                        hit.setDeltaMovement(to.x * 0.32, 0.35, to.z * 0.32);
                        hit.hurtMarked = true;
                        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5F, 0.6F);
                    }
                }
            }
            case REACH_SWEEP -> {                   // 伸びる腕（第2形態）  両腕を伸ばして左右から挟む
                if (t < 10) face(target, 30);
                if (t >= 12 && t <= 18) {
                    for (LivingEntity e : inArc(REACH, 80)) {
                        if (e.getY() < getY() + 4.5 && hitThisAttack.add(e.getUUID())) damage(e, 15.0F, 1.5);
                    }
                    if (t == 12) playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.6F);
                }
            }
            case GRAVITY -> {                       // 重力異常（第2形態）  周りを浮かせる
                if (t == 14) {
                    playSound(SoundEvents.SHULKER_BULLET_HIT, 2.0F, 0.5F);
                    for (LivingEntity e : inRadius(this.position(), 14.0, 8.0)) {
                        if (damage(e, 8.0F, 0)) e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 2));
                    }
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.5, getZ(), 200, 7, 0.3, 7, 0.1);
                    }
                }
            }
            case COLLAPSE -> {                      // 虚空の崩壊（第2形態）  ブラックホール
                face(target, 15);
                if (t == 16 && target != null) {
                    BlackHoleEntity.spawn(this, target.position(), 22.0F);
                    playSound(SoundEvents.END_PORTAL_SPAWN, 2.0F, 1.2F);
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.5 秒に光がシアンに変わる
                if (t == 30) {
                    setPhase(2);
                    shake(15);
                    playSound(SoundEvents.BEACON_POWER_SELECT, 3.0F, 1.2F);
                    playSound(SoundEvents.ENDERMAN_SCREAM, 3.0F, 0.6F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 3, getZ(), 120, 1.5, 2, 1.5, 0.3);
                        sl.sendParticles(new DustParticleOptions(CYAN, 2.0F), getX(), getY() + 3, getZ(), 80, 2, 2, 2, 0);
                    }
                    for (LivingEntity e : inRadius(this.position(), 7.0, 5.0)) {
                        e.knockback(2.0, getX() - e.getX(), getZ() - e.getZ());
                        launch(e, 0.5);
                    }
                }
            }
            default -> {}
        }
        if (attackTick >= attack.length) endAttack();
    }

    private Vec3 laserOrigin() {
        return this.position().add(0, 3.9, 0);
    }

    /** バイザーから yaw の方向へ光線を伸ばす。ブロックで止まる。hurt が true の tick にダメージ */
    private void fireLaser(float yaw, boolean hurt) {
        Vec3 o = laserOrigin();
        float r = yaw * Mth.DEG_TO_RAD;
        Vec3 dir = new Vec3(-Mth.sin(r), laserDy, Mth.cos(r)).normalize();
        Vec3 end = o.add(dir.scale(LASER_LENGTH));
        var block = level().clip(new ClipContext(o, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        end = block.getLocation();
        if (level() instanceof ServerLevel sl) {
            DustParticleOptions dust = new DustParticleOptions(isPhase2() ? CYAN : PURPLE, 1.6F);
            double len = end.distanceTo(o);
            for (double d = 0.8; d < len; d += 0.5) {
                Vec3 p = o.add(dir.scale(d));
                sl.sendParticles(dust, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
            }
            sl.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 2, 0.1, 0.1, 0.1, 0.05);
        }
        if (!hurt) return;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(o, end).inflate(1.0), this::canHit)) {
            if (e.getBoundingBox().inflate(0.3).clip(o, end).isPresent()) {
                e.hurt(this.damageSources().mobAttack(this), dmg(7, 9));
            }
        }
    }

    /** (x, z) 付近の地面へテレポート。成功したら両方の場所に紫の粒 */
    private boolean teleport(double x, double z) {
        double ox = getX(), oy = getY(), oz = getZ();
        double y = groundY(x, getY() + 6, z);
        if (!this.randomTeleport(x, y, z, false)) return false;
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.PORTAL, ox, oy + 2, oz, 60, 0.5, 1.5, 0.5, 0.5);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2, getZ(), 40, 0.5, 1.5, 0.5, 0.1);
        }
        level().playSound(null, ox, oy, oz, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.5F, 0.7F);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5F, 0.7F);
        return true;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        if (!level().isClientSide) playSound(SoundEvents.ENDERMAN_DEATH, 2.0F, 0.5F);
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        for (int i = 0; i < 2; i++) {
            level().addParticle(ParticleTypes.PORTAL, getX() + (random.nextDouble() - 0.5) * 1.5, getY() + random.nextDouble() * 4.4,
                    getZ() + (random.nextDouble() - 0.5) * 1.5, (random.nextDouble() - 0.5) * 2, -random.nextDouble(), (random.nextDouble() - 0.5) * 2);
        }
        if (isPhase2() && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 1.5, getY() + random.nextDouble() * 4.4,
                    getZ() + (random.nextDouble() - 0.5) * 1.5, 0, 0.02, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(ParticleTypes.PORTAL, getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 4,
                getZ() + (random.nextDouble() - 0.5) * 2, 0, 0, 0);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.ENDERMAN_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENDERMAN_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 0.6F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<EnderSentinelEntity> main = new AnimationController<>(this, "main", 3, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DEATH);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        });
        for (Attack a : Attack.values()) {
            if (a.anim != null) main.triggerableAnim(a.name(), RawAnimation.begin().thenPlay(A + a.anim));
        }
        main.triggerableAnim("death", DEATH);
        controllers.add(main);
        // 光の輪の回転とドローンの上下は、技の最中もずっと動かし続ける
        controllers.add(new AnimationController<>(this, "ambient", 0, state -> state.setAndContinue(AMBIENT)));
    }
}
