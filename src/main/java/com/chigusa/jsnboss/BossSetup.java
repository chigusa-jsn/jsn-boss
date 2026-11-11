package com.chigusa.jsnboss;

import com.chigusa.jsnboss.entity.FireKnightEntity;
import com.chigusa.jsnboss.entity.IceWitchEntity;
import com.chigusa.jsnboss.entity.ZombieKingEntity;
import com.chigusa.jsnboss.entity.DarkMageEntity;
import com.chigusa.jsnboss.registry.ModEffects;
import com.chigusa.jsnboss.registry.ModEntities;
import com.chigusa.jsnboss.registry.ModItems;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * ボス関係の登録をまとめたもの。
 * 自分の MOD のメインクラス（@Mod がついたクラス）のコンストラクタで
 *     BossSetup.init(modEventBus);
 * を1行呼ぶだけで OK。
 */
public final class BossSetup {
    /** 最大HPの上限。バニラは1024なので、HP1500以上のボスのために引き上げる */
    public static final double MAX_HEALTH_CAP = 100000.0;

    public static void init(IEventBus modBus) {
        raiseMaxHealthCap();
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEffects.MOB_EFFECTS.register(modBus);
        modBus.addListener(BossSetup::onAttributes);
        modBus.addListener(BossSetup::onCreativeTab);
    }

    /** accesstransformer.cfg で書き換え可能にした上限値を引き上げる（ほかのMODがもっと高くしていたら触らない） */
    private static void raiseMaxHealthCap() {
        if (Attributes.MAX_HEALTH.value() instanceof RangedAttribute health && health.getMaxValue() < MAX_HEALTH_CAP) {
            health.maxValue = MAX_HEALTH_CAP;
        }
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FIRE_KNIGHT.get(), FireKnightEntity.createAttributes().build());
        event.put(ModEntities.ICE_WITCH.get(), IceWitchEntity.createAttributes().build());
        event.put(ModEntities.ICE_WITCH_CLONE.get(), IceWitchEntity.createCloneAttributes().build());
        event.put(ModEntities.ZOMBIE_KING.get(), ZombieKingEntity.createAttributes().build());
        event.put(ModEntities.DARK_MAGE.get(), DarkMageEntity.createAttributes().build());
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.FIRE_KNIGHT_SPAWN_EGG.get());
            event.accept(ModItems.ICE_WITCH_SPAWN_EGG.get());
            event.accept(ModItems.ZOMBIE_KING_SPAWN_EGG.get());
            event.accept(ModItems.DARK_MAGE_SPAWN_EGG.get());
        }
    }

    private BossSetup() {}
}
