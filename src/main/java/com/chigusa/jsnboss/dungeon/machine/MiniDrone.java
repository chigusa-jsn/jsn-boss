package com.chigusa.jsnboss.dungeon.machine;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.DroneShotEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.List;

/** 小型ドローン（弱い敵・HP60）。離れたところを飛んで光弾を撃つ */
public class MiniDrone extends DungeonMob {
    static final Move SHOOT = new Move("shoot", 16, 30, 1, 18, 1, true);
    private static final List<Move> MOVES = List.of(SHOOT);

    public MiniDrone(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.RED);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ARMOR, 4.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.FLYING_SPEED, 0.3);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 3.0; }
    @Override protected double keepDistance() { return 6.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t == 7 && target != null) {
            face(target, 40);
            DroneShotEntity.shoot(this, eyePos().add(aim(target).scale(0.6)), target, 3.0F, false);
            playSound(SoundEvents.DISPENSER_LAUNCH, 0.5F, 1.8F);
        }
    }
}
