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

    private ModItems() {}
}
