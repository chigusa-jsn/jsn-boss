package com.chigusa.jsnboss.registry;

import com.chigusa.jsnboss.mob.FireSkeleton;
import com.chigusa.jsnboss.mob.FrostBlaze;
import com.chigusa.jsnboss.mob.FrostCreeper;
import com.chigusa.jsnboss.mob.MagmaSpider;
import com.chigusa.jsnboss.mob.PoisonCloudArrow;
import com.chigusa.jsnboss.mob.PoisonSkeleton;
import com.chigusa.jsnboss.mob.SoulBlaze;
import com.chigusa.jsnboss.mob.ThunderZombie;
import com.chigusa.jsnboss.mob.TombZombie;
import com.chigusa.jsnboss.mob.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.chigusa.jsnboss.registry.ModEntities.ENTITY_TYPES;
import static com.chigusa.jsnboss.registry.ModItems.ITEMS;

/** 敵モブの亜種（バニラのモブの色ちがい・技ちがい）。大きさはバニラと同じ */
public final class ModMobs {
    public static final DeferredHolder<EntityType<?>, EntityType<SoulBlaze>> SOUL_BLAZE =
            ENTITY_TYPES.register("soul_blaze", () -> EntityType.Builder.<SoulBlaze>of(SoulBlaze::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).fireImmune().clientTrackingRange(8).build("soul_blaze"));
    public static final DeferredHolder<EntityType<?>, EntityType<FrostBlaze>> FROST_BLAZE =
            ENTITY_TYPES.register("frost_blaze", () -> EntityType.Builder.<FrostBlaze>of(FrostBlaze::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).fireImmune().clientTrackingRange(8).build("frost_blaze"));
    public static final DeferredHolder<EntityType<?>, EntityType<TombZombie>> TOMB_ZOMBIE =
            ENTITY_TYPES.register("tomb_zombie", () -> EntityType.Builder.<TombZombie>of(TombZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).build("tomb_zombie"));
    public static final DeferredHolder<EntityType<?>, EntityType<ThunderZombie>> THUNDER_ZOMBIE =
            ENTITY_TYPES.register("thunder_zombie", () -> EntityType.Builder.<ThunderZombie>of(ThunderZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).build("thunder_zombie"));
    public static final DeferredHolder<EntityType<?>, EntityType<FireSkeleton>> FIRE_SKELETON =
            ENTITY_TYPES.register("fire_skeleton", () -> EntityType.Builder.<FireSkeleton>of(FireSkeleton::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).eyeHeight(1.74F).fireImmune().clientTrackingRange(8).build("fire_skeleton"));
    public static final DeferredHolder<EntityType<?>, EntityType<PoisonSkeleton>> POISON_SKELETON =
            ENTITY_TYPES.register("poison_skeleton", () -> EntityType.Builder.<PoisonSkeleton>of(PoisonSkeleton::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(8).build("poison_skeleton"));
    public static final DeferredHolder<EntityType<?>, EntityType<FrostCreeper>> FROST_CREEPER =
            ENTITY_TYPES.register("frost_creeper", () -> EntityType.Builder.<FrostCreeper>of(FrostCreeper::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).clientTrackingRange(8).build("frost_creeper"));
    public static final DeferredHolder<EntityType<?>, EntityType<MagmaSpider>> MAGMA_SPIDER =
            ENTITY_TYPES.register("magma_spider", () -> EntityType.Builder.<MagmaSpider>of(MagmaSpider::new, MobCategory.MONSTER)
                    .sized(1.4F, 0.9F).eyeHeight(0.65F).fireImmune().clientTrackingRange(8).build("magma_spider"));

    // ---------------------------------------------------------------- 友達の案の亜種
    public static final DeferredHolder<EntityType<?>, EntityType<MossZombie>> MOSS_ZOMBIE =
            ENTITY_TYPES.register("moss_zombie", () -> EntityType.Builder.<MossZombie>of(MossZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).build("moss_zombie"));
    public static final DeferredHolder<EntityType<?>, EntityType<HoneySpider>> HONEY_SPIDER =
            ENTITY_TYPES.register("honey_spider", () -> EntityType.Builder.<HoneySpider>of(HoneySpider::new, MobCategory.MONSTER)
                    .sized(1.4F, 0.9F).eyeHeight(0.65F).clientTrackingRange(8).build("honey_spider"));
    public static final DeferredHolder<EntityType<?>, EntityType<PrismarineDrowned>> PRISMARINE_DROWNED =
            ENTITY_TYPES.register("prismarine_drowned", () -> EntityType.Builder.<PrismarineDrowned>of(PrismarineDrowned::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).build("prismarine_drowned"));
    public static final DeferredHolder<EntityType<?>, EntityType<SkySkeleton>> SKY_SKELETON =
            ENTITY_TYPES.register("sky_skeleton", () -> EntityType.Builder.<SkySkeleton>of(SkySkeleton::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(8).build("sky_skeleton"));
    public static final DeferredHolder<EntityType<?>, EntityType<GoldZombie>> GOLD_ZOMBIE =
            ENTITY_TYPES.register("gold_zombie", () -> EntityType.Builder.<GoldZombie>of(GoldZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).build("gold_zombie"));
    public static final DeferredHolder<EntityType<?>, EntityType<SculkSkeleton>> SCULK_SKELETON =
            ENTITY_TYPES.register("sculk_skeleton", () -> EntityType.Builder.<SculkSkeleton>of(SculkSkeleton::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(8).build("sculk_skeleton"));
    public static final DeferredHolder<EntityType<?>, EntityType<OreZombie>> ORE_ZOMBIE =
            ENTITY_TYPES.register("ore_zombie", () -> EntityType.Builder.<OreZombie>of(OreZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).build("ore_zombie"));
    public static final DeferredHolder<EntityType<?>, EntityType<GlowSlime>> GLOW_SLIME =
            ENTITY_TYPES.register("glow_slime", () -> EntityType.Builder.<GlowSlime>of(GlowSlime::new, MobCategory.MONSTER)
                    .sized(0.52F, 0.52F).eyeHeight(0.325F).spawnDimensionsScale(4.0F).clientTrackingRange(10).build("glow_slime"));

    public static final DeferredHolder<EntityType<?>, EntityType<PoisonCloudArrow>> POISON_CLOUD_ARROW =
            ENTITY_TYPES.register("poison_cloud_arrow", () -> EntityType.Builder.<PoisonCloudArrow>of(PoisonCloudArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build("poison_cloud_arrow"));

    public static final DeferredItem<DeferredSpawnEggItem> SOUL_BLAZE_EGG = egg("soul_blaze", SOUL_BLAZE, 0x1F4F7A, 0x7FD0FF);
    public static final DeferredItem<DeferredSpawnEggItem> FROST_BLAZE_EGG = egg("frost_blaze", FROST_BLAZE, 0xCFEEFF, 0x3AA0FF);
    public static final DeferredItem<DeferredSpawnEggItem> TOMB_ZOMBIE_EGG = egg("tomb_zombie", TOMB_ZOMBIE, 0xD8C8A0, 0xD4A020);
    public static final DeferredItem<DeferredSpawnEggItem> THUNDER_ZOMBIE_EGG = egg("thunder_zombie", THUNDER_ZOMBIE, 0x4F7A8A, 0x8FE8FF);
    public static final DeferredItem<DeferredSpawnEggItem> FIRE_SKELETON_EGG = egg("fire_skeleton", FIRE_SKELETON, 0x2A2420, 0xFF6A1A);
    public static final DeferredItem<DeferredSpawnEggItem> POISON_SKELETON_EGG = egg("poison_skeleton", POISON_SKELETON, 0xB8C8A0, 0x7A3AA0);
    public static final DeferredItem<DeferredSpawnEggItem> FROST_CREEPER_EGG = egg("frost_creeper", FROST_CREEPER, 0xA8D8F0, 0x1A3A6A);
    public static final DeferredItem<DeferredSpawnEggItem> MAGMA_SPIDER_EGG = egg("magma_spider", MAGMA_SPIDER, 0x2A1C18, 0xFF5A0A);

    public static final DeferredItem<DeferredSpawnEggItem> MOSS_ZOMBIE_EGG = egg("moss_zombie", MOSS_ZOMBIE, 0x4A6A2E, 0x8AE05A);
    public static final DeferredItem<DeferredSpawnEggItem> HONEY_SPIDER_EGG = egg("honey_spider", HONEY_SPIDER, 0x4A3218, 0xFFB020);
    public static final DeferredItem<DeferredSpawnEggItem> PRISMARINE_DROWNED_EGG = egg("prismarine_drowned", PRISMARINE_DROWNED, 0x4AA08E, 0xA8FFF0);
    public static final DeferredItem<DeferredSpawnEggItem> SKY_SKELETON_EGG = egg("sky_skeleton", SKY_SKELETON, 0xE8F4FF, 0x8AB8E8);
    public static final DeferredItem<DeferredSpawnEggItem> GOLD_ZOMBIE_EGG = egg("gold_zombie", GOLD_ZOMBIE, 0xE8B830, 0xFFF4A0);
    public static final DeferredItem<DeferredSpawnEggItem> SCULK_SKELETON_EGG = egg("sculk_skeleton", SCULK_SKELETON, 0x1A2A30, 0x2AC8C8);
    public static final DeferredItem<DeferredSpawnEggItem> ORE_ZOMBIE_EGG = egg("ore_zombie", ORE_ZOMBIE, 0x7A7A7A, 0x5AF0E8);
    public static final DeferredItem<DeferredSpawnEggItem> GLOW_SLIME_EGG = egg("glow_slime", GLOW_SLIME, 0xA8E83A, 0xF4FFB0);

    /** 亜種タブに並べる順番 */
    public static final java.util.List<DeferredItem<DeferredSpawnEggItem>> EGGS = java.util.List.of(
            SOUL_BLAZE_EGG, FROST_BLAZE_EGG, TOMB_ZOMBIE_EGG, THUNDER_ZOMBIE_EGG, FIRE_SKELETON_EGG, POISON_SKELETON_EGG,
            FROST_CREEPER_EGG, MAGMA_SPIDER_EGG, MOSS_ZOMBIE_EGG, HONEY_SPIDER_EGG, PRISMARINE_DROWNED_EGG, SKY_SKELETON_EGG,
            GOLD_ZOMBIE_EGG, SCULK_SKELETON_EGG, ORE_ZOMBIE_EGG, GLOW_SLIME_EGG);

    private static DeferredItem<DeferredSpawnEggItem> egg(String name, java.util.function.Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> type,
                                                         int bg, int fg) {
        return ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, bg, fg, new Item.Properties()));
    }

    /** BossSetup から呼んで、このクラスを読み込ませる（ここの登録を有効にする） */
    public static void init() {}

    private ModMobs() {}
}
