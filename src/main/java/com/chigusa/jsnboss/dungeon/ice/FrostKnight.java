package com.chigusa.jsnboss.dungeon.ice;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 氷像の騎士（強い敵・HP260）。斬られると凍える。氷の盾を構えている間は、正面からの攻撃をほとんど受けない */
public class FrostKnight extends DungeonMob {
    static final Move SLASH = new Move("slash", 20, 24, 0, 3.6, 4, false);
    static final Move GUARD = new Move("guard", 50, 120, 0, 8, 2, false);
    private static final List<Move> MOVES = List.of(SLASH, GUARD);

    private boolean guarding;

    public FrostKnight(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.BLUE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0).add(Attributes.ARMOR, 12.0).add(Attributes.ARMOR_TOUGHNESS, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override protected List<Move> moves() { return MOVES; }
    @Override public boolean canFreeze() { return false; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == SLASH) {
            if (t == 10) {
                for (LivingEntity e : hitFront(3.8, 65, 9.0F, 0.5)) chill(e, 80);
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
                if (level() instanceof ServerLevel sl) {
                    Vec3 f = forward().scale(2);
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, getX() + f.x, getY() + 1.2, getZ() + f.z, 20, 0.8, 0.3, 0.8, 0.02);
                }
            }
        } else if (m == GUARD) {
            guarding = t >= 5 && t < 45;
            if (t == 5) playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.4F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (guarding && isAttacking() && source.getSourcePosition() != null) {
            Vec3 to = source.getSourcePosition().subtract(position()).multiply(1, 0, 1).normalize();
            if (to.dot(forward()) > 0.35) {                     // 正面から：盾ではじく
                playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.2F);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SNOWFLAKE, getX() + to.x, getY() + 1.2, getZ() + to.z, 8, 0.2, 0.3, 0.2, 0.05);
                amount *= 0.2F;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(6) == 0) level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, -0.02, 0);
    }
}
