package com.chigusa.jsnboss.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;

/** べたべたグモ：ハチミツとスライムまみれ。攻撃でくっつく（鈍足＋ジャンプできない）（効果は VariantEffects、ドロップは VariantDrops） */
public class HoneySpider extends Spider {
    public HoneySpider(EntityType<? extends Spider> type, Level level) { super(type, level); }
}
