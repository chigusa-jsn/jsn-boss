package com.chigusa.jsnboss.dungeon.thunder;

import com.chigusa.jsnboss.dungeon.DungeonMob;
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

import java.util.List;

/** 電気クラゲ（弱い敵・HP50）。空中をただよう。触手に触れるとしびれる */
public class SparkJelly extends DungeonMob {
    static final Move ZAP = new Move("zap", 16, 30, 0, 2.0, 1, false);
    private static final List<Move> MOVES = List.of(ZAP);

    public SparkJelly(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.BLUE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 20.0).add(Attributes.FLYING_SPEED, 0.2);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 1.4; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t == 5) {
            for (LivingEntity e : hitAround(2.0, 3.0F, 0.2)) shock(e, 40);
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.4, getZ(), 20, 0.8, 0.4, 0.8, 0.2);
            playSound(SoundEvents.BEEHIVE_WORK, 1.0F, 1.6F);
        }
    }
}
