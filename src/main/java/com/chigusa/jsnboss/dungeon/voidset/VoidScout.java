package com.chigusa.jsnboss.dungeon.voidset;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.DroneShotEntity;
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

/** 虚空の斥候（中ボス・HP500）。虚空の監視者の斥候。瞬間移動斬り、浮かせる光線、引き寄せ、虚空弾の連射 */
public class VoidScout extends DungeonMob {
    static final Move BLINK = new Move("blink", 24, 50, 3, 18, 4, true);
    static final Move LEVITATE = new Move("levitate", 28, 100, 0, 16, 2, true);
    static final Move PULL = new Move("pull", 26, 90, 5, 14, 2, true);
    static final Move BARRAGE = new Move("barrage", 28, 70, 3, 20, 3, true);
    private static final List<Move> MOVES = List.of(BLINK, LEVITATE, PULL, BARRAGE);

    public VoidScout(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 6.0).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        if (m == BLINK) {
            if (t == 8) {
                Vec3 side = Vec3.directionFromRotation(0, target.getYRot() + (random.nextBoolean() ? 90 : -90)).scale(2.0);
                blinkTo(target.getX() + side.x, target.getZ() + side.z, target.getY());
                face(target, 360);
            }
            if (t == 13) { hitFront(3.2, 70, 11.0F, 0.5); playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F); }
        } else if (m == LEVITATE) {
            if (t == 12 && hasLineOfSight(target)) {
                sparkBeam(target);
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, enraged() ? 60 : 40, 0));
                damage(target, 4.0F, 0);
                playSound(SoundEvents.SHULKER_SHOOT, 1.0F, 0.7F);
            }
        } else if (m == PULL) {
            if (t == 12 && hasLineOfSight(target)) {
                Vec3 pull = position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(1.6);
                target.setDeltaMovement(pull.x, 0.35, pull.z);
                target.hurtMarked = true;
                damage(target, 3.0F, 0);
                sparkBeam(target);
                playSound(SoundEvents.ENDERMAN_SCREAM, 0.8F, 1.2F);
            }
        } else if (m == BARRAGE) {
            int n = enraged() ? 6 : 4;
            if (t >= 8 && t < 8 + n * 3 && (t - 8) % 3 == 0) {
                DroneShotEntity.shoot(this, eyePos().add(aim(target)), target, 5.0F, false);
                playSound(SoundEvents.SHULKER_SHOOT, 0.6F, 1.4F);
            }
        }
    }

    private void sparkBeam(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 a = eyePos(), b = target.position().add(0, target.getBbHeight() * 0.5, 0);
        for (int i = 0; i <= 14; i++) {
            Vec3 p = a.lerp(b, i / 14.0);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.PORTAL, getRandomX(0.6), getRandomY() - 0.25, getRandomZ(0.6), 0, 0, 0);
    }
}
