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
 * 風の猫。風をまとった大きな山猫。軽やかに跳び回り、風の刃・竜巻・突風・突進で戦う。
 * 第2形態（HP800以下）：体が半分風になって透け、周りを回る竜巻を2つ引き連れる。
 */
public class WindCatEntity extends AbstractBoss {
    private static final String A = "animation.wind_cat.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop(A + "run");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");

    private static final Vector3f MINT = new Vector3f(0.55F, 1.0F, 0.86F);

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        KAMAITACHI(20, 30, "kamaitachi"),
        TORNADO(32, 140, "tornado"),
        GUST(32, 120, "gust"),
        CHARGE(32, 80, "charge"),
        AIR_JUMP(28, 90, "air_jump"),
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
    private Vec3 chargeDir = Vec3.ZERO;
    private boolean landed;

    public WindCatEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN, BossEvent.BossBarColor.BLUE);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1600.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
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

    // ============================================================ AI
    @Override
    protected void bossAiStep() {
        LivingEntity target = this.getTarget();

        // 第2形態：周りを回る竜巻を2つ保つ
        if (isPhase2() && this.tickCount % 40 == 0 && attack != Attack.PHASE_CHANGE) {
            int have = TornadoEntity.countOrbiting(this);
            for (int i = have; i < 2; i++) TornadoEntity.spawnOrbit(this, i * Math.PI);
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

    private Attack chooseAttack(LivingEntity target) {
        double dist = Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ()))
                - (this.getBbWidth() + target.getBbWidth()) / 2.0;
        boolean sight = this.hasLineOfSight(target);
        List<Attack> pool = new ArrayList<>();
        if (dist < 4) {
            add(pool, Attack.GUST, 3);
            add(pool, Attack.KAMAITACHI, 3);
            add(pool, Attack.AIR_JUMP, 2);
        } else if (dist < 14) {
            if (sight) add(pool, Attack.KAMAITACHI, 3);
            add(pool, Attack.CHARGE, 3);
            add(pool, Attack.AIR_JUMP, 3);
            add(pool, Attack.TORNADO, 2);
        } else {
            if (sight) add(pool, Attack.KAMAITACHI, 3);
            add(pool, Attack.TORNADO, 3);
            add(pool, Attack.CHARGE, 2);
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
        this.landed = false;
        this.hitThisAttack.clear();
        this.getNavigation().stop();
        this.triggerAnim("main", a.name());
    }

    private void endAttack() {
        Attack done = this.attack;
        this.attack = Attack.NONE;
        this.lastAttack = done;
        this.cooldowns.put(done, done.cooldown);
        this.globalCooldown = done == Attack.PHASE_CHANGE ? 10 : (isPhase2() ? 10 : 18);
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case KAMAITACHI -> {                    // かまいたち  0.5 秒（第2形態は 0.75 秒にもう1発）
                if (t < 9) face(target, 30);
                if (t == 10 || (isPhase2() && t == 15)) {
                    Vec3 dir = target != null
                            ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    if (t == 15) dir = dir.yRot(8 * Mth.DEG_TO_RAD);
                    WindBladeEntity.shoot(this, this.position().add(dir.scale(1.5)), dir.scale(1.1), dmg(9, 11));
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 1.4F);
                    playSound(SoundEvents.BREEZE_SHOOT, 1.5F, 1.0F);
                }
            }
            case TORNADO -> {                       // 竜巻  0.9 秒に生まれて、ゆっくり追いかける
                if (t < 14) face(target, 20);
                if (t == 18) {
                    int n = isPhase2() ? 3 : 1;
                    for (int i = 0; i < n; i++) {
                        float ang = (i - (n - 1) / 2.0F) * 40.0F * Mth.DEG_TO_RAD;
                        Vec3 at = this.position().add(forward().yRot(ang).scale(3.5));
                        TornadoEntity.spawnSeeker(this, at.x, groundY(at.x, getY() + 2, at.z), at.z, target, dmg(10, 12));
                    }
                    playSound(SoundEvents.BREEZE_SHOOT, 2.0F, 0.6F);
                }
            }
            case GUST -> {                          // 突風  0.7〜1.3 秒、前方10ブロックを吹き飛ばす
                if (t < 12) face(target, 25);
                if (t == 14) {
                    for (LivingEntity e : inArc(10.0, 40)) {
                        if (damage(e, dmg(4, 6), 3.0)) launch(e, 0.4);
                    }
                    playSound(SoundEvents.BREEZE_DEFLECT, 2.0F, 0.6F);
                }
                if (t >= 14 && t <= 26 && level() instanceof ServerLevel sl) {
                    Vec3 f = forward();
                    for (int i = 0; i < 8; i++) {
                        Vec3 v = f.yRot((random.nextFloat() - 0.5F) * 1.2F).scale(0.8 + random.nextDouble() * 0.6);
                        sl.sendParticles(ParticleTypes.CLOUD, getX() + f.x * 2, getY() + 1.2 + random.nextDouble(), getZ() + f.z * 2,
                                0, v.x, 0.02, v.z, 1.0);
                    }
                }
            }
            case CHARGE -> {                        // 疾風の突進  0.5 秒から一直線に駆け抜ける
                if (t < 10) face(target, 25);
                if (t == 10) {
                    chargeDir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    playSound(SoundEvents.BREEZE_JUMP, 2.0F, 0.6F);
                }
                if (t >= 10 && t <= 24) {
                    this.setDeltaMovement(chargeDir.x * 1.4, this.getDeltaMovement().y, chargeDir.z * 1.4);
                    this.hasImpulse = true;
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8), this::canHit)) {
                        if (hitThisAttack.add(e.getUUID()) && damage(e, dmg(15, 18), 2.0)) launch(e, 0.4);
                    }
                    if (isPhase2() && t % 4 == 0) {
                        TornadoEntity.spawnTrail(this, getX(), groundY(getX(), getY() + 1, getZ()), getZ(), 5);
                    }
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.8, getZ(), 3, 0.6, 0.4, 0.6, 0.02);
                    }
                }
                if (t == 25) this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1, 0.2));
            }
            case AIR_JUMP -> {                      // 空中跳び  高く跳んで、空中でもう一度蹴って踏みつける
                if (t < 4) face(target, 30);
                if (t == 5) {
                    Vec3 d = target != null ? target.position().subtract(position()).multiply(1, 0, 1) : forward().scale(4);
                    Vec3 h = d.lengthSqr() > 1e-4 ? d.normalize().scale(Math.min(0.6, d.length() / 14)) : Vec3.ZERO;
                    this.setDeltaMovement(h.x, 1.0, h.z);
                    this.hasImpulse = true;
                    playSound(SoundEvents.BREEZE_JUMP, 2.0F, 1.0F);
                }
                if (t == 12) {
                    // 空中を蹴る：ターゲットの真上めがけて
                    Vec3 d = target != null ? target.position().subtract(position()).multiply(1, 0, 1) : Vec3.ZERO;
                    Vec3 h = d.lengthSqr() > 1e-4 ? d.normalize().scale(Math.min(1.2, d.length() / 6)) : Vec3.ZERO;
                    this.setDeltaMovement(h.x, 0.35, h.z);
                    this.hasImpulse = true;
                    playSound(SoundEvents.BREEZE_SHOOT, 1.5F, 1.4F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
                    }
                }
                if (t == 18) this.setDeltaMovement(this.getDeltaMovement().add(0, -1.2, 0));
                if (t > 18 && !landed && (this.onGround() || t >= 27)) {
                    landed = true;
                    for (LivingEntity e : inRadius(this.position(), 4.0, 3.0)) {
                        if (damage(e, dmg(16, 20), 1.6)) launch(e, 0.5);
                    }
                    landImpact();
                    shake(12);
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.4 秒で体が風になる
                if (t == 28) {
                    setPhase(2);
                    shake(15);
                    playSound(SoundEvents.BREEZE_SHOOT, 3.0F, 0.5F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
                        sl.sendParticles(new DustParticleOptions(MINT, 2.0F), getX(), getY() + 1.5, getZ(), 120, 2.5, 1.5, 2.5, 0);
                    }
                    for (LivingEntity e : inRadius(this.position(), 7.0, 4.0)) {
                        e.knockback(2.5, getX() - e.getX(), getZ() - e.getZ());
                        launch(e, 0.6);
                    }
                    TornadoEntity.spawnOrbit(this, 0);
                    TornadoEntity.spawnOrbit(this, Math.PI);
                }
            }
            default -> {}
        }
        if (attackTick >= attack.length) endAttack();
    }

    private void landImpact() {
        playSound(SoundEvents.BREEZE_LAND, 2.0F, 0.6F);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.4F);
        if (!(level() instanceof ServerLevel sl)) return;
        BlockState ground = level().getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
        if (!ground.isAir()) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.2, getZ(), 60, 2, 0.2, 2, 0.2);
        }
        sl.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        if (random.nextInt(isPhase2() ? 1 : 3) == 0) {
            level().addParticle(isPhase2() ? new DustParticleOptions(MINT, 1.0F) : ParticleTypes.CLOUD,
                    getX() + (random.nextDouble() - 0.5) * 2.5, getY() + random.nextDouble() * 2.2,
                    getZ() + (random.nextDouble() - 0.5) * 2.5, (random.nextDouble() - 0.5) * 0.05, 0.02, (random.nextDouble() - 0.5) * 0.05);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(ParticleTypes.CLOUD, getX() + (random.nextDouble() - 0.5) * 2.5, getY() + random.nextDouble() * 2,
                getZ() + (random.nextDouble() - 0.5) * 2.5, 0, 0.05, 0);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        if (!level().isClientSide) playSound(SoundEvents.OCELOT_DEATH, 2.0F, 0.5F);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.OCELOT_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.OCELOT_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 0.5F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<WindCatEntity> main = new AnimationController<>(this, "main", 3, state -> {
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
