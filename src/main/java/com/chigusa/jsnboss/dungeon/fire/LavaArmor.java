package com.chigusa.jsnboss.dungeon.fire;

import com.chigusa.jsnboss.dungeon.DungeonMob;
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

/** 溶岩の甲冑兵（強い敵・HP280）。中が溶岩の空っぽの鎧。重いパンチと、燃えながらの突進 */
public class LavaArmor extends DungeonMob {
    static final Move PUNCH = new Move("punch", 20, 24, 0, 3.2, 4, false);
    static final Move CHARGE = new Move("charge", 44, 90, 4, 14, 3, true);
    private static final List<Move> MOVES = List.of(PUNCH, CHARGE);

    private Vec3 dashDir = Vec3.ZERO;

    public LavaArmor(EntityType<? extends Monster> type, Level level) {
        super(type, level, Tier.STRONG, BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280.0).add(Attributes.ARMOR, 12.0).add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected List<Move> moves() { return MOVES; }

    @Override
    protected void tickMove(Move m, int t, LivingEntity target) {
        if (m == PUNCH) {
            if (t == 10) {
                for (LivingEntity e : hitFront(3.4, 60, 9.0F, 0.8)) e.igniteForSeconds(3.0F);
                playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.7F);
            }
        } else if (m == CHARGE) {
            ServerLevel sl = level() instanceof ServerLevel s ? s : null;
            if (t < 12 && sl != null) sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 2, 0.4, 0.4, 0.4, 0.01);
            if (t == 12) { dashDir = forward(); playSound(SoundEvents.RAVAGER_ROAR, 0.8F, 1.3F); }
            if (t >= 12 && t < 34) {
                dash(dashDir, 0.55);
                for (LivingEntity e : hitAround(1.8, 10.0F, 1.4)) e.igniteForSeconds(4.0F);
                if (sl != null) {
                    sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.2, getZ(), 6, 0.5, 0.1, 0.5, 0.01);
                    sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.5, getZ(), 1, 0.3, 0.2, 0.3, 0);
                }
            }
            if (t == 34) setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    protected void clientTick() {
        if (random.nextInt(6) == 0) level().addParticle(ParticleTypes.LAVA, getRandomX(0.5), getY() + 1.4, getRandomZ(0.5), 0, 0, 0);
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 2.5, getZ(), 0, 0.03, 0);
    }
}
