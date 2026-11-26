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

/** つらら虫（弱い敵・HP70）。近づくと背中のつららを飛ばす。くっつくと噛む */
public class IcicleCrawler extends DungeonMob {
    static final Move SHOOT = new Move("shoot", 20, 45, 2, 10, 2, true);
    static final Move BITE = new Move("bite", 12, 20, 0, 2.0, 2, false);
    private static final List<Move> MOVES = List.of(SHOOT, BITE);

    public IcicleCrawler(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.BLUE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override protected List<Move> moves() { return MOVES; }
    @Override public boolean canFreeze() { return false; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == SHOOT) {
            if (t == 10 && target != null) {
                Vec3 dir = aim(target).add(0, 0.15, 0).normalize();
                IceSpearEntity.shoot(this, new Vec3(getX(), getY() + 0.8, getZ()).add(dir.scale(0.6)), dir.scale(0.8), 4.0F, 40, false);
                playSound(SoundEvents.GLASS_HIT, 1.0F, 1.4F);
            }
        } else if (m == BITE) {
            if (t == 6) {
                for (LivingEntity e : hitFront(2.2, 60, 4.0F, 0.2)) chill(e, 40);
                playSound(SoundEvents.SILVERFISH_HURT, 0.8F, 0.6F);
            }
        }
    }
}
