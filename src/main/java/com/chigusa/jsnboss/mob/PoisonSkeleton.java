package com.chigusa.jsnboss.mob;

import com.chigusa.jsnboss.registry.ModMobs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;

/** 毒の弓兵：毒がしたたる緑のスケルトン。当たった場所に毒の霧が広がる矢を撃つ。 */
public class PoisonSkeleton extends Skeleton {
    public PoisonSkeleton(EntityType<? extends Skeleton> type, Level level) { super(type, level); }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        PoisonCloudArrow arrow = new PoisonCloudArrow(ModMobs.POISON_CLOUD_ARROW.get(), level());
        arrow.setOwner(this);
        arrow.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + h * 0.2, dz, 1.6F, 14 - this.level().getDifficulty().getId() * 4);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        this.level().addFreshEntity(arrow);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.ITEM_SLIME, getRandomX(0.4), getRandomY(), getRandomZ(0.4), 0, 0, 0);
        }
    }
}
