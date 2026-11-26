package com.chigusa.jsnboss.dungeon.wind;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.TornadoEntity;
import com.chigusa.jsnboss.entity.WindBladeEntity;
import net.minecraft.core.particles.ParticleTypes;
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

/** 旋風の山猫（中ボス・HP500）。風の猫の眷属。引っかき3連、飛びかかり、追いかける竜巻、風の刃 */
public class WhirlLynx extends DungeonMob {
    static final Move CLAWS = new Move("claws", 24, 30, 0, 3.0, 5, false);
    static final Move POUNCE = new Move("pounce", 30, 70, 4, 12, 3, true);
    static final Move TORNADO = new Move("tornado", 28, 120, 3, 20, 2, true);
    static final Move BLADES = new Move("blades", 20, 70, 4, 18, 3, true);
    private static final List<Move> MOVES = List.of(CLAWS, POUNCE, TORNADO, BLADES);

    public WhirlLynx(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 6.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36).add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == CLAWS) {
            if (t == 5 || t == 11 || t == 17) {
                hitFront(3.0, 60, 6.0F, 0.3);
                playSound(SoundEvents.CAT_HISS, 0.6F, 1.4F);
            }
        } else if (m == POUNCE) {
            if (t == 10 && target != null) {
                Vec3 h = target.position().subtract(position()).multiply(1, 0, 1);
                Vec3 v = h.normalize().scale(Math.min(1.5, 0.3 + h.length() * 0.12));
                setDeltaMovement(v.x, 0.5, v.z);
                hurtMarked = true;
                playSound(SoundEvents.CAT_HISS, 1.2F, 0.8F);
            }
            if (t > 10 && t < 26) hitAround(1.8, 10.0F, 0.7);
        } else if (m == TORNADO) {
            if (t == 14 && target != null) {
                int n = enraged() ? 2 : 1;
                for (int i = 0; i < n; i++) {
                    Vec3 side = forward().yRot((float) Math.toRadians(i == 0 ? -40 : 40)).scale(2);
                    TornadoEntity.spawnSeeker(this, getX() + side.x, getY(), getZ() + side.z, target, 6.0F);
                }
                playSound(SoundEvents.ELYTRA_FLYING, 0.8F, 1.4F);
            }
        } else if (m == BLADES) {
            if (t == 10 && target != null) {
                Vec3 dir = aim(target);
                for (int i = -1; i <= 1; i++) {
                    Vec3 d = dir.yRot((float) Math.toRadians(i * 15));
                    WindBladeEntity.shoot(this, new Vec3(getX(), getY() + 1.0, getZ()).add(d), d.scale(1.0), 7.0F);
                }
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);
            }
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.CLOUD, getRandomX(0.8), getY() + 0.2, getRandomZ(0.8), 0, 0.01, 0);
    }
}
