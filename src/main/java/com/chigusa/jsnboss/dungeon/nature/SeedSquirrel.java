package com.chigusa.jsnboss.dungeon.nature;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.BoulderEntity;
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

/** 種まきリス（弱い敵・HP40）。少し離れて木の実を投げつける */
public class SeedSquirrel extends DungeonMob {
    static final Move THROW = new Move("throw", 16, 30, 2, 14, 1, true);
    private static final List<Move> MOVES = List.of(THROW);

    public SeedSquirrel(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.YELLOW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override protected double keepDistance() { return 6.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t == 8 && target != null) {
            BoulderEntity.throwSized(this, new Vec3(getX(), getY() + 0.6, getZ()), target.position().add(0, 0.5, 0), 3.0F, 0.3F);
            playSound(SoundEvents.SNOWBALL_THROW, 1.0F, 1.4F);
        }
    }
}
