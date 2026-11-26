package com.chigusa.jsnboss.dungeon.nature;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.BoulderEntity;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 毒花の魔物（強い敵・HP260）。動かない食虫植物。ツタで引き寄せて噛む（毒）、毒の種を吐く */
public class VenusSnapper extends DungeonMob {
    static final Move BITE = new Move("bite", 18, 20, 0, 3.4, 4, false);
    static final Move VINE = new Move("vine", 26, 60, 3, 10, 3, true);
    static final Move SPIT = new Move("spit", 22, 60, 4, 16, 2, true);
    private static final List<Move> MOVES = List.of(BITE, VINE, SPIT);

    public VenusSnapper(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override protected boolean stationary() { return true; }
    @Override protected List<Move> moves() { return MOVES; }
    @Override public boolean isPushable() { return false; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == BITE) {
            if (t == 9) {
                for (LivingEntity e : hitFront(3.6, 50, 9.0F, 0.3)) e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                playSound(SoundEvents.EVOKER_FANGS_ATTACK, 1.0F, 0.6F);
            }
        } else if (m == VINE) {
            if (t == 12 && target != null && hasLineOfSight(target)) {
                Vec3 pull = position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(1.3);
                target.setDeltaMovement(pull.x, 0.3, pull.z);
                target.hurtMarked = true;
                damage(target, 3.0F, 0);
                if (level() instanceof ServerLevel sl) {
                    Vec3 a = position().add(0, 1.6, 0), b = target.position().add(0, 0.8, 0);
                    for (int i = 0; i <= 10; i++) {
                        Vec3 p = a.lerp(b, i / 10.0);
                        sl.sendParticles(ParticleTypes.COMPOSTER, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
                    }
                }
                playSound(SoundEvents.VINE_BREAK, 1.2F, 0.7F);
            }
        } else if (m == SPIT) {
            if (t == 11 && target != null) {
                BoulderEntity.throwSeed(this, position().add(0, getBbHeight(), 0), target.position(), 5.0F);
                playSound(SoundEvents.LLAMA_SPIT, 1.2F, 0.6F);
            }
        }
    }
}
