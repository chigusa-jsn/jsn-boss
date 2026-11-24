package com.chigusa.jsnboss.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

/** 残響のスケルトン：スカルクに侵された骨。当たると暗闇。倒すと低確率でウォーデン（効果は VariantEffects、ドロップは VariantDrops） */
public class SculkSkeleton extends Skeleton {
    public SculkSkeleton(EntityType<? extends Skeleton> type, Level level) { super(type, level); }
}
