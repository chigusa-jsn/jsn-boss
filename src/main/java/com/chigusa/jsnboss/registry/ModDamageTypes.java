package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.BossConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * 蒼炎の火柱用のダメージの種類。中身は data/jsnboss/damage_type/blue_fire.json、
 * 防具・エンチャント・効果・盾を無視する設定は data/minecraft/tags/damage_type/ のタグ。
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> BLUE_FIRE =
            ResourceKey.create(Registries.DAMAGE_TYPE, BossConstants.id("blue_fire"));

    public static DamageSource blueFire(Level level, @Nullable Entity attacker) {
        Holder<DamageType> type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLUE_FIRE);
        return new DamageSource(type, attacker);
    }

    private ModDamageTypes() {}
}
