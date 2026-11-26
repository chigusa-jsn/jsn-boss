package com.chigusa.jsnboss.dungeon.dark;

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

/** 影の暗殺者（強い敵・HP220）。二刀流の連撃と、姿を消して背後から一撃 */
public class ShadowAssassin extends DungeonMob {
    static final Move SLASH = new Move("slash", 20, 22, 0, 3.0, 4, false);
    static final Move VANISH = new Move("vanish", 50, 120, 3, 16, 3, true);
    private static final List<Move> MOVES = List.of(SLASH, VANISH);

    public ShadowAssassin(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == SLASH) {
            if (t == 5 || t == 11) {
                for (LivingEntity e : hitFront(3.0, 60, 6.0F, 0.2)) {
                    if (t == 11) e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                }
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.5F);
            }
        } else if (m == VANISH) {
            if (t == 4) { setInvisible(true); puff(); playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F); }
            if (t > 4 && t < 36 && target != null && t % 10 == 0) getNavigation().moveTo(target, 1.4);
            if (t == 36 && target != null) {
                Vec3 behind = target.position().subtract(Vec3.directionFromRotation(0, target.getYRot()).scale(1.5));
                teleportTo(behind.x, groundY(behind.x, target.getY() + 1, behind.z), behind.z);
                face(target, 360);
                setInvisible(false);
                puff();
            }
            if (t == 40) {
                for (LivingEntity e : hitFront(2.8, 80, 12.0F, 0.6)) e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.8F);
            }
            if (t >= m.length() - 1) setInvisible(false);
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        setInvisible(false);
        super.die(source);
    }

    private void puff() {
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 20, 0.3, 0.7, 0.3, 0.02);
    }
}
