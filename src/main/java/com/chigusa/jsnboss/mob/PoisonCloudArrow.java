package com.chigusa.jsnboss.mob;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** 毒の弓兵の矢：当たった場所に毒の霧（半径2.5、4秒）が広がる */
public class PoisonCloudArrow extends Arrow {
    private boolean clouded;

    public PoisonCloudArrow(EntityType<? extends Arrow> type, Level level) {
        super(type, level);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide && !clouded) {
            clouded = true;
            AreaEffectCloud cloud = new AreaEffectCloud(level(), result.getLocation().x, result.getLocation().y, result.getLocation().z);
            if (this.getOwner() instanceof LivingEntity le) cloud.setOwner(le);
            cloud.setRadius(2.5F);
            cloud.setDuration(80);
            cloud.setWaitTime(0);
            cloud.setRadiusPerTick(-0.01F);
            cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
            level().addFreshEntity(cloud);
        }
    }
}
