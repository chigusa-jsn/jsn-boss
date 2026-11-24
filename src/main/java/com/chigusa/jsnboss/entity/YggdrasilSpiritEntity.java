package com.chigusa.jsnboss.entity;

import com.chigusa.jsnboss.block.FieldLayerBlock;
import com.chigusa.jsnboss.compat.CraftBridge;
import com.chigusa.jsnboss.registry.ModBlocks;
import com.chigusa.jsnboss.registry.ModEffects;
import com.chigusa.jsnboss.registry.ModEntities;
import com.chigusa.jsnboss.vfx.Circles;
import com.chigusa.jsnboss.vfx.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ユグドラシルの精霊。第3形態の神樹が倒れると、幹の中から現れる（ボスとしては第4・第5形態）。
 *
 * 第4形態「精霊」：杖で 炎→氷→風→雷→土→闇→光→木 の順に魔法を使う。足元に属性の詠唱陣。
 * 第5形態「魔法剣士」：光の翼と結晶の剣。真ん中からほとんど動かず、床を光らせて予告してから範囲を斬る。
 *   属性は毎回ランダム。攻撃が当たった床はその属性の床（マグマ・氷・帯電・ひび割れ・闇の霧・花）に変わる。
 *   白く光る聖域の中は床も変わらず、攻撃も来ない。
 * 大詠唱（バーが半分になったとき・第5形態になった直後）：結界で無敵になり、妖精を4体呼ぶ。
 *   20秒以内に妖精を全部倒すと結界が割れてスタン。間に合わないと周りのスタミナとマナを吸って750回復し、
 *   またバーが半分まで減ると大詠唱。
 */
