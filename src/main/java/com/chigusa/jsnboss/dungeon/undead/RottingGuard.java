package com.chigusa.jsnboss.dungeon.undead;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 腐肉の近衛（中ボス・HP500）。ゾンビの王の近衛兵。ハルバードのなぎ払い・突き・叩きつけ、ゾンビを呼ぶ */
public class RottingGuard extends DungeonMob {
    static final Move SWEEP = new Move("sweep", 26, 30, 0, 4.5, 4, false);
    static final Move THRUST = new Move("thrust", 20, 40, 2.5, 6, 3, true);
    static final Move SLAM = new Move("slam", 32, 90, 0, 5, 2, false);
    static final Move SUMMON = new Move("summon", 30, 400, 0, 20, 2, false);
    private static final List<Move> MOVES = List.of(SWEEP, THRUST, SLAM, SUMMON);

    public RottingGuard(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 10.0).add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == SWEEP) {
            if (t == 13) { hitFront(4.6, 100, 11.0F, 0.9); playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F); }
        } else if (m == THRUST) {
            if (t == 10) {                                       // 細長い突き：前方 6 ブロックの線
                Vec3 f = forward();
                for (LivingEntity e : inArc(6.2, 18)) damage(e, 9.0F, 0.8);
                if (level() instanceof ServerLevel sl) {
                    for (int i = 1; i <= 6; i++) sl.sendParticles(ParticleTypes.CRIT, getX() + f.x * i, getY() + 1.3, getZ() + f.z * i, 2, 0.05, 0.05, 0.05, 0);
                }
                playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 0.6F);
            }
        } else if (m == SLAM) {
            if (t == 16) {
                Vec3 at = position().add(forward().scale(2.5));
                hitRadius(at, 3.5, 10.0F, 0.6);
                playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.2F, 0.6F);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()), at.x, getY() + 0.2, at.z, 50, 1.6, 0.1, 1.6, 0.2);
                }
                if (enraged()) {                                 // 怒ると叩きつけの衝撃が広がる
                    hitRadius(position(), 5.5, 6.0F, 0.4);
                }
            }
        } else if (m == SUMMON) {
            if (t == 10) playSound(SoundEvents.ZOMBIE_AMBIENT, 2.0F, 0.5F);
            if (t == 16) summonZombies(target);
        }
    }

    /** ゾンビを2体呼ぶ（まわりに4体以上いたら呼ばない） */
    private void summonZombies(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        int near = level().getEntitiesOfClass(Zombie.class, getBoundingBox().inflate(16)).size();
        for (int i = 0; i < Math.min(2, 4 - near); i++) {
            Zombie z = EntityType.ZOMBIE.create(sl);
            if (z == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            double x = getX() + Math.cos(a) * 2.5, zz = getZ() + Math.sin(a) * 2.5;
            z.moveTo(x, groundY(x, getY() + 1, zz), zz, random.nextFloat() * 360, 0);
            z.finalizeSpawn(sl, sl.getCurrentDifficultyAt(z.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            if (target != null) z.setTarget(target);
            sl.addFreshEntity(z);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), x, z.getY() + 0.2, zz, 30, 0.4, 0.1, 0.4, 0.1);
        }
    }
}
