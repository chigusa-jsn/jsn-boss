package com.chigusa.jsnboss.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
 * 堕ちた戦乙女（高精細モデル）。羽根の刃の翼で空を飛び、光る長槍で戦う。
 * 地上：槍の連突き・なぎ払い・地より出ずる槍・羽根の刃・飛翔。空中：槍の雨・羽根の刃・急降下突き（着地して地上へ戻る）。
 * 第2形態（HP1100以下）「完全なる堕天」：光が赤紫に、翼がボロボロに。槍の牢獄が増え、槍が3列になる。
 */
public class FallenValkyrieEntity extends AbstractBoss {
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(FallenValkyrieEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double FLY_HEIGHT = 7.0;
    private static final int MAX_FLIGHT = 240;

    private static final String A = "animation.fallen_valkyrie.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop(A + "hover");
    private static final RawAnimation CAPE = RawAnimation.begin().thenLoop(A + "cape");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");
    private static final Vector3f CYAN = new Vector3f(0.6F, 0.95F, 1.0F);
    private static final Vector3f MAGENTA = new Vector3f(1.0F, 0.2F, 0.45F);

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        THRUST(34, 50, "thrust"),
        SWEEP(32, 80, "sweep"),
        GROUND_SPEARS(36, 120, "ground_spears"),
        FEATHERS(28, 80, "feathers"),
        TAKEOFF(30, 300, "takeoff"),
        SPEAR_RAIN(36, 90, "spear_rain"),
        DIVE(60, 0, "dive"),
        PRISON(48, 260, "prison"),
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
    private int flightTicks;
    private final Map<Attack, Integer> cooldowns = new EnumMap<>(Attack.class);
    private final Set<UUID> hitThisAttack = new HashSet<>();
    private Vec3 diveDir = Vec3.ZERO;

    public FallenValkyrieEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE, BossEvent.BossBarColor.PINK);
        this.xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2200.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
    }

    public boolean isFlying() { return this.entityData.get(FLYING); }

    private void setFlying(boolean f) {
        this.entityData.set(FLYING, f);
        this.setNoGravity(f);
        this.flightTicks = 0;
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
        if (isFlying()) flightTicks++;
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
        if (isFlying()) {
            hoverNear(target);
            if (target != null) this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (target == null || !target.isAlive()) {
            if (isFlying() && flightTicks > MAX_FLIGHT) land();
            return;
        }
        if (!isFlying()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.tickCount % 8 == 0 || this.getNavigation().isDone()) this.getNavigation().moveTo(target, 1.0);
        }
        if (isFlying() && flightTicks > MAX_FLIGHT) {             // 飛びすぎたら急降下して降りてくる
            startAttack(Attack.DIVE);
            return;
        }
        if (globalCooldown <= 0) {
            Attack next = chooseAttack(target);
            if (next != null) startAttack(next);
        }
    }

    /** 空中：相手から 7 ブロックほど離れた、地面から FLY_HEIGHT の高さに浮かぶ */
    private void hoverNear(LivingEntity target) {
        this.getNavigation().stop();
        Vec3 v = this.getDeltaMovement().scale(0.7);
        if (target != null) {
            Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-3) away = new Vec3(1, 0, 0);
            Vec3 want = target.position().add(away.normalize().yRot(0.02F).scale(7.0));
            Vec3 d = want.subtract(this.position()).multiply(1, 0, 1);
            if (d.length() > 0.5) v = v.add(d.normalize().scale(Math.min(0.12, d.length() * 0.05)));
        }
        double floor = groundY(getX(), getY() + 0.5, getZ()) + FLY_HEIGHT;
        double vy = Mth.clamp((floor - getY()) * 0.1, -0.2, 0.2) + Math.sin(this.tickCount * 0.15) * 0.02;
        this.setDeltaMovement(v.x, vy, v.z);
    }

    private void land() {
        setFlying(false);
    }

    private Attack chooseAttack(LivingEntity target) {
        double dist = Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ()));
        List<Attack> pool = new ArrayList<>();
        if (isFlying()) {
            add(pool, Attack.SPEAR_RAIN, 3);
            add(pool, Attack.FEATHERS, 2);
            if (flightTicks > 80) add(pool, Attack.DIVE, 2 + flightTicks / 60);
        } else {
            add(pool, Attack.TAKEOFF, 2);
            if (isPhase2()) add(pool, Attack.PRISON, 2);
            if (dist < 5) {
                add(pool, Attack.THRUST, 4);
                add(pool, Attack.SWEEP, 3);
            } else if (dist < 14) {
                add(pool, Attack.GROUND_SPEARS, 3);
                add(pool, Attack.FEATHERS, 2);
                add(pool, Attack.THRUST, 1);
            } else {
                add(pool, Attack.GROUND_SPEARS, 3);
                add(pool, Attack.FEATHERS, 2);
            }
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
        this.globalCooldown = done == Attack.PHASE_CHANGE || done == Attack.TAKEOFF ? 10 : (isPhase2() ? 22 : 36);
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        if (isFlying() && attack != Attack.DIVE && attack != Attack.TAKEOFF) hoverNear(target);
        switch (attack) {
            case THRUST -> {                        // 槍の連突き  0.45 / 0.85 / 1.25 秒
                face(target, 25);
                if (t == 9 || t == 17 || t == 25) {
                    this.setDeltaMovement(this.getDeltaMovement().add(forward().scale(0.45)));
                    for (LivingEntity e : inArc(5.5, 30)) damage(e, dmg(12, 14), 0.5);
                    playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.5F, 1.2F);
                }
            }
            case SWEEP -> {                         // なぎ払い  0.7 秒に一回転
                if (t >= 13 && t <= 19) {
                    for (LivingEntity e : inRadius(this.position(), 6.0, 3.5)) {
                        if (hitThisAttack.add(e.getUUID()) && damage(e, dmg(16, 20), 2.0)) launch(e, 0.4);
                    }
                    if (t == 13) playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.6F);
                }
            }
            case GROUND_SPEARS -> {                 // 地より出ずる槍  0.7 秒に槍を突き立てる
                if (t < 12) face(target, 25);
                if (t == 14) {
                    Vec3 dir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    int lines = isPhase2() ? 3 : 1;
                    for (int l = 0; l < lines; l++) {
                        Vec3 d = dir.yRot((l - (lines - 1) / 2.0F) * 22.0F * Mth.DEG_TO_RAD);
                        for (int i = 0; i < 13; i++) {
                            double dist = 2.0 + i * 1.4;
                            double x = getX() + d.x * dist, z = getZ() + d.z * dist;
                            BladeSpikeEntity.spawnSpear(this, x, groundY(x, getY() + 3, z), z, 6 + i * 2, dmg(14, 16));
                        }
                    }
                    if (isPhase2()) {
                        for (int i = 0; i < 10; i++) {
                            double a = Math.PI * 2 * i / 10, x = getX() + Math.cos(a) * 3.5, z = getZ() + Math.sin(a) * 3.5;
                            BladeSpikeEntity.spawnSpear(this, x, groundY(x, getY() + 3, z), z, 10, 16.0F);
                        }
                    }
                    playSound(SoundEvents.ANVIL_LAND, 1.5F, 1.2F);
                    shake(6);
                }
            }
            case FEATHERS -> {                      // 羽根の刃  0.7 秒に翼から扇状に飛ばす
                face(target, 25);
                if (t == 14 && target != null) {
                    int n = isPhase2() ? 7 : 5;
                    Vec3 from = this.position().add(0, 2.6, 0);
                    Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from).normalize();
                    for (int i = 0; i < n; i++) {
                        Vec3 v = aim.yRot((i - (n - 1) / 2.0F) * 11.0F * Mth.DEG_TO_RAD).scale(1.1);
                        FeatherShotEntity.shoot(this, from.add(v.scale(1.2)), v, dmg(8, 9));
                    }
                    playSound(SoundEvents.PHANTOM_FLAP, 2.0F, 1.4F);
                }
            }
            case TAKEOFF -> {                       // 飛翔  0.5 秒に跳び上がる
                if (t == 10) {
                    setFlying(true);
                    this.setDeltaMovement(0, 0.9, 0);
                    playSound(SoundEvents.PHANTOM_FLAP, 3.0F, 0.6F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 30, 1.5, 0.1, 1.5, 0.08);
                    }
                }
                if (t > 10) {
                    double floor = groundY(getX(), getY() + 0.5, getZ()) + FLY_HEIGHT;
                    this.setDeltaMovement(0, Mth.clamp((floor - getY()) * 0.15, -0.2, 0.6), 0);
                }
            }
            case SPEAR_RAIN -> {                    // 槍の雨（空中）  0.8 秒に光の槍を降らせる
                if (t == 16) {
                    int n = isPhase2() ? 10 : 6;
                    List<LivingEntity> victims = new ArrayList<>(level().getEntitiesOfClass(Player.class,
                            this.getBoundingBox().inflate(24, 16, 24), p -> p.isAlive() && !p.isSpectator() && !p.isCreative()));
                    if (victims.isEmpty() && target != null) victims.add(target);
                    for (int i = 0; i < n && !victims.isEmpty(); i++) {
                        LivingEntity v = victims.get(i % victims.size());
                        double r = i < victims.size() ? 0 : 1.5 + random.nextDouble() * 3.5, a = random.nextDouble() * Math.PI * 2;
                        double x = v.getX() + Math.cos(a) * r, z = v.getZ() + Math.sin(a) * r;
                        LightSwordEntity.spawnSpear(this, x, groundY(x, v.getY() + 3, z), z, 14 + i * 2, dmg(10, 12));
                    }
                    playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 3.0F, 0.6F);
                }
            }
            case DIVE -> {                          // 急降下突き  空から一直線に突っ込み、着地で衝撃
                if (t < 12) {
                    if (isFlying()) hoverNear(target);
                    face(target, 30);
                }
                if (t == 12) {
                    Vec3 aim = target != null ? target.position().add(target.getDeltaMovement().scale(5)) : this.position().add(forward().scale(6)).add(0, -FLY_HEIGHT, 0);
                    diveDir = aim.subtract(this.position()).normalize();
                    this.setNoGravity(true);
                    playSound(SoundEvents.PHANTOM_SWOOP, 3.0F, 0.6F);
                }
                if (t >= 12 && t < 40) {
                    if (!this.onGround()) {
                        this.setDeltaMovement(diveDir.scale(1.6));
                        this.hasImpulse = true;
                        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8), this::canHit)) {
                            if (hitThisAttack.add(e.getUUID())) damage(e, dmg(20, 24), 1.5);
                        }
                        if (level() instanceof ServerLevel sl) {
                            sl.sendParticles(new DustParticleOptions(isPhase2() ? MAGENTA : CYAN, 1.5F), getX(), getY() + 2, getZ(), 6, 0.4, 0.8, 0.4, 0);
                        }
                    } else {
                        diveImpact();
                        attackTick = Math.max(attackTick, 40);
                    }
                }
                if (t == 40 && isFlying()) diveImpact();
            }
            case PRISON -> {                        // 槍の牢獄（第2形態）  外側の輪→内側の輪→真ん中
                if (t == 20 && target != null) {
                    double cx = target.getX(), cz = target.getZ();
                    for (int i = 0; i < 14; i++) {
                        double a = Math.PI * 2 * i / 14, x = cx + Math.cos(a) * 5, z = cz + Math.sin(a) * 5;
                        BladeSpikeEntity.spawnSpear(this, x, groundY(x, target.getY() + 3, z), z, 10, 18.0F);
                    }
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * 2 * i / 8 + 0.3, x = cx + Math.cos(a) * 2.5, z = cz + Math.sin(a) * 2.5;
                        BladeSpikeEntity.spawnSpear(this, x, groundY(x, target.getY() + 3, z), z, 22, 18.0F);
                    }
                    BladeSpikeEntity.spawnSpear(this, cx, groundY(cx, target.getY() + 3, cz), cz, 34, 18.0F);
                    playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 2.0F, 1.4F);
                    shake(8);
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.5 秒に翼を広げて光が赤紫に
                if (t == 1 && isFlying()) setFlying(false);
                if (t == 30) {
                    setPhase(2);
                    shake(20);
                    playSound(SoundEvents.WITHER_SPAWN, 2.0F, 1.6F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(new DustParticleOptions(MAGENTA, 2.0F), getX(), getY() + 2.5, getZ(), 120, 2.5, 1.5, 2.5, 0);
                        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.5, getZ(), 60, 1.5, 1.5, 1.5, 0.3);
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

    private void diveImpact() {
        setFlying(false);
        this.setDeltaMovement(Vec3.ZERO);
        for (LivingEntity e : inRadius(this.position(), 4.0, 3.0)) {
            if (hitThisAttack.add(e.getUUID()) && damage(e, dmg(20, 24), 1.8)) launch(e, 0.6);
        }
        if (isPhase2()) {
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8, x = getX() + Math.cos(a) * 3, z = getZ() + Math.sin(a) * 3;
                BladeSpikeEntity.spawnSpear(this, x, groundY(x, getY() + 3, z), z, 4, 16.0F);
            }
        }
        shake(14);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 1.2F);
        playSound(SoundEvents.ANVIL_LAND, 1.5F, 0.8F);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 3, 1, 0.2, 1, 0);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 40, 1.5, 0.3, 1.5, 0.3);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        if (isFlying()) setFlying(false);
        if (!level().isClientSide) playSound(SoundEvents.WITHER_DEATH, 1.5F, 1.8F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Flying", isFlying());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFlying(tag.getBoolean("Flying"));
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        Vector3f c = isPhase2() ? MAGENTA : CYAN;
        if (random.nextInt(isFlying() ? 1 : 3) == 0) {             // 翼から舞い落ちる光の粒
            level().addParticle(new DustParticleOptions(c, 1.0F), getX() + (random.nextDouble() - 0.5) * 3,
                    getY() + 1.5 + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 3, 0, -0.03, 0);
        }
        if (isPhase2() && random.nextInt(5) == 0) {
            level().addParticle(ParticleTypes.ASH, getX() + (random.nextDouble() - 0.5) * 2, getY() + 3, getZ() + (random.nextDouble() - 0.5) * 2, 0, -0.02, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 3,
                getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.05, 0);
    }

    @Override
    protected SoundEvent getAmbientSound() { return null; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 1.4F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<FallenValkyrieEntity> main = new AnimationController<>(this, "main", 4, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DEATH);
            if (this.isFlying()) return state.setAndContinue(HOVER);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        });
        for (Attack a : Attack.values()) {
            if (a.anim != null) main.triggerableAnim(a.name(), RawAnimation.begin().thenPlay(A + a.anim));
        }
        main.triggerableAnim("death", DEATH);
        controllers.add(main);
        // マントはずっとなびき続ける
        controllers.add(new AnimationController<>(this, "cape", 0, state -> state.setAndContinue(CAPE)));
    }
}