public class YggdrasilSpiritEntity extends AbstractBoss {
    private static final EntityDataAccessor<Boolean> EMERGING =
            SynchedEntityData.defineId(YggdrasilSpiritEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHANTING =
            SynchedEntityData.defineId(YggdrasilSpiritEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STUNNED =
            SynchedEntityData.defineId(YggdrasilSpiritEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ELEMENT =
            SynchedEntityData.defineId(YggdrasilSpiritEntity.class, EntityDataSerializers.INT);

    public static final float MAX_HP = 3000.0F, BAR = 1500.0F, CHANT_HEAL = 750.0F;
    private static final int CHANT_TICKS = 400, STUN_TICKS = 100, WARN = 20;
    private static final int FIELD_LIFE = 200, SANCTUARY_LIFE = 400;

    private static final String A = "animation.yggdrasil_spirit.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(A + "idle");
    private static final RawAnimation IDLE_SWORD = RawAnimation.begin().thenLoop(A + "idle_sword");
    private static final RawAnimation CHANT = RawAnimation.begin().thenLoop(A + "chant");
    private static final RawAnimation CHANT_SWORD = RawAnimation.begin().thenLoop(A + "chant_sword");
    private static final RawAnimation STUN = RawAnimation.begin().thenLoop(A + "stun");
    private static final RawAnimation EMERGE = RawAnimation.begin().thenPlayAndHold(A + "emerge");
    private static final RawAnimation HALO = RawAnimation.begin().thenLoop(A + "halo");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(A + "death");

    public enum Attack {
        NONE(0, 0, null),
        EMERGE(60, 0, null),
        BOLT(14, 0, "bolt"),
        SPELL(34, 0, "cast_big"),
        TRANSFORM(80, 0, "transform"),
        CHANT(CHANT_TICKS, 0, null),
        STUNNED(STUN_TICKS, 0, null),
        // 第5形態の剣技
        SPIN(28, 60, "slash_spin"),
        LINES(30, 60, "slash_lines"),
        STARFALL(44, 100, "star_fall"),
        RIPPLE(52, 100, "ripple"),
        CROSS(40, 80, "cross"),
        CHECKER(44, 120, "cross"),
        DIVE(64, 200, "dive"),
        FEATHERS(36, 80, "feather_storm"),
        EIGHT(104, 600, "eight_blade");

        public final int length, cooldown;
        public final String anim;

        Attack(int length, int cooldown, String anim) {
            this.length = length;
            this.cooldown = cooldown;
            this.anim = anim;
        }
    }

    /** 予告してから落ちる範囲攻撃。tiles は (x,z) → 床の層を置く高さ */
    private static final class Strike {
        final Map<Long, Integer> tiles;
        final Element element;
        final int fireAt;
        final float damage;
        final boolean field;

        Strike(Map<Long, Integer> tiles, Element element, int fireAt, float damage, boolean field) {
            this.tiles = tiles;
            this.element = element;
            this.fireAt = fireAt;
            this.damage = damage;
            this.field = field;
        }
    }

    private record Sanctuary(Vec3 center, double radius) {}

    private Attack attack = Attack.NONE;
    private Attack lastAttack = Attack.NONE;
    private int attackTick;
    private int globalCooldown = 40;
    private final Map<Attack, Integer> cooldowns = new EnumMap<>(Attack.class);
    private Element spellElement = Element.FIRE;        // 第4形態：次に使う属性（順番）
    private Element lastRandom = null;                   // 第5形態：前の属性（続けて同じにしない）
    private Vec3 home;                                   // 戦いの真ん中（神樹が立っていた場所）
    private Vec3 diveTarget = Vec3.ZERO;

    private final List<Strike> strikes = new ArrayList<>();
    private final Map<BlockPos, Long> fieldTiles = new HashMap<>();
    private final List<Sanctuary> sanctuaries = new ArrayList<>();
    private long sanctuaryUntil;

    // 大詠唱
    private boolean chantCleared;        // この形態の大詠唱を止めた
    private boolean chantArmed = true;   // バーが半分を切ったら大詠唱
    private boolean openingChant;        // 第5形態になった直後の大詠唱
    private boolean chantIsOpening;      // 今の大詠唱が「直後の大詠唱」か（止めても半分の大詠唱は残る）
    private final List<UUID> fairies = new ArrayList<>();
    private final ServerBossEvent chantBar = new ServerBossEvent(Component.translatable("boss.jsnboss.grand_chant"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    public YggdrasilSpiritEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN, BossEvent.BossBarColor.WHITE);
        this.xpReward = 800;
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HP)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.ARMOR_TOUGHNESS, 5.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    /** 神樹の幹の中から現れる */
    public static YggdrasilSpiritEntity emergeFrom(YggdrasilEntity tree) {
        YggdrasilSpiritEntity s = new YggdrasilSpiritEntity(ModEntities.YGGDRASIL_SPIRIT.get(), tree.level());
        s.moveTo(tree.getX(), tree.getY() + 2.5, tree.getZ(), tree.getYRot(), 0);
        s.home = tree.position();
        s.entityData.set(EMERGING, true);
        s.attack = Attack.EMERGE;
        tree.level().addFreshEntity(s);
        return s;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(EMERGING, false);
        builder.define(CHANTING, false);
        builder.define(STUNNED, false);
        builder.define(ELEMENT, 0);
    }

    public boolean isEmerging() { return this.entityData.get(EMERGING); }
    public boolean isChanting() { return this.entityData.get(CHANTING); }
    public boolean isStunned() { return this.entityData.get(STUNNED); }
    public Element element() { return Element.values()[Mth.clamp(this.entityData.get(ELEMENT), 0, 7)]; }

    private void setElement(Element e) { this.entityData.set(ELEMENT, e.ordinal()); }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected boolean isInvulnerableNow() {
        return attack == Attack.EMERGE || attack == Attack.TRANSFORM || attack == Attack.CHANT;
    }

    @Override
    protected boolean canHit(LivingEntity e) {
        return super.canHit(e) && !(e instanceof SpiritFairyEntity);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isStunned()) amount *= 1.5F;                    // 結界が割れて動けない間は大ダメージ
        return super.hurt(source, amount);
    }

    // ============================================================ AI
    @Override
    protected void bossAiStep() {
        if (home == null) home = this.position();
        LivingEntity target = this.getTarget();
        tickStrikes();
        tickFields();

        if (attack != Attack.NONE) {
            tickAttack(target);
            return;
        }
        // 第5形態へ
        if (!isPhase2() && getHealth() <= BAR) {
            startAttack(Attack.TRANSFORM);
            return;
        }
        // 大詠唱
        float half = isPhase2() ? BAR * 0.5F : BAR * 1.5F;
        if (!chantCleared && getHealth() > half + 1) chantArmed = true;
        if (openingChant || (!chantCleared && chantArmed && getHealth() <= half)) {
            chantIsOpening = openingChant;
            openingChant = false;
            chantArmed = false;
            startAttack(Attack.CHANT);
            return;
        }

        if (globalCooldown > 0) globalCooldown--;
        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
        if (isPhase2()) {
            hoverAt(home.add(0, 2.5, 0), 0.08);
            if (target != null) face(target, 10);
        } else {
            hoverAround(target);
            if (target != null) face(target, 20);
        }
        if (target == null || !target.isAlive()) return;
        if (globalCooldown <= 0) {
            Attack next = chooseAttack(target);
            if (next != null) startAttack(next);
        }
    }

    /** 第4形態：相手から 10 ブロックほど離れて、地面から 4 ブロックの高さを漂う */
    private void hoverAround(LivingEntity target) {
        Vec3 want;
        if (target != null) {
            Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-3) away = new Vec3(1, 0, 0);
            want = target.position().add(away.normalize().yRot(0.015F).scale(10.0));
            if (want.distanceTo(home) > 18) want = home.add(want.subtract(home).normalize().scale(18));
        } else {
            want = home;
        }
        double gy = groundY(want.x, getY() + 1, want.z);
        hoverAt(new Vec3(want.x, gy + 4.0 + Math.sin(tickCount * 0.05) * 0.8, want.z), 0.06);
    }

    private void hoverAt(Vec3 want, double accel) {
        Vec3 d = want.subtract(this.position());
        Vec3 v = this.getDeltaMovement().scale(0.8);
        if (d.length() > 0.3) v = v.add(d.normalize().scale(Math.min(accel, d.length() * 0.04)));
        this.setDeltaMovement(v);
    }

    private Attack chooseAttack(LivingEntity target) {
        List<Attack> pool = new ArrayList<>();
        if (!isPhase2()) {
            add(pool, Attack.SPELL, 3);
            add(pool, Attack.BOLT, 2);
        } else {
            add(pool, Attack.SPIN, inRadius(this.position(), 7, 6).isEmpty() ? 1 : 4);
            add(pool, Attack.LINES, 3);
            add(pool, Attack.STARFALL, 3);
            add(pool, Attack.RIPPLE, 2);
            add(pool, Attack.CROSS, 2);
            add(pool, Attack.CHECKER, 2);
            add(pool, Attack.DIVE, 2);
            add(pool, Attack.FEATHERS, 2);
            add(pool, Attack.EIGHT, 2);
        }
        if (pool.isEmpty()) return isPhase2() ? null : Attack.BOLT;
        return pool.get(this.random.nextInt(pool.size()));
    }

    private void add(List<Attack> pool, Attack a, int weight) {
        if (cooldowns.getOrDefault(a, 0) > 0 || (a == lastAttack && a != Attack.SPELL)) return;
        for (int i = 0; i < weight; i++) pool.add(a);
    }

    private void startAttack(Attack a) {
        this.attack = a;
        this.attackTick = 0;
        this.getNavigation().stop();
        if (a.anim != null) this.triggerAnim("main", a.name());
        if (isPhase2() && a.cooldown > 0) setElement(randomElement());
        switch (a) {
            case CHANT -> beginChant();
            case STUNNED -> this.entityData.set(STUNNED, true);
            default -> {}
        }
    }

    private void endAttack() {
        Attack done = this.attack;
        this.attack = Attack.NONE;
        this.lastAttack = done;
        this.cooldowns.put(done, done.cooldown);
        switch (done) {
            case EMERGE -> this.entityData.set(EMERGING, false);
            case STUNNED -> this.entityData.set(STUNNED, false);
            case TRANSFORM -> openingChant = true;
            default -> {}
        }
        this.globalCooldown = switch (done) {
            case BOLT -> 16;
            case SPELL -> 34;
            case EMERGE, TRANSFORM, CHANT, STUNNED -> 20;
            default -> 26;
        };
    }

    private Element randomElement() {
        Element e;
        do {
            e = Element.values()[random.nextInt(8)];
        } while (e == lastRandom);
        lastRandom = e;
        return e;
    }

    // ============================================================ attacks
    private void tickAttack(LivingEntity target) {
        int t = ++this.attackTick;
        if (attack != Attack.DIVE && attack != Attack.CHANT && attack != Attack.STUNNED && attack != Attack.EMERGE) {
            if (isPhase2()) hoverAt(home.add(0, 2.5, 0), 0.05);
            else this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
        }
        switch (attack) {
            case EMERGE -> {
                this.setDeltaMovement(Vec3.ZERO);
                if (t == 1) {
                    playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 0.8F);
                    shake(30);
                }
                if (t % 4 == 0 && level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 12, 0.6, 1.5, 0.6, 0.08);
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 2, getZ(), 10, 2, 2, 2, 0.02);
                }
                if (t == 50) {
                    playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0F, 0.6F);
                    if (level() instanceof ServerLevel sl) Circles.flash(sl, home, Circles.NATURE, 10);
                }
            }
            case BOLT -> {                              // 杖の魔弾
                if (target != null) face(target, 30);
                if (t == 6 && target != null) {
                    Vec3 from = this.position().add(0, 2.4, 0).add(forward().scale(1.2));
                    Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from).normalize();
                    for (int i = -1; i <= 1; i++) {
                        FeatherShotEntity.shoot(this, from, aim.yRot(i * 9 * Mth.DEG_TO_RAD).scale(1.0), 8.0F);
                    }
                    playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 1.4F);
                }
            }
            case SPELL -> tickSpell(target, t);
            case TRANSFORM -> {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
                if (t == 1) {
                    playSound(SoundEvents.BEACON_POWER_SELECT, 3.0F, 0.6F);
                    if (level() instanceof ServerLevel sl) Circles.grand(sl, home, Circles.HOLY, 12, 80);
                }
                if (t == 40) {
                    setPhase(2);
                    shake(30);
                    playSound(SoundEvents.TOTEM_USE, 2.0F, 0.8F);
                    playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0F, 0.6F);
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.5, getZ(), 150, 3, 2, 3, 0.3);
                        sl.sendParticles(new DustParticleOptions(new Vector3f(0.6F, 1.0F, 0.8F), 2.0F), getX(), getY() + 2.5, getZ(), 100, 4, 2, 4, 0);
                    }
                    for (LivingEntity e : inRadius(this.position(), 9.0, 6.0)) {
                        e.knockback(2.5, getX() - e.getX(), getZ() - e.getZ());
                        launch(e, 0.6);
                    }
                    refreshSanctuaries();
                }
            }
            case CHANT -> tickChant(t);
            case STUNNED -> {
                hoverAt(new Vec3(getX(), groundY(getX(), getY() + 1, getZ()) + 0.6, getZ()), 0.05);
                if (t % 10 == 0 && level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 3.5, getZ(), 6, 0.4, 0.2, 0.4, 0.1);
                }
            }
            case SPIN -> {                              // 円舞斬：周り 5 ブロックの円
                if (t == 1) strike(disk(position(), 5.5, home.y), element(), WARN - 2, 18, true);
            }
            case LINES -> {                             // 五方閃：外へ 15 ブロックの線を 5 本
                if (t == 1) {
                    double a0 = random.nextDouble() * Math.PI * 2;
                    Map<Long, Integer> tiles = new HashMap<>();
                    for (int i = 0; i < 5; i++) {
                        Vec3 d = new Vec3(Math.cos(a0 + i * Math.PI * 2 / 5), 0, Math.sin(a0 + i * Math.PI * 2 / 5));
                        line(tiles, position(), d, 2.0, 16.0, 1.2, home.y);
                    }
                    strike(tiles, element(), 16, 18, true);
                }
            }
            case STARFALL -> {                          // 星降り：玉を打ち上げて、3×3 を 10 か所
                if (t == 12) {
                    playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 3.0F, 0.6F);
                    if (level() instanceof ServerLevel sl) {
                        for (int i = 0; i < 10; i++) {
                            sl.sendParticles(dust(element(), 2.0F), getX(), getY() + 4 + i * 1.5, getZ(), 6, 0.3, 0.3, 0.3, 0);
                        }
                    }
                    List<Vec3> spots = new ArrayList<>();
                    for (Player p : players(24)) spots.add(p.position());
                    while (spots.size() < 10) {
                        double a = random.nextDouble() * Math.PI * 2, r = 3 + random.nextDouble() * 13;
                        spots.add(home.add(Math.cos(a) * r, 0, Math.sin(a) * r));
                    }
                    for (int i = 0; i < 10; i++) {
                        Vec3 c = spots.get(i);
                        Map<Long, Integer> tiles = new HashMap<>();
                        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                            addTile(tiles, Mth.floor(c.x) + dx, Mth.floor(c.z) + dz, home.y);
                        strike(tiles, element(), 22 + i * 2, 16, true);
                    }
                }
            }
            case RIPPLE -> {                            // 波紋斬：ドーナツ型の輪が 3 回外へ
                for (int i = 0; i < 3; i++) {
                    if (t == 2 + i * 12) {
                        double r0 = 3 + i * 4.5;
                        strike(ring(position(), r0, r0 + 2.5, home.y), element(), WARN - 4, 16, true);
                    }
                }
            }
            case CROSS -> {                             // 十字閃：十字 → バツ印
                if (t == 1) strike(star(position(), 0, 17, home.y), element(), 12, 18, true);
                if (t == 12) strike(star(position(), Math.PI / 4, 17, home.y), element(), 12, 18, true);
            }
            case CHECKER -> {                           // 市松斬：白黒交互に 2 回
                if (t == 2) strike(checker(home, 15, 0, home.y), element(), WARN, 14, true);
                if (t == 22) strike(checker(home, 15, 1, home.y), element(), WARN - 4, 14, true);
            }
            case DIVE -> tickDive(target, t);
            case FEATHERS -> {                          // 羽根の嵐：羽根が落ちたところ（2×2）が光って爆ぜる
                if (t == 14) {
                    playSound(SoundEvents.PHANTOM_FLAP, 3.0F, 1.2F);
                    for (int i = 0; i < 16; i++) {
                        double a = random.nextDouble() * Math.PI * 2, r = 3 + random.nextDouble() * 12;
                        int x = Mth.floor(getX() + Math.cos(a) * r), z = Mth.floor(getZ() + Math.sin(a) * r);
                        Map<Long, Integer> tiles = new HashMap<>();
                        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) addTile(tiles, x + dx, z + dz, home.y);
                        strike(tiles, element(), WARN - 4 + (i % 4) * 2, 14, true);
                    }
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 3, getZ(), 60, 3, 1.5, 3, 0.25);
                    }
                }
            }
            case EIGHT -> {                             // 八属の剣舞：8 つの扇形を、8 属性で順番に
                for (int i = 0; i < 8; i++) {
                    if (t == 4 + i * 10) {
                        Element el = Element.values()[i];
                        setElement(el);
                        double a0 = i * Math.PI / 4 + getYRot() * Mth.DEG_TO_RAD;
                        strike(sector(home, 18, a0, a0 + Math.PI / 4, home.y), el, WARN, 18, true);
                    }
                }
            }
            default -> {}
        }
        if (attackTick >= attack.length) endAttack();
    }

    /** 第4形態の魔法。属性は順番に回る */
    private void tickSpell(LivingEntity target, int t) {
        Element el = spellElement;
        if (t == 1) {
            setElement(el);
            if (level() instanceof ServerLevel sl) {
                Circles.charge(sl, new Vec3(getX(), groundY(getX(), getY() + 1, getZ()), getZ()), el.circle, 4.5F, 20);
                for (Player p : players(32)) Circles.charge(sl, p.position(), el.circle, 3.5F, 20);
            }
            playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 2.0F, 1.3F);
        }
        if (target != null) face(target, 20);
        if (t != 20) return;
        spellElement = el.next();
        List<Player> ps = players(32);
        if (ps.isEmpty() && target instanceof Player p) ps = List.of(p);
        if (level() instanceof ServerLevel sl) Circles.flash(sl, new Vec3(getX(), groundY(getX(), getY() + 1, getZ()), getZ()), el.circle, 5);
        switch (el) {
            case FIRE -> {                               // 炎：火の玉が 3 つずつ降ってくる
                for (Player p : ps) {
                    for (int i = 0; i < 3; i++) {
                        Vec3 c = p.position().add(random.nextGaussian() * 2.5, 0, random.nextGaussian() * 2.5);
                        strike(disk(c, 2.2, p.getY()), el, 14 + i * 5, 14, false);
                    }
                }
                playSound(SoundEvents.BLAZE_SHOOT, 2.5F, 0.6F);
            }
            case ICE -> {                                // 氷：氷の槍が列になって突き出す
                for (Player p : ps) {
                    Vec3 d = p.position().subtract(position()).multiply(1, 0, 1).normalize();
                    for (int i = 0; i < 12; i++) {
                        double x = getX() + d.x * (2 + i * 1.5), z = getZ() + d.z * (2 + i * 1.5);
                        IceSpikeEntity.spawn(this, x, groundY(x, p.getY() + 3, z), z, 8 + i * 2, IceSpikeEntity.SPIKE, 13.0F);
                    }
                }
                playSound(SoundEvents.GLASS_BREAK, 2.0F, 0.7F);
            }
            case WIND -> {                               // 風：吸い寄せて、竜巻のような風の刃
                for (Player p : ps) {
                    Vec3 pull = position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(1.2);
                    p.setDeltaMovement(p.getDeltaMovement().add(pull.x, 0.35, pull.z));
                    p.hurtMarked = true;
                    Vec3 from = position().add(0, 2.2, 0);
                    Vec3 aim = p.position().add(0, 1, 0).subtract(from).normalize();
                    for (int i = -2; i <= 2; i++) WindBladeEntity.shoot(this, from, aim.yRot(i * 14 * Mth.DEG_TO_RAD).scale(0.9), 11.0F);
                }
                playSound(SoundEvents.ENDER_DRAGON_FLAP, 2.5F, 1.2F);
            }
            case THUNDER -> {                            // 雷：落雷が続けて落ちる
                for (Player p : ps) {
                    for (int i = 0; i < 3; i++) {
                        Vec3 c = p.position().add(i == 0 ? Vec3.ZERO : new Vec3(random.nextGaussian() * 3, 0, random.nextGaussian() * 3));
                        strike(disk(c, 1.8, p.getY()), el, 12 + i * 8, 14, false);
                    }
                }
                playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5F, 1.4F);
            }
            case EARTH -> {                              // 土：岩の柱が輪になって突き出す
                for (Player p : ps) {
                    RockPillarEntity.spawn(this, p.getX(), p.getZ(), 16, 15.0F);
                    for (int i = 0; i < 6; i++) {
                        double a = i * Math.PI / 3, x = p.getX() + Math.cos(a) * 3, z = p.getZ() + Math.sin(a) * 3;
                        RockPillarEntity.spawn(this, x, z, 24, 15.0F);
                    }
                }
                shake(8);
            }
            case DARK -> {                               // 闇：小さなブラックホール
                if (target != null) BlackHoleEntity.spawn(this, target.position().add(0, 1.5, 0), 6.0F);
                for (Player p : ps) strike(ring(p.position(), 2.5, 4.0, p.getY()), el, 30, 12, false);
            }
            case LIGHT -> {                              // 光：光の柱
                for (Player p : ps) {
                    LightPillarEntity.spawn(this, p.getX(), groundY(p.getX(), p.getY() + 2, p.getZ()), p.getZ(), 16.0F);
                    for (int i = 0; i < 4; i++) {
                        double a = i * Math.PI / 2 + 0.4, x = p.getX() + Math.cos(a) * 4, z = p.getZ() + Math.sin(a) * 4;
                        LightPillarEntity.spawn(this, x, groundY(x, p.getY() + 2, z), z, 16.0F);
                    }
                }
                playSound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 1.6F);
            }
            case WOOD -> {                               // 木：根が突き出し、花びらが舞う
                for (Player p : ps) {
                    Vec3 d = p.position().subtract(position()).multiply(1, 0, 1).normalize();
                    for (int i = 0; i < 8; i++) {
                        RockPillarEntity.spawnRoot(this, p.getX() + d.x * (i - 4) * 1.6, p.getZ() + d.z * (i - 4) * 1.6, 10 + i * 2, 14.0F);
                    }
                }
                Vec3 from = position().add(0, 2.4, 0);
                for (int i = 0; i < 16; i++) {
                    Vec3 v = new Vec3(Math.cos(i * Math.PI / 8), 0.05, Math.sin(i * Math.PI / 8)).scale(0.7);
                    FeatherShotEntity.shoot(this, from.add(v.scale(1.5)), v, 8.0F);
                }
                playSound(SoundEvents.AZALEA_LEAVES_BREAK, 3.0F, 0.6F);
            }
        }
    }

    /** 天翔斬：舞い上がって、着地点を光らせてから急降下 */
    private void tickDive(LivingEntity target, int t) {
        if (t < 24) {
            hoverAt(home.add(0, 12, 0), 0.12);
            if (t == 2) playSound(SoundEvents.PHANTOM_FLAP, 3.0F, 0.6F);
        }
        if (t == 14) {
            diveTarget = target != null ? target.position() : home;
            if (diveTarget.distanceTo(home) > 18) diveTarget = home.add(diveTarget.subtract(home).normalize().scale(18));
            strike(disk(diveTarget, 6.0, home.y), element(), 24, 22, true);
        }
        if (t >= 24 && t < 36) hoverAt(diveTarget.add(0, 12, 0), 0.25);
        if (t == 36) {
            playSound(SoundEvents.PHANTOM_SWOOP, 3.0F, 0.6F);
            this.setDeltaMovement(diveTarget.add(0, 0.5, 0).subtract(position()).scale(0.5));
        }
        if (t == 38) {
            this.setPos(diveTarget.x, groundY(diveTarget.x, diveTarget.y + 2, diveTarget.z) + 0.3, diveTarget.z);
            this.setDeltaMovement(Vec3.ZERO);
            shake(16);
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 1.2F);
        }
        if (t > 44) hoverAt(home.add(0, 2.5, 0), 0.1);
    }

    // ============================================================ ボスバー
    /** 形態ではなくHPでゲージを決める：1500超なら2本目、以下なら最後の1本。第5形態で回復しすぎるとゲージが戻る */
    @Override
    protected float bossBarProgress() {
        float hp = getHealth();
        return Mth.clamp(hp > BAR ? (hp - BAR) / BAR : hp / BAR, 0.0F, 1.0F);
    }

    @Override
    protected int bossBarsLeft() {
        return getHealth() > BAR ? 1 : 0;
    }

    // ============================================================ 大詠唱
    private void beginChant() {
        this.entityData.set(CHANTING, true);
        fairies.clear();
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 3.0F, 0.6F);
        playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 1.2F);
        if (level() instanceof ServerLevel sl) {
            Circles.grand(sl, home, isPhase2() ? Circles.STAR : Circles.NATURE, 15.0F, CHANT_TICKS);
            List<Element> els = new ArrayList<>(List.of(Element.values()));
            java.util.Collections.shuffle(els, new java.util.Random(random.nextLong()));
            int n = isPhase2() ? 6 : 4;                  // 第5形態は6体
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n + random.nextDouble() * 0.5;
                SpiritFairyEntity f = SpiritFairyEntity.summon(this, home.add(Math.cos(a) * 9, 4, Math.sin(a) * 9), els.get(i), a);
                fairies.add(f.getUUID());
                Circles.circle(sl, f.position(), els.get(i).circle, 1.5F, 30, com.chigusa.jsnboss.vfx.CircleOptions.FLASH);
            }
            chantBar.setProgress(1.0F);
            for (ServerPlayer p : sl.getPlayers(p -> p.distanceToSqr(this) < 64 * 64)) chantBar.addPlayer(p);
        }
    }

    private void tickChant(int t) {
        hoverAt(home.add(0, 6, 0), 0.08);
        chantBar.setProgress(Math.max(0, 1.0F - (float) t / CHANT_TICKS));
        if (level() instanceof ServerLevel sl) {
            if (t % 5 == 0) sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 6, 1.2, 1.5, 1.2, 0.05);
            if (t % 20 == 0) {
                for (ServerPlayer p : sl.getPlayers(p -> p.distanceToSqr(this) < 64 * 64)) chantBar.addPlayer(p);
                playSound(SoundEvents.BEACON_AMBIENT, 3.0F, 0.6F + t / (float) CHANT_TICKS);
            }
            // 結界
            if (t % 3 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = random.nextDouble() * Math.PI * 2, b = random.nextDouble() * Math.PI;
                    sl.sendParticles(dust(Element.LIGHT, 1.2F), getX() + Math.cos(a) * Math.sin(b) * 3, getY() + 2 + Math.cos(b) * 3,
                            getZ() + Math.sin(a) * Math.sin(b) * 3, 1, 0, 0, 0, 0);
                }
            }
        }
        if (t > 10 && fairiesAlive() == 0) {                 // 妖精を全部倒した → 結界が割れる
            endChant();
            if (!chantIsOpening) chantCleared = true;
            playSound(SoundEvents.GLASS_BREAK, 3.0F, 0.5F);
            playSound(SoundEvents.WITHER_HURT, 2.0F, 1.6F);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 120, 2.5, 2.5, 2.5, 0.4);
            }
            for (Player p : players(48)) {
                p.displayClientMessage(Component.translatable("message.jsnboss.chant_broken"), true);
            }
            this.attack = Attack.NONE;
            startAttack(Attack.STUNNED);
            return;
        }
        if (t >= CHANT_TICKS) {                              // 間に合わなかった → スタミナとマナを吸って回復
            endChant();
            for (Entity e : fairyEntities()) e.discard();
            // 第5形態でも上限はボス全体の最大HP。1500を超えた分はゲージが1本戻る（姿は第5形態のまま）
            setHealth(Math.min(MAX_HP, getHealth() + CHANT_HEAL));
            playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 3.0F, 0.6F);
            shake(30);
            if (level() instanceof ServerLevel sl) {
                Circles.flash(sl, home, isPhase2() ? Circles.STAR : Circles.NATURE, 18);
                for (Player p : players(40)) {
                    if (p instanceof ServerPlayer sp) CraftBridge.drain(sp);
                    p.displayClientMessage(Component.translatable("message.jsnboss.chant_drain"), true);
                    Vec3 from = p.position().add(0, 1, 0);
                    Vec3 to = position().add(0, 2, 0).subtract(from);
                    for (int i = 0; i < 12; i++) {
                        Vec3 q = from.add(to.scale(i / 12.0));
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, q.x, q.y, q.z, 2, 0.1, 0.1, 0.1, 0);
                    }
                }
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 2, getZ(), 80, 1.5, 2, 1.5, 0.4);
            }
        }
    }

    private void endChant() {
        this.entityData.set(CHANTING, false);
        chantBar.removeAllPlayers();
    }

    private List<Entity> fairyEntities() {
        List<Entity> out = new ArrayList<>();
        if (level() instanceof ServerLevel sl) {
            for (UUID id : fairies) {
                Entity e = sl.getEntity(id);
                if (e != null && e.isAlive()) out.add(e);
            }
        }
        return out;
    }

    private int fairiesAlive() {
        return fairyEntities().size();
    }

    // ============================================================ 予告 → 攻撃 → 床
    /** 予告の床を置き、delay tick 後に攻撃が落ちる */
    private void strike(Map<Long, Integer> tiles, Element el, int delay, float damage, boolean field) {
        tiles.entrySet().removeIf(e -> inSanctuary(BlockPos.getX(e.getKey()), BlockPos.getZ(e.getKey())));
        if (tiles.isEmpty()) return;
        BlockState warn = ModBlocks.FIELD_LAYER.get().defaultBlockState()
                .setValue(FieldLayerBlock.KIND, kindOf(el)).setValue(FieldLayerBlock.WARN, true);
        for (Map.Entry<Long, Integer> e : tiles.entrySet()) {
            BlockPos p = new BlockPos(BlockPos.getX(e.getKey()), e.getValue(), BlockPos.getZ(e.getKey()));
            if (canLayer(p)) level().setBlock(p, warn, 3);
        }
        strikes.add(new Strike(tiles, el, tickCount + Math.max(4, delay), dmg(damage * 0.85F, damage), field && isPhase2()));
    }

    private void tickStrikes() {
        Iterator<Strike> it = strikes.iterator();
        List<Strike> fire = new ArrayList<>();
        while (it.hasNext()) {
            Strike s = it.next();
            if (tickCount >= s.fireAt) {
                fire.add(s);
                it.remove();
            }
        }
        for (Strike s : fire) resolve(s);
    }

    private void resolve(Strike s) {
        if (!(level() instanceof ServerLevel sl)) return;
        Element el = s.element;
        // 当たり判定
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(40, 20, 40), this::canHit)) {
            Integer y = s.tiles.get(BlockPos.asLong(e.getBlockX(), 0, e.getBlockZ()));
            if (y == null || e.getY() < y - 1.5 || e.getY() > y + 3.5) continue;
            if (damage(e, s.damage, 0.3)) applyElement(e, el);
        }
        // 見た目
        int i = 0, step = Math.max(1, s.tiles.size() / 40);
        double sx = 0, sz = 0, sy = 0;
        for (Map.Entry<Long, Integer> e : s.tiles.entrySet()) {
            double x = BlockPos.getX(e.getKey()) + 0.5, z = BlockPos.getZ(e.getKey()) + 0.5, y = e.getValue() + 0.1;
            sx += x; sz += z; sy += y;
            if (i++ % step != 0) continue;
            sl.sendParticles(dust(el, 1.6F), x, y + 0.3, z, 3, 0.3, 0.3, 0.3, 0);
            ParticleOptions p = switch (el) {
                case FIRE -> ParticleTypes.FLAME;
                case ICE -> ParticleTypes.SNOWFLAKE;
                case WIND -> ParticleTypes.CLOUD;
                case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
                case EARTH -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
                case DARK -> ParticleTypes.SQUID_INK;
                case LIGHT -> ParticleTypes.END_ROD;
                case WOOD -> ParticleTypes.CHERRY_LEAVES;
            };
            sl.sendParticles(p, x, y + 0.4, z, 4, 0.3, 0.5, 0.3, el == Element.EARTH ? 0.01 : 0.08);
            if (el == Element.FIRE && i % (step * 4) == 1) sl.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.5, z, 1, 0, 0, 0, 0);
        }
        int n = s.tiles.size();
        Vec3 c = new Vec3(sx / n, sy / n, sz / n);
        if (el == Element.THUNDER) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
            if (bolt != null) {
                bolt.moveTo(c.x, c.y, c.z);
                bolt.setVisualOnly(true);
                level().addFreshEntity(bolt);
            }
        }
        SoundEvent sound = switch (el) {
            case FIRE -> SoundEvents.GENERIC_EXPLODE.value();
            case ICE -> SoundEvents.GLASS_BREAK;
            case WIND -> SoundEvents.PHANTOM_SWOOP;
            case THUNDER -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case EARTH -> SoundEvents.DRIPSTONE_BLOCK_BREAK;
            case DARK -> SoundEvents.SCULK_SHRIEKER_SHRIEK;
            case LIGHT -> SoundEvents.AMETHYST_BLOCK_BREAK;
            case WOOD -> SoundEvents.AZALEA_LEAVES_BREAK;
        };
        level().playSound(null, c.x, c.y, c.z, sound, getSoundSource(), 1.5F, 0.8F + random.nextFloat() * 0.3F);
        // 床が属性の床に変わる（風・光は残らない）
        BlockState field = ModBlocks.FIELD_LAYER.get().defaultBlockState().setValue(FieldLayerBlock.KIND, kindOf(el));
        long until = level().getGameTime() + FIELD_LIFE;
        for (Map.Entry<Long, Integer> e : s.tiles.entrySet()) {
            BlockPos p = new BlockPos(BlockPos.getX(e.getKey()), e.getValue(), BlockPos.getZ(e.getKey()));
            BlockState now = level().getBlockState(p);
            if (!now.is(ModBlocks.FIELD_LAYER.get())) continue;
            if (s.field && el.leavesField && !inSanctuary(p.getX(), p.getZ())) {
                level().setBlock(p, field, 3);
                fieldTiles.put(p, until);
            } else if (now.getValue(FieldLayerBlock.WARN)) {
                level().removeBlock(p, false);
                fieldTiles.remove(p);
            }
        }
    }

    private void applyElement(LivingEntity e, Element el) {
        switch (el) {
            case FIRE -> e.igniteForSeconds(4.0F);
            case ICE -> IceWitchEntity.freeze(e, 60);
            case WIND -> {
                launch(e, 0.9);
                e.knockback(1.2, getX() - e.getX(), getZ() - e.getZ());
            }
            case THUNDER -> e.addEffect(new MobEffectInstance(ModEffects.STUN, 20, 0));
            case EARTH -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            case DARK -> {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
            }
            case LIGHT -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            case WOOD -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
        }
    }

    /** 残っている床を見回る：時間が来たら消す。花の床が残っていると精霊が回復する */
    private void tickFields() {
        if (tickCount % 10 != 0) return;
        long now = level().getGameTime();
        int flowers = 0;
        Iterator<Map.Entry<BlockPos, Long>> it = fieldTiles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> e = it.next();
            BlockState s = level().getBlockState(e.getKey());
            if (!s.is(ModBlocks.FIELD_LAYER.get())) {
                it.remove();
                continue;
            }
            if (now >= e.getValue()) {
                level().removeBlock(e.getKey(), false);
                it.remove();
            } else if (s.getValue(FieldLayerBlock.KIND) == FieldLayerBlock.Kind.WOOD) {
                flowers++;
            }
        }
        if (flowers > 0 && tickCount % 20 == 0 && isAlive()) heal(Math.min(15.0F, flowers * 0.25F));
        if (isPhase2() && now >= sanctuaryUntil && attack != Attack.CHANT) refreshSanctuaries();
    }

    /** 聖域（白く光る安全な円）を置き直す。20 秒ごとに場所が変わる */
    private void refreshSanctuaries() {
        clearSanctuaries();
        BlockState holy = ModBlocks.FIELD_LAYER.get().defaultBlockState().setValue(FieldLayerBlock.KIND, FieldLayerBlock.Kind.SANCTUARY);
        long until = level().getGameTime() + SANCTUARY_LIFE + 20;
        for (int k = 0; k < 2; k++) {
            double a = random.nextDouble() * Math.PI * 2, r = 7 + random.nextDouble() * 7;
            Sanctuary s = new Sanctuary(home.add(Math.cos(a) * r, 0, Math.sin(a) * r), 2.6);
            sanctuaries.add(s);
            for (Map.Entry<Long, Integer> e : disk(s.center, s.radius, home.y).entrySet()) {
                BlockPos p = new BlockPos(BlockPos.getX(e.getKey()), e.getValue(), BlockPos.getZ(e.getKey()));
                if (canLayer(p)) {
                    level().setBlock(p, holy, 3);
                    fieldTiles.put(p, until);
                }
            }
            if (level() instanceof ServerLevel sl) Circles.circle(sl, s.center.add(0, home.y - s.center.y + 0.1, 0), Circles.HOLY, (float) s.radius + 0.5F, SANCTUARY_LIFE, 0);
        }
        sanctuaryUntil = level().getGameTime() + SANCTUARY_LIFE;
    }

    private void clearSanctuaries() {
        for (Map.Entry<BlockPos, Long> e : new ArrayList<>(fieldTiles.entrySet())) {
            BlockState s = level().getBlockState(e.getKey());
            if (s.is(ModBlocks.FIELD_LAYER.get()) && s.getValue(FieldLayerBlock.KIND) == FieldLayerBlock.Kind.SANCTUARY) {
                level().removeBlock(e.getKey(), false);
                fieldTiles.remove(e.getKey());
            }
        }
        sanctuaries.clear();
    }

    private boolean inSanctuary(int x, int z) {
        for (Sanctuary s : sanctuaries) {
            double dx = x + 0.5 - s.center.x, dz = z + 0.5 - s.center.z;
            if (dx * dx + dz * dz <= (s.radius + 0.6) * (s.radius + 0.6)) return true;
        }
        return false;
    }

    private void clearAllTiles() {
        for (BlockPos p : fieldTiles.keySet()) {
            if (level().getBlockState(p).is(ModBlocks.FIELD_LAYER.get())) level().removeBlock(p, false);
        }
        fieldTiles.clear();
        for (Strike s : strikes) {
            for (Map.Entry<Long, Integer> e : s.tiles.entrySet()) {
                BlockPos p = new BlockPos(BlockPos.getX(e.getKey()), e.getValue(), BlockPos.getZ(e.getKey()));
                if (level().getBlockState(p).is(ModBlocks.FIELD_LAYER.get())) level().removeBlock(p, false);
            }
        }
        strikes.clear();
        sanctuaries.clear();
    }

    // ------------------------------------------------------------ 形
    private void addTile(Map<Long, Integer> tiles, int x, int z, double refY) {
        long key = BlockPos.asLong(x, 0, z);
        if (tiles.containsKey(key)) return;
        BlockPos p = surface(x, z, refY);
        if (p != null) tiles.put(key, p.getY());
    }

    private Map<Long, Integer> disk(Vec3 c, double r, double refY) {
        Map<Long, Integer> t = new HashMap<>();
        int R = Mth.ceil(r);
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            int x = Mth.floor(c.x) + dx, z = Mth.floor(c.z) + dz;
            double ddx = x + 0.5 - c.x, ddz = z + 0.5 - c.z;
            if (ddx * ddx + ddz * ddz <= r * r) addTile(t, x, z, refY);
        }
        return t;
    }

    private Map<Long, Integer> ring(Vec3 c, double r0, double r1, double refY) {
        Map<Long, Integer> t = new HashMap<>();
        int R = Mth.ceil(r1);
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            int x = Mth.floor(c.x) + dx, z = Mth.floor(c.z) + dz;
            double ddx = x + 0.5 - c.x, ddz = z + 0.5 - c.z, d2 = ddx * ddx + ddz * ddz;
            if (d2 >= r0 * r0 && d2 <= r1 * r1) addTile(t, x, z, refY);
        }
        return t;
    }

    private void line(Map<Long, Integer> t, Vec3 c, Vec3 dir, double start, double len, double halfWidth, double refY) {
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        for (double s = start; s <= len; s += 0.5) {
            for (double w = -halfWidth; w <= halfWidth; w += 0.5) {
                addTile(t, Mth.floor(c.x + dir.x * s + side.x * w), Mth.floor(c.z + dir.z * s + side.z * w), refY);
            }
        }
    }

    /** 十字（rot=0）やバツ印（rot=45°） */
    private Map<Long, Integer> star(Vec3 c, double rot, double len, double refY) {
        Map<Long, Integer> t = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            double a = rot + i * Math.PI / 2;
            line(t, c, new Vec3(Math.cos(a), 0, Math.sin(a)), 0, len, 1.3, refY);
        }
        return t;
    }

    /** 市松模様（2×2 のマス） */
    private Map<Long, Integer> checker(Vec3 c, double r, int parity, double refY) {
        Map<Long, Integer> t = new HashMap<>();
        int R = Mth.ceil(r);
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            int x = Mth.floor(c.x) + dx, z = Mth.floor(c.z) + dz;
            if (((Math.floorDiv(x, 2) + Math.floorDiv(z, 2)) & 1) != parity) continue;
            double ddx = x + 0.5 - c.x, ddz = z + 0.5 - c.z;
            if (ddx * ddx + ddz * ddz <= r * r) addTile(t, x, z, refY);
        }
        return t;
    }

    /** 扇形（a0〜a1 ラジアン） */
    private Map<Long, Integer> sector(Vec3 c, double r, double a0, double a1, double refY) {
        Map<Long, Integer> t = new HashMap<>();
        int R = Mth.ceil(r);
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            int x = Mth.floor(c.x) + dx, z = Mth.floor(c.z) + dz;
            double ddx = x + 0.5 - c.x, ddz = z + 0.5 - c.z, d2 = ddx * ddx + ddz * ddz;
            if (d2 > r * r || d2 < 1.5) continue;
            double a = Math.atan2(ddz, ddx);
            double rel = Mth.positiveModulo(a - a0, Math.PI * 2);
            if (rel <= Mth.positiveModulo(a1 - a0, Math.PI * 2)) addTile(t, x, z, refY);
        }
        return t;
    }

    /** (x,z) の地面の上の、床の層を置ける高さ（空気か、すでに床の層があるところ） */
    private BlockPos surface(int x, int z, double refY) {
        int y0 = Mth.floor(refY);
        for (int y = y0 + 4; y >= y0 - 8; y--) {
            BlockPos p = new BlockPos(x, y, z);
            BlockState s = level().getBlockState(p);
            if ((s.isAir() || s.is(ModBlocks.FIELD_LAYER.get()))
                    && level().getBlockState(p.below()).isFaceSturdy(level(), p.below(), Direction.UP)) return p;
        }
        return null;
    }

    private boolean canLayer(BlockPos p) {
        BlockState s = level().getBlockState(p);
        if (s.is(ModBlocks.FIELD_LAYER.get())) return s.getValue(FieldLayerBlock.KIND) != FieldLayerBlock.Kind.SANCTUARY;
        return s.isAir();
    }

    private static FieldLayerBlock.Kind kindOf(Element e) {
        return FieldLayerBlock.Kind.values()[e.ordinal()];
    }

    private static DustParticleOptions dust(Element e, float size) {
        return new DustParticleOptions(e.color(), size);
    }

    private List<Player> players(double r) {
        return new ArrayList<>(level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(r, 16, r),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative()));
    }

    // ============================================================ death / save
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) return;
        this.attack = Attack.NONE;
        endChant();
        for (Entity e : fairyEntities()) e.discard();
        clearAllTiles();
        this.entityData.set(STUNNED, false);
        playSound(SoundEvents.ALLAY_DEATH, 3.0F, 0.6F);
        this.setNoGravity(true);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            endChant();
            clearAllTiles();
        }
        super.remove(reason);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        chantBar.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.putDouble("HomeX", home.x);
            tag.putDouble("HomeY", home.y);
            tag.putDouble("HomeZ", home.z);
        }
        tag.putInt("SpellElement", spellElement.ordinal());
        tag.putBoolean("ChantCleared", chantCleared);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeX")) home = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
        spellElement = Element.values()[Mth.clamp(tag.getInt("SpellElement"), 0, 7)];
        chantCleared = tag.getBoolean("ChantCleared");
        this.setNoGravity(true);
    }

    @Override
    protected void onPhaseChanged(int phase) {
        chantCleared = false;
        chantArmed = false;
    }

    // ============================================================ client
    @Override
    protected void clientTick() {
        Vector3f c = element().color();
        if (random.nextInt(2) == 0) {
            level().addParticle(new DustParticleOptions(isPhase2() ? c : new Vector3f(0.6F, 1.0F, 0.75F), 1.0F),
                    getX() + (random.nextDouble() - 0.5) * (isPhase2() ? 6 : 2), getY() + 1 + random.nextDouble() * 3,
                    getZ() + (random.nextDouble() - 0.5) * (isPhase2() ? 6 : 2), 0, -0.02, 0);
        }
        if (random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.CHERRY_LEAVES, getX() + (random.nextDouble() - 0.5) * 2, getY() + 0.3,
                    getZ() + (random.nextDouble() - 0.5) * 2, 0, 0, 0);
        }
    }

    @Override
    protected void deathParticles() {
        level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 3,
                getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.08, 0);
        level().addParticle(ParticleTypes.CHERRY_LEAVES, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 3,
                getZ() + (random.nextDouble() - 0.5) * 3, 0, 0, 0);
    }

    @Override
    protected SoundEvent getAmbientSound() { return null; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.AMETHYST_BLOCK_HIT; }

    @Override
    protected SoundEvent getDeathSound() { return null; }

    // ============================================================ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<YggdrasilSpiritEntity> main = new AnimationController<>(this, "main", 5, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DEATH);
            if (this.isEmerging()) return state.setAndContinue(EMERGE);
            if (this.isStunned()) return state.setAndContinue(STUN);
            if (this.isChanting()) return state.setAndContinue(isPhase2() ? CHANT_SWORD : CHANT);
            return state.setAndContinue(isPhase2() ? IDLE_SWORD : IDLE);
        });
        for (Attack a : Attack.values()) {
            if (a.anim != null) main.triggerableAnim(a.name(), RawAnimation.begin().thenPlay(A + a.anim));
        }
        main.triggerableAnim("death", DEATH);
        controllers.add(main);
        controllers.add(new AnimationController<>(this, "halo", 0, state -> state.setAndContinue(HALO)));
    }
}
