package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.dungeon.DungeonMob;
import com.chigusa.jsnboss.dungeon.fire.*;
import com.chigusa.jsnboss.dungeon.ice.*;
import com.chigusa.jsnboss.dungeon.undead.*;
import com.chigusa.jsnboss.dungeon.dark.*;
import com.chigusa.jsnboss.dungeon.wind.*;
import com.chigusa.jsnboss.dungeon.thunder.*;
import com.chigusa.jsnboss.dungeon.earth.*;
import com.chigusa.jsnboss.dungeon.voidset.*;
import com.chigusa.jsnboss.dungeon.nature.*;
import com.chigusa.jsnboss.dungeon.machine.*;
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

    // ------------------------------------------------------------ 闇
    public static final DeferredHolder<EntityType<?>, EntityType<ShadowApprentice>> SHADOW_APPRENTICE =
            mob("shadow_apprentice", ShadowApprentice::new, 0.7F, 2.1F, false, ShadowApprentice::createAttributes, 0x3A2A52, 0xC060FF);
    public static final DeferredHolder<EntityType<?>, EntityType<ShadowAssassin>> SHADOW_ASSASSIN =
            mob("shadow_assassin", ShadowAssassin::new, 0.6F, 2.0F, false, ShadowAssassin::createAttributes, 0x1E1A26, 0x7A2A52);
    public static final DeferredHolder<EntityType<?>, EntityType<Grimoire>> GRIMOIRE =
            mob("grimoire", Grimoire::new, 0.9F, 0.8F, false, Grimoire::createAttributes, 0x4A1A2A, 0xE0D4B8);
    public static final DeferredHolder<EntityType<?>, EntityType<ShadeImp>> SHADE_IMP =
            mob("shade_imp", ShadeImp::new, 0.5F, 1.2F, false, ShadeImp::createAttributes, 0x16121E, 0xD080FF);
    public static final DeferredHolder<EntityType<?>, EntityType<DarkEye>> DARK_EYE =
            mob("dark_eye", DarkEye::new, 0.6F, 0.6F, false, DarkEye::createAttributes, 0x2A1E2E, 0xA040FF);

    // ------------------------------------------------------------ 風
    public static final DeferredHolder<EntityType<?>, EntityType<WhirlLynx>> WHIRL_LYNX =
            mob("whirl_lynx", WhirlLynx::new, 1.2F, 1.6F, false, WhirlLynx::createAttributes, 0xA8C4B4, 0x8AF0C8);
    public static final DeferredHolder<EntityType<?>, EntityType<GaleEagle>> GALE_EAGLE =
            mob("gale_eagle", GaleEagle::new, 1.2F, 1.1F, false, GaleEagle::createAttributes, 0x6A4A32, 0xF0ECE4);
    public static final DeferredHolder<EntityType<?>, EntityType<TornadoSpirit>> TORNADO_SPIRIT =
            mob("tornado_spirit", TornadoSpirit::new, 0.8F, 1.7F, false, TornadoSpirit::createAttributes, 0xA8F0D8, 0x3A987A);
    public static final DeferredHolder<EntityType<?>, EntityType<WindPuff>> WIND_PUFF =
            mob("wind_puff", WindPuff::new, 0.7F, 0.8F, false, WindPuff::createAttributes, 0xF4FBF8, 0x7AD070);
    public static final DeferredHolder<EntityType<?>, EntityType<GaleWeasel>> GALE_WEASEL =
            mob("gale_weasel", GaleWeasel::new, 0.6F, 0.5F, false, GaleWeasel::createAttributes, 0xA87A4A, 0x8AF0C8);

    // ------------------------------------------------------------ 雷
    public static final DeferredHolder<EntityType<?>, EntityType<ThunderLioness>> THUNDER_LIONESS =
            mob("thunder_lioness", ThunderLioness::new, 1.2F, 1.6F, false, ThunderLioness::createAttributes, 0xC8A060, 0x7AD8FF);
    public static final DeferredHolder<EntityType<?>, EntityType<VoltSentinel>> VOLT_SENTINEL =
            mob("volt_sentinel", VoltSentinel::new, 1.0F, 2.5F, false, VoltSentinel::createAttributes, 0xC87A4A, 0x7AD8FF);
    public static final DeferredHolder<EntityType<?>, EntityType<StormLizard>> STORM_LIZARD =
            mob("storm_lizard", StormLizard::new, 1.3F, 0.9F, false, StormLizard::createAttributes, 0x3A4A62, 0x7AD8FF);
    public static final DeferredHolder<EntityType<?>, EntityType<SparkJelly>> SPARK_JELLY =
            mob("spark_jelly", SparkJelly::new, 0.7F, 1.0F, false, SparkJelly::createAttributes, 0x7AC0FF, 0xE8F8FF);
    public static final DeferredHolder<EntityType<?>, EntityType<VoltBeetle>> VOLT_BEETLE =
            mob("volt_beetle", VoltBeetle::new, 0.7F, 0.5F, false, VoltBeetle::createAttributes, 0x2A3A5A, 0x7AD8FF);

    // ------------------------------------------------------------ 大地
    public static final DeferredHolder<EntityType<?>, EntityType<BoulderGolem>> BOULDER_GOLEM =
            mob("boulder_golem", BoulderGolem::new, 1.5F, 2.4F, false, BoulderGolem::createAttributes, 0x8A8A84, 0xFFB040);
    public static final DeferredHolder<EntityType<?>, EntityType<StoneTortoise>> STONE_TORTOISE =
            mob("stone_tortoise", StoneTortoise::new, 1.7F, 1.3F, false, StoneTortoise::createAttributes, 0x8A8A84, 0x5A8A3A);
    public static final DeferredHolder<EntityType<?>, EntityType<ClayBrute>> CLAY_BRUTE =
            mob("clay_brute", ClayBrute::new, 1.3F, 2.2F, false, ClayBrute::createAttributes, 0xB0704A, 0x6A3E24);
    public static final DeferredHolder<EntityType<?>, EntityType<PebbleGolem>> PEBBLE_GOLEM =
            mob("pebble_golem", PebbleGolem::new, 0.6F, 0.9F, false, PebbleGolem::createAttributes, 0x9A968C, 0xFFB040);
    public static final DeferredHolder<EntityType<?>, EntityType<BurrowMole>> BURROW_MOLE =
            mob("burrow_mole", BurrowMole::new, 0.7F, 0.5F, false, BurrowMole::createAttributes, 0x3A3036, 0xE8A0A8);

    // ------------------------------------------------------------ 虚空
    public static final DeferredHolder<EntityType<?>, EntityType<VoidScout>> VOID_SCOUT =
            mob("void_scout", VoidScout::new, 0.7F, 2.6F, false, VoidScout::createAttributes, 0x1A1220, 0xC070FF);
    public static final DeferredHolder<EntityType<?>, EntityType<VoidStalker>> VOID_STALKER =
            mob("void_stalker", VoidStalker::new, 0.7F, 2.0F, false, VoidStalker::createAttributes, 0x241A30, 0xE080FF);
    public static final DeferredHolder<EntityType<?>, EntityType<RiftWraith>> RIFT_WRAITH =
            mob("rift_wraith", RiftWraith::new, 0.8F, 1.8F, false, RiftWraith::createAttributes, 0x4A2A6A, 0xFFFFFF);
    public static final DeferredHolder<EntityType<?>, EntityType<VoidShard>> VOID_SHARD =
            mob("void_shard", VoidShard::new, 0.5F, 0.9F, false, VoidShard::createAttributes, 0x8A40D0, 0xF0D0FF);
    public static final DeferredHolder<EntityType<?>, EntityType<WatcherMote>> WATCHER_MOTE =
            mob("watcher_mote", WatcherMote::new, 0.6F, 0.6F, false, WatcherMote::createAttributes, 0x2A2234, 0xC070FF);

    // ------------------------------------------------------------ 自然
    public static final DeferredHolder<EntityType<?>, EntityType<SporeNursery>> SPORE_NURSERY =
            mob("spore_nursery", SporeNursery::new, 2.0F, 3.2F, false, SporeNursery::createAttributes, 0x5AA04A, 0x8AE06A);
    public static final DeferredHolder<EntityType<?>, EntityType<VenusSnapper>> VENUS_SNAPPER =
            mob("venus_snapper", VenusSnapper::new, 1.2F, 2.4F, false, VenusSnapper::createAttributes, 0x7AC04A, 0xC03040);
    public static final DeferredHolder<EntityType<?>, EntityType<ThornStag>> THORN_STAG =
            mob("thorn_stag", ThornStag::new, 1.2F, 2.2F, false, ThornStag::createAttributes, 0x7A6A4A, 0x4A8A3A);
    public static final DeferredHolder<EntityType<?>, EntityType<MushroomSoldier>> MUSHROOM_SOLDIER =
            mob("mushroom_soldier", MushroomSoldier::new, 0.6F, 0.95F, false, MushroomSoldier::createAttributes, 0xE8DCC0, 0xC02A2A);
    public static final DeferredHolder<EntityType<?>, EntityType<SeedSquirrel>> SEED_SQUIRREL =
            mob("seed_squirrel", SeedSquirrel::new, 0.5F, 0.7F, false, SeedSquirrel::createAttributes, 0xB06A3A, 0xF0DCC0);

    // ------------------------------------------------------------ 機械
    public static final DeferredHolder<EntityType<?>, EntityType<ReconDrone>> RECON_DRONE =
            mob("recon_drone", ReconDrone::new, 1.6F, 1.4F, false, ReconDrone::createAttributes, 0x4A4E56, 0xFF3A2A);
    public static final DeferredHolder<EntityType<?>, EntityType<CogTank>> COG_TANK =
            mob("cog_tank", CogTank::new, 1.6F, 1.2F, false, CogTank::createAttributes, 0xC89A3A, 0x2A2C32);
    public static final DeferredHolder<EntityType<?>, EntityType<SteamAutomaton>> STEAM_AUTOMATON =
            mob("steam_automaton", SteamAutomaton::new, 1.0F, 2.3F, false, SteamAutomaton::createAttributes, 0xC89A3A, 0xFF8A20);
    public static final DeferredHolder<EntityType<?>, EntityType<ClockworkMouse>> CLOCKWORK_MOUSE =
            mob("clockwork_mouse", ClockworkMouse::new, 0.5F, 0.4F, false, ClockworkMouse::createAttributes, 0xA8ACB4, 0xC89A3A);
    public static final DeferredHolder<EntityType<?>, EntityType<MiniDrone>> MINI_DRONE =
            mob("mini_drone", MiniDrone::new, 0.7F, 0.6F, false, MiniDrone::createAttributes, 0x4A4E56, 0xFF3A2A);

    static {
        MID_BAR_COLOR.put("flame_squire", 0xFFFF6A20);
        MID_BAR_COLOR.put("recon_drone", 0xFFFF5040);
        RENDER_SCALE.put("recon_drone", 1.3F);
        RENDER_SCALE.put("cog_tank", 1.2F);
        MID_BAR_COLOR.put("spore_nursery", 0xFF8AE06A);
        RENDER_SCALE.put("spore_nursery", 1.4F);
        RENDER_SCALE.put("venus_snapper", 1.3F);
        RENDER_SCALE.put("thorn_stag", 1.2F);
        MID_BAR_COLOR.put("void_scout", 0xFFC070FF);
        TRANSLUCENT.add("rift_wraith");
        MID_BAR_COLOR.put("boulder_golem", 0xFFC8A050);
        RENDER_SCALE.put("boulder_golem", 1.2F);
        RENDER_SCALE.put("stone_tortoise", 1.2F);
        MID_BAR_COLOR.put("thunder_lioness", 0xFFFFE04A);
        RENDER_SCALE.put("thunder_lioness", 1.4F);
        RENDER_SCALE.put("storm_lizard", 1.3F);
        TRANSLUCENT.add("spark_jelly");
        MID_BAR_COLOR.put("whirl_lynx", 0xFF7AF0B8);
        RENDER_SCALE.put("whirl_lynx", 1.4F);
        RENDER_SCALE.put("gale_eagle", 1.6F);
        TRANSLUCENT.add("tornado_spirit");
        MID_BAR_COLOR.put("shadow_apprentice", 0xFFB060FF);
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
