package com.chigusa.jsnboss.dungeon.fire;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.List;

/** 火の粉コウモリ（弱い敵・HP40）。群れで飛び回って、噛みつくと燃やす */
public class CinderBat extends DungeonMob {
    static final Move BITE = new Move("bite", 10, 24, 0, 2.0, 1, false);
    private static final List<Move> MOVES = List.of(BITE);

    public CinderBat(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.RED);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 1.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t == 4) {
            for (LivingEntity e : hitAround(1.6, 3.0F, 0.2)) e.igniteForSeconds(2.0F);
            playSound(SoundEvents.BAT_HURT, 0.6F, 0.7F);
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.SMALL_FLAME, getRandomX(0.6), getY() + 0.4, getRandomZ(0.6), 0, -0.02, 0);
    }
}
