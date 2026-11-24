package com.chigusa.jsnboss.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** 黄金のゾンビ：全身が金ピカ。攻撃で弱体化と毒（効果は VariantEffects、ドロップは VariantDrops） */
public class GoldZombie extends Zombie implements Themed {
    @Override
    public Theme theme() { return Theme.GOLD; }

    public GoldZombie(EntityType<? extends Zombie> type, Level level) { super(type, level); }

    @Override
    protected boolean convertsInWater() { return false; }
}
