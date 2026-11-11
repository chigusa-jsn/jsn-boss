package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import java.util.UUID;

/**
 * 光の白狐。9本の尻尾を持つ白い狐。距離をとって光の魔法で戦う。
 * 第2形態（HP800以下）：尻尾が金色に輝き、幻の分身・光の剣の雨を使う。
 * 同じクラスで分身（WHITE_FOX_CLONE）も動く。分身は1発で消え、尻尾が光っていない。
 */
public class WhiteFoxEntity extends AbstractBoss {
    private static final String A = "animation.white_fox.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop(A + "run");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");

    private static final Vector3f GOLD = new Vector3f(1.0F, 0.86F, 0.45F);
    private static final int CLONE_LIFE = 300;

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        FOXFIRE(28, 40, "foxfire"),
        PILLAR(28, 80, "pillar"),
        FLASH(28, 140, "flash"),
        ILLUSION(24, 400, "illusion"),
        LASER(56, 140, "laser"),
        LEAP(20, 80, "leap"),
        SWORD_RAIN(48, 220, "sword_rain"),
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
    private float laserYaw, laserPitch;
    private UUID ownerId;

    public WhiteFoxEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarColor.WHITE);
        this.xpReward = isClone() ? 0 : 250;
    }

    public static AttributeSupplier.Builder createAttributes() { return attributes(1600.0, 6.0); }

    public static AttributeSupplier.Builder createCloneAttributes() { return attributes(1.0, 0.0); }

    private static AttributeSupplier.Builder attributes(double health, double armor) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    public boolean isClone() {
        return this.getType() == ModEntities.WHITE_FOX_CLONE.get();
    }

    @Override
    protected boolean showsBossBar() { return !isClone(); }

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
        if (isClone() && cloneExpired()) {
            vanishBurst(this.position());
            this.discard();
            return;
        }
        if (attack != Attack.NONE) {
            tickAttack(target);
            return;
        }
        if (!isClone() && !isPhase2() && getHealth() <= getMaxHealth() * 0.5F) {
            startAttack(Attack.PHASE_CHANGE);
            return;
        }
        if (globalCooldown > 0) globalCooldown--;
        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
        if (target == null || !target.isAlive()) return;

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double dist = Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ()));
        if (this.tickCount % 10 == 0) {           // 6〜12 ブロックの距離を保つ
            if (dist < 6) {
                Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(8);
                this.getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.3);
            } else if (dist > 12) {
                this.getNavigation().moveTo(target, 1.1);
            } else {
                this.getNavigation().stop();
            }
        }
        if (globalCooldown <= 0) {
            Attack next = chooseAttack(target, dist);
            if (next != null) startAttack(next);
        }
    }

    private Attack chooseAttack(LivingEntity target, double dist) {
        boolean sight = this.hasLineOfSight(target);
        List<Attack> pool = new ArrayList<>();
        if (isClone()) {
            if (sight) add(pool, Attack.FOXFIRE, 1);
            return pool.isEmpty() ? null : pool.get(0);
        }
        if (dist < 4) add(pool, Attack.LEAP, 5);
        if (sight) add(pool, Attack.FOXFIRE, 4);
        add(pool, Attack.PILLAR, 3);
        if (sight && dist < 20) add(pool, Attack.FLASH, 2);
        if (sight && dist > 5) add(pool, Attack.LASER, 3);
        if (isPhase2()) {
            if (countClones() == 0) add(pool, Attack.ILLUSION, 3);
            add(pool, Attack.SWORD_RAIN, 3);
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
        this.cooldowns.put(done, isClone() ? 60 + this.random.nextInt(30) : done.cooldown);
        this.globalCooldown = done == Attack.PHASE_CHANGE ? 10 : (isPhase2() ? 14 : 22);
    }

    // ============================================================ attacks
    private Vec3 mouth() {
        return this.position().add(forward().scale(2.3)).add(0, 2.2, 0);
    }

    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case FOXFIRE -> {                       // 狐火  0.6 / 0.8 / 1.0 秒
                if (t < 20) face(target, 25);
                if ((t == 12 || t == 16 || t == 20) && target != null) {
                    int n = isClone() ? 1 : (isPhase2() && t != 16 ? 2 : 1);
                    for (int i = 0; i < n; i++) {
                        float spread = (n == 1 ? 0 : (i == 0 ? -20 : 20)) * Mth.DEG_TO_RAD;
                        Vec3 start = this.position().add(0, 3.2, 0).add(forward().scale(-1.5));
                        Vec3 v = target.getEyePosition().subtract(start).normalize().yRot(spread).add(0, 0.3, 0).normalize().scale(0.45);
                        FoxfireEntity.shoot(this, target, start, v, isClone() ? 4 : dmg(8, 10));
                    }
                    playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.6F);
                }
            }
            case PILLAR -> {                        // 光の柱  0.7 秒に踏み鳴らす、その 1 秒後に光が立つ
                if (t < 12) face(target, 25);
                if (t == 14 && target != null) {
                    spawnPillar(target.getX(), target.getY(), target.getZ());
                    if (isPhase2()) {
                        for (int i = 0; i < 2; i++) {
                            double a = random.nextDouble() * Math.PI * 2, r = 2.5 + random.nextDouble() * 1.5;
                            spawnPillar(target.getX() + Math.cos(a) * r, target.getY(), target.getZ() + Math.sin(a) * r);
                        }
                    }
                    playSound(SoundEvents.BELL_BLOCK, 2.0F, 1.2F);
                }
            }
            case FLASH -> {                         // 閃光  0.8 秒。光る瞬間にこちらを見ていると盲目
                if (t == 4) playSound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 1.6F);
                if (t == 16) {
                    Vec3 eye = this.position().add(0, 2.4, 0);
                    for (LivingEntity e : inRadius(this.position(), 20.0, 10.0)) {
                        if (!e.hasLineOfSight(this)) continue;
                        Vec3 toFox = eye.subtract(e.getEyePosition()).normalize();
                        if (e.getViewVector(1.0F).dot(toFox) > 0.5) {
                            e.hurt(this.damageSources().indirectMagic(this, this), dmg(6, 8));
                            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                        }
                    }
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.FLASH, eye.x, eye.y, eye.z, 1, 0, 0, 0, 0);
                        sl.sendParticles(ParticleTypes.END_ROD, eye.x, eye.y, eye.z, 80, 0.5, 0.5, 0.5, 0.4);
                    }
                    playSound(SoundEvents.FIREWORK_ROCKET_BLAST, 2.0F, 1.4F);
                    shake(6);
                }
            }
            case ILLUSION -> {                      // 幻：3匹に分かれる（本物だけ尻尾が光る）
                if (t == 14 && target != null) summonClones(target);
            }
            case LASER -> {                         // 白い光線  1.0 秒ためて、2.4 秒までゆっくり追いかける
                if (t < 20) {
                    face(target, 25);
                    if (t % 3 == 0 && level() instanceof ServerLevel sl) {
                        Vec3 m = mouth();
                        sl.sendParticles(new DustParticleOptions(GOLD, 1.5F), m.x, m.y, m.z, 6, 0.4, 0.4, 0.4, 0);
                    }
                }
                if (t == 20) {
                    laserYaw = this.getYRot();
                    laserPitch = target != null ? pitchTo(target) : 10;
                    playSound(SoundEvents.BEACON_ACTIVATE, 2.0F, 1.5F);
                }
                if (t >= 20 && t <= 48) {
                    if (target != null) {   // 少しずつターゲットの方へ向きを変える
                        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
                        float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
                        laserYaw = Mth.approachDegrees(laserYaw, want, 2.5F);
                        laserPitch = Mth.approach(laserPitch, pitchTo(target), 1.5F);
                    }
                    this.setYRot(laserYaw);
                    this.yBodyRot = laserYaw;
                    this.yHeadRot = laserYaw;
                    laser(t);
                }
            }
            case LEAP -> {                          // 跳躍：後ろへ大きく跳んで距離をとる
                if (t == 4) {
                    Vec3 back = forward().scale(-1.3);
                    this.setDeltaMovement(back.x, 0.55, back.z);
                    this.hasImpulse = true;
                    playSound(SoundEvents.FOX_AGGRO, 1.5F, 0.8F);
                }
            }
            case SWORD_RAIN -> {                    // 光の剣の雨  0.8〜2.0 秒に 12 本
                if (t >= 16 && t <= 38 && t % 2 == 0 && target != null) {
                    double a = random.nextDouble() * Math.PI * 2, r = (t == 16) ? 0 : random.nextDouble() * 7;
                    double x = target.getX() + Math.cos(a) * r, z = target.getZ() + Math.sin(a) * r;
                    LightSwordEntity.spawn(this, x, groundY(x, target.getY() + 2, z), z, 16, 14);
                }
                if (t == 16) playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 0.8F);
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.4 秒で尻尾が輝く
                if (t == 28) {
                    setPhase(2);
                    shake(15);
                    playSound(SoundEvents.BELL_RESONATE, 3.0F, 1.0F);
                    playSound(SoundEvents.FOX_SCREECH, 3.0F, 0.7F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 150, 2, 1.5, 2, 0.25);
                        sl.sendParticles(new DustParticleOptions(GOLD, 2.5F), getX(), getY() + 2, getZ(), 100, 2.5, 1.5, 2.5, 0);
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

    private void spawnPillar(double x, double y, double z) {
        LightPillarEntity.spawn(this, x, groundY(x, y + 2, z), z, dmg(14, 16));
    }

    private float pitchTo(LivingEntity target) {
        Vec3 from = mouth();
        Vec3 d = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from);
        return (float) (-Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
    }

    /** 光線：口から長さ22ブロック。0.2 秒ごとに線の近くの相手に当たる */
    private void laser(int t) {
        Vec3 dir = Vec3.directionFromRotation(laserPitch, laserYaw);
        Vec3 from = mouth();
        double len = 22.0;
        if (level() instanceof ServerLevel sl && t % 2 == 0) {
            for (int i = 0; i < 44; i++) {
                Vec3 p = from.add(dir.scale(len * i / 44));
                sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0);
                if (i % 4 == 0) sl.sendParticles(new DustParticleOptions(GOLD, 1.8F), p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0);
            }
        }
        if (t % 4 != 0) return;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(from, from.add(dir.scale(len))).inflate(1.5), this::canHit)) {
            Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
            double along = Mth.clamp(c.subtract(from).dot(dir), 0, len);
            if (c.distanceTo(from.add(dir.scale(along))) < 0.9 + e.getBbWidth() / 2.0) {
                e.hurt(this.damageSources().indirectMagic(this, this), dmg(8, 10));
            }
        }
    }

    private void summonClones(LivingEntity target) {
        double base = random.nextDouble() * Math.PI * 2;
        int real = random.nextInt(3);
        vanishBurst(this.position());
        for (int i = 0; i < 3; i++) {
            double a = base + i * Math.PI * 2 / 3;
            double x = target.getX() + Math.cos(a) * 8, z = target.getZ() + Math.sin(a) * 8;
            double y = groundY(x, target.getY() + 3, z);
            if (i == real) {
                this.teleportTo(x, y, z);
            } else {
                WhiteFoxEntity clone = new WhiteFoxEntity(ModEntities.WHITE_FOX_CLONE.get(), level());
                clone.moveTo(x, y, z, this.getYRot(), 0);
                clone.ownerId = this.getUUID();
                clone.setTarget(target);
                level().addFreshEntity(clone);
            }
            vanishBurst(new Vec3(x, y, z));
        }
        playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 2.0F, 1.2F);
    }

    private int countClones() {
        return level().getEntitiesOfClass(WhiteFoxEntity.class, this.getBoundingBox().inflate(48),
                e -> e.isClone() && this.getUUID().equals(e.ownerId)).size();
    }

    private boolean cloneExpired() {
        if (this.tickCount > CLONE_LIFE || ownerId == null) return true;
        Entity owner = level() instanceof ServerLevel sl ? sl.getEntity(ownerId) : null;
        return !(owner instanceof WhiteFoxEntity f) || !f.isAlive();
    }

    private void vanishBurst(Vec3 at) {
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.5, at.z, 30, 0.8, 1.0, 0.8, 0.1);
        sl.sendParticles(new DustParticleOptions(GOLD, 1.5F), at.x, at.y + 1.5, at.z, 20, 0.8, 1.0, 0.8, 0);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, getSoundSource(), 1.5F, 1.4F);
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        if (isClone()) return;
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double bx = Mth.sin(yaw) * 2.6, bz = -Mth.cos(yaw) * 2.6;      // 尻尾の付け根あたり（体の後ろ）
        int n = isPhase2() ? 2 : (random.nextInt(3) == 0 ? 1 : 0);
        for (int i = 0; i < n; i++) {
            level().addParticle(isPhase2() ? ParticleTypes.END_ROD : new DustParticleOptions(GOLD, 0.9F),
                    getX() + bx + (random.nextDouble() - 0.5) * 3.5, getY() + 2.5 + random.nextDouble() * 2,
                    getZ() + bz + (random.nextDouble() - 0.5) * 3.5, 0, 0.02, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 3,
                getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.05, 0);
    }

    // ============================================================ damage / death
    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        if (!level().isClientSide) {
            if (isClone()) vanishBurst(this.position());
            else playSound(SoundEvents.FOX_DEATH, 2.0F, 0.6F);
        }
    }

    /** 分身は死亡アニメを待たずにすぐ消える */
    @Override
    protected void tickDeath() {
        if (isClone()) {
            if (!level().isClientSide() && !this.isRemoved()) this.remove(RemovalReason.KILLED);
            return;
        }
        super.tickDeath();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
    }

    @Override
    protected SoundEvent getAmbientSound() { return isClone() ? null : SoundEvents.FOX_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return isClone() ? SoundEvents.AMETHYST_BLOCK_BREAK : SoundEvents.FOX_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 0.6F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<WhiteFoxEntity> main = new AnimationController<>(this, "main", 3, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DEATH);
            if (!state.isMoving()) return state.setAndContinue(IDLE);
            return state.setAndContinue(this.getDeltaMovement().horizontalDistanceSqr() > 0.01 ? RUN : WALK);
        });
        for (Attack a : Attack.values()) {
            if (a.anim != null) main.triggerableAnim(a.name(), RawAnimation.begin().thenPlay(A + a.anim));
        }
        main.triggerableAnim("death", DEATH);
        controllers.add(main);
    }
}
