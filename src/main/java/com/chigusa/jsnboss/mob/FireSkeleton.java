package com.chigusa.jsnboss.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 炎の弓兵：焦げた骨に残り火が光るスケルトン。火のついた矢を 3 本同時に扇状に撃つ。火が効かない。 */
public class FireSkeleton extends Skeleton {
    public FireSkeleton(EntityType<? extends Skeleton> type, Level level) { super(type, level); }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ItemStack weapon = this.getMainHandItem();
        for (int i = -1; i <= 1; i++) {
            AbstractArrow arrow = this.getArrow(new ItemStack(Items.ARROW), power, weapon);
            double dx = target.getX() - this.getX();
            double dy = target.getY(0.3333) - arrow.getY();
            double dz = target.getZ() - this.getZ();
            double h = Math.sqrt(dx * dx + dz * dz);
            Vec3 dir = new Vec3(dx, 0, dz).yRot(i * 10.0F * Mth.DEG_TO_RAD);
            arrow.shoot(dir.x, dy + h * 0.2, dir.z, 1.6F, 14 - this.level().getDifficulty().getId() * 4);
            arrow.igniteForSeconds(100);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            this.level().addFreshEntity(arrow);
        }
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.playSound(SoundEvents.BLAZE_SHOOT, 0.5F, 1.4F);
    }

    @Override
    protected boolean isSunBurnTick() { return false; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.SMALL_FLAME, getRandomX(0.4), getRandomY(), getRandomZ(0.4), 0, 0.02, 0);
        }
    }
}
