package com.chigusa.jsnboss;

import com.chigusa.jsnboss.entity.FireKnightEntity;
import com.chigusa.jsnboss.registry.ModEffects;
import com.chigusa.jsnboss.registry.ModEntities;
import com.chigusa.jsnboss.registry.ModItems;
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
    public static void init(IEventBus modBus) {
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEffects.MOB_EFFECTS.register(modBus);
        modBus.addListener(BossSetup::onAttributes);
        modBus.addListener(BossSetup::onCreativeTab);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FIRE_KNIGHT.get(), FireKnightEntity.createAttributes().build());
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.FIRE_KNIGHT_SPAWN_EGG.get());
        }
    }

    private BossSetup() {}
}
