package com.chigusa.jsnboss.mob;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** 亜種ブレイズの共通部分。バニラのブレイズと同じように 3 連射するが、撃つ弾が違う。 */
public abstract class VariantBlaze extends Blaze {
    protected VariantBlaze(EntityType<? extends Blaze> type, Level level) {
        super(type, level);
    }

    /** MobBoltEntity.SOUL / FROST */
    protected abstract int boltKind();

    protected abstract float boltDamage();

    protected abstract ParticleOptions ambientParticle();

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new BoltAttackGoal(this));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(2) == 0) {
            level().addParticle(ambientParticle(), getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.02, 0);
        }
    }

    /** バニラの BlazeAttackGoal とほぼ同じ動き：近ければ殴り、遠ければためてから 3 連射 */
    static class BoltAttackGoal extends Goal {
        private final VariantBlaze blaze;
        private int attackStep, attackTime, lastSeen;

        BoltAttackGoal(VariantBlaze blaze) {
            this.blaze = blaze;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = blaze.getTarget();
            return t != null && t.isAlive() && blaze.canAttack(t);
        }

        @Override
        public void start() { attackStep = 0; }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            attackTime--;
            LivingEntity t = blaze.getTarget();
            if (t == null) return;
            boolean sees = blaze.getSensing().hasLineOfSight(t);
            lastSeen = sees ? 0 : lastSeen + 1;
            double d = blaze.distanceToSqr(t);
            if (d < 4.0) {
                if (!sees) return;
                if (attackTime <= 0) {
                    attackTime = 20;
                    blaze.doHurtTarget(t);
                }
                blaze.getMoveControl().setWantedPosition(t.getX(), t.getY(), t.getZ(), 1.0);
            } else if (d < 48 * 48 / 4.0 && sees) {
                if (attackTime <= 0) {
                    attackStep++;
                    if (attackStep == 1) {
                        attackTime = 60;                   // ためる
                    } else if (attackStep <= 4) {
                        attackTime = 6;
                    } else {
                        attackTime = 100;
                        attackStep = 0;
                    }
                    if (attackStep > 1) {
                        Vec3 from = new Vec3(blaze.getX(), blaze.getY(0.5) + 0.5, blaze.getZ());
                        double spread = Math.sqrt(Math.sqrt(d)) * 0.4;
                        Vec3 dir = new Vec3(t.getX() - from.x + blaze.getRandom().triangle(0, 2.297 * spread * 0.3),
                                t.getY(0.5) - from.y, t.getZ() - from.z + blaze.getRandom().triangle(0, 2.297 * spread * 0.3)).normalize();
                        MobBoltEntity.shoot(blaze, from.add(dir.scale(0.6)), dir.scale(0.9), blaze.boltKind(), blaze.boltDamage());
                        blaze.playSound(blaze.boltKind() == MobBoltEntity.SOUL
                                ? net.minecraft.sounds.SoundEvents.BLAZE_SHOOT : net.minecraft.sounds.SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 1.0F);
                    }
                }
                blaze.getLookControl().setLookAt(t, 10.0F, 10.0F);
            } else if (lastSeen < 5) {
                blaze.getMoveControl().setWantedPosition(t.getX(), t.getY(), t.getZ(), 1.0);
            }
        }
    }
}
