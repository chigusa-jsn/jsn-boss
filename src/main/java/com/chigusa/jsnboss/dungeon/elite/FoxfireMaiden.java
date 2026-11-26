package com.chigusa.jsnboss.dungeon.elite;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.FoxfireEntity;
import com.chigusa.jsnboss.entity.LightPillarEntity;
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

/** 狐火の巫女（中ボス・HP500）。光の白狐の巫女。追いかける狐火、光の柱、お札で動きを止める、近づかれると狐のように跳んで離れる */
public class FoxfireMaiden extends DungeonMob {
    static final Move FOXFIRE = new Move("foxfire", 24, 40, 2, 20, 4, true);
    static final Move PILLAR = new Move("pillar", 28, 80, 0, 18, 3, true);
    static final Move CHARM = new Move("charm", 20, 110, 0, 12, 2, true);
    static final Move BLINK = new Move("blink", 16, 80, 0, 4, 3, false);
    private static final List<Move> MOVES = List.of(FOXFIRE, PILLAR, CHARM, BLINK);

    public FoxfireMaiden(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.BLUE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.4).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override protected double keepDistance() { return 8.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        if (m == FOXFIRE) {
            if (t == 10) {
                int n = enraged() ? 5 : 3;
                for (int i = 0; i < n; i++) {
                    Vec3 d = aim(target).yRot((float) Math.toRadians((i - (n - 1) / 2.0) * 25)).add(0, 0.2, 0);
                    FoxfireEntity.shoot(this, target, eyePos().add(d), d.scale(0.45), 6.0F);
                }
                playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.6F);
            }
        } else if (m == PILLAR) {
            if (t == 12) {
                LightPillarEntity.spawn(this, target.getX(), groundY(target.getX(), target.getY() + 1, target.getZ()), target.getZ(), 9.0F);
                playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 1.6F);
            }
        } else if (m == CHARM) {
            if (t == 9 && hasLineOfSight(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4));      // ほぼ止まる
                damage(target, 4.0F, 0);
                if (level() instanceof ServerLevel sl) {
                    Vec3 a = eyePos(), b = target.position().add(0, 1, 0);
                    for (int i = 0; i <= 10; i++) {
                        Vec3 p = a.lerp(b, i / 10.0);
                        sl.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 3, 0.05, 0.05, 0.05, 0);
                    }
                    sl.sendParticles(ParticleTypes.END_ROD, b.x, b.y, b.z, 12, 0.3, 0.6, 0.3, 0.02);
                }
                playSound(SoundEvents.BOOK_PAGE_TURN, 1.5F, 1.2F);
            }
        } else if (m == BLINK) {
            if (t == 6) {
                double a = random.nextDouble() * Math.PI * 2;
                blinkTo(target.getX() + Math.cos(a) * 9, target.getZ() + Math.sin(a) * 9, target.getY());
            }
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getRandomX(1.0), getY() + 1.5 + random.nextDouble(), getRandomZ(1.0), 0, 0.01, 0);
    }
}
