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

/** 闇の目玉（弱い敵・HP40）。コウモリの翼で飛ぶ目玉。見つめた相手を暗闇にする */
public class DarkEye extends DungeonMob {
    static final Move GAZE = new Move("gaze", 30, 60, 0, 14, 1, true);
    private static final List<Move> MOVES = List.of(GAZE);

    public DarkEye(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.PURPLE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.FLYING_SPEED, 0.32);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 2.5; }
    @Override protected double keepDistance() { return 5.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        face(target, 30.0F);
        if (t >= 8 && t < 28 && t % 5 == 0 && hasLineOfSight(target) && distanceTo(target) < 15) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            if (t == 20) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
            damage(target, 1.0F, 0);
            if (level() instanceof ServerLevel sl) {
                Vec3 a = eyePos(), b = target.getEyePosition();
                for (int i = 0; i <= 8; i++) {
                    Vec3 p = a.lerp(b, i / 8.0);
                    sl.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
            if (t == 8) playSound(SoundEvents.ENDER_EYE_DEATH, 1.0F, 0.6F);
        }
    }
}
