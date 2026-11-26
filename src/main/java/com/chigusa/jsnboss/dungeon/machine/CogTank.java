package com.chigusa.jsnboss.dungeon.machine;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.BoulderEntity;
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

/** 歯車の戦車（強い敵・HP300）。砲弾（爆発しても地形は壊れない）と、轢き突進 */
public class CogTank extends DungeonMob {
    static final Move CANNON = new Move("cannon", 28, 60, 4, 22, 4, true);
    static final Move RAM = new Move("ram", 32, 70, 0, 10, 3, true);
    private static final List<Move> MOVES = List.of(CANNON, RAM);
    private Vec3 dashDir = Vec3.ZERO;

    public CogTank(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.YELLOW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0).add(Attributes.ARMOR, 16.0).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected double keepDistance() { return 8.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == CANNON) {
            if (t == 12 && target != null) {
                Vec3 f = forward();
                BoulderEntity.throwTnt(this, new Vec3(getX() + f.x * 2, getY() + 1.0, getZ() + f.z * 2), target.position(), 9.0F);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX() + f.x * 2.2, getY() + 1.0, getZ() + f.z * 2.2, 10, 0.2, 0.2, 0.2, 0.05);
                playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.6F, 1.6F);
            }
        } else if (m == RAM) {
            if (t == 8) { dashDir = forward(); playSound(SoundEvents.PISTON_EXTEND, 1.0F, 0.6F); }
            if (t >= 10 && t < 26) {
                dash(dashDir, 0.6);
                hitAround(1.9, 10.0F, 1.4);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 2, 0.6, 0.1, 0.6, 0.01);
            }
            if (t == 26) setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SMOKE, getX() + 0.3, getY() + 1.3, getZ() + 0.4, 0, 0.05, 0);
    }
}
