package com.chigusa.jsnboss.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 岩の巨人。苔むした岩の巨人で、重くて遅いがとても硬い。ノックバックしない。
 * 叩きつけ・薙ぎ払い・岩の柱・大岩投げ・地割れで戦う。
 * 第2形態（HP1100以下）：割れ目からマグマが光り、落石と怒りの連打が増える。地割れの後にマグマが残る。
 */
public class RockGiantEntity extends AbstractBoss {
    private static final String A = "animation.rock_giant.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        SLAM(40, 60, "slam"),
        SWEEP(36, 70, "sweep"),
        PILLARS(36, 120, "pillars"),
        THROW(48, 120, "throw"),
        FISSURE(32, 90, "fissure"),
        ROCKFALL(44, 260, "rockfall"),
        FURY(60, 200, "fury"),
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

    public RockGiantEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN, BossEvent.BossBarColor.RED);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2200.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
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
    protected boolean isInvulnerableNow() {
        return attack == Attack.PHASE_CHANGE;
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
        if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
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
        if (isPhase2()) add(pool, Attack.ROCKFALL, 2);
        if (dist < 4.5) {
            add(pool, Attack.SLAM, 3);
            add(pool, Attack.SWEEP, 3);
            if (isPhase2()) add(pool, Attack.FURY, 3);
            add(pool, Attack.FISSURE, 1);
        } else if (dist < 14) {
            add(pool, Attack.PILLARS, 3);
            add(pool, Attack.FISSURE, 3);
            add(pool, Attack.THROW, 2);
        } else {
            add(pool, Attack.THROW, 3);
            add(pool, Attack.PILLARS, 2);
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
        this.lastAttack = done;
        this.cooldowns.put(done, done.cooldown);
        this.globalCooldown = done == Attack.PHASE_CHANGE ? 10 : (isPhase2() ? 30 : 50);
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case SLAM -> {                          // 両こぶし叩きつけ  1.05 秒
                if (t < 16) face(target, 15);
                if (t == 21) {
                    hitArc(5.5, 65, dmg(18, 22), 1.5);
                    groundImpact(forward().scale(3.5), 2.5F, 12);
                }
            }
            case SWEEP -> {                         // 薙ぎ払い  0.85 秒
                if (t < 10) face(target, 20);
                if (t == 17) {
                    for (LivingEntity e : inRadius(this.position(), 5.5, 4.0)) {
                        if (damage(e, dmg(14, 18), 3.0)) launch(e, 0.4);
                    }
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
                }
            }
            case PILLARS -> {                       // 岩の柱  0.8 秒に地面を突く
                if (t < 12) face(target, 20);
                if (t == 16) {
                    groundImpact(forward().scale(3.0), 1.5F, 6);
                    Vec3 dir = target != null
                            ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    int lines = isPhase2() ? 3 : 1;
                    for (int l = 0; l < lines; l++) {
                        Vec3 d = dir.yRot((l - (lines - 1) / 2.0F) * 25.0F * Mth.DEG_TO_RAD);
                        for (int i = 0; i < 12; i++) {
                            double dist = 3.0 + i * 1.4;
                            RockPillarEntity.spawn(this, getX() + d.x * dist, getZ() + d.z * dist, 8 + i * 2, dmg(14, 17));
                        }
                    }
                }
            }
            case THROW -> {                         // 大岩投げ  1.5 秒に投げる
                face(target, 15);
                if (t == 12) groundImpact(forward().scale(2.5), 1.5F, 0);
                if (t == 30 && target != null) {
                    Vec3 from = this.position().add(0, getBbHeight() + 1.0, 0).add(forward().scale(1.0));
                    Vec3 aim = target.position().add(target.getDeltaMovement().scale(10));
                    BoulderEntity.throwAt(this, from, aim, dmg(16, 20), isPhase2());
                    playSound(SoundEvents.IRON_GOLEM_ATTACK, 2.0F, 0.5F);
                }
            }
            case FISSURE -> {                       // 地割れ  0.8 秒に踏みつけ
                if (t < 10) face(target, 25);
                if (t == 16) {
                    Vec3 dir = target != null
                            ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    FissureEntity.spawn(this, this.position().add(dir.scale(2.0)), dir, dmg(12, 15), isPhase2());
                    groundImpact(Vec3.ZERO, 2.0F, 8);
                }
            }
            case ROCKFALL -> {                      // 落石（第2形態）  1.0 秒に胸を叩くと岩が降ってくる
                if (t == 14) playSound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.5F);
                if (t == 20) {
                    shake(10);
                    List<Player> players = level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24, 10, 24),
                            p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
                    int n = 0;
                    for (Player p : players) {
                        BoulderEntity.drop(this, p.getX(), p.getZ(), 14, 12.0F);
                        n++;
                    }
                    for (; n < 8; n++) {
                        double a = random.nextDouble() * Math.PI * 2, r = 3 + random.nextDouble() * 9;
                        Vec3 c = target != null ? target.position() : this.position();
                        BoulderEntity.drop(this, c.x + Math.cos(a) * r, c.z + Math.sin(a) * r, 14 + random.nextDouble() * 6, 12.0F);
                    }
                }
            }
            case FURY -> {                          // 怒りの連打（第2形態）  0.7 / 1.4 / 2.3 秒
                if (t < 12 || (t > 16 && t < 26) || (t > 30 && t < 42)) face(target, 25);
                if (t == 14 || t == 28) {
                    hitArc(5.5, 60, 16.0F, 1.2);
                    groundImpact(forward().scale(3.5), 2.0F, 8);
                }
                if (t == 46) {
                    hitArc(6.0, 70, 16.0F, 2.0);
                    groundImpact(forward().scale(3.5), 3.0F, 14);
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.6 秒に吠えるとマグマが噴き出す
                if (t == 32) {
                    setPhase(2);
                    shake(20);
                    playSound(SoundEvents.RAVAGER_ROAR, 4.0F, 0.4F);
                    playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.6F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 3, getZ(), 60, 2.0, 1.5, 2.0, 0);
                        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2, getZ(), 60, 2.5, 2.0, 2.5, 0.05);
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

    /** 地面を叩いた衝撃：offset の位置に砂ぼこりと揺れ、半径 radius の小さな衝撃（damage 0 なら見た目だけ） */
    private void groundImpact(Vec3 offset, float radius, float damage) {
        Vec3 c = this.position().add(offset);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.6F);
        playSound(SoundEvents.IRON_GOLEM_HURT, 1.5F, 0.4F);
        shake(8);
        if (damage > 0) {
            for (LivingEntity e : inRadius(c, radius, 2.0)) {
                if (damage(e, dmg(damage, damage * 1.25F), 0.5)) launch(e, 0.4);
            }
        }
        if (!(level() instanceof ServerLevel sl)) return;
        BlockState ground = level().getBlockState(BlockPos.containing(c.x, getY() - 0.5, c.z));
        if (ground.isAir()) ground = Blocks.STONE.defaultBlockState();
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), c.x, getY() + 0.2, c.z, 60, radius * 0.6, 0.2, radius * 0.6, 0.25);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, getY() + 0.3, c.z, 8, radius * 0.5, 0.1, radius * 0.5, 0.02);
        if (isPhase2()) sl.sendParticles(ParticleTypes.LAVA, c.x, getY() + 0.3, c.z, 8, radius * 0.4, 0.1, radius * 0.4, 0);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        if (!level().isClientSide) playSound(SoundEvents.IRON_GOLEM_DEATH, 2.0F, 0.4F);
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        if (isPhase2()) {
            if (random.nextInt(6) == 0) {
                level().addParticle(ParticleTypes.LAVA, getX() + (random.nextDouble() - 0.5) * 2.5, getY() + 1 + random.nextDouble() * 3,
                        getZ() + (random.nextDouble() - 0.5) * 2.5, 0, 0, 0);
            }
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * 2.5, getY() + 2 + random.nextDouble() * 2.5,
                        getZ() + (random.nextDouble() - 0.5) * 2.5, 0, 0.03, 0);
            }
        } else if (random.nextInt(10) == 0) {
            level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX() + (random.nextDouble() - 0.5) * 2.0, getY() + 3.5 + random.nextDouble(),
                    getZ() + (random.nextDouble() - 0.5) * 2.0, 0, 0, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 3, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0, 0);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 2.0F, 0.5F);
    }

    @Override
    protected SoundEvent getAmbientSound() { return null; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 0.5F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<RockGiantEntity> main = new AnimationController<>(this, "main", 4, state -> {
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
