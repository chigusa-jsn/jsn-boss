package com.chigusa.jsnboss.dungeon.dark;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.DarkOrbEntity;
import com.chigusa.jsnboss.registry.ModDungeon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 影の弟子（中ボス・HP500）。闇の魔導士の弟子。闇の玉、背後への瞬間移動斬り、暗闇の閃光、影の小人を呼ぶ */
public class ShadowApprentice extends DungeonMob {
    static final Move ORBS = new Move("orbs", 24, 36, 2, 20, 4, true);
    static final Move BACKSTAB = new Move("backstab", 24, 90, 3, 14, 3, true);
    static final Move FLASH = new Move("flash", 28, 120, 0, 7, 2, false);
    static final Move SUMMON = new Move("summon", 30, 360, 0, 20, 2, false);
    private static final List<Move> MOVES = List.of(ORBS, BACKSTAB, FLASH, SUMMON);

    public ShadowApprentice(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.MID, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.KNOCKBACK_RESISTANCE, 0.4).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override protected double keepDistance() { return 7.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        if (m == ORBS) {
            if (t == 10) {
                int n = enraged() ? 5 : 3;
                Vec3 dir = aim(target);
                for (int i = 0; i < n; i++) {
                    Vec3 d = dir.yRot((float) Math.toRadians((i - (n - 1) / 2.0) * 16));
                    DarkOrbEntity.shoot(this, target, eyePos().add(d), d.scale(0.55), 6.0F, false);
                }
                playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.2F);
            }
        } else if (m == BACKSTAB) {
            if (t == 7) {
                Vec3 behind = target.position().subtract(Vec3.directionFromRotation(0, target.getYRot()).scale(1.8));
                puff();
                teleportTo(behind.x, groundY(behind.x, target.getY() + 1, behind.z), behind.z);
                face(target, 360);
                puff();
                playSound(SoundEvents.ENDERMAN_TELEPORT, 0.8F, 0.8F);
            }
            if (t == 15) {
                for (LivingEntity e : hitFront(3.0, 70, 10.0F, 0.4)) e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            }
        } else if (m == FLASH) {
            if (t == 14) {
                for (LivingEntity e : inRadius(position(), 7, 3)) {
                    damage(e, 4.0F, 0.5);
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
                }
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.2, getZ(), 80, 3, 1, 3, 0.05);
                playSound(SoundEvents.WARDEN_SONIC_CHARGE, 0.8F, 1.6F);
            }
        } else if (m == SUMMON) {
            if (t == 16 && level() instanceof ServerLevel sl) {
                int near = level().getEntitiesOfClass(ShadeImp.class, getBoundingBox().inflate(16)).size();
                for (int i = 0; i < Math.min(2, 4 - near); i++) {
                    ShadeImp imp = ModDungeon.SHADE_IMP.get().create(sl);
                    if (imp == null) continue;
                    double a = random.nextDouble() * Math.PI * 2;
                    double x = getX() + Math.cos(a) * 2, z = getZ() + Math.sin(a) * 2;
                    imp.moveTo(x, groundY(x, getY() + 1, z), z, random.nextFloat() * 360, 0);
                    imp.finalizeSpawn(sl, sl.getCurrentDifficultyAt(imp.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    imp.setTarget(target);
                    sl.addFreshEntity(imp);
                    sl.sendParticles(ParticleTypes.SQUID_INK, x, imp.getY() + 0.5, z, 15, 0.3, 0.4, 0.3, 0.02);
                }
                playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 1.2F);
            }
        }
    }

    private void puff() {
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 25, 0.3, 0.7, 0.3, 0.02);
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.PORTAL, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0, 0);
    }
}
