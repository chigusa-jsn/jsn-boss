package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEffects;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
 * 鋼鉄の機甲蜘蛛（機械系ボス）。6本脚の蜘蛛型の機械。とても硬く、ノックバックしない。
 * 回転ノコギリ・ミサイル・火炎放射・主砲・ジャンプ踏みつけ・蒸気噴射。
 * 主砲とミサイルの後は「オーバーヒート」で止まり、その間は受けるダメージが2倍になる。
 * 第2形態（HP1000以下）「暴走」：光が赤くなり、動きが速く、オーバーヒートが短くなる。
 */
public class IronSpiderEntity extends AbstractBoss {
    private static final String A = "animation.iron_spider.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation SAWS = RawAnimation.begin().thenLoop(A + "saws");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");
    private static final Vector3f RED = new Vector3f(1.0F, 0.15F, 0.1F);
    private static final double CANNON_RANGE = 32.0;

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        SAW_SWEEP(30, 50, "saw_sweep"),
        MISSILES(44, 160, "missiles"),
        FLAMETHROWER(52, 140, "flamethrower"),
        MAIN_CANNON(56, 240, "main_cannon"),
        JUMP_SLAM(36, 120, "jump_slam"),
        STEAM(28, 120, "steam"),
        OVERHEAT(60, 0, "overheat"),
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
    private Vec3 cannonDir = Vec3.ZERO;

    public IronSpiderEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarColor.RED);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2000.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 2.0);
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

    /** オーバーヒート中は受けるダメージが2倍。自分のミサイルの爆発では傷つかない */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() == this) return false;
        return super.hurt(source, attack == Attack.OVERHEAT ? amount * 2.0F : amount);
    }

    @Override
    protected void onPhaseChanged(int phase) {
        if (phase >= 2 && this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
        }
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

        this.getLookControl().setLookAt(target, 20.0F, 30.0F);
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
        List<Attack> pool = new ArrayList<>();
        if (dist < 4) {
            add(pool, Attack.SAW_SWEEP, 4);
            add(pool, Attack.STEAM, 2);
            add(pool, Attack.FLAMETHROWER, 2);
        } else if (dist < 9) {
            add(pool, Attack.FLAMETHROWER, 3);
            add(pool, Attack.JUMP_SLAM, 3);
            add(pool, Attack.MISSILES, 2);
            add(pool, Attack.SAW_SWEEP, 1);
        } else {
            add(pool, Attack.MISSILES, 3);
            add(pool, Attack.MAIN_CANNON, 3);
            add(pool, Attack.JUMP_SLAM, 2);
        }
        if (pool.isEmpty()) return null;
        return pool.get(this.random.nextInt(pool.size()));
    }

    private void add(List<Attack> pool, Attack a, int weight) {
        if (cooldowns.getOrDefault(a, 0) > 0 || a == lastAttack) return;
        for (int i = 0; i < weight; i++) pool.add(a);
    }

    private int attackLength() {
        return attack == Attack.OVERHEAT && isPhase2() ? 36 : attack.length;
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
        this.lastAttack = done;
        this.cooldowns.put(done, done.cooldown);
        this.globalCooldown = done == Attack.PHASE_CHANGE || done == Attack.OVERHEAT ? 10 : (isPhase2() ? 24 : 40);
        // 主砲とミサイルの後はオーバーヒートで止まる
        if (done == Attack.MAIN_CANNON || done == Attack.MISSILES) startAttack(Attack.OVERHEAT);
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case SAW_SWEEP -> {                     // 回転ノコギリ  0.6〜0.9 秒に前を挟む
                if (t < 10) face(target, 25);
                if (t == 8) playSound(SoundEvents.GRINDSTONE_USE, 2.0F, 0.6F);
                if (t == 14) {
                    for (LivingEntity e : inArc(5.5, 80)) {
                        if (damage(e, dmg(14, 18), 0.8)) e.addEffect(new MobEffectInstance(ModEffects.LACERATION, 60, 0));
                    }
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
                    if (level() instanceof ServerLevel sl) {
                        Vec3 c = this.position().add(forward().scale(3.5));
                        sl.sendParticles(ParticleTypes.CRIT, c.x, getY() + 1.2, c.z, 30, 1.5, 0.4, 1.5, 0.3);
                    }
                }
            }
            case MISSILES -> {                      // ミサイル連射  0.6〜1.6 秒に両肩から交互に
                face(target, 15);
                int every = isPhase2() ? 2 : 4;
                int count = isPhase2() ? 10 : 6;
                if (t >= 12 && (t - 12) % every == 0 && (t - 12) / every < count) {
                    int i = (t - 12) / every;
                    Vec3 f = forward();
                    Vec3 side = new Vec3(-f.z, 0, f.x).scale(i % 2 == 0 ? 0.9 : -0.9);
                    Vec3 pos = this.position().add(side).add(0, 2.4, 0);
                    Vec3 vel = side.scale(0.2).add(0, 0.7, 0).add(f.scale(0.15));
                    MissileEntity.launch(this, pos, vel, target, dmg(8, 10));
                    playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 2.0F, 0.6F);
                }
            }
            case FLAMETHROWER -> {                  // 火炎放射  0.6〜2.2 秒、前方 8 ブロック
                face(target, 5);
                if (t >= 12 && t <= 44) {
                    Vec3 f = forward();
                    Vec3 nozzle = this.position().add(f.scale(1.3)).add(0, 2.0, 0);
                    if (level() instanceof ServerLevel sl) {
                        for (int i = 0; i < 6; i++) {
                            Vec3 v = f.scale(0.5 + random.nextDouble() * 0.4).add((random.nextDouble() - 0.5) * 0.3,
                                    (random.nextDouble() - 0.7) * 0.2, (random.nextDouble() - 0.5) * 0.3);
                            sl.sendParticles(i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME,
                                    nozzle.x, nozzle.y, nozzle.z, 0, v.x, v.y, v.z, 1.0);
                        }
                    }
                    if (t % 5 == 0) {
                        for (LivingEntity e : inArc(8.5, 28)) {
                            if (e.hurt(this.damageSources().mobAttack(this), dmg(4, 5))) e.igniteForSeconds(4);
                        }
                    }
                    if (t % 8 == 0) playSound(SoundEvents.FIRECHARGE_USE, 1.5F, 0.6F);
                }
            }
            case MAIN_CANNON -> {                   // 主砲  2 秒間赤いレーザーで狙い、2.0 秒に太いビーム
                Vec3 eye = eyePos();
                if (t < 36) {
                    face(target, 4);
                    if (target != null) cannonDir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye).normalize();
                    if (t % 2 == 0) drawLine(eye, beamEnd(eye), new DustParticleOptions(RED, 0.8F), 0.8);
                    if (t % 10 == 0) playSound(SoundEvents.BEACON_AMBIENT, 2.0F, 1.0F + t * 0.03F);
                }
                if (t == 36) playSound(SoundEvents.BEACON_POWER_SELECT, 3.0F, 2.0F);
                if (t == 40) {
                    Vec3 end = beamEnd(eye);
                    drawLine(eye, end, new DustParticleOptions(RED, 3.0F), 0.3);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 3, 0.5, 0.5, 0.5, 0);
                        double len = end.distanceTo(eye);
                        for (double d = 1; d < len; d += 1.0) {
                            Vec3 p = eye.add(cannonDir.scale(d));
                            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.2, 0.2, 0.2, 0.02);
                        }
                    }
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.5), this::canHit)) {
                        if (e.getBoundingBox().inflate(0.6).clip(eye, end).isPresent() && damage(e, dmg(24, 28), 1.5)) e.igniteForSeconds(3);
                    }
                    playSound(SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 1.4F);
                    playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 2.0F, 0.6F);
                    shake(10);
                }
            }
            case JUMP_SLAM -> {                     // ジャンプ踏みつけ  0.5 秒に跳んで 1.0 秒に着地
                if (t < 8) face(target, 30);
                if (t == 9) {
                    Vec3 d = target != null ? target.position().subtract(position()) : forward().scale(6);
                    double h = Math.sqrt(d.x * d.x + d.z * d.z);
                    double sp = Math.min(1.2, h / 11.0);
                    Vec3 dir = h > 0.01 ? new Vec3(d.x / h, 0, d.z / h) : forward();
                    this.setDeltaMovement(dir.x * sp, 0.95, dir.z * sp);
                    this.hasImpulse = true;
                    playSound(SoundEvents.PISTON_EXTEND, 2.0F, 0.5F);
                }
                if (t == 20) {
                    for (LivingEntity e : inRadius(this.position(), 5.0, 3.0)) {
                        if (damage(e, dmg(18, 22), 1.5)) launch(e, 0.5);
                    }
                    shake(12);
                    playSound(SoundEvents.ANVIL_LAND, 2.0F, 0.5F);
                    playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.8F);
                    if (level() instanceof ServerLevel sl) {
                        BlockState g = level().getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
                        if (!g.isAir()) {
                            sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, g),
                                    getX(), getY() + 0.2, getZ(), 60, 2.5, 0.2, 2.5, 0.3);
                        }
                        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 20, 2.5, 0.1, 2.5, 0.05);
                    }
                }
            }
            case STEAM -> {                         // 蒸気噴射  0.5 秒に周りへ吹き出す
                if (t == 10) {
                    for (LivingEntity e : inRadius(this.position(), 6.0, 4.0)) {
                        if (damage(e, dmg(6, 8), 3.0)) {
                            launch(e, 0.5);
                            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                        }
                    }
                    playSound(SoundEvents.FIRE_EXTINGUISH, 3.0F, 0.5F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.5, getZ(), 120, 3, 1, 3, 0.15);
                    }
                }
            }
            case OVERHEAT -> {                      // オーバーヒート  蒸気を吹いて止まる（ダメージ2倍）
                if (t == 1) playSound(SoundEvents.FIRE_EXTINGUISH, 2.5F, 0.4F);
                if (t % 4 == 0 && level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2.4, getZ(), 6, 1.2, 0.3, 1.2, 0.05);
                    sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.5, getZ(), 1, 1, 0.3, 1, 0);
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.4 秒に光が赤くなる
                if (t == 2) playSound(SoundEvents.BEACON_DEACTIVATE, 3.0F, 0.5F);
                if (t == 28) {
                    setPhase(2);
                    shake(20);
                    playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 0.6F);
                    playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.6F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(new DustParticleOptions(RED, 2.0F), getX(), getY() + 2, getZ(), 80, 2, 1, 2, 0);
                        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2, getZ(), 80, 2.5, 1, 2.5, 0.1);
                    }
                    for (LivingEntity e : inRadius(this.position(), 7.0, 4.0)) {
                        e.knockback(2.5, getX() - e.getX(), getZ() - e.getZ());
                        launch(e, 0.5);
                    }
                }
            }
            default -> {}
        }
        if (attackTick >= attackLength()) endAttack();
    }

    private Vec3 eyePos() {
        return this.position().add(forward().scale(1.1)).add(0, 2.1, 0);
    }

    private Vec3 beamEnd(Vec3 eye) {
        Vec3 dir = cannonDir.lengthSqr() > 1e-4 ? cannonDir : forward();
        Vec3 end = eye.add(dir.scale(CANNON_RANGE));
        return level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
    }

    private void drawLine(Vec3 a, Vec3 b, DustParticleOptions dust, double step) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 d = b.subtract(a);
        double len = d.length();
        Vec3 n = d.normalize();
        for (double s = 0.5; s < len; s += step) {
            Vec3 p = a.add(n.scale(s));
            sl.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        if (!level().isClientSide) {
            playSound(SoundEvents.BEACON_DEACTIVATE, 3.0F, 0.4F);
            Blasts.blast(level(), this, this.position().add(0, 1, 0), 4.0, 0.0F);
        }
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        if (random.nextInt(isPhase2() ? 2 : 5) == 0) {      // 背中の排気筒から煙
            Vec3 back = new Vec3(Mth.sin(yBodyRot * Mth.DEG_TO_RAD), 0, -Mth.cos(yBodyRot * Mth.DEG_TO_RAD)).scale(0.5);
            level().addParticle(isPhase2() ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                    getX() + back.x + (random.nextDouble() - 0.5) * 0.6, getY() + 2.5, getZ() + back.z + (random.nextDouble() - 0.5) * 0.6, 0, 0.08, 0);
        }
        if (isPhase2() && random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5) * 3, getY() + 1 + random.nextDouble(),
                    getZ() + (random.nextDouble() - 0.5) * 3, 0, 0, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2,
                getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.05, 0);
        if (random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2,
                    getZ() + (random.nextDouble() - 0.5) * 3, 0, 0, 0);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.5F, 1.4F);
    }

    @Override
    protected SoundEvent getAmbientSound() { return null; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 1.3F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<IronSpiderEntity> main = new AnimationController<>(this, "main", 3, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DEATH);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        });
        for (Attack a : Attack.values()) {
            if (a.anim != null) main.triggerableAnim(a.name(), RawAnimation.begin().thenPlay(A + a.anim));
        }
        main.triggerableAnim("death", DEATH);
        controllers.add(main);
        // ノコギリは止まるまでずっと回り続ける
        controllers.add(new AnimationController<>(this, "saws", 0, state -> this.isDeadOrDying() ? software.bernie.geckolib.animation.PlayState.STOP : state.setAndContinue(SAWS)));
    }
}
