package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.dungeon.fire.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.chigusa.jsnboss.registry.ModEntities.ENTITY_TYPES;
import static com.chigusa.jsnboss.registry.ModItems.ITEMS;

/**
 * ダンジョンモブ（自然には湧かない。スポーンエッグ・/summon・Party Dungeon のスポナー専用）。
 * 登録した順にクリエイティブタブ「ダンジョンモブ」に並ぶ。
 */
public final class ModDungeon {
    /** 全部のダンジョンモブ（id → 種類） */
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<? extends DungeonMob>>> TYPES = new LinkedHashMap<>();
    public static final List<DeferredItem<DeferredSpawnEggItem>> EGGS = new ArrayList<>();
    /** 中ボス id → ボスバーの色（ARGB） */
    public static final Map<String, Integer> MID_BAR_COLOR = new HashMap<>();
    private static final Map<String, Supplier<AttributeSupplier.Builder>> ATTRS = new HashMap<>();

    // ------------------------------------------------------------ 炎
    public static final DeferredHolder<EntityType<?>, EntityType<FlameSquire>> FLAME_SQUIRE =
            mob("flame_squire", FlameSquire::new, 0.8F, 2.3F, true, FlameSquire::createAttributes, 0x3A3640, 0xFF7A20);
    public static final DeferredHolder<EntityType<?>, EntityType<LavaArmor>> LAVA_ARMOR =
            mob("lava_armor", LavaArmor::new, 1.2F, 2.6F, true, LavaArmor::createAttributes, 0x2A2422, 0xFF5A10);
    public static final DeferredHolder<EntityType<?>, EntityType<FlameEye>> FLAME_EYE =
            mob("flame_eye", FlameEye::new, 1.2F, 1.6F, true, FlameEye::createAttributes, 0xF0E4D4, 0xFF6A10);
    public static final DeferredHolder<EntityType<?>, EntityType<EmberImp>> EMBER_IMP =
            mob("ember_imp", EmberImp::new, 0.6F, 1.3F, true, EmberImp::createAttributes, 0xC8402A, 0xFFE040);
    public static final DeferredHolder<EntityType<?>, EntityType<CinderBat>> CINDER_BAT =
            mob("cinder_bat", CinderBat::new, 0.9F, 0.8F, true, CinderBat::createAttributes, 0x3A2420, 0xFF7A20);

    static {
        MID_BAR_COLOR.put("flame_squire", 0xFFFF6A20);
    }

    private static <T extends DungeonMob> DeferredHolder<EntityType<?>, EntityType<T>> mob(
            String id, EntityType.EntityFactory<T> factory, float w, float h, boolean fireImmune,
            Supplier<AttributeSupplier.Builder> attrs, int eggBg, int eggFg) {
        DeferredHolder<EntityType<?>, EntityType<T>> type = ENTITY_TYPES.register(id, () -> {
            EntityType.Builder<T> b = EntityType.Builder.of(factory, MobCategory.MONSTER).sized(w, h).clientTrackingRange(10);
            if (fireImmune) b = b.fireImmune();
            return b.build(id);
        });
        TYPES.put(id, type);
        ATTRS.put(id, attrs);
        EGGS.add(ITEMS.register(id + "_spawn_egg", () -> new DeferredSpawnEggItem(type, eggBg, eggFg, new Item.Properties())));
        return type;
    }

    public static void onAttributes(EntityAttributeCreationEvent event) {
        for (var e : TYPES.entrySet()) event.put(e.getValue().get(), ATTRS.get(e.getKey()).get().build());
    }

    /** BossSetup から呼んで、このクラスを読み込ませる（ここの登録を有効にする） */
    public static void init() {}

    private ModDungeon() {}
}
