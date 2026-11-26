package com.chigusa.jsnboss.dungeon.dark;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.List;

/** 影の小人（弱い敵・HP60）。暗いところだと速くなる。ひっかく */
public class ShadeImp extends DungeonMob {
    static final Move SCRATCH = new Move("scratch", 12, 14, 0, 2.2, 1, false);
    private static final List<Move> MOVES = List.of(SCRATCH);
    private static final ResourceLocation DARK_SPEED = ResourceLocation.fromNamespaceAndPath("jsnboss", "shade_dark_speed");

    public ShadeImp(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ARMOR, 2.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void bossAiStep() {
        if (tickCount % 20 == 0) {                                // 暗いところ（明るさ7以下）では速い
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            boolean dark = level().getMaxLocalRawBrightness(blockPosition()) <= 7;
            if (speed != null) {
                if (dark && speed.getModifier(DARK_SPEED) == null) {
                    speed.addTransientModifier(new AttributeModifier(DARK_SPEED, 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                } else if (!dark) {
                    speed.removeModifier(DARK_SPEED);
                }
            }
        }
        super.bossAiStep();
    }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (t == 6) {
            hitFront(2.2, 60, 4.0F, 0.2);
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 1.8F);
        }
    }
}
