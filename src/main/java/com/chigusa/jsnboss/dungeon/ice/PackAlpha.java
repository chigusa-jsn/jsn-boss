package com.chigusa.jsnboss.dungeon.ice;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 群れの長狼（中ボス・HP500）。フェンリルの眷属。噛みつき、飛びかかり、鎖の振り回し、遠吠えで狼を呼ぶ */
public class PackAlpha extends DungeonMob {
    static final Move BITE = new Move("bite", 16, 20, 0, 3.0, 5, false);
    static final Move COMBO = new Move("combo", 30, 70, 0, 3.5, 2, false);
    static final Move POUNCE = new Move("pounce", 30, 80, 4, 12, 3, true);
    static final Move HOWL = new Move("howl", 30, 400, 0, 24, 2, false);
    static final Move CHAIN = new Move("chain", 24, 90, 0, 4.5, 2, false);
    private static final List<Move> MOVES = List.of(BITE, COMBO, POUNCE, HOWL, CHAIN);

    public PackAlpha(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.WHITE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 6.0).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected List<Move> moves() { return MOVES; }
    @Override public boolean canFreeze() { return false; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == BITE) {
            if (t == 8) bite(11.0F);
        } else if (m == COMBO) {
            if (t == 8 || t == 16 || t == 24) bite(6.0F);
            if (t == 8 || t == 16) dash(forward(), 0.3);
        } else if (m == POUNCE) {
            if (t == 10 && target != null) {
                Vec3 to = target.position().subtract(position());
                Vec3 h = new Vec3(to.x, 0, to.z);
                double d = h.length();
                Vec3 v = h.normalize().scale(Math.min(1.4, 0.3 + d * 0.11));
                setDeltaMovement(v.x, 0.55, v.z);
                hurtMarked = true;
                playSound(SoundEvents.WOLF_GROWL, 1.2F, 0.7F);
            }
            if (t > 10 && t < 26) hitAround(1.8, 10.0F, 0.8);
        } else if (m == HOWL) {
            if (t == 10) playSound(SoundEvents.WOLF_HOWL, 2.0F, 0.6F);
            if (t == 15 && target != null) callPack(target);
        } else if (m == CHAIN) {
            if (t == 12) {
                hitFront(4.6, 110, 8.0F, 1.3);
                playSound(SoundEvents.CHAIN_BREAK, 1.2F, 0.6F);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 3, 1.5, 0.2, 1.5, 0);
            }
        }
    }

    private void bite(float dmg) {
        for (LivingEntity e : hitFront(3.2, 50, dmg, 0.4)) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        }
        playSound(SoundEvents.WOLF_GROWL, 1.0F, 1.0F);
    }

    /** 狼を2匹呼ぶ（まわりに4匹以上いたら呼ばない） */
    private void callPack(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        int near = level().getEntitiesOfClass(Wolf.class, getBoundingBox().inflate(16), w -> w.isAngry()).size();
        for (int i = 0; i < Math.min(2, 4 - near); i++) {
            Wolf w = EntityType.WOLF.create(sl);
            if (w == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            double x = getX() + Math.cos(a) * 2.5, z = getZ() + Math.sin(a) * 2.5;
            w.moveTo(x, groundY(x, getY() + 1, z), z, random.nextFloat() * 360, 0);
            w.finalizeSpawn(sl, sl.getCurrentDifficultyAt(w.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            w.setPersistentAngerTarget(target.getUUID());
            w.startPersistentAngerTimer();
            w.setTarget(target);
            sl.addFreshEntity(w);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, x, w.getY() + 0.5, z, 20, 0.3, 0.4, 0.3, 0.05);
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(5) == 0) level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.8), getY() + 1.8, getRandomZ(0.8), 0, -0.02, 0);
    }
}
