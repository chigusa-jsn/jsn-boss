package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.BossConstants;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BossConstants.MODID);

    /** テスト用のスポーンエッグ（黒地に溶岩色の斑点） */
    public static final DeferredItem<DeferredSpawnEggItem> FIRE_KNIGHT_SPAWN_EGG =
            ITEMS.register("fire_knight_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.FIRE_KNIGHT, 0x1D1A24, 0xFF7A1C, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ICE_WITCH_SPAWN_EGG =
            ITEMS.register("ice_witch_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ICE_WITCH, 0x1D2B53, 0x7FE3FF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ZOMBIE_KING_SPAWN_EGG =
            ITEMS.register("zombie_king_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ZOMBIE_KING, 0x4A1D4F, 0xC9A227, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> DARK_MAGE_SPAWN_EGG =
            ITEMS.register("dark_mage_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.DARK_MAGE, 0x1B1426, 0xB06BFF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> BLADE_DRAGON_SPAWN_EGG =
            ITEMS.register("blade_dragon_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.BLADE_DRAGON, 0x17181F, 0xFF3048, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> WHITE_FOX_SPAWN_EGG =
            ITEMS.register("white_fox_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.WHITE_FOX, 0xFBFAF6, 0xE0402A, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> WIND_CAT_SPAWN_EGG =
            ITEMS.register("wind_cat_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.WIND_CAT, 0xCDD6CF, 0x3AA688, new Item.Properties()));

    private ModItems() {}
}
