package com.chigusa.jsnboss.dungeon.dark;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.entity.DarkOrbEntity;
import com.chigusa.jsnboss.registry.ModDungeon;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 魔導書の魔物（強い敵・HP240）。浮かぶ本。闇の玉をばらまき、近づくと噛みつき、影の小人を呼ぶ */
public class Grimoire extends DungeonMob {
    static final Move ORBS = new Move("orbs", 28, 50, 2, 20, 4, true);
    static final Move BITE = new Move("bite", 16, 24, 0, 2.2, 3, false);
    static final Move SUMMON = new Move("summon", 28, 300, 0, 20, 1, false);
    private static final List<Move> MOVES = List.of(ORBS, BITE, SUMMON);

    public Grimoire(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.PURPLE);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 240.0).add(Attributes.ARMOR, 4.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.FLYING_SPEED, 0.28);
    }

    @Override protected boolean flying() { return true; }
    @Override protected double hoverHeight() { return 1.5; }
    @Override protected double keepDistance() { return 6.0; }
    @Override protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (target == null) return;
        face(target, 20.0F);
        if (m == ORBS) {
            if (t >= 10 && t <= 22 && t % 3 == 1) {
                Vec3 d = aim(target).yRot((float) Math.toRadians((random.nextDouble() - 0.5) * 50));
                DarkOrbEntity.shoot(this, target, eyePos().add(d), d.scale(0.5), 5.0F, false);
                playSound(SoundEvents.BOOK_PAGE_TURN, 1.2F, 0.7F);
            }
        } else if (m == BITE) {
            if (t == 5) dash(aim(target), 0.6);
            if (t == 8) { hitAround(1.8, 6.0F, 0.3); playSound(SoundEvents.EVOKER_FANGS_ATTACK, 0.8F, 1.4F); }
        } else if (m == SUMMON) {
            if (t == 16 && level() instanceof ServerLevel sl
                    && level().getEntitiesOfClass(ShadeImp.class, getBoundingBox().inflate(16)).size() < 4) {
                ShadeImp imp = ModDungeon.SHADE_IMP.get().create(sl);
                if (imp != null) {
                    double x = getX(), z = getZ();
                    imp.moveTo(x, groundY(x, getY(), z), z, random.nextFloat() * 360, 0);
                    imp.finalizeSpawn(sl, sl.getCurrentDifficultyAt(imp.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    imp.setTarget(target);
                    sl.addFreshEntity(imp);
                    sl.sendParticles(ParticleTypes.SQUID_INK, x, imp.getY() + 0.5, z, 15, 0.3, 0.4, 0.3, 0.02);
                }
                playSound(SoundEvents.BOOK_PUT, 1.2F, 0.6F);
            }
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.ENCHANT, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0, 0.2, 0);
    }
}
