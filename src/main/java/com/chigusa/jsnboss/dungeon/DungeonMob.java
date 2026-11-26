package com.chigusa.jsnboss.dungeon;

import com.chigusa.jsnboss.entity.AbstractBoss;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ダンジョン用のオリジナル敵モブの共通部分（自然には湧かない。スポーンエッグ・/summon・Party Dungeon のスポナーで出す）。
 *
 * 段階（Tier）ごとに：中ボスは小さめの専用ボスバー、強い敵・弱い敵は頭の上の HP バー。
 * 倒すとライトマンズカレンシーのコイン（中ボス=金、強い敵=鉄、弱い敵=銅）を落とす。
 *
 * 技は Move（名前・長さ・クールダウン・届く距離）で並べ、tickMove に中身を書く。
 * アニメは animations/entity/&lt;id&gt;.animation.json の idle / walk / death / attack_&lt;技名&gt;。
 */
public abstract class DungeonMob extends AbstractBoss {

    public enum Tier {
        MID(40, "lightmanscurrency:coin_gold", 1, 2, 60, 24),
        STRONG(20, "lightmanscurrency:coin_iron", 1, 3, 25, 30),
        WEAK(14, "lightmanscurrency:coin_copper", 1, 4, 6, 16);

        public final int deathTicks, coinMin, coinMax, xp, globalCooldown;
        public final String coin;

        Tier(int deathTicks, String coin, int coinMin, int coinMax, int xp, int globalCooldown) {
            this.deathTicks = deathTicks; this.coin = coin; this.coinMin = coinMin; this.coinMax = coinMax;
            this.xp = xp; this.globalCooldown = globalCooldown;
        }
    }

    /** 技。minRange〜maxRange（相手との横の距離）で、needsSight なら相手が見えているときだけ選ぶ */
    public record Move(String name, int length, int cooldown, double minRange, double maxRange, int weight, boolean needsSight) {}

    private static final ResourceLocation ENRAGE_ID = ResourceLocation.fromNamespaceAndPath("jsnboss", "dungeon_enrage");

    protected final Tier tier;
    private Move current;
    private int moveTick;
    private int globalCd = 30;
    private Move lastMove;
    private final Map<String, Integer> cooldowns = new HashMap<>();
    /** 1回の技で同じ相手に何度も当たらないように */
    protected final Set<UUID> hitOnce = new HashSet<>();
    private boolean enrageApplied;
    private double orbit = Math.random() * Math.PI * 2;

    protected DungeonMob(EntityType<? extends Monster> type, Level level, Tier tier, BossEvent.BossBarColor color) {
        super(type, level, color, color);
        this.tier = tier;
        this.xpReward = tier.xp;
        this.bossEvent.setDarkenScreen(false);
        this.bossEvent.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
    }

    // ============================================================ 各モブで決めること
    protected abstract List<Move> moves();

    /** 技の中身。t は技が始まってからの tick */
    protected abstract void tickMove(Move m, int t, LivingEntity target);

    /** 空を飛ぶモブ（重力なし。相手の少し上に浮いてついてくる） */
    protected boolean flying() { return false; }

    /** その場から動かないモブ */
    protected boolean stationary() { return false; }

    /** 飛ぶモブ：相手からどれくらい上に浮くか */
    protected double hoverHeight() { return 3.0; }

    /** 飛ぶモブ：相手からどれくらい離れて回るか（0 なら真上に寄ってくる） */
    protected double keepDistance() { return 0.0; }

    // ============================================================ 共通
    public Tier tier() { return tier; }

    /** 中ボスだけボスバーを出す（強い敵・弱い敵は頭の上の HP バー） */
    @Override
    protected boolean showsBossBar() { return tier == Tier.MID; }

    public boolean hasBossBar() { return showsBossBar(); }

