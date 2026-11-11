package com.chigusa.jsnboss.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
 * ゾンビの王。重くて遅いが一撃が重い。手下が生きている間は体力が回復していく。
 * 第2形態「不死の王」（HP1000以下）：毒のブレス、死者の手、2重の衝撃波、手下が近衛兵に。
 * 第2形態で倒されると、一度だけ王冠が光って HP500 で復活する。
 */
public class ZombieKingEntity extends AbstractBoss {
    private static final String A = "animation.zombie_king.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(A + "walk");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");

    /** 手下につけるタグ（王の攻撃が当たらない・王と一緒に崩れる） */
    public static final String MINION_TAG = "jsnboss_minion";
    private static final Vector3f TOXIC = new Vector3f(0.45F, 0.85F, 0.2F);
    private static final float REVIVE_HEALTH = 500.0F;
    private static final int MAX_MINIONS = 6;

    /** length = アニメーションの長さ(tick)、cooldown = 同じ技を再び使えるまで(tick) */
    public enum Attack {
        NONE(0, 0, null),
        SMASH(36, 50, "smash"),
        SWEEP(32, 50, "sweep"),
        POUND(40, 100, "pound"),
        SUMMON(40, 300, "summon"),
        BREATH(52, 160, "breath"),
        HANDS(32, 140, "hands"),
        PHASE_CHANGE(60, 0, "roar"),
        REVIVE(100, 0, "revive");

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
    private boolean revived;

    public ZombieKingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED, BossEvent.BossBarColor.GREEN);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2000.0)
                .add(Attributes.ARMOR, 12.0)
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

    public static boolean isMinion(LivingEntity e) {
        return e.getTags().contains(MINION_TAG);
    }

    /** 王の攻撃は手下には当たらない */
    @Override
    protected boolean canHit(LivingEntity e) {
        return super.canHit(e) && !isMinion(e);
    }

    @Override
    protected void onPhaseChanged(int phase) {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(phase >= 2 ? 0.25 : 0.22);
    }

    @Override
    protected boolean isInvulnerableNow() {
        return attack == Attack.PHASE_CHANGE || attack == Attack.REVIVE;
    }

    // ============================================================ AI
    @Override
    protected void bossAiStep() {
        LivingEntity target = this.getTarget();

        // 手下が生きている間、1体につき毎秒2回復
        if (this.tickCount % 20 == 0 && attack != Attack.REVIVE) {
            int minions = minions().size();
            if (minions > 0 && getHealth() < getMaxHealth()) {
                this.heal(2.0F * minions);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(new DustParticleOptions(TOXIC, 1.5F), getX(), getY() + 2.5, getZ(), 4 * minions, 0.8, 0.8, 0.8, 0);
                }
            }
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
        boolean fewMinions = minions().size() < 3;
        List<Attack> pool = new ArrayList<>();
        if (!isPhase2()) {
            if (dist < 4) { add(pool, Attack.SMASH, 4); add(pool, Attack.SWEEP, 4); }
            else if (dist < 12) { add(pool, Attack.POUND, 3); if (fewMinions) add(pool, Attack.SUMMON, 2); }
            else { add(pool, Attack.POUND, 2); if (fewMinions) add(pool, Attack.SUMMON, 3); }
        } else {
            if (dist < 4) { add(pool, Attack.SMASH, 3); add(pool, Attack.SWEEP, 3); add(pool, Attack.BREATH, 2); }
            else if (dist < 12) {
                add(pool, Attack.BREATH, 3); add(pool, Attack.HANDS, 3); add(pool, Attack.POUND, 3);
                if (fewMinions) add(pool, Attack.SUMMON, 2);
            } else {
                add(pool, Attack.HANDS, 3); add(pool, Attack.POUND, 2);
                if (fewMinions) add(pool, Attack.SUMMON, 2);
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
        this.cooldowns.put(done, done.cooldown);
        this.globalCooldown = (done == Attack.PHASE_CHANGE || done == Attack.REVIVE) ? 10 : (isPhase2() ? 18 : 28);
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        switch (attack) {
            case SMASH -> {                         // 王笏の振り下ろし  0.95 秒
                if (t < 14) face(target, 15);
                if (t == 19) {
                    hitArc(5.5, 50, dmg(18, 22), 1.2);
                    groundImpact(this.position().add(forward().scale(4.0)), 2.5F);
                    shake(12);
                }
            }
            case SWEEP -> {                         // 薙ぎ払い  0.8 秒
                if (t < 10) face(target, 15);
                if (t == 16) {
                    hitArc(5.5, 80, dmg(13, 17), 2.0);
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
                }
            }
            case POUND -> {                         // 衝撃波  1.0 秒（第2形態は 1.4 秒にもう一度）
                if (t < 12) face(target, 15);
                if (t == 20 || (isPhase2() && t == 28)) {
                    ShockwaveEntity.spawn(this, getX(), getY(), getZ(), 12.0F, 0.45F, dmg(12, 15));
                    groundImpact(this.position().add(forward().scale(2.0)), 3.0F);
                    shake(16);
                }
            }
            case SUMMON -> {                        // 手下の召喚  1.0 秒
                if (t == 20) summon(target, isPhase2() ? 4 : 3, isPhase2());
            }
            case BREATH -> {                        // 腐毒のブレス  0.8〜2.2 秒
                if (t < 16) face(target, 20);
                if (t > 16 && t < 44) {
                    face(target, 3);
                    breathTick(t);
                }
                if (t == 16) playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.5F);
            }
            case HANDS -> {                         // 死者の手  0.8 秒に地面へ王笏を突く
                if (t < 12) face(target, 20);
                if (t == 16) {
                    groundImpact(this.position().add(forward().scale(3.0)), 1.5F);
                    playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.5F, 0.5F);
                    for (LivingEntity e : inRadius(this.position(), 18.0, 6.0)) {
                        GraveHandEntity.spawn(this, e.getX(), groundY(e.getX(), e.getY() + 1, e.getZ()), e.getZ(), 18, 10.0F);
                    }
                }
            }
            case PHASE_CHANGE -> {                  // 第2形態へ  1.4 秒の咆哮で目と宝石が緑に
                if (t == 28) {
                    setPhase(2);
                    shake(20);
                    playSound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.4F);
                    playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 3.0F, 0.5F);
                    toxicBurst(80);
                    for (LivingEntity e : inRadius(this.position(), 6.0, 4.0)) {
                        e.knockback(2.0, getX() - e.getX(), getZ() - e.getZ());
                        launch(e, 0.5);
                    }
                    summon(target, 2, true);
                }
            }
            case REVIVE -> {                        // 復活  4.2 秒で起き上がる
                if (t == 1) playSound(SoundEvents.WITHER_DEATH, 2.0F, 0.6F);
                if (t > 30 && t < 70 && t % 5 == 0 && level() instanceof ServerLevel sl) {
                    sl.sendParticles(new DustParticleOptions(TOXIC, 2.0F), getX(), getY() + 3, getZ(), 12, 1.0, 1.0, 1.0, 0);
                    sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 3, 1.0, 0.5, 1.0, 0.02);
                }
                if (t == 84) {
                    this.setHealth(REVIVE_HEALTH);
                    shake(25);
                    playSound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.35F);
                    playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.6F);
                    toxicBurst(150);
                    for (LivingEntity e : inRadius(this.position(), 7.0, 4.0)) {
                        if (damage(e, 10, 2.0)) launch(e, 0.6);
                    }
                    summon(target, 3, true);
                }
            }
            default -> {}
        }
        if (t >= attack.length) endAttack();
    }

    /** 毒のブレス：前方8ブロック・左右35度。0.25 秒ごとに3ダメージ＋毒 */
    private void breathTick(int t) {
        Vec3 f = forward();
        Vec3 mouth = this.position().add(f.scale(1.2)).add(0, 2.9, 0);
        if (level() instanceof ServerLevel sl) {
            for (int i = 0; i < 6; i++) {
                Vec3 v = f.yRot((random.nextFloat() - 0.5F) * 1.1F).scale(0.5 + random.nextDouble() * 0.4).add(0, -0.12, 0);
                sl.sendParticles(new DustParticleOptions(TOXIC, 2.0F), mouth.x, mouth.y, mouth.z, 0, v.x, v.y, v.z, 1.0);
                sl.sendParticles(ParticleTypes.SNEEZE, mouth.x, mouth.y, mouth.z, 0, v.x, v.y, v.z, 1.0);
            }
        }
        if (t % 5 == 0) {
            for (LivingEntity e : inArc(8.0, 35)) {
                if (e.hurt(this.damageSources().mobAttack(this), 3.0F)) {
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                }
            }
        }
    }

    /** 手下を呼ぶ。第2形態は鉄の鎧と剣を持った近衛兵 */
    private void summon(LivingEntity target, int count, boolean guards) {
        int room = MAX_MINIONS - minions().size();
        if (!(level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < Math.min(count, room); i++) {
            double a = random.nextDouble() * Math.PI * 2, r = 3 + random.nextDouble() * 3;
            double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
            double y = groundY(x, getY() + 2, z);
            Zombie z0 = EntityType.ZOMBIE.create(sl);
            if (z0 == null) continue;
            z0.moveTo(x, y, z, random.nextFloat() * 360, 0);
            z0.addTag(MINION_TAG);
            z0.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));   // 昼でも燃えない
            z0.setDropChance(EquipmentSlot.HEAD, 0);
            if (guards) {
                z0.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                z0.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                z0.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                z0.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                for (EquipmentSlot slot : EquipmentSlot.values()) z0.setDropChance(slot, 0);
                AttributeInstance speed = z0.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) speed.setBaseValue(0.28);
            }
            z0.setPersistenceRequired();
            if (target != null) z0.setTarget(target);
            sl.addFreshEntity(z0);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(BlockPos.containing(x, y - 0.5, z))),
                    x, y + 0.2, z, 30, 0.4, 0.2, 0.4, 0.15);
            sl.sendParticles(ParticleTypes.SOUL, x, y + 0.5, z, 6, 0.3, 0.5, 0.3, 0.02);
        }
        playSound(SoundEvents.ZOMBIE_AMBIENT, 2.0F, 0.5F);
    }

    private List<Zombie> minions() {
        return level().getEntitiesOfClass(Zombie.class, this.getBoundingBox().inflate(40), z -> z.isAlive() && isMinion(z));
    }

    /** 地面を叩いた演出 */
    private void groundImpact(Vec3 at, float radius) {
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.6F);
        playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 2.0F, 0.5F);
        if (!(level() instanceof ServerLevel sl)) return;
        BlockState ground = level().getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z,
                    (int) (radius * 25), radius * 0.5, 0.2, radius * 0.5, 0.2);
        }
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.2, at.z, (int) (radius * 4), radius * 0.4, 0.1, radius * 0.4, 0.02);
    }

    private void toxicBurst(int count) {
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(new DustParticleOptions(TOXIC, 2.5F), getX(), getY() + 2.5, getZ(), count, 2.0, 1.5, 2.0, 0);
        sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 2.0, getZ(), count / 4, 1.5, 1.0, 1.5, 0.05);
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        if (random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.ASH, getX() + (random.nextDouble() - 0.5) * 2.5, getY() + random.nextDouble() * 3.5,
                    getZ() + (random.nextDouble() - 0.5) * 2.5, 0, 0, 0);
        }
        if (isPhase2() && random.nextInt(3) == 0) {
            float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
            double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
            level().addParticle(new DustParticleOptions(TOXIC, 1.0F), getX() + fx * 0.9 + (random.nextDouble() - 0.5) * 0.5,
                    getY() + 3.0, getZ() + fz * 0.9 + (random.nextDouble() - 0.5) * 0.5, 0, 0.02, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 2.5,
                getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.03, 0);
        if (random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.SOUL, getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 2,
                    getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.05, 0);
        }
    }

    // ============================================================ revive / death
    /** 第2形態で倒されたら、一度だけ死なずに復活の動きに入る（/kill では復活しない） */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && isPhase2() && !revived && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            revived = true;
            this.setHealth(1.0F);
            this.startAttack(Attack.REVIVE);
            return;
        }
        super.die(source);
        this.attack = Attack.NONE;
        if (!level().isClientSide) {
            playSound(SoundEvents.WITHER_DEATH, 2.0F, 0.5F);
            // 王が倒れると手下も崩れ落ちる
            for (Zombie z : minions()) {
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SOUL, z.getX(), z.getY() + 1, z.getZ(), 8, 0.3, 0.5, 0.3, 0.03);
                }
                z.discard();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Revived", revived);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        revived = tag.getBoolean("Revived");
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.ZOMBIE_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ZOMBIE_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 1.2F, 0.6F);
    }

    @Override
    public float getVoicePitch() { return 0.45F; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<ZombieKingEntity> main = new AnimationController<>(this, "main", 5, state -> {
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
