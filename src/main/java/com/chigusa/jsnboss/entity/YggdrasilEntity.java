package com.chigusa.jsnboss.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Bogged;
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
 * ユグドラシル（高精細モデル）。一歩も動かない巨大な世界樹。とても硬く、遠くまで攻撃が届く。
 * 周りに浮かぶ黄金の実（GoldenFruitEntity）が毎秒 HP を回復するので、まず実を壊すのが攻略のコツ。
 * 第2形態（HP1500以下）「怒れる世界樹」：葉が紅葉し、光が赤橙に。実が2つ生え直し、大地の脈動を使う。
 */
public class YggdrasilEntity extends AbstractBoss {
    public static final String GUARD_TAG = "jsnboss_ygg_guard";
    private static final int MAX_GUARDS = 4;

    private static final String A = "animation.yggdrasil.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");
    private static final Vector3f GREEN = new Vector3f(0.48F, 1.0F, 0.8F);
    private static final Vector3f EMBER = new Vector3f(1.0F, 0.5F, 0.15F);

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        SLAM(40, 60, "slam"),
        ROOT_SPIKES(36, 80, "root_spikes"),
        ROOT_SWEEP(36, 120, "root_sweep"),
        POLLEN(36, 160, "pollen"),
        SEEDS(32, 100, "seeds"),
        SUMMON(36, 400, "summon"),
        PULSE(44, 240, "pulse"),
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
    /** 生やす黄金の実の数（出現時 4、第2形態で +2）。読み込み直したときにも使う */
    private int fruitsToSpawn = 4;

    public YggdrasilEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN, BossEvent.BossBarColor.RED);
        this.xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 3000.0)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected boolean isInvulnerableNow() {
        return attack == Attack.PHASE_CHANGE;
    }

    /** 自分の番兵や黄金の実には攻撃を当てない */
    @Override
    protected boolean canHit(LivingEntity e) {
        return super.canHit(e) && !e.getTags().contains(GUARD_TAG) && !(e instanceof GoldenFruitEntity);
    }

    /** 木なので毒や衰弱などの悪い効果は効かない */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL && super.canBeAffected(effect);
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public void push(double x, double y, double z) {}

    // ============================================================ AI
    @Override
    protected void bossAiStep() {
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);    // 一歩も動かない
        if (fruitsToSpawn > 0) {
            for (int i = 0; i < fruitsToSpawn; i++) GoldenFruitEntity.spawn(this, Math.PI * 2 * i / fruitsToSpawn + random.nextDouble() * 0.3);
            fruitsToSpawn = 0;
        }
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
        face(target, 2);                                           // ゆっくり向きを変える
        if (globalCooldown <= 0) {
            Attack next = chooseAttack(target);
            if (next != null) startAttack(next);
        }
    }

    private Attack chooseAttack(LivingEntity target) {
        double dist = Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ())) - this.getBbWidth() / 2.0;
        List<Attack> pool = new ArrayList<>();
        if (countGuards() < MAX_GUARDS - 1) add(pool, Attack.SUMMON, 1);
        if (isPhase2()) add(pool, Attack.PULSE, 2);
        if (dist < 7) {
            add(pool, Attack.SLAM, 4);
            add(pool, Attack.ROOT_SWEEP, 3);
            add(pool, Attack.POLLEN, 2);
        } else {
            add(pool, Attack.ROOT_SPIKES, 4);
            add(pool, Attack.SEEDS, 3);
            add(pool, Attack.POLLEN, 2);
            if (dist < 12) add(pool, Attack.ROOT_SWEEP, 1);
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
        this.triggerAnim("main", a.name());
    }

    private void endAttack() {
        Attack done = this.attack;
        this.attack = Attack.NONE;
        this.lastAttack = done;
        this.cooldowns.put(done, done.cooldown);
        this.globalCooldown = done == Attack.PHASE_CHANGE ? 10 : (isPhase2() ? 24 : 40);
    }

    private List<Player> players(double r) {
        return level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(r, 12, r),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
    }

    private List<LivingEntity> victims(LivingEntity target, double r) {
        List<LivingEntity> v = new ArrayList<>(players(r));
        if (v.isEmpty() && target != null) v.add(target);
        return v;
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case SLAM -> {                          // 枝の叩きつけ  0.9 秒に前方を叩く
                if (t < 12) face(target, 6);
                if (t == 18) {
                    for (LivingEntity e : inArc(9.0, 60)) {
                        if (damage(e, dmg(20, 24), 1.5)) launch(e, 0.6);
                    }
                    shake(12);
                    playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.6F);
                    playSound(SoundEvents.WOOD_BREAK, 3.0F, 0.4F);
                    if (level() instanceof ServerLevel sl) {
                        Vec3 c = this.position().add(forward().scale(6));
                        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, getY() + 0.3, c.z, 12, 2, 0.1, 2, 0.02);
                        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                                net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState()), c.x, getY() + 0.3, c.z, 50, 2.5, 0.2, 2.5, 0.2);
                    }
                }
            }
            case ROOT_SPIKES -> {                   // 根の突き上げ  0.8 秒に爪を地面へ刺すと、足元から根が出る
                if (t == 16) {
                    for (LivingEntity v : victims(target, 32)) {
                        Vec3 p = v.position().add(v.getDeltaMovement().scale(6));
                        RockPillarEntity.spawnRoot(this, p.x, p.z, 16, dmg(16, 20));
                        if (isPhase2()) {
                            for (int i = 0; i < 2; i++) {
                                double a = random.nextDouble() * Math.PI * 2, r = 1.8 + random.nextDouble() * 1.5;
                                RockPillarEntity.spawnRoot(this, p.x + Math.cos(a) * r, p.z + Math.sin(a) * r, 20 + i * 4, 20.0F);
                            }
                        }
                    }
                    playSound(SoundEvents.ROOTED_DIRT_BREAK, 3.0F, 0.5F);
                }
            }
            case ROOT_SWEEP -> {                    // 根のなぎ払い  地面を走る輪（ジャンプで避ける）
                if (t == 12) {
                    ShockwaveEntity.spawn(this, getX(), getY(), getZ(), 13.0F, 0.5F, dmg(12, 15));
                    playSound(SoundEvents.ROOTED_DIRT_BREAK, 3.0F, 0.4F);
                    shake(8);
                }
            }
            case POLLEN -> {                        // 毒の花粉  プレイヤーの足元に毒の霧
                if (t == 16) {
                    for (LivingEntity v : victims(target, 28)) {
                        AreaEffectCloud cloud = new AreaEffectCloud(level(), v.getX(), v.getY(), v.getZ());
                        cloud.setOwner(this);
                        cloud.setRadius(3.0F);
                        cloud.setDuration(120);
                        cloud.setWaitTime(10);
                        cloud.setRadiusPerTick(-0.01F);
                        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                        if (isPhase2()) cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
                        level().addFreshEntity(cloud);
                    }
                    playSound(SoundEvents.SPORE_BLOSSOM_BREAK, 3.0F, 0.5F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 5, getZ(), 80, 4, 1.5, 4, 0.05);
                    }
                }
            }
            case SEEDS -> {                         // 種の爆弾  冠から種を放り投げる
                if (t == 16) {
                    int n = isPhase2() ? 8 : 5;
                    List<LivingEntity> vs = victims(target, 32);
                    Vec3 from = this.position().add(0, 5.5, 0);
                    for (int i = 0; i < n && !vs.isEmpty(); i++) {
                        LivingEntity v = vs.get(i % vs.size());
                        double a = random.nextDouble() * Math.PI * 2, r = i < vs.size() ? 0 : 1.5 + random.nextDouble() * 4;
                        BoulderEntity.throwSeed(this, from, v.position().add(Math.cos(a) * r, 0, Math.sin(a) * r), dmg(10, 12));
                    }
                    playSound(SoundEvents.AZALEA_LEAVES_BREAK, 3.0F, 0.5F);
                }
            }
            case SUMMON -> {                        // 森の番兵召喚  根元から苔むしたスケルトン（ボグド）
                if (t == 20 && level() instanceof ServerLevel sl) {
                    int n = Math.min(isPhase2() ? 4 : 2, MAX_GUARDS - countGuards());
                    for (int i = 0; i < n; i++) {
                        Bogged b = EntityType.BOGGED.create(sl);
                        if (b == null) continue;
                        double a = Math.PI * 2 * i / Math.max(1, n) + random.nextDouble(), r = 4.5;
                        double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
                        b.moveTo(x, groundY(x, getY() + 3, z), z, random.nextFloat() * 360F, 0);
                        b.finalizeSpawn(sl, sl.getCurrentDifficultyAt(b.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                        b.addTag(GUARD_TAG);
                        if (target != null) b.setTarget(target);
                        sl.addFreshEntity(b);
                        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                                net.minecraft.world.level.block.Blocks.ROOTED_DIRT.defaultBlockState()), x, b.getY() + 0.5, z, 20, 0.4, 0.5, 0.4, 0.1);
                    }
                    playSound(SoundEvents.BOGGED_AMBIENT, 2.0F, 0.6F);
                }
            }
            case PULSE -> {                         // 大地の脈動（第2形態）  根の輪が外へ何重にも広がる
                if (t == 20) {
                    for (int ring = 0; ring < 5; ring++) {
                        double r = 4 + ring * 3.2;
                        int n = (int) (r * 2.2);
                        double off = random.nextDouble();
                        for (int i = 0; i < n; i++) {
                            double a = Math.PI * 2 * (i + off) / n;
                            RockPillarEntity.spawnRoot(this, getX() + Math.cos(a) * r, getZ() + Math.sin(a) * r, 8 + ring * 7, 18.0F);
                        }
                    }
                    shake(16);
                    playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 0.5F);
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.5 秒に葉が紅葉し、実が2つ生え直す
                if (t == 30) {
                    setPhase(2);
                    shake(20);
                    fruitsToSpawn = 2;
                    playSound(SoundEvents.WARDEN_ROAR, 3.0F, 0.6F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(new DustParticleOptions(EMBER, 2.0F), getX(), getY() + 5, getZ(), 150, 4, 2, 4, 0);
                        sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 3, getZ(), 60, 2, 2, 2, 0.05);
                    }
                    for (LivingEntity e : inRadius(this.position(), 8.0, 5.0)) {
                        e.knockback(2.5, getX() - e.getX(), getZ() - e.getZ());
                        launch(e, 0.6);
                    }
                }
            }
            default -> {}
        }
        if (attackTick >= attack.length) endAttack();
    }

    private int countGuards() {
        return level().getEntitiesOfClass(Bogged.class, this.getBoundingBox().inflate(32), b -> b.isAlive() && b.getTags().contains(GUARD_TAG)).size();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        if (!level().isClientSide) {
            playSound(SoundEvents.WARDEN_DEATH, 2.0F, 0.5F);
            for (GoldenFruitEntity f : level().getEntitiesOfClass(GoldenFruitEntity.class, this.getBoundingBox().inflate(10))) f.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // 黄金の実は保存されないので、残っている数を覚えておいて読み込み時に生やし直す
        tag.putInt("Fruits", level().getEntitiesOfClass(GoldenFruitEntity.class, this.getBoundingBox().inflate(10)).size() + fruitsToSpawn);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Fruits")) fruitsToSpawn = tag.getInt("Fruits");
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        if (random.nextInt(2) == 0) {                               // 舞い落ちる葉と胞子
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * 5;
            level().addParticle(new DustParticleOptions(isPhase2() ? EMBER : GREEN, 1.2F), getX() + Math.cos(a) * r,
                    getY() + 4 + random.nextDouble() * 2, getZ() + Math.sin(a) * r, 0, -0.04, 0);
        }
        if (random.nextInt(6) == 0) {
            level().addParticle(isPhase2() ? ParticleTypes.SMALL_FLAME : ParticleTypes.SPORE_BLOSSOM_AIR,
                    getX() + (random.nextDouble() - 0.5) * 8, getY() + 3 + random.nextDouble() * 3, getZ() + (random.nextDouble() - 0.5) * 8, 0, 0, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(new DustParticleOptions(isPhase2() ? EMBER : GREEN, 1.5F), getX() + (random.nextDouble() - 0.5) * 6,
                getY() + random.nextDouble() * 6, getZ() + (random.nextDouble() - 0.5) * 6, 0, -0.05, 0);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.WOOD_STEP; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WOOD_HIT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 0.5F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<YggdrasilEntity> main = new AnimationController<>(this, "main", 4, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DEATH);
            return state.setAndContinue(IDLE);
        });
        for (Attack a : Attack.values()) {
            if (a.anim != null) main.triggerableAnim(a.name(), RawAnimation.begin().thenPlay(A + a.anim));
        }
        main.triggerableAnim("death", DEATH);
        controllers.add(main);
    }
}