    @Override
    protected float bossBarProgress() { return Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F); }

    @Override
    protected int bossBarsLeft() { return 0; }

    /** 中ボスは HP が半分を切ると速くなる */
    public boolean enraged() { return tier == Tier.MID && getHealth() <= getMaxHealth() * 0.5F; }

    public boolean isAttacking() { return current != null; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void bossAiStep() {
        LivingEntity target = getTarget();
        if (enraged() && !enrageApplied) {
            enrageApplied = true;
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(ENRAGE_ID) == null) {
                speed.addTransientModifier(new AttributeModifier(ENRAGE_ID, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + getBbHeight() + 0.3, getZ(), 4, 0.4, 0.2, 0.4, 0);
            }
        }
        if (flying()) flyStep(target);

        if (current != null) {
            if (target != null && !flying()) face(target, 20.0F);
            tickMove(current, moveTick, target);
            moveTick++;
            if (current != null && moveTick >= current.length()) endMove();
            return;
        }
        if (globalCd > 0) globalCd--;
        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));
        if (target == null || !target.isAlive()) return;

        getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (!flying() && !stationary() && (tickCount % 10 == 0 || getNavigation().isDone())) {
            getNavigation().moveTo(target, 1.0);
        }
        if (stationary() || flying()) face(target, 15.0F);
        if (globalCd <= 0) {
            Move next = choose(target);
            if (next != null) startMove(next);
        }
    }

    private Move choose(LivingEntity target) {
        double dist = Math.sqrt(distanceToSqr(target.getX(), getY(), target.getZ())) - (getBbWidth() + target.getBbWidth()) / 2.0;
        boolean sight = hasLineOfSight(target);
        List<Move> pool = new ArrayList<>();
        List<Move> all = moves();
        for (Move m : all) {
            if (dist < m.minRange() || dist > m.maxRange()) continue;
            if (m.needsSight() && !sight) continue;
            if (cooldowns.getOrDefault(m.name(), 0) > 0) continue;
            if (m == lastMove && all.size() > 1 && tier != Tier.WEAK) continue;
            for (int i = 0; i < m.weight(); i++) pool.add(m);
        }
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
    }

    protected void startMove(Move m) {
        current = m;
        moveTick = 0;
        hitOnce.clear();
        if (!flying()) getNavigation().stop();
        triggerAnim("main", m.name());
    }

    private void endMove() {
        Move done = current;
        current = null;
        lastMove = done;
        float speedUp = enraged() ? 0.7F : 1.0F;
        cooldowns.put(done.name(), (int) (done.cooldown() * speedUp));
        globalCd = (int) (tier.globalCooldown * speedUp);
    }

    // ============================================================ 飛ぶモブ
    private void flyStep(LivingEntity target) {
        Vec3 goal;
        if (target == null || !target.isAlive()) {
            setDeltaMovement(getDeltaMovement().scale(0.85).add(0, Math.sin(tickCount * 0.1) * 0.01, 0));
            return;
        }
        orbit += 0.025;
        double kd = keepDistance();
        if (kd > 0) {
            goal = target.position().add(Math.cos(orbit) * kd, hoverHeight(), Math.sin(orbit) * kd);
        } else {
            goal = target.position().add(Math.cos(orbit * 4) * 1.5, hoverHeight() + Math.sin(tickCount * 0.2) * 0.5, Math.sin(orbit * 4) * 1.5);
        }
        Vec3 d = goal.subtract(position());
        double speed = getAttributeValue(Attributes.MOVEMENT_SPEED);
        Vec3 push = d.lengthSqr() > 1.0E-4 ? d.normalize().scale(Math.min(1.0, d.length()) * speed * 0.18) : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().scale(0.82).add(push));
    }

    // ============================================================ 技で使う道具
    /** 前に走る（突進） */
    protected void dash(Vec3 dir, double speed) {
        setDeltaMovement(dir.x * speed, getDeltaMovement().y, dir.z * speed);
        hurtMarked = true;
    }

    /** 半径内の相手に、1回の技につき1度だけ当てる。当たった相手を返す */
    protected List<LivingEntity> hitAround(double radius, float amount, double knock) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : inRadius(position(), radius, getBbHeight())) {
            if (hitOnce.add(e.getUUID()) && damage(e, amount, knock)) out.add(e);
        }
        return out;
    }

    /** 前方の扇形にいる相手に当てる。当たった相手を返す */
    protected List<LivingEntity> hitFront(double range, double halfAngle, float amount, double knock) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : inArc(range, halfAngle)) {
            if (damage(e, amount, knock)) out.add(e);
        }
        return out;
    }

    protected Vec3 eyePos() { return new Vec3(getX(), getEyeY(), getZ()); }

    /** 目から相手の胸への向き */
    protected Vec3 aim(LivingEntity target) {
        return new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ()).subtract(eyePos()).normalize();
    }

    // ============================================================ 倒れる・落とす
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= tier.deathTicks && !level().isClientSide() && !this.isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(tier.coin)).ifPresent(item ->
                spawnAtLocation(new ItemStack(item, tier.coinMin + random.nextInt(tier.coinMax - tier.coinMin + 1))));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ============================================================ アニメ
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
        String a = "animation." + id + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(a + "idle");
        RawAnimation walk = RawAnimation.begin().thenLoop(a + (flying() || stationary() ? "idle" : "walk"));
        RawAnimation death = RawAnimation.begin().thenPlayAndHold(a + "death");
        AnimationController<DungeonMob> main = new AnimationController<>(this, "main", 4, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(death);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        });
        for (Move m : moves()) main.triggerableAnim(m.name(), RawAnimation.begin().thenPlay(a + "attack_" + m.name()));
        main.triggerableAnim("death", death);
        controllers.add(main);
    }
}
