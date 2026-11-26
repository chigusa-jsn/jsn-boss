package com.chigusa.jsnboss.dungeon.thunder;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.GroundBoltEntity;
import com.chigusa.jsnboss.entity.LightningMarkEntity;
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

/** 雷鳴の牝獅子（中ボス・HP500）。雷の獅子の眷属。雷の印、電撃の体当たり、しびれる咆哮、地を走る雷 */
public class ThunderLioness extends DungeonMob {
    static final Move MARKS = new Move("marks", 28, 80, 2, 20, 3, true);
    static final Move TACKLE = new Move("tackle", 32, 70, 3, 14, 3, true);
    static final Move ROAR = new Move("roar", 30, 140, 0, 6, 2, false);
    static final Move BOLT = new Move("bolt", 24, 60, 3, 18, 3, true);
    private static final List<Move> MOVES = List.of(MARKS, TACKLE, ROAR, BOLT);

    private Vec3 dashDir = Vec3.ZERO;

    public ThunderLioness(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.YELLOW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 6.0).add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == MARKS) {
            if (t == 10 && target != null) {
                int n = enraged() ? 5 : 3;
                LightningMarkEntity.spawn(this, target.getX(), target.getZ(), 20, 8.0F);
                for (int i = 1; i < n; i++) {
                    double a = random.nextDouble() * Math.PI * 2, r = 2.5 + random.nextDouble() * 2;
                    LightningMarkEntity.spawn(this, target.getX() + Math.cos(a) * r, target.getZ() + Math.sin(a) * r, 20 + i * 4, 8.0F);
                }
                playSound(SoundEvents.TRIDENT_THUNDER.value(), 0.6F, 1.4F);
            }
        } else if (m == TACKLE) {
            if (t == 10) { dashDir = forward(); playSound(SoundEvents.RAVAGER_ROAR, 0.7F, 1.6F); }
            if (t >= 12 && t < 26) {
                dash(dashDir, 0.85);
                for (LivingEntity e : hitAround(1.8, 10.0F, 1.2)) shock(e, 40);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.8, getZ(), 6, 0.6, 0.5, 0.6, 0.1);
            }
            if (t == 26) setDeltaMovement(Vec3.ZERO);
        } else if (m == ROAR) {
            if (t == 12) {
                for (LivingEntity e : inRadius(position(), 6, 3)) {
                    damage(e, 3.0F, 0.6);
                    shock(e, 60);
                    sparkLine(position().add(0, 1, 0), e.position().add(0, 1, 0));
                }
                playSound(SoundEvents.RAVAGER_ROAR, 1.4F, 1.2F);
            }
        } else if (m == BOLT) {
            if (t == 10 && target != null) {
                Vec3 dir = target.position().subtract(position());
                GroundBoltEntity.spawn(this, position().add(forward().scale(1.5)), dir, 8.0F);
                if (enraged()) {
                    GroundBoltEntity.spawn(this, position(), dir.yRot((float) Math.toRadians(25)), 8.0F);
                    GroundBoltEntity.spawn(this, position(), dir.yRot((float) Math.toRadians(-25)), 8.0F);
                }
                playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.6F);
            }
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(4) == 0) level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0, 0, 0);
    }
}
