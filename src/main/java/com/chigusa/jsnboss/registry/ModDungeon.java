package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.dungeon.fire.*;
import com.chigusa.jsnboss.dungeon.ice.*;
import com.chigusa.jsnboss.dungeon.undead.*;
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
    /** 見た目の大きさ（モデルを拡大して描く。当たり判定は登録の大きさ） */
    public static final Map<String, Float> RENDER_SCALE = new HashMap<>();
    /** 半透明で描くモブ */
    public static final java.util.Set<String> TRANSLUCENT = new java.util.HashSet<>();
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

    // ------------------------------------------------------------ 氷
    public static final DeferredHolder<EntityType<?>, EntityType<FrostMaiden>> FROST_MAIDEN =
            mob("frost_maiden", FrostMaiden::new, 0.7F, 2.1F, false, FrostMaiden::createAttributes, 0x5AA0D8, 0xE8F6FF);
    public static final DeferredHolder<EntityType<?>, EntityType<PackAlpha>> PACK_ALPHA =
            mob("pack_alpha", PackAlpha::new, 1.6F, 1.9F, false, PackAlpha::createAttributes, 0x9AA2AC, 0x7AD8FF);
    public static final DeferredHolder<EntityType<?>, EntityType<FrostKnight>> FROST_KNIGHT =
            mob("frost_knight", FrostKnight::new, 1.0F, 2.4F, false, FrostKnight::createAttributes, 0x9AD8F4, 0x2A5A84);
    public static final DeferredHolder<EntityType<?>, EntityType<FrostfangBear>> FROSTFANG_BEAR =
            mob("frostfang_bear", FrostfangBear::new, 1.6F, 1.7F, false, FrostfangBear::createAttributes, 0xE8EEF2, 0x7AD8FF);
    public static final DeferredHolder<EntityType<?>, EntityType<FrostSprite>> FROST_SPRITE =
            mob("frost_sprite", FrostSprite::new, 0.6F, 1.0F, false, FrostSprite::createAttributes, 0xF4FBFF, 0x9AD8F4);
    public static final DeferredHolder<EntityType<?>, EntityType<IcicleCrawler>> ICICLE_CRAWLER =
            mob("icicle_crawler", IcicleCrawler::new, 0.9F, 0.6F, false, IcicleCrawler::createAttributes, 0x4A6A84, 0xBFEAFF);

    // ------------------------------------------------------------ アンデッド
    public static final DeferredHolder<EntityType<?>, EntityType<RottingGuard>> ROTTING_GUARD =
            mob("rotting_guard", RottingGuard::new, 0.9F, 2.6F, false, RottingGuard::createAttributes, 0x6A8A5A, 0x7A4A2A);
    public static final DeferredHolder<EntityType<?>, EntityType<GraveReaper>> GRAVE_REAPER =
            mob("grave_reaper", GraveReaper::new, 0.8F, 2.3F, false, GraveReaper::createAttributes, 0x2A2630, 0x7AF0A0);
    public static final DeferredHolder<EntityType<?>, EntityType<BoneJuggernaut>> BONE_JUGGERNAUT =
            mob("bone_juggernaut", BoneJuggernaut::new, 1.1F, 2.4F, false, BoneJuggernaut::createAttributes, 0xD8D0B8, 0x3A3A42);
    public static final DeferredHolder<EntityType<?>, EntityType<CrawlingBones>> CRAWLING_BONES =
            mob("crawling_bones", CrawlingBones::new, 0.8F, 0.5F, false, CrawlingBones::createAttributes, 0xD8D0B8, 0x7AF0A0);
    public static final DeferredHolder<EntityType<?>, EntityType<GraveWisp>> GRAVE_WISP =
            mob("grave_wisp", GraveWisp::new, 0.6F, 1.0F, false, GraveWisp::createAttributes, 0xA8F0E0, 0x0A1A18);

    static {
        MID_BAR_COLOR.put("flame_squire", 0xFFFF6A20);
        MID_BAR_COLOR.put("rotting_guard", 0xFF8ACC5A);
        RENDER_SCALE.put("rotting_guard", 1.25F);
        TRANSLUCENT.add("grave_wisp");
        MID_BAR_COLOR.put("frost_maiden", 0xFF6AC8FF);
        MID_BAR_COLOR.put("pack_alpha", 0xFFB8D8F0);
        RENDER_SCALE.put("pack_alpha", 1.5F);
        RENDER_SCALE.put("frostfang_bear", 1.3F);
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
