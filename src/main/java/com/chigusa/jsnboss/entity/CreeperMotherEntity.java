package com.chigusa.jsnboss.entity;

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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * マザークリーパー。苔とツタに覆われた大きなクリーパーで、爆発で戦う。爆発は地形を壊さない。
 * 自爆（本人は死なない）・子クリーパー召喚・地雷まき・TNT投げ・体当たり。
 * 第2形態（HP800以下）「臨界」：白く光るひびが走る。自爆が2連続になり、連鎖爆発が増える。
 */
public class CreeperMotherEntity extends AbstractBoss {
    /** 呼び出した子クリーパーにつけるタグ。この子たちの爆発はブロックを壊さず、ボスにも当たらない（BossEvents） */
    public static final String KID_TAG = "jsnboss_creeper_kid";
    private static final int MAX_KIDS = 6;

    private static final String A = "animation.creeper_mother.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");
    private static final Vector3f HOT = new Vector3f(1.0F, 0.9F, 0.5F);

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        SWELL(44, 140, "swell"),
        SPAWN_KIDS(32, 300, "spawn_kids"),
        MINES(32, 140, "mines"),
        THROW_TNT(32, 100, "throw_tnt"),
        TACKLE(36, 80, "tackle"),
        CHAIN(32, 140, "chain"),
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

    public CreeperMotherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN, BossEvent.BossBarColor.YELLOW);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1600.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
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

    /** 爆発（自分の子どもの爆発も）では傷つかない */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) return false;
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
        if (countKids() < MAX_KIDS - 2) add(pool, Attack.SPAWN_KIDS, 2);
        if (dist < 4) {
            add(pool, Attack.SWELL, 4);
            add(pool, Attack.MINES, 2);
            if (isPhase2()) add(pool, Attack.CHAIN, 1);
        } else if (dist < 12) {
            add(pool, Attack.TACKLE, 3);
            add(pool, Attack.THROW_TNT, 3);
            add(pool, Attack.MINES, 1);
            if (isPhase2()) add(pool, Attack.CHAIN, 3);
        } else {
            add(pool, Attack.THROW_TNT, 3);
            add(pool, Attack.TACKLE, 2);
            if (isPhase2()) add(pool, Attack.CHAIN, 2);
        }
        if (pool.isEmpty()) return null;
        return pool.get(this.random.nextInt(pool.size()));
    }

    private void add(List<Attack> pool, Attack a, int weight) {
        if (cooldowns.getOrDefault(a, 0) > 0 || a == lastAttack) return;
        for (int i = 0; i < weight; i++) pool.add(a);
    }

    private int attackLength() {
        return attack == Attack.SWELL && isPhase2() ? 78 : attack.length;
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
        this.globalCooldown = done == Attack.PHASE_CHANGE ? 10 : (isPhase2() ? 26 : 44);
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case SWELL -> {                         // 自爆  1.5 秒ふくらんで大爆発（第2形態はもう一度）
                if (t == 1 || t == 35) playSound(SoundEvents.CREEPER_PRIMED, 2.0F, 0.5F);
                if (t == 34 && isPhase2()) this.triggerAnim("main", Attack.SWELL.name());
                if (t == 30 || (t == 64 && isPhase2())) {
                    Blasts.blast(level(), this, this.position().add(0, 1, 0), 6.0, dmg(20, 26));
                    shake(15);
                }
            }
            case SPAWN_KIDS -> {                    // 子クリーパー召喚  0.9 秒に産む
                if (t == 18 && level() instanceof ServerLevel sl) {
                    int n = Math.min(isPhase2() ? 4 : 3, MAX_KIDS - countKids());
                    for (int i = 0; i < n; i++) {
                        Creeper kid = EntityType.CREEPER.create(sl);
                        if (kid == null) continue;
                        double a = Math.PI * 2 * i / n + random.nextDouble(), r = 2.5;
                        kid.moveTo(getX() + Math.cos(a) * r, getY() + 0.5, getZ() + Math.sin(a) * r, random.nextFloat() * 360F, 0);
                        kid.finalizeSpawn(sl, sl.getCurrentDifficultyAt(kid.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                        kid.addTag(KID_TAG);
                        kid.setDeltaMovement(Math.cos(a) * 0.4, 0.5, Math.sin(a) * 0.4);
                        if (isPhase2()) kid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60, 1));
                        if (target != null) kid.setTarget(target);
                        sl.addFreshEntity(kid);
                        sl.sendParticles(ParticleTypes.POOF, kid.getX(), kid.getY() + 0.5, kid.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                    }
                    playSound(SoundEvents.SLIME_SQUISH, 2.0F, 0.5F);
                }
            }
            case MINES -> {                         // 地雷まき  0.7 秒にばらまく
                if (t == 14) {
                    int n = isPhase2() ? 8 : 5;
                    for (int i = 0; i < n; i++) {
                        double a = Math.PI * 2 * i / n + random.nextDouble() * 0.4, sp = 0.25 + random.nextDouble() * 0.2;
                        MineEntity.toss(this, this.position().add(0, 3.0, 0), new Vec3(Math.cos(a) * sp, 0.45, Math.sin(a) * sp), dmg(10, 12));
                    }
                    playSound(SoundEvents.DISPENSER_LAUNCH, 1.5F, 0.7F);
                }
            }
            case THROW_TNT -> {                     // TNT 投げ  0.8 秒に背中から放り出す
                if (t < 14) face(target, 25);
                if (t == 16 && target != null) {
                    int n = isPhase2() ? 5 : 3;
                    Vec3 from = this.position().add(0, 3.5, 0).subtract(forward().scale(0.5));
                    for (int i = 0; i < n; i++) {
                        Vec3 aim = target.position().add(target.getDeltaMovement().scale(12));
                        if (i > 0) {
                            double a = random.nextDouble() * Math.PI * 2, r = 2 + random.nextDouble() * 3;
                            aim = aim.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                        }
                        BoulderEntity.throwTnt(this, from, aim, dmg(12, 14));
                    }
                    playSound(SoundEvents.TNT_PRIMED, 2.0F, 0.8F);
                }
            }
            case TACKLE -> {                        // 体当たり  0.5〜1.4 秒
                if (t < 10) face(target, 25);
                if (t == 10) rushDir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                if (t >= 10 && t <= 28) {
                    this.setDeltaMovement(rushDir.x * 0.85, this.getDeltaMovement().y, rushDir.z * 0.85);
                    this.hasImpulse = true;
                    for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.6), this::canHit)) {
                        if (hitThisAttack.add(e.getUUID()) && damage(e, dmg(14, 18), 2.0)) launch(e, 0.4);
                    }
                }
                if (t == 29) this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1, 0.2));
            }
            case CHAIN -> {                         // 連鎖爆発（第2形態）  0.7 秒に着地すると爆発が一列に走る
                if (t < 10) face(target, 30);
                if (t == 14) {
                    Vec3 dir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    ChainBlastEntity.spawn(this, this.position(), dir, 16.0F);
                    ChainBlastEntity.spawn(this, this.position(), dir.yRot(30 * Mth.DEG_TO_RAD), 16.0F);
                    ChainBlastEntity.spawn(this, this.position(), dir.yRot(-30 * Mth.DEG_TO_RAD), 16.0F);
                    Blasts.blast(level(), this, this.position(), 3.0, 10.0F);
                    shake(8);
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.4 秒に破裂寸前まで膨らむ
                if (t == 2) playSound(SoundEvents.CREEPER_PRIMED, 3.0F, 0.4F);
                if (t == 28) {
                    setPhase(2);
                    shake(20);
                    Blasts.blast(level(), this, this.position().add(0, 1, 0), 5.0, 0.0F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(new DustParticleOptions(HOT, 2.0F), getX(), getY() + 2, getZ(), 80, 1.5, 1.5, 1.5, 0);
                    }
                    for (LivingEntity e : inRadius(this.position(), 6.0, 4.0)) {
                        e.knockback(2.0, getX() - e.getX(), getZ() - e.getZ());
                        launch(e, 0.5);
                    }
                }
            }
            default -> {}
        }
        if (attackTick >= attackLength()) endAttack();
    }

    private int countKids() {
        return level().getEntitiesOfClass(Creeper.class, this.getBoundingBox().inflate(32), c -> c.isAlive() && c.getTags().contains(KID_TAG)).size();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        if (!level().isClientSide) {
            playSound(SoundEvents.CREEPER_DEATH, 3.0F, 0.4F);
            Blasts.blast(level(), this, this.position().add(0, 1, 0), 4.0, 0.0F);
        }
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        if (isPhase2()) {
            if (random.nextInt(3) == 0) {
                level().addParticle(new DustParticleOptions(HOT, 1.2F), getX() + (random.nextDouble() - 0.5) * 2.4,
                        getY() + random.nextDouble() * 4, getZ() + (random.nextDouble() - 0.5) * 2.4, 0, 0.02, 0);
            }
            if (random.nextInt(8) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * 2, getY() + 3.5, getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.05, 0);
            }
        }
        if (random.nextInt(4) == 0) {                       // 背中の導火線の火花
            Vec3 back = new Vec3(Mth.sin(yBodyRot * Mth.DEG_TO_RAD), 0, -Mth.cos(yBodyRot * Mth.DEG_TO_RAD)).scale(1.1);
            level().addParticle(ParticleTypes.SMALL_FLAME, getX() + back.x + (random.nextDouble() - 0.5),
                    getY() + 3.0 + random.nextDouble() * 0.5, getZ() + back.z + (random.nextDouble() - 0.5), 0, 0.02, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2,
                getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.05, 0);
    }

    @Override
    protected SoundEvent getAmbientSound() { return null; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.CREEPER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 0.5F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<CreeperMotherEntity> main = new AnimationController<>(this, "main", 3, state -> {
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
