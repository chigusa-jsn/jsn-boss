package com.chigusa.jsnboss.dungeon.wind;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.WindBladeEntity;
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

/** 竜巻の精（強い敵・HP250）。下半身が竜巻の精霊。まわりの相手を吸い寄せ、風の刃を四方に飛ばす */
public class TornadoSpirit extends DungeonMob {
    static final Move PULL = new Move("pull", 40, 110, 0, 10, 3, false);
    static final Move BLADES = new Move("blades", 24, 50, 2, 16, 3, true);
    private static final List<Move> MOVES = List.of(PULL, BLADES);

    public TornadoSpirit(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override protected double keepDistance() { return 5.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    public boolean causeFallDamage(float distance, float mult, net.minecraft.world.damagesource.DamageSource source) { return false; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == PULL) {
            if (t == 6) playSound(SoundEvents.ELYTRA_FLYING, 1.2F, 0.7F);
            if (t >= 8 && t < 36) {
                for (LivingEntity e : inRadius(position(), 10, 4)) {
                    Vec3 to = position().subtract(e.position()).multiply(1, 0, 1);
                    double d = to.length();
                    if (d < 0.5) continue;
                    e.setDeltaMovement(e.getDeltaMovement().add(to.normalize().scale(0.08)));
                    e.hurtMarked = true;
                    if (d < 2.2 && t % 8 == 0) damage(e, 3.0F, 0);
                }
                if (level() instanceof ServerLevel sl && t % 2 == 0) {
                    double a = t * 0.6;
                    sl.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(a) * 5, getY() + 0.5, getZ() + Math.sin(a) * 5, 2, 0.2, 0.2, 0.2, 0.02);
                }
            }
        } else if (m == BLADES) {
            if (t == 12) {
                double off = random.nextDouble() * Math.PI;
                for (int i = 0; i < 6; i++) {
                    double a = off + i * Math.PI / 3;
                    Vec3 d = new Vec3(Math.cos(a), 0, Math.sin(a));
                    WindBladeEntity.shoot(this, new Vec3(getX(), getY() + 1.2, getZ()).add(d), d.scale(0.8), 6.0F);
                }
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 1.4F);
            }
        }
    }

    @Override
    protected void clientTick() {
        double a = tickCount * 0.5;
        level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(a) * 0.6, getY() + 0.2, getZ() + Math.sin(a) * 0.6, 0, 0.05, 0);
    }
}
