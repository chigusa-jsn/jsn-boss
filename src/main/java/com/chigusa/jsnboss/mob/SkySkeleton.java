package com.chigusa.jsnboss.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

/** 空の弓兵：雲と空の色の骨。当たると打ち上げられる（効果は VariantEffects、ドロップは VariantDrops） */
public class SkySkeleton extends Skeleton implements Themed {
    @Override
    public Theme theme() { return Theme.SKY; }

    public SkySkeleton(EntityType<? extends Skeleton> type, Level level) { super(type, level); }
}
