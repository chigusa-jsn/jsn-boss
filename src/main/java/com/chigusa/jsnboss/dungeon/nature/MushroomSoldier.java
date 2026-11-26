package com.chigusa.jsnboss.dungeon.nature;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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

/** キノコ兵（弱い敵・HP60）。槍で突き、胞子の煙で吐き気にする */
public class MushroomSoldier extends DungeonMob {
    static final Move POKE = new Move("poke", 16, 20, 0, 2.6, 2, false);
    static final Move SPORE = new Move("spore", 20, 70, 0, 3, 1, false);
    private static final List<Move> MOVES = List.of(POKE, SPORE);

    public MushroomSoldier(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ARMOR, 2.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == POKE) {
            if (t == 8) hitFront(2.8, 30, 4.0F, 0.3);
        } else if (m == SPORE) {
            if (t == 8) {
                for (LivingEntity e : inRadius(position(), 2.8, 2)) {
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                    damage(e, 2.0F, 0);
                }
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1, getZ(), 40, 1.2, 0.6, 1.2, 0.01);
                playSound(SoundEvents.FUNGUS_BREAK, 1.2F, 0.8F);
            }
        }
    }
}
