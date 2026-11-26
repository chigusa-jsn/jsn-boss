package com.chigusa.jsnboss.dungeon.wind;

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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 疾風のイタチ（弱い敵・HP70）。しっぽが刃。一瞬で駆け抜けて切りつける（かまいたち） */
public class GaleWeasel extends DungeonMob {
    static final Move CUT = new Move("cut", 16, 30, 0, 5, 1, false);
    private static final List<Move> MOVES = List.of(CUT);
    private Vec3 dir = Vec3.ZERO;

    public GaleWeasel(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70.0).add(Attributes.ARMOR, 2.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t == 3) { dir = forward(); playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.9F); }
        if (t >= 3 && t < 9) {
            dash(dir, 0.9);
            hitAround(1.3, 5.0F, 0.2);
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.4, getZ(), 1, 0, 0, 0, 0);
        }
    }
}
