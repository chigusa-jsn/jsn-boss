package com.chigusa.jsnboss.dungeon.undead;

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

/** 墓守の大鎌（強い敵・HP280）。大鎌の大きななぎ払い（ウィザー）と、離れた相手を鎌で引き寄せる */
public class GraveReaper extends DungeonMob {
    static final Move REAP = new Move("reap", 28, 34, 0, 4.6, 4, false);
    static final Move HOOK = new Move("hook", 26, 90, 4, 9, 3, true);
    private static final List<Move> MOVES = List.of(REAP, HOOK);

    public GraveReaper(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == REAP) {
            if (t == 16) {
                for (LivingEntity e : hitFront(4.8, 140, 9.0F, 0.5)) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.5F);
                if (level() instanceof ServerLevel sl) {
                    Vec3 f = forward();
                    for (int i = -4; i <= 4; i++) {
                        Vec3 d = f.yRot(i * 0.3F).scale(3.2);
                        sl.sendParticles(ParticleTypes.SOUL, getX() + d.x, getY() + 1.2, getZ() + d.z, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
        } else if (m == HOOK) {
            if (t == 12 && target != null && hasLineOfSight(target) && distanceTo(target) < 10.5) {
                Vec3 pull = position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(1.4);
                target.setDeltaMovement(pull.x, 0.35, pull.z);
                target.hurtMarked = true;
                damage(target, 4.0F, 0);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                playSound(SoundEvents.CHAIN_PLACE, 1.2F, 0.6F);
                if (level() instanceof ServerLevel sl) {
                    Vec3 a = position().add(0, 1.2, 0), b = target.position().add(0, 1, 0);
                    for (int i = 0; i <= 10; i++) {
                        Vec3 p = a.lerp(b, i / 10.0);
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(4) == 0) level().addParticle(ParticleTypes.SOUL, getRandomX(0.5), getY() + 0.2, getRandomZ(0.5), 0, 0.02, 0);
    }
}
