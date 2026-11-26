package com.chigusa.jsnboss.dungeon.elite;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.BladeSpikeEntity;
import com.chigusa.jsnboss.entity.FlyingSwordEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 刃の飛竜（中ボス・HP500）。影刃竜の眷属。急降下、剣の雨（飛ぶ剣）、尻尾の回転斬り、足もとから刃の突き上げ */
public class BladeWyvern extends DungeonMob {
    static final Move DIVE = new Move("dive", 28, 50, 0, 18, 3, true);
    static final Move SWORDS = new Move("swords", 24, 60, 3, 22, 4, true);
    static final Move TAIL = new Move("tail", 20, 40, 0, 4.0, 3, false);
    static final Move RAIN = new Move("rain", 28, 110, 0, 18, 2, true);
    private static final List<Move> MOVES = List.of(DIVE, SWORDS, TAIL, RAIN);

    public BladeWyvern(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.PURPLE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 8.0).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.FLYING_SPEED, 0.36);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 5.0; }
    @Override protected double keepDistance() { return 7.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        face(target, 30.0F);
        if (m == DIVE) {
            if (t == 6) playSound(SoundEvents.PHANTOM_SWOOP, 1.4F, 0.8F);
            if (t >= 8 && t < 22) {
                Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(position());
                setDeltaMovement(to.normalize().scale(1.1));
                hurtMarked = true;
                hitAround(2.0, 11.0F, 1.2);
            }
            if (t == 22) setDeltaMovement(getDeltaMovement().scale(0.2).add(0, 0.7, 0));
        } else if (m == SWORDS) {
            if (t == 10) {
                Vec3 dir = aim(target);
                int n = enraged() ? 7 : 5;
                for (int i = 0; i < n; i++) {
                    Vec3 d = dir.yRot((float) Math.toRadians((i - (n - 1) / 2.0) * 12));
                    FlyingSwordEntity.shoot(this, eyePos().add(d), d.scale(1.0), 7.0F, false);
                }
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 1.4F);
            }
        } else if (m == TAIL) {
            if (t == 6 || t == 10) {
                hitAround(4.0, 8.0F, 1.0);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.8, getZ(), 4, 1.5, 0.2, 1.5, 0);
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
            }
        } else if (m == RAIN) {
            if (t == 12) {
                BladeSpikeEntity.spawn(this, target.getX(), groundY(target.getX(), target.getY() + 1, target.getZ()), target.getZ(), 14, 9.0F);
                int n = enraged() ? 8 : 5;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n, r = 2.5;
                    double x = target.getX() + Math.cos(a) * r, z = target.getZ() + Math.sin(a) * r;
                    BladeSpikeEntity.spawn(this, x, groundY(x, target.getY() + 1, z), z, 18 + i * 2, 9.0F);
                }
                playSound(SoundEvents.ANVIL_PLACE, 0.6F, 1.6F);
            }
        }
    }
}
