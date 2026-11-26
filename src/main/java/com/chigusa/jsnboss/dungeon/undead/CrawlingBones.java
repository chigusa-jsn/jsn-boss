package com.chigusa.jsnboss.dungeon.undead;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.List;

/** 這う骨（弱い敵・HP50）。下半身のない骨。足をつかんで動きを遅くする */
public class CrawlingBones extends DungeonMob {
    static final Move GRAB = new Move("grab", 16, 30, 0, 2.0, 1, false);
    private static final List<Move> MOVES = List.of(GRAB);

    public CrawlingBones(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.WHITE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ARMOR, 2.0).add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t == 8) {
            for (LivingEntity e : hitFront(2.2, 70, 3.0F, 0)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
            playSound(SoundEvents.SKELETON_STEP, 1.0F, 0.7F);
        }
    }
}
