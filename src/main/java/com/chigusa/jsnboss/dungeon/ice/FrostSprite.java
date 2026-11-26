package com.chigusa.jsnboss.dungeon.ice;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.mob.MobBoltEntity;
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

/** 氷の妖精（弱い敵・HP50）。離れたところを飛んで、凍える雪玉を投げる */
public class FrostSprite extends DungeonMob {
    static final Move THROW = new Move("throw", 16, 30, 1, 16, 1, true);
    private static final List<Move> MOVES = List.of(THROW);

    public FrostSprite(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.BLUE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.FLYING_SPEED, 0.3);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 2.5; }
    @Override protected double keepDistance() { return 6.0; }
    @Override protected List<Move> moves() { return MOVES; }
    @Override public boolean canFreeze() { return false; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        face(target, 30.0F);
        if (t == 8) {
            Vec3 dir = aim(target);
            MobBoltEntity.shoot(this, eyePos().add(dir.scale(0.6)), dir.scale(0.7), MobBoltEntity.FROST, 3.0F);
            playSound(SoundEvents.SNOWBALL_THROW, 1.0F, 1.2F);
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.5), getY() + 0.5, getRandomZ(0.5), 0, -0.02, 0);
    }
}
