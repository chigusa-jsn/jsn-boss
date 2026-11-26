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

/** 燃える小鬼（弱い敵・HP60）。すばしっこく駆け寄って、ひっかくと燃やす */
public class EmberImp extends DungeonMob {
    static final Move SCRATCH = new Move("scratch", 12, 14, 0, 2.4, 1, false);
    private static final List<Move> MOVES = List.of(SCRATCH);

    public EmberImp(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ARMOR, 2.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t == 6) {
            for (LivingEntity e : hitFront(2.4, 60, 4.0F, 0.3)) e.igniteForSeconds(2.0F);
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.6F);
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.FLAME, getX(), getY() + 1.35, getZ(), 0, 0.02, 0);
    }
}
