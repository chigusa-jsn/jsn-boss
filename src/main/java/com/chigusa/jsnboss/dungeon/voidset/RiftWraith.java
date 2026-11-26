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

/** 次元の歪み（強い敵・HP230）。光る裂け目から手が伸びる幽霊。相手を吸い寄せ、近くへ無理やりワープさせる */
public class RiftWraith extends DungeonMob {
    static final Move PULL = new Move("pull", 40, 90, 2, 12, 3, true);
    static final Move WARP = new Move("warp", 28, 110, 4, 16, 2, true);
    private static final List<Move> MOVES = List.of(PULL, WARP);

    public RiftWraith(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.PURPLE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 230.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.FLYING_SPEED, 0.22);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 1.0; }
    @Override protected double keepDistance() { return 6.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        if (m == PULL) {
            if (t >= 8 && t < 36 && hasLineOfSight(target) && distanceTo(target) < 13) {
                Vec3 to = position().subtract(target.position());
                target.setDeltaMovement(target.getDeltaMovement().add(to.normalize().scale(0.09)));
                target.hurtMarked = true;
                if (distanceTo(target) < 2.2 && t % 10 == 0) damage(target, 4.0F, 0);
                if (level() instanceof ServerLevel sl && t % 2 == 0) {
                    Vec3 p = target.position().add(0, 1, 0);
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 4, 0.3, 0.5, 0.3, 0.05);
                }
            }
            if (t == 8) playSound(SoundEvents.PORTAL_AMBIENT, 1.0F, 1.6F);
        } else if (m == WARP) {
            if (t == 14 && hasLineOfSight(target)) {
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1, target.getZ(), 40, 0.3, 0.8, 0.3, 0.4);
                double a = random.nextDouble() * Math.PI * 2;
                double x = getX() + Math.cos(a) * 3, z = getZ() + Math.sin(a) * 3;
                target.teleportTo(x, groundY(x, getY() + 2, z), z);
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                damage(target, 3.0F, 0);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.2F, 0.6F);
            }
        }
    }

    @Override
    protected void clientTick() {
        level().addParticle(ParticleTypes.PORTAL, getRandomX(0.4), getY() + 0.5 + random.nextDouble() * 1.2, getRandomZ(0.4), 0, 0, 0);
    }
}
