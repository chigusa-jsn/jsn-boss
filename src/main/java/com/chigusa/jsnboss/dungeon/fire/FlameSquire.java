package com.chigusa.jsnboss.dungeon.fire;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.FirePillarEntity;
import com.chigusa.jsnboss.entity.FlameSlashEntity;
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

/** 炎の従騎士（中ボス・HP500）。炎の騎士の部下。剣と盾で戦う若い騎士 */
public class FlameSquire extends DungeonMob {
    static final Move SLASH = new Move("slash", 20, 26, 0, 3.5, 5, false);
    static final Move BASH = new Move("bash", 32, 80, 3, 10, 3, true);
    static final Move RING = new Move("ring", 34, 140, 0, 7, 2, false);
    static final Move WAVE = new Move("wave", 24, 90, 5, 18, 3, true);
    private static final List<Move> MOVES = List.of(SLASH, BASH, RING, WAVE);

    private Vec3 dashDir = Vec3.ZERO;

    public FlameSquire(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 8.0).add(Attributes.ARMOR_TOUGHNESS, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == SLASH) {
            if (t == 9) playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
            if (t == 10) {
                for (LivingEntity e : hitFront(3.8, 70, 10.0F, 0.5)) e.igniteForSeconds(3.0F);
                sweepParticles();
            }
        } else if (m == BASH) {
            if (t == 10) { dashDir = forward(); playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.7F); }
            if (t >= 10 && t < 24) {
                dash(dashDir, 0.75);
                for (LivingEntity e : hitAround(1.6, 8.0F, 1.3)) e.igniteForSeconds(2.0F);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.6, getZ(), 4, 0.3, 0.3, 0.3, 0.02);
            }
            if (t == 24) setDeltaMovement(Vec3.ZERO);
        } else if (m == RING) {
            if (t == 13) playSound(SoundEvents.BLAZE_SHOOT, 1.2F, 0.6F);
            if (t == 14) ring(3.2, 6);
            if (t == 24 && enraged()) ring(5.6, 9);
        } else if (m == WAVE) {
            if (t == 12 && target != null) {
                Vec3 dir = aim(target);
                FlameSlashEntity.shoot(this, new Vec3(getX(), getY() + 1.0, getZ()).add(dir), new Vec3(dir.x, 0, dir.z).normalize().scale(0.9),
                        false, false, 8.0F);
                playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
            }
        }
    }

    /** 自分のまわりに火柱の輪（予告のあと噴き上がる） */
    private void ring(double radius, int n) {
        double off = random.nextDouble() * Math.PI;
        for (int i = 0; i < n; i++) {
            double a = off + i * Math.PI * 2 / n;
            double x = getX() + Math.cos(a) * radius, z = getZ() + Math.sin(a) * radius;
            FirePillarEntity.spawn(this, x, groundY(x, getY() + 1, z), z, false, false);
        }
    }

    private void sweepParticles() {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 f = forward();
        for (int i = -3; i <= 3; i++) {
            Vec3 d = f.yRot(i * 0.3F).scale(2.4);
            sl.sendParticles(ParticleTypes.FLAME, getX() + d.x, getY() + 1.1, getZ() + d.z, 2, 0.1, 0.1, 0.1, 0.01);
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + f.x * 1.8, getY() + 1.1, getZ() + f.z * 1.8, 1, 0, 0, 0, 0);
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 2.3, getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
        }
        if (enraged() && random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.FLAME, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.03, 0);
        }
    }
}
