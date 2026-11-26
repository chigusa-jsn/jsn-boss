package com.chigusa.jsnboss.dungeon.voidset;

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

/** 監視の小目（弱い敵・HP60）。虚空の監視者の小さな目。光線で相手を少し浮かせる */
public class WatcherMote extends DungeonMob {
    static final Move BEAM = new Move("beam", 32, 60, 0, 14, 1, true);
    private static final List<Move> MOVES = List.of(BEAM);

    public WatcherMote(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.WEAK, BossEvent.BossBarColor.PURPLE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ARMOR, 4.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.FLYING_SPEED, 0.24);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 3.0; }
    @Override protected double keepDistance() { return 6.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        face(target, 30.0F);
        if (t >= 10 && t < 28 && t % 4 == 2 && hasLineOfSight(target) && distanceTo(target) < 15) {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
            damage(target, 1.0F, 0);
            if (level() instanceof ServerLevel sl) {
                Vec3 a = eyePos(), b = target.position().add(0, target.getBbHeight() * 0.5, 0);
                for (int i = 0; i <= 10; i++) {
                    Vec3 p = a.lerp(b, i / 10.0);
                    sl.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
            if (t == 10) playSound(SoundEvents.SHULKER_SHOOT, 0.8F, 1.6F);
        }
    }
}
