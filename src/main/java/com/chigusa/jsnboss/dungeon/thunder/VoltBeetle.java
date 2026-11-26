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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 雷虫（弱い敵・HP70）。角が避雷針の甲虫。電気をためて体当たり */
public class VoltBeetle extends DungeonMob {
    static final Move RAM = new Move("ram", 20, 40, 0, 5, 1, false);
    private static final List<Move> MOVES = List.of(RAM);
    private Vec3 dir = Vec3.ZERO;

    public VoltBeetle(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.BLUE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70.0).add(Attributes.ARMOR, 6.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t < 7 && level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.7, getZ(), 2, 0.2, 0.2, 0.2, 0.05);
        if (t == 7) { dir = forward(); playSound(SoundEvents.BEEHIVE_WORK, 0.8F, 1.8F); }
        if (t >= 7 && t < 13) {
            dash(dir, 0.7);
            for (LivingEntity e : hitAround(1.3, 4.0F, 0.5)) shock(e, 30);
        }
    }
}
