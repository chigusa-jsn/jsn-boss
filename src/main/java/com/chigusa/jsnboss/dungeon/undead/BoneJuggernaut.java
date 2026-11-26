package com.chigusa.jsnboss.dungeon.undead;

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

/** 骨の重装兵（強い敵・HP300）。大盾を構えている間は正面から効かない。メイスで大きく吹き飛ばす */
public class BoneJuggernaut extends DungeonMob {
    static final Move SMASH = new Move("smash", 28, 30, 0, 3.4, 4, false);
    static final Move GUARD = new Move("guard", 60, 110, 0, 10, 3, false);
    private static final List<Move> MOVES = List.of(SMASH, GUARD);

    public BoneJuggernaut(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.WHITE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0).add(Attributes.ARMOR, 14.0).add(Attributes.ARMOR_TOUGHNESS, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == SMASH) {
            if (t == 16) {
                for (LivingEntity e : hitFront(3.6, 60, 10.0F, 1.8)) launch(e, 0.45);
                playSound(SoundEvents.ANVIL_LAND, 0.8F, 0.6F);
            }
        } else if (m == GUARD) {
            setGuarding(t >= 6 && t < 54, 0.1F);
            if (t == 6) playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F);
        }
    }
}
