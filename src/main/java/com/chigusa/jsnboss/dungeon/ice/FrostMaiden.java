package com.chigusa.jsnboss.dungeon.ice;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.FrostFieldEntity;
import com.chigusa.jsnboss.entity.IceSpearEntity;
import com.chigusa.jsnboss.entity.IceSpikeEntity;
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

/** 霜の侍女（中ボス・HP500）。氷の魔女の侍女。距離をとって氷の魔法を使い、近づかれると瞬間移動で逃げる */
public class FrostMaiden extends DungeonMob {
    static final Move NEEDLES = new Move("needles", 24, 36, 2, 20, 4, true);
    static final Move CIRCLE = new Move("circle", 30, 110, 0, 14, 3, true);
    static final Move BLINK = new Move("blink", 16, 80, 0, 4, 3, false);
    static final Move SPIKES = new Move("spikes", 30, 100, 2, 14, 2, true);
    private static final List<Move> MOVES = List.of(NEEDLES, CIRCLE, BLINK, SPIKES);

    public FrostMaiden(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.BLUE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.KNOCKBACK_RESISTANCE, 0.4).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override protected double keepDistance() { return 9.0; }
    @Override protected List<Move> moves() { return MOVES; }
    @Override public boolean canFreeze() { return false; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        if (m == NEEDLES) {
            if (t == 12) {
                Vec3 dir = aim(target);
                int n = enraged() ? 5 : 3;
                for (int i = 0; i < n; i++) {
                    Vec3 d = dir.yRot((float) Math.toRadians((i - (n - 1) / 2.0) * 14));
                    IceSpearEntity.shoot(this, eyePos().add(d), d.scale(1.0), 6.0F, 60, false);
                }
                playSound(SoundEvents.GLASS_BREAK, 0.8F, 1.6F);
            }
        } else if (m == CIRCLE) {
            if (t == 12) {
                FrostFieldEntity.spawn(this, target.getX(), groundY(target.getX(), target.getY() + 1, target.getZ()), target.getZ(), 3.5F, 100);
                playSound(SoundEvents.POWDER_SNOW_STEP, 1.5F, 0.6F);
            }
        } else if (m == BLINK) {
            if (t == 7) blinkAway(target);
        } else if (m == SPIKES) {
            if (t == 12) {
                Vec3 dir = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ()).normalize();
                for (int i = 1; i <= 7; i++) {
                    double x = getX() + dir.x * i * 1.6, z = getZ() + dir.z * i * 1.6;
                    IceSpikeEntity.spawn(this, x, groundY(x, getY() + 1, z), z, 10 + i * 3, IceSpikeEntity.SPIKE, 8.0F);
                }
            }
        }
    }

    /** 相手から離れた場所へ瞬間移動 */
    private void blinkAway(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        for (int tries = 0; tries < 10; tries++) {
            double a = random.nextDouble() * Math.PI * 2;
            double x = target.getX() + Math.cos(a) * 9, z = target.getZ() + Math.sin(a) * 9;
            double y = groundY(x, target.getY() + 2, z);
            if (randomTeleport(x, y, z, false)) break;
        }
        sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.5F);
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.8), getY() + 0.3, getRandomZ(0.8), 0, -0.01, 0);
    }
}
