package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.level.block.Blocks;
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
 * 氷の魔女。距離をとって魔法で戦い、近づかれるとテレポートで逃げる。
 * 第2形態「吹雪」：周りが吹雪で見えにくくなり、分身・雹・絶対零度を使う。
 * 同じクラスで分身（ICE_WITCH_CLONE）も動く。分身は1発で砕け、弱い氷の槍しか撃たない。
 */
public class IceWitchEntity extends AbstractBoss {
    private static final String A = "animation.ice_witch.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");

    private static final Vector3f AURORA = new Vector3f(0.75F, 0.5F, 1.0F);
    private static final int CLONE_LIFE = 300;

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        SPEAR(32, 40, "cast_spear"),
        SPIKES(28, 60, "cast_spikes"),
        FIELD(32, 200, "cast_field"),
        NOVA(24, 60, "nova"),
        TELEPORT(20, 80, "teleport"),
        HAIL(40, 120, "cast_hail"),
        CLONES(28, 400, "summon_clones"),
        ABSOLUTE_ZERO(80, 500, "absolute_zero"),
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
    private Vec3 teleportTo;
    private UUID ownerId;          // 分身のとき：本物の UUID

    public IceWitchEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE, BossEvent.BossBarColor.PURPLE);
        if (isClone()) this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(1500.0, 6.0);
    }

    /** 分身：HP1、防具なし（同じ属性を2回 add するとエラーになるので別に作る） */
    public static AttributeSupplier.Builder createCloneAttributes() {
        return attributes(1.0, 0.0);
    }

    private static AttributeSupplier.Builder attributes(double health, double armor) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public boolean isClone() {
        return this.getType() == ModEntities.ICE_WITCH_CLONE.get();
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

    /** 当たった相手を凍えさせる（粉雪に埋まったときと同じ状態。革防具を着ていると防げる） */
    public static void freeze(LivingEntity e, int ticks) {
        if (!e.canFreeze()) return;
        e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 120, e.getTicksFrozen() + ticks));
    }

    // ============================================================ AI
    @Override
    protected void bossAiStep() {
        LivingEntity target = this.getTarget();
        if (isClone() && cloneExpired()) {
            shatter();
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
        double dist = flatDistance(target);
        // 7〜12ブロックの距離を保つ
        if (this.tickCount % 10 == 0) {
            if (dist < 6) {
                Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(8);
                this.getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.15);
            } else if (dist > 13) {
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

    private double flatDistance(LivingEntity target) {
        return Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ()));
    }

    private Attack chooseAttack(LivingEntity target, double dist) {
        List<Attack> pool = new ArrayList<>();
        boolean sight = this.hasLineOfSight(target);
        if (isClone()) {
            if (sight && dist > 3) add(pool, Attack.SPEAR, 1);
            return pool.isEmpty() ? null : pool.get(0);
        }
        if (dist < 4.5) {
            add(pool, Attack.NOVA, 4);
            add(pool, Attack.TELEPORT, 4);
        } else {
            if (sight) add(pool, Attack.SPEAR, 4);
            add(pool, Attack.SPIKES, 3);
            if (target.onGround()) add(pool, Attack.FIELD, 2);
            if (isPhase2()) {
                add(pool, Attack.HAIL, 3);
                if (countClones() == 0) add(pool, Attack.CLONES, 3);
                if (getHealth() < getMaxHealth() * 0.4F) add(pool, Attack.ABSOLUTE_ZERO, 3);
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
        this.getNavigation().stop();
        this.triggerAnim("main", a.name());
    }

    private void endAttack() {
        Attack done = this.attack;
        this.attack = Attack.NONE;
        this.lastAttack = done;
        this.cooldowns.put(done, isClone() ? 60 + this.random.nextInt(30) : done.cooldown);
        this.globalCooldown = done == Attack.PHASE_CHANGE ? 10 : (isPhase2() ? 16 : 26);
    }

    @Override
    protected boolean isInvulnerableNow() {
        return attack == Attack.PHASE_CHANGE;
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case SPEAR -> {                         // 氷の槍  0.6 / 0.8 / 1.0 秒
                if (t < 24) face(target, 20);
                if ((t == 12 || t == 16 || t == 20) && target != null) {
                    Vec3 f = forward();
                    Vec3 right = new Vec3(-f.z, 0, f.x);     // 右手（杖の先）から撃つ
                    Vec3 start = this.position().add(f.scale(1.2)).add(right.scale(0.6)).add(0, 2.2, 0);
                    Vec3 aim = target.getEyePosition().subtract(start).normalize();
                    int count = isPhase2() && !isClone() ? 3 : 1;
                    float damage = isClone() ? 4 : dmg(9, 12);
                    for (int i = 0; i < count; i++) {
                        float spread = (i - (count - 1) / 2.0F) * 12.0F * Mth.DEG_TO_RAD;
                        IceSpearEntity.shoot(this, start, aim.yRot(spread).scale(1.3), damage, isClone() ? 30 : 70, isPhase2());
                    }
                    playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 1.6F);
                    playSound(SoundEvents.GLASS_BREAK, 0.6F, 1.8F);
                }
            }
            case SPIKES -> {                        // 氷のトゲ  0.7 秒に杖を突く
                if (t < 12) face(target, 25);
                if (t == 14 && target != null) {
                    playSound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.8F);
                    if (!isPhase2()) {
                        IceSpikeEntity.spawn(this, target.getX(), groundY(target.getX(), target.getY(), target.getZ()), target.getZ(),
                                16, IceSpikeEntity.SPIKE, dmg(12, 15));
                    } else {
                        // 第2形態：自分からターゲットの奥まで、トゲが順番に走る
                        Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1);
                        double len = dir.length() + 4;
                        dir = dir.normalize();
                        int n = (int) Math.min(14, len / 1.4);
                        for (int i = 1; i <= n; i++) {
                            double x = getX() + dir.x * i * 1.4, z = getZ() + dir.z * i * 1.4;
                            IceSpikeEntity.spawn(this, x, groundY(x, getY() + 2, z), z, 10 + i * 2, IceSpikeEntity.SPIKE, 15);
                        }
                    }
                }
            }
            case FIELD -> {                         // 凍てつく大地  0.9 秒
                if (t < 16) face(target, 25);
                if (t == 18 && target != null) {
                    FrostFieldEntity.spawn(this, target.getX(), groundY(target.getX(), target.getY(), target.getZ()), target.getZ(),
                            isPhase2() ? 7.0F : 5.0F, 160);
                    playSound(SoundEvents.POWDER_SNOW_PLACE, 2.0F, 0.6F);
                }
            }
            case NOVA -> {                          // 氷の爆発（近づかれたとき）  0.6 秒
                if (t == 12) {
                    for (LivingEntity e : inRadius(this.position(), 4.5, 3.5)) {
                        if (damage(e, dmg(12, 16), 1.6)) {
                            freeze(e, 100);
                            launch(e, 0.4);
                        }
                    }
                    iceBurst(this.position(), 4.5F, 60);
                    shake(8);
                }
            }
            case TELEPORT -> {                      // テレポート  0.5 秒で消えて移動
                if (t == 1) teleportTo = findTeleportSpot(target, 9, 13);
                if (t == 10) {
                    Vec3 from = this.position();
                    // 消えるときに足元で小さく冷気が弾ける
                    for (LivingEntity e : inRadius(from, 3.0, 3.0)) {
                        if (damage(e, dmg(8, 10), 1.0)) freeze(e, 60);
                    }
                    iceBurst(from, 2.5F, 30);
                    if (teleportTo != null) {
                        this.teleportTo(teleportTo.x, teleportTo.y, teleportTo.z);
                        this.getNavigation().stop();
                        iceBurst(this.position(), 1.5F, 20);
                    }
                    playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5F, 1.4F);
                }
                if (t > 10) face(target, 40);
            }
            case HAIL -> {                          // 雹  0.8 秒から空から氷塊が降る
                if (t >= 16 && t <= 37 && t % 3 == 1 && target != null) {
                    double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * 6;
                    if (t == 16) r = 0;                     // 1発目は必ず真上から
                    double x = target.getX() + Math.cos(a) * r, z = target.getZ() + Math.sin(a) * r;
                    IceSpikeEntity.spawn(this, x, groundY(x, target.getY() + 1, z), z, 16, IceSpikeEntity.HAIL, 12);
                }
                if (t == 16) playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.0F, 1.6F);
            }
            case CLONES -> {                        // 分身  0.7 秒に3人に分かれる
                if (t == 14 && target != null) summonClones(target);
            }
            case ABSOLUTE_ZERO -> {                 // 絶対零度  3.2 秒に半径14ブロックへ冷気
                if (t == 1) playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 3.0F, 0.6F);
                if (t < 64 && t % 4 == 0 && level() instanceof ServerLevel sl) {
                    // 届く範囲を雪の輪で見せる（この外に逃げれば当たらない）
                    for (int i = 0; i < 48; i++) {
                        double a = Math.PI * 2 * i / 48;
                        sl.sendParticles(ParticleTypes.SNOWFLAKE, getX() + Math.cos(a) * 14, getY() + 0.3, getZ() + Math.sin(a) * 14,
                                1, 0, 0.1, 0, 0.0);
                    }
                    sl.sendParticles(new DustParticleOptions(AURORA, 2.0F), getX(), getY() + 3, getZ(), 6, 0.8, 0.8, 0.8, 0);
                }
                if (t == 64) {
                    for (LivingEntity e : inRadius(this.position(), 14.0, 8.0)) {
                        if (damage(e, 22, 1.2)) {
                            freeze(e, 200);
                            launch(e, 0.3);
                        }
                    }
                    iceBurst(this.position(), 14.0F, 300);
                    shake(25);
                    playSound(SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 1.2F);
                    playSound(SoundEvents.GLASS_BREAK, 3.0F, 0.5F);
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.4 秒で吹雪が始まる
                if (t == 28) {
                    setPhase(2);
                    shake(20);
                    playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 3.0F, 0.8F);
                    playSound(SoundEvents.GLASS_BREAK, 3.0F, 0.6F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 2, getZ(), 300, 2, 2, 2, 0.3);
                        sl.sendParticles(new DustParticleOptions(AURORA, 2.5F), getX(), getY() + 2, getZ(), 80, 2, 2, 2, 0);
                    }
                    for (LivingEntity e : inRadius(this.position(), 6.0, 4.0)) {
                        e.knockback(2.0, getX() - e.getX(), getZ() - e.getZ());
                        launch(e, 0.5);
                    }
                }
            }
            default -> {}
        }
        if (t >= attack.length) endAttack();
    }

    /** ターゲットから min〜max ブロック離れた、立てる場所を探す */
    private Vec3 findTeleportSpot(LivingEntity target, double min, double max) {
        Vec3 center = target != null ? target.position() : this.position();
        for (int i = 0; i < 16; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = min + random.nextDouble() * (max - min);
            double x = center.x + Math.cos(a) * r, z = center.z + Math.sin(a) * r;
            double y = groundY(x, center.y + 3, z);
            if (Math.abs(y - center.y) > 6) continue;
            if (level().noCollision(this, this.getBoundingBox().move(x - getX(), y - getY(), z - getZ()))) {
                return new Vec3(x, y, z);
            }
        }
        return null;
    }

    private void summonClones(LivingEntity target) {
        double base = random.nextDouble() * Math.PI * 2;
        List<Vec3> spots = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double a = base + i * Math.PI * 2 / 3;
            double x = target.getX() + Math.cos(a) * 9, z = target.getZ() + Math.sin(a) * 9;
            double y = groundY(x, target.getY() + 3, z);
            spots.add(new Vec3(x, y, z));
        }
        // 本物は3か所のどれかにランダムで移動する
        int real = random.nextInt(3);
        iceBurst(this.position(), 2.0F, 40);
        for (int i = 0; i < 3; i++) {
            Vec3 p = spots.get(i);
            if (i == real) {
                this.teleportTo(p.x, p.y, p.z);
            } else {
                IceWitchEntity clone = new IceWitchEntity(ModEntities.ICE_WITCH_CLONE.get(), level());
                clone.moveTo(p.x, p.y, p.z, this.getYRot(), 0);
                clone.ownerId = this.getUUID();
                clone.setPhase(2);
                clone.setTarget(target);
                level().addFreshEntity(clone);
            }
            iceBurst(p, 1.5F, 30);
        }
        playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 2.0F, 1.0F);
    }

    private int countClones() {
        return level().getEntitiesOfClass(IceWitchEntity.class, this.getBoundingBox().inflate(48),
                e -> e.isClone() && this.getUUID().equals(e.ownerId)).size();
    }

    private boolean cloneExpired() {
        if (this.tickCount > CLONE_LIFE || ownerId == null) return true;
        Entity owner = level() instanceof ServerLevel sl ? sl.getEntity(ownerId) : null;
        return !(owner instanceof IceWitchEntity w) || !w.isAlive();
    }

    /** 冷気がはじける演出（server 側） */
    private void iceBurst(Vec3 at, float radius, int count) {
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 1, at.z, count, radius * 0.4, 0.6, radius * 0.4, 0.15);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                at.x, at.y + 0.5, at.z, count / 2, radius * 0.4, 0.4, radius * 0.4, 0.2);
        if (isPhase2()) sl.sendParticles(new DustParticleOptions(AURORA, 1.5F), at.x, at.y + 1, at.z, count / 3, radius * 0.4, 0.6, radius * 0.4, 0);
        playSound(SoundEvents.GLASS_BREAK, 1.5F, 0.7F);
    }

    /** 分身が消えるとき */
    private void shatter() {
        iceBurst(this.position(), 1.2F, 40);
    }

    // ============================================================ client particles
    @Override
    protected void clientTick() {
        // 周りに舞う雪と、結晶のきらめき
        if (random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 3.2,
                    getZ() + (random.nextDouble() - 0.5) * 3, 0, -0.02, 0);
        }
        if (random.nextInt(4) == 0) {
            float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
            double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
            ParticleOptions sparkle = isPhase2() ? new DustParticleOptions(AURORA, 1.0F) : ParticleTypes.END_ROD;
            level().addParticle(sparkle, getX() + rx * 0.7, getY() + 3.0, getZ() + rz * 0.7, 0, 0.01, 0);
        }
    }

    @Override
    protected void deathParticles() {
        for (int i = 0; i < 3; i++) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getX() + (random.nextDouble() - 0.5) * 1.5, getY() + random.nextDouble() * 3,
                    getZ() + (random.nextDouble() - 0.5) * 1.5, 0, 0.05, 0);
        }
    }

    // ============================================================ damage / death
    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.attack = Attack.NONE;
        if (!level().isClientSide) {
            if (isClone()) shatter();
            else playSound(SoundEvents.WITCH_DEATH, 2.0F, 0.7F);
        }
    }

    /** 分身は死亡アニメを待たずにすぐ砕ける */
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
    protected SoundEvent getAmbientSound() { return isClone() ? null : SoundEvents.WITCH_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return isClone() ? SoundEvents.GLASS_BREAK : SoundEvents.WITCH_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    public float getVoicePitch() { return 0.75F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<IceWitchEntity> main = new AnimationController<>(this, "main", 4, state -> {
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
