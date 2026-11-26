package com.chigusa.jsnboss.dungeon.earth;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.BoulderEntity;
import com.chigusa.jsnboss.entity.RockPillarEntity;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 岩石のゴーレム（中ボス・HP500）。岩の巨人の眷属。岩投げ、岩の柱の列、地鳴らし、殴り */
public class BoulderGolem extends DungeonMob {
    static final Move PUNCH = new Move("punch", 20, 24, 0, 3.4, 4, false);
    static final Move THROW = new Move("throw", 32, 70, 4, 22, 3, true);
    static final Move PILLARS = new Move("pillars", 32, 90, 2, 14, 3, true);
    static final Move QUAKE = new Move("quake", 28, 110, 0, 6, 2, false);
    private static final List<Move> MOVES = List.of(PUNCH, THROW, PILLARS, QUAKE);

    public BoulderGolem(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 14.0).add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == PUNCH) {
            if (t == 10) { hitFront(3.6, 60, 12.0F, 1.2); playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.6F); }
        } else if (m == THROW) {
            if (t == 16 && target != null) {
                Vec3 from = new Vec3(getX(), getY() + getBbHeight() + 0.5, getZ());
                BoulderEntity.throwAt(this, from, target.position(), 12.0F, enraged());
                playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.2F, 0.5F);
            }
        } else if (m == PILLARS) {
            if (t == 14 && target != null) {
                Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                for (int i = 1; i <= 6; i++) {
                    RockPillarEntity.spawn(this, getX() + dir.x * i * 1.7, getZ() + dir.z * i * 1.7, 6 + i * 3, 9.0F);
                }
                dust(position().add(forward().scale(2)), 30);
                playSound(SoundEvents.STONE_BREAK, 1.5F, 0.6F);
            }
        } else if (m == QUAKE) {
            if (t == 13) {
                for (LivingEntity e : inRadius(position(), 6, 2)) {
                    if (e.onGround() && damage(e, 8.0F, 0.8)) launch(e, 0.5);
                }
                dust(position(), 80);
                playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.5F);
            }
        }
    }

    private void dust(Vec3 at, int n) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), at.x, getY() + 0.2, at.z, n, 2.5, 0.1, 2.5, 0.2);
        }
    }
}
