package com.chigusa.jsnboss.dungeon.machine;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.DroneShotEntity;
import com.chigusa.jsnboss.entity.MineEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 機甲の偵察機（中ボス・HP500）。鋼鉄の機甲蜘蛛の偵察機。光弾の連射、地雷まき、サーチライト、体当たり */
public class ReconDrone extends DungeonMob {
    static final Move VOLLEY = new Move("volley", 28, 40, 2, 22, 4, true);
    static final Move MINES = new Move("mines", 24, 100, 0, 16, 3, false);
    static final Move SPOTLIGHT = new Move("spotlight", 32, 160, 0, 18, 2, true);
    static final Move RAM = new Move("ram", 28, 80, 3, 14, 2, true);
    private static final List<Move> MOVES = List.of(VOLLEY, MINES, SPOTLIGHT, RAM);

    public ReconDrone(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.RED);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 10.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.FLYING_SPEED, 0.3);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 4.0; }
    @Override protected double keepDistance() { return 7.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == VOLLEY) {
            int n = enraged() ? 7 : 5;
            if (target != null && t >= 8 && t < 8 + n * 3 && (t - 8) % 3 == 0) {
                face(target, 40);
                DroneShotEntity.shoot(this, eyePos().add(aim(target)), target, 5.0F, false);
                playSound(SoundEvents.DISPENSER_LAUNCH, 0.6F, 1.6F);
            }
        } else if (m == MINES) {
            if (t == 12) {
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2 + random.nextDouble() * 0.6, sp = 0.2 + random.nextDouble() * 0.2;
                    MineEntity.toss(this, position(), new Vec3(Math.cos(a) * sp, 0.1, Math.sin(a) * sp), 8.0F);
                }
                playSound(SoundEvents.DISPENSER_LAUNCH, 1.2F, 0.8F);
            }
        } else if (m == SPOTLIGHT) {
            if (target != null && t >= 8 && t < 28 && hasLineOfSight(target)) {
                face(target, 40);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                if (t % 5 == 0) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
                if (level() instanceof ServerLevel sl && t % 2 == 0) {
                    Vec3 a = eyePos(), b = target.position().add(0, 1, 0);
                    for (int i = 0; i <= 10; i++) {
                        Vec3 p = a.lerp(b, i / 10.0);
                        sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                    }
                }
                if (t == 8) playSound(SoundEvents.BEACON_ACTIVATE, 0.8F, 1.6F);
            }
        } else if (m == RAM) {
            if (target != null && t >= 8 && t < 22) {
                Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(position());
                setDeltaMovement(to.normalize().scale(0.9));
                hurtMarked = true;
                hitAround(1.8, 10.0F, 1.2);
            }
            if (t == 22) setDeltaMovement(getDeltaMovement().scale(0.2).add(0, 0.5, 0));
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 0, -0.05, 0);
    }
}
