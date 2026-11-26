package com.chigusa.jsnboss.dungeon.machine;

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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 蒸気の自動人形（強い敵・HP260）。蒸気を吹きつけて目くらまし（盲目）、ピストンの連続パンチ */
public class SteamAutomaton extends DungeonMob {
    static final Move STEAM = new Move("steam", 32, 90, 0, 6, 3, true);
    static final Move PUNCHES = new Move("punches", 32, 40, 0, 3.2, 4, false);
    private static final List<Move> MOVES = List.of(STEAM, PUNCHES);

    public SteamAutomaton(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.YELLOW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0).add(Attributes.ARMOR, 10.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == STEAM) {
            if (t >= 10 && t < 26) {
                Vec3 f = forward();
                if (t % 4 == 2) {
                    for (LivingEntity e : inArc(6.0, 40)) {
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                        damage(e, 2.0F, 0.2);
                    }
                }
                if (level() instanceof ServerLevel sl) {
                    for (int i = 1; i <= 5; i++) {
                        sl.sendParticles(ParticleTypes.CLOUD, getX() + f.x * i, getY() + 1.4, getZ() + f.z * i, 3, 0.2 * i, 0.2 * i, 0.2 * i, 0.02);
                    }
                }
                if (t == 10) playSound(SoundEvents.FIRE_EXTINGUISH, 1.5F, 0.6F);
            }
        } else if (m == PUNCHES) {
            if (t == 5 || t == 11 || t == 17 || t == 23) {
                hitFront(3.2, 50, 4.5F, 0.4);
                playSound(SoundEvents.PISTON_EXTEND, 0.8F, 1.2F);
            }
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.CLOUD, getX(), getY() + 2.4, getZ() + 0.2, 0, 0.06, 0);
    }
}
