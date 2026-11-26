package com.chigusa.jsnboss.dungeon.elite;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.RockPillarEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 樹人の守り手（中ボス・HP500）。ユグドラシルの守り手。足もとから根を突き上げ、枝でなぎ払い、若木を育ててまわりの仲間を回復する */
public class TreantWarden extends DungeonMob {
    static final Move ROOTS = new Move("roots", 32, 70, 2, 16, 4, true);
    static final Move SWEEP = new Move("sweep", 26, 30, 0, 4.5, 4, false);
    static final Move HEAL = new Move("heal", 40, 240, 0, 32, 2, false);
    private static final List<Move> MOVES = List.of(ROOTS, SWEEP, HEAL);

    public TreantWarden(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 10.0).add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == ROOTS) {
            if (t == 16 && target != null) {
                RockPillarEntity.spawnRoot(this, target.getX(), target.getZ(), 14, 10.0F);
                Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                for (int i = 1; i <= 4; i++) RockPillarEntity.spawnRoot(this, getX() + dir.x * i * 1.8, getZ() + dir.z * i * 1.8, 6 + i * 3, 10.0F);
                if (enraged()) {
                    for (int i = 0; i < 6; i++) {
                        double a = i * Math.PI / 3, r = 3;
                        RockPillarEntity.spawnRoot(this, target.getX() + Math.cos(a) * r, target.getZ() + Math.sin(a) * r, 22, 10.0F);
                    }
                }
                playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.5F, 0.5F);
            }
        } else if (m == SWEEP) {
            if (t == 14) { hitFront(4.6, 110, 11.0F, 1.2); playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.5F); }
        } else if (m == HEAL) {
            if (t == 20) {
                heal(40.0F);
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12),
                        e -> e != this && e instanceof Enemy && e.isAlive())) {
                    e.heal(30.0F);
                    if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 1, e.getZ(), 10, 0.4, 0.6, 0.4, 0);
                }
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 2, getZ(), 40, 1.5, 1.5, 1.5, 0);
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 3, getZ(), 40, 3, 1, 3, 0);
                }
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 0.6F);
            }
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(8) == 0) level().addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM, getRandomX(1.0), getY() + 3.5, getRandomZ(1.0), 0, 0, 0);
    }
}
