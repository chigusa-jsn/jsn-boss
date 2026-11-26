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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 茨の大鹿（強い敵・HP240）。茨の角で突進して吹き飛ばし、前脚を踏み鳴らしてまわりに茨を生やす */
public class ThornStag extends DungeonMob {
    static final Move GORE = new Move("gore", 20, 24, 0, 3.2, 3, false);
    static final Move CHARGE = new Move("charge", 36, 70, 4, 14, 3, true);
    static final Move THORNS = new Move("thorns", 36, 90, 0, 4.5, 2, false);
    private static final List<Move> MOVES = List.of(GORE, CHARGE, THORNS);
    private Vec3 dashDir = Vec3.ZERO;

    public ThornStag(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 240.0).add(Attributes.ARMOR, 6.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == GORE) {
            if (t == 9) { hitFront(3.4, 50, 8.0F, 1.0); playSound(SoundEvents.GOAT_RAM_IMPACT, 1.0F, 0.8F); }
        } else if (m == CHARGE) {
            if (t == 8) { dashDir = forward(); playSound(SoundEvents.GOAT_SCREAMING_LONG_JUMP, 1.0F, 0.6F); }
            if (t >= 10 && t < 28) {
                dash(dashDir, 0.8);
                hitAround(1.9, 10.0F, 1.6);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.COMPOSTER, getX(), getY() + 0.3, getZ(), 3, 0.5, 0.1, 0.5, 0);
            }
            if (t == 28) setDeltaMovement(Vec3.ZERO);
        } else if (m == THORNS) {
            if (t == 11 || t == 21 || t == 31) {
                for (LivingEntity e : inRadius(position(), 4.0, 2)) {
                    if (damage(e, 3.0F, 0.3)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                }
                if (level() instanceof ServerLevel sl) {
                    for (int i = 0; i < 16; i++) {
                        double a = i * Math.PI / 8, r = 2 + random.nextDouble() * 2;
                        sl.sendParticles(ParticleTypes.COMPOSTER, getX() + Math.cos(a) * r, getY() + 0.3, getZ() + Math.sin(a) * r, 3, 0.1, 0.4, 0.1, 0);
                    }
                }
                playSound(SoundEvents.SWEET_BERRY_BUSH_BREAK, 1.5F, 0.6F);
            }
        }
    }
}
