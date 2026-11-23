package com.chigusa.jsnboss.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
 * フェンリル。千切れた鎖をつけた巨大な狼。とても速い。
 * 噛みつき・飛びかかり・爪の連撃・氷の息・鎖の鞭・遠吠え（狼の群れ）。
 * 第2形態（HP1000以下）「ラグナロク」：銀の毛が金色に、目とルーンが赤に変わる。
 * ときどき周りが暗闇に包まれ、月喰い（3回の突進）を使う。
 */
public class FenrirEntity extends AbstractBoss {
    /** 遠吠えで呼んだ狼につけるタグ。フェンリルの攻撃は当たらない */
    public static final String PACK_TAG = "jsnboss_wolf_pack";
    private static final int MAX_WOLVES = 6;

    private static final String A = "animation.fenrir.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop(A + "run");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        BITE(24, 40, "bite"),
        POUNCE(32, 80, "pounce"),
        CLAWS(32, 60, "claws"),
        BREATH(52, 140, "frost_breath"),
        CHAIN_WHIP(30, 100, "chain_whip"),
        HOWL(48, 400, "wolf_howl"),
        ECLIPSE(60, 300, "eclipse"),
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
    private Vec3 dashDir = Vec3.ZERO;

    public FenrirEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE, BossEvent.BossBarColor.RED);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2000.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
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
    protected boolean isInvulnerableNow() {
        return attack == Attack.PHASE_CHANGE;
    }

    /** 自分の群れの狼には当てない */
    @Override
    protected boolean canHit(LivingEntity e) {
        return super.canHit(e) && !e.getTags().contains(PACK_TAG);
    }

    // ============================================================ AI
    @Override
    protected void bossAiStep() {
        LivingEntity target = this.getTarget();
        // 第2形態：10秒ごとに周りが暗闇に包まれる
        if (isPhase2() && this.tickCount % 200 == 0) darkness(60);

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
        if (this.tickCount % 6 == 0 || this.getNavigation().isDone()) {
            this.getNavigation().moveTo(target, 1.1);
        }
        if (globalCooldown <= 0) {
            Attack next = chooseAttack(target);
            if (next != null) startAttack(next);
        }
    }

    private Attack chooseAttack(LivingEntity target) {
        double dist = Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ()))
                - (this.getBbWidth() + target.getBbWidth()) / 2.0;
        List<Attack> pool = new ArrayList<>();
        if (countWolves() < MAX_WOLVES - 2) add(pool, Attack.HOWL, 1);
        if (isPhase2()) add(pool, Attack.ECLIPSE, 2);
        if (dist < 3.5) {
            add(pool, Attack.BITE, 3);
            add(pool, Attack.CLAWS, 3);
            add(pool, Attack.CHAIN_WHIP, 2);
            add(pool, Attack.BREATH, 1);
        } else if (dist < 8) {
            add(pool, Attack.BREATH, 3);
            add(pool, Attack.POUNCE, 3);
            add(pool, Attack.CHAIN_WHIP, 1);
        } else if (dist < 18) {
            add(pool, Attack.POUNCE, 4);
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
        this.globalCooldown = done == Attack.PHASE_CHANGE ? 10 : (isPhase2() ? 22 : 36);
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case BITE -> {                          // 噛みつき  0.5 秒
                if (t < 8) face(target, 30);
                if (t == 10) {
                    hitArc(4.5, 50, dmg(16, 20), 0.8);
                    playSound(SoundEvents.WOLF_GROWL, 2.0F, 0.5F);
                }
            }
            case POUNCE -> {                        // 飛びかかり  0.5 秒に跳んで 1.0 秒に着地
                if (t < 10) face(target, 30);
                if (t == 10) {
                    Vec3 d = target != null ? target.position().subtract(position()) : forward().scale(8);
                    double h = Math.sqrt(d.x * d.x + d.z * d.z);
                    double sp = Math.min(1.6, h / 10.0);
                    Vec3 dir = h > 0.01 ? new Vec3(d.x / h, 0, d.z / h) : forward();
                    this.setDeltaMovement(dir.x * sp, 0.75, dir.z * sp);
                    this.hasImpulse = true;
                    playSound(SoundEvents.WOLF_GROWL, 2.0F, 0.4F);
                }
                if (t >= 10 && t <= 24) {
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8), this::canHit)) {
                        if (hitThisAttack.add(e.getUUID()) && damage(e, dmg(18, 22), 1.5)) launch(e, 0.3);
                    }
                }
                if (t == 22) landDust();
            }
            case CLAWS -> {                         // 爪の連撃  右・左・右（第2形態は3回）
                if (t < 6 || t == 12 || t == 20) face(target, 30);
                if (t == 8 || t == 16 || (t == 24 && isPhase2())) {
                    hitArc(4.5, 70, dmg(12, 14), 0.6);
                    this.setDeltaMovement(this.getDeltaMovement().add(forward().scale(0.4)));
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.8F);
                }
            }
            case BREATH -> {                        // 氷の息  0.6〜2.2 秒、前方 8 ブロックに吹きつける
                face(target, 6);
                if (t >= 12 && t <= 44) {
                    Vec3 f = forward();
                    Vec3 mouth = this.position().add(f.scale(3.4)).add(0, 2.2, 0);
                    if (level() instanceof ServerLevel sl) {
                        for (int i = 0; i < 6; i++) {
                            Vec3 v = f.scale(0.6 + random.nextDouble() * 0.5).add((random.nextDouble() - 0.5) * 0.35,
                                    (random.nextDouble() - 0.7) * 0.25, (random.nextDouble() - 0.5) * 0.35);
                            sl.sendParticles(i % 2 == 0 ? ParticleTypes.SNOWFLAKE : ParticleTypes.CLOUD,
                                    mouth.x, mouth.y, mouth.z, 0, v.x, v.y, v.z, 1.0);
                        }
                    }
                    if (t % 5 == 0) {
                        for (LivingEntity e : inArc(9.0, 30)) {
                            if (e.hurt(this.damageSources().mobAttack(this), dmg(4, 5))) IceWitchEntity.freeze(e, 60);
                        }
                    }
                    if (t % 10 == 2) playSound(SoundEvents.POWDER_SNOW_STEP, 2.0F, 0.5F);
                }
            }
            case CHAIN_WHIP -> {                    // 鎖の鞭  その場で一回転
                if (t >= 8 && t <= 20) {
                    for (LivingEntity e : inRadius(this.position(), 6.0, 3.0)) {
                        if (hitThisAttack.add(e.getUUID()) && damage(e, dmg(12, 15), 2.0)) launch(e, 0.35);
                    }
                    if (t % 4 == 0) playSound(SoundEvents.CHAIN_HIT, 2.0F, 0.6F);
                }
            }
            case HOWL -> {                          // 遠吠え  狼の群れを呼ぶ（第2形態はプレイヤーに弱体化）
                if (t == 14) {
                    playSound(SoundEvents.WOLF_HOWL, 4.0F, 0.5F);
                    if (level() instanceof ServerLevel sl) {
                        int n = Math.min(isPhase2() ? 5 : 3, MAX_WOLVES - countWolves());
                        for (int i = 0; i < n; i++) {
                            Wolf w = EntityType.WOLF.create(sl);
                            if (w == null) continue;
                            double a = Math.PI * 2 * i / Math.max(1, n) + random.nextDouble(), r = 4 + random.nextDouble() * 3;
                            double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
                            w.moveTo(x, groundY(x, getY() + 3, z), z, random.nextFloat() * 360F, 0);
                            w.finalizeSpawn(sl, sl.getCurrentDifficultyAt(w.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                            w.addTag(PACK_TAG);
                            if (target != null) {
                                w.setPersistentAngerTarget(target.getUUID());
                                w.startPersistentAngerTimer();
                                w.setTarget(target);
                            }
                            sl.addFreshEntity(w);
                            sl.sendParticles(ParticleTypes.POOF, x, w.getY() + 0.5, z, 12, 0.3, 0.3, 0.3, 0.05);
                        }
                    }
                    if (isPhase2()) {
                        for (Player p : nearbyPlayers(16)) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
                    }
                }
            }
            case ECLIPSE -> {                       // 月喰い（第2形態）  暗闇に包んで、影のように3回突進
                if (t == 4) {
                    darkness(120);
                    playSound(SoundEvents.WOLF_HOWL, 4.0F, 0.35F);
                }
                for (int start : new int[]{12, 28, 44}) {
                    if (t == start - 1) {
                        face(target, 360);
                        dashDir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                        hitThisAttack.clear();
                    }
                    if (t >= start && t < start + 6) {
                        this.setDeltaMovement(dashDir.x * 1.8, 0.1, dashDir.z * 1.8);
                        this.hasImpulse = true;
                        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8), this::canHit)) {
                            if (hitThisAttack.add(e.getUUID()) && damage(e, 18.0F, 1.8)) launch(e, 0.4);
                        }
                        if (level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.2, getZ(), 8, 0.8, 0.6, 0.8, 0.01);
                        }
                    }
                    if (t == start + 6) this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1, 0.2));
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.4 秒に天へ吠えると金色に変わる
                if (t == 28) {
                    setPhase(2);
                    shake(20);
                    playSound(SoundEvents.WOLF_HOWL, 5.0F, 0.4F);
                    playSound(SoundEvents.CHAIN_BREAK, 3.0F, 0.5F);
                    darkness(80);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);
                        sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 2, getZ(), 120, 2.5, 1.5, 2.5, 0.1);
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

    private List<Player> nearbyPlayers(double r) {
        return level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(r, 10, r),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
    }

    /** 近くのプレイヤーの視界を暗くする（夜が来たように） */
    private void darkness(int ticks) {
        for (Player p : nearbyPlayers(24)) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks, 0, false, false));
    }

    private int countWolves() {
        return level().getEntitiesOfClass(Wolf.class, this.getBoundingBox().inflate(32), w -> w.isAlive() && w.getTags().contains(PACK_TAG)).size();
    }

    private void landDust() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 20, 1.2, 0.1, 1.2, 0.05);
        }
        playSound(SoundEvents.GENERIC_SMALL_FALL, 2.0F, 0.5F);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        if (!level().isClientSide) playSound(SoundEvents.WOLF_DEATH, 3.0F, 0.4F);
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        if (isPhase2()) {
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.CRIMSON_SPORE, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2.5,
                        getZ() + (random.nextDouble() - 0.5) * 3, 0, 0, 0);
            }
        } else if (random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2.5,
                    getZ() + (random.nextDouble() - 0.5) * 3, 0, -0.02, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(isPhase2() ? ParticleTypes.CRIMSON_SPORE : ParticleTypes.SNOWFLAKE,
                getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.05, 0);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.WOLF_GROWL; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WOLF_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 0.45F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<FenrirEntity> main = new AnimationController<>(this, "main", 3, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DEATH);
            if (!state.isMoving()) return state.setAndContinue(IDLE);
            return state.setAndContinue(this.getDeltaMovement().horizontalDistanceSqr() > 0.015 ? RUN : WALK);
        });
        for (Attack a : Attack.values()) {
            if (a.anim != null) main.triggerableAnim(a.name(), RawAnimation.begin().thenPlay(A + a.anim));
        }
        main.triggerableAnim("death", DEATH);
        controllers.add(main);
    }
}
