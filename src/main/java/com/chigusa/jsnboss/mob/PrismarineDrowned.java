package com.chigusa.jsnboss.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.Level;

/** 海のドラウンド：プリズマリンをまとったドラウンド。攻撃で採掘速度低下（効果は VariantEffects） */
public class PrismarineDrowned extends Drowned {
    public PrismarineDrowned(EntityType<? extends Drowned> type, Level level) { super(type, level); }
}
