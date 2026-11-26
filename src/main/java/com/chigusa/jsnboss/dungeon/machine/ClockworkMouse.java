package com.chigusa.jsnboss.dungeon.machine;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.Blasts;
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

/** ぜんまいねずみ（弱い敵・HP40）。走り寄って小さく爆発する（地形は壊さない・爆発したら消える） */
public class ClockworkMouse extends DungeonMob {
    static final Move BOOM = new Move("boom", 20, 0, 0, 1.6, 1, false);
    private static final List<Move> MOVES = List.of(BOOM);

    public ClockworkMouse(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.WHITE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ARMOR, 4.0).add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t == 1) playSound(SoundEvents.CREEPER_PRIMED, 0.8F, 1.8F);
        if (t < 18 && level() instanceof ServerLevel sl && t % 3 == 0) sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 2, 0.1, 0.1, 0.1, 0);
        if (t == 18 && level() instanceof ServerLevel) {
            Blasts.blast(level(), this, position().add(0, 0.3, 0), 2.5, 6.0F);
            discard();
        }
    }
}
