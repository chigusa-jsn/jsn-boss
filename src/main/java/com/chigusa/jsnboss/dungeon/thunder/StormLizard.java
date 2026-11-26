package com.chigusa.jsnboss.dungeon.thunder;

import com.chigusa.jsnboss.dungeon.DungeonMob;
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

/** 雷鳴の大トカゲ（強い敵・HP240）。電撃の突進と、相手のいる場所に雷を落とす（火はつかない） */
public class StormLizard extends DungeonMob {
    static final Move CHARGE = new Move("charge", 32, 60, 3, 12, 3, true);
    static final Move BOLT = new Move("bolt", 28, 80, 0, 18, 3, true);
    private static final List<Move> MOVES = List.of(CHARGE, BOLT);
    private Vec3 dashDir = Vec3.ZERO;

    public StormLizard(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.YELLOW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 240.0).add(Attributes.ARMOR, 8.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == CHARGE) {
            if (t == 8) { dashDir = forward(); playSound(SoundEvents.HOGLIN_ANGRY, 0.8F, 1.4F); }
            if (t >= 10 && t < 24) {
                dash(dashDir, 0.75);
                for (LivingEntity e : hitAround(1.8, 9.0F, 1.0)) shock(e, 40);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.5, getZ(), 4, 0.5, 0.3, 0.5, 0.1);
            }
            if (t == 24) setDeltaMovement(Vec3.ZERO);
        } else if (m == BOLT) {
            if (t == 12 && target != null) {
                LightningMarkEntity.spawn(this, target.getX(), target.getZ(), 16, 9.0F);
                playSound(SoundEvents.TRIDENT_THUNDER.value(), 0.4F, 1.6F);
            }
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(4) == 0) level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.6), getY() + 0.8, getRandomZ(0.6), 0, 0.02, 0);
    }
}
