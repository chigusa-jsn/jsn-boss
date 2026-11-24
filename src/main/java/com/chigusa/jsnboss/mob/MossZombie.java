package com.chigusa.jsnboss.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** 森の苔ゾンビ：苔とツタに覆われたゾンビ。攻撃で鈍足（効果は VariantEffects、ドロップは VariantDrops） */
public class MossZombie extends Zombie implements Themed {
    @Override
    public Theme theme() { return Theme.MOSS; }

    public MossZombie(EntityType<? extends Zombie> type, Level level) { super(type, level); }

    @Override
    protected boolean convertsInWater() { return false; }
}
