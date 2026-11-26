package com.chigusa.jsnboss.dungeon.fire;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.KnightFireballEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 炎の魔眼（強い敵・HP220）。浮かぶ目玉。火の玉の3連射と、にらんだ相手を燃やす視線 */
public class FlameEye extends DungeonMob {
    static final Move VOLLEY = new Move("volley", 30, 50, 2, 22, 4, true);
    static final Move GLARE = new Move("glare", 44, 120, 0, 14, 2, true);
    private static final List<Move> MOVES = List.of(VOLLEY, GLARE);

    public FlameEye(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.RED);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0).add(Attributes.ARMOR, 4.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.FLYING_SPEED, 0.3);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 3.5; }
    @Override protected double keepDistance() { return 8.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        face(target, 20.0F);
        if (m == VOLLEY) {
            if (t == 10 || t == 15 || t == 20) {
                Vec3 dir = aim(target);
                KnightFireballEntity.shoot(this, eyePos().add(dir.scale(1.0)), dir.scale(0.8), false, 6.0F);
                playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.2F);
            }
        } else if (m == GLARE) {
            if (t == 8) playSound(SoundEvents.BLAZE_AMBIENT, 1.2F, 0.6F);
            if (t >= 10 && t < 40 && level() instanceof ServerLevel sl) {
                Vec3 from = eyePos();
                Vec3 to = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
                boolean see = hasLineOfSight(target) && from.distanceTo(to) < 15;
                Vec3 step = to.subtract(from).scale(1.0 / 12);
                for (int i = 1; i <= 12; i += (see ? 1 : 3)) {
                    Vec3 p = from.add(step.scale(i));
                    sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0);
                }
                if (see && t % 5 == 0) {
                    target.igniteForSeconds(3.0F);
                    damage(target, 2.0F, 0);
                }
            }
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.FLAME, getRandomX(0.5), getY() + 0.2, getRandomZ(0.5), 0, -0.03, 0);
    }
}
