package com.chigusa.jsnboss.dungeon.voidset;

import com.chigusa.jsnboss.dungeon.DungeonMob;
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

/** 虚空のかけら（弱い敵・HP40）。光る紫の結晶。短い瞬間移動をくりかえして体当たり */
public class VoidShard extends DungeonMob {
    static final Move RAM = new Move("ram", 16, 30, 0, 8, 1, true);
    private static final List<Move> MOVES = List.of(RAM);

    public VoidShard(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.PURPLE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 20.0).add(Attributes.FLYING_SPEED, 0.28);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 1.0; }
    @Override protected double keepDistance() { return 4.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        if (t == 4) {
            Vec3 to = position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(1.4);
            blinkTo(target.getX() + to.x, target.getZ() + to.z, target.getY());
            setPos(getX(), target.getY() + 0.8, getZ());
        }
        if (t == 7) { hitAround(1.5, 4.0F, 0.4); playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.0F, 1.2F); }
    }
}
