package com.chigusa.jsnboss.dungeon.wind;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.List;

/** 風のわたぼうし（弱い敵・HP40）。ふわふわ浮かぶ綿毛。体当たりで大きく吹き飛ばす */
public class WindPuff extends DungeonMob {
    static final Move BUMP = new Move("bump", 12, 30, 0, 1.8, 1, false);
    private static final List<Move> MOVES = List.of(BUMP);

    public WindPuff(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.WHITE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 20.0).add(Attributes.FLYING_SPEED, 0.25);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 0.8; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t == 5) {
            for (LivingEntity e : hitAround(1.6, 1.0F, 2.2)) launch(e, 0.5);
            playSound(SoundEvents.WOOL_PLACE, 1.2F, 1.4F);
        }
    }
}
