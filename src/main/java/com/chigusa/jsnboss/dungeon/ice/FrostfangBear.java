package com.chigusa.jsnboss.dungeon.ice;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.IceSpearEntity;
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

/** 氷牙の白熊（強い敵・HP250）。立ち上がってののしかかりと、背中の氷のトゲを飛ばす */
public class FrostfangBear extends DungeonMob {
    static final Move MAUL = new Move("maul", 24, 30, 0, 3.2, 4, false);
    static final Move SPIKES = new Move("spikes", 26, 90, 3, 14, 3, true);
    private static final List<Move> MOVES = List.of(MAUL, SPIKES);

    public FrostfangBear(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.WHITE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0).add(Attributes.ARMOR, 6.0).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected List<Move> moves() { return MOVES; }
    @Override public boolean canFreeze() { return false; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == MAUL) {
            if (t == 6) playSound(SoundEvents.POLAR_BEAR_WARNING, 1.2F, 0.9F);
            if (t == 14) hitFront(3.4, 70, 10.0F, 1.0);
        } else if (m == SPIKES) {
            if (t == 12 && target != null) {
                Vec3 dir = aim(target);
                Vec3 from = new Vec3(getX(), getY() + getBbHeight(), getZ());
                for (int i = -2; i <= 2; i++) {
                    Vec3 d = dir.yRot((float) Math.toRadians(i * 12)).add(0, 0.12, 0).normalize();
                    IceSpearEntity.shoot(this, from.add(d), d.scale(0.9), 5.0F, 40, false);
                }
                playSound(SoundEvents.GLASS_BREAK, 1.0F, 0.8F);
            }
        }
    }
}
